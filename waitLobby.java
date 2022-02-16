import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class waitLobby implements Runnable {

	Socket socket;
	String name;
	int wins=0, losses=0, draws=0;
	boolean inGame = true;
	List<Socket> list;
	List<String> names;
	gameThread game;

	public boolean playAgain = true;
	public boolean odd = true;

	TicTacData dataObject;

	public waitLobby(Socket clntSocket, List<Socket> list, List<String> names, gameThread game) throws IOException {
		this.socket = clntSocket;
		this.list = list;
		this.names = names;
		this.game = game;
	}

	@Override
	public void run() {
		// find out who the client is.
		SocketAddress clientAddress = socket.getRemoteSocketAddress();
		System.out.printf("Client (%s) added to wait queue\n", clientAddress);
		boolean p2FirstTimeJoin = list.size() % 2 != 0;

		// get name
		try {
			dataObject = recieveGuessFromClient(socket);
			name = dataObject.getName();
			names.add(name);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// loop for multiple games
		try {
			dataObject = new TicTacData(odd);
			dataObject.setMessage("Welcome to Number Tic Tac Toe, " + name + ".");
			if (names.size() %2 != 0) {
				dataObject.setMessage("Welcome to Number Tic Tac Toe, " + name + ".\nYou are the first player here! Go first and wait for player 2.");
				dataObject.playMove = true;
			} else {
				dataObject.playersJoined();
			}
			
			sendFeedbackToClient(socket, dataObject);	// Intro

			if (names.size() % 2 != 0) {
				dataObject.setMessage("Waiting for player 2 to join the game.");
				dataObject.playMove = false;
				sendFeedbackToClient(socket, dataObject);
			}

			while (names.size() % 2 != 0) {
				// Game waiting for player two
			}
			if (p2FirstTimeJoin) {
				p2FirstTimeJoin = false;
				int index = list.indexOf(socket) == 0 ? 1 : 0;
				dataObject.setMessage("Player 2 joined. Now playing with " + names.get(index));
				dataObject.playMove = false;
				dataObject.playersJoined();
				game.setPlayer1(socket);
				game.p1Name = names.get(list.indexOf(socket));
				sendFeedbackToClient(socket, dataObject);
			} else {
				int index = list.indexOf(socket) == 0 ? 1 : 0;
				dataObject.setMessage("You've joined a game. Now playing with " + names.get(index));
				dataObject.playersJoined();
				game.setPlayer2(socket);
				game.p2Name = names.get(list.indexOf(socket));
				sendFeedbackToClient(socket, dataObject);
			}

			// Now that we have both clients "linked", let's stop the thread on the 1st instance and "merge" them into 1 game
			// if (list.indexOf(socket) == 0)
			// 	System.out.exit(0);

			
			//if client said bye, remove client from list of clients
			// if (dataObject.message.equals("exit") || dataObject.message.equals("bye")) {
			// 	list.remove(socket);
			// }

			// if (odd) {// server goes first
			// 	// HERE
			// 	dataObject = recieveGuessFromClient(socket);	// Player 2 initial move
			// 	System.out.println("First game, Player 2 is playing odd numbers");
			// }

			// board = dataObject.getBoard();
			// System.out.println("Starting looks like:");
			// board.printBoard();
			// sendFeedbackToClient(socket, dataObject); // send board to client

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendFeedbackToClient(Socket clntSock, TicTacData toClient) throws IOException {
		try {
			OutputStream os = clntSock.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(toClient);

		} catch (EOFException e) { // needed to catch when client is done
			System.out.println("in Send EOFException: goodbye client at " + clntSock.getRemoteSocketAddress()
					+ " with port# " + clntSock.getPort());
			clntSock.close(); // Close the socket. We are done with this client!
		} catch (IOException e) {
			System.out.println("in Send IOException: goodbye client at " + clntSock.getRemoteSocketAddress()
					+ " with port# " + clntSock.getPort());
			clntSock.close(); // this requires the throws IOException
		}
	}

	public static TicTacData recieveGuessFromClient(Socket clntSock) throws IOException {
		// client transport and network info
		SocketAddress clientAddress = clntSock.getRemoteSocketAddress();
		int port = clntSock.getPort();
		TicTacData fromClient = null;
		try {
			InputStream is = clntSock.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			fromClient = (TicTacData) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (EOFException e) { // needed to catch when client is done
			System.out.println("in receive EOF: goodbye client at " + clientAddress + " with port# " + port);
			clntSock.close(); // Close the socket. We are done with this client!
			// now terminate the thread
		} catch (IOException e) {
			System.out.println("in receive IO: goodbye client at " + clientAddress + " with port# " + port);
			clntSock.close(); // this requires the throws IOException
		}
		return fromClient;
	}

}
