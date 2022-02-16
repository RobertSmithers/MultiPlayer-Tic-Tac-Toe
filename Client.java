import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;

public class client {
	
    private static boolean playing = true;
    private static String name;
	static final int DEFAULT_PORT = 5000;
	static final String DEFAULT_IP = "127.0.0.1";

	public static void main(String[] args) throws Exception {
		Scanner fromKeyboard = new Scanner(System.in);
		Socket socket = null;
		String stringPort;
		String server,msg="let's keep playing!";
		InetAddress serverAddress;
		int servPort;
		gameBoard board = null;
		int index;
		int userNumber;
		
		System.out.print("Enter computer name or IP address or press return to use default: ");
		server = fromKeyboard.nextLine();
		if (server.length() == 0)
			server = DEFAULT_IP;

		System.out.print("Enter port number or press return to use default:");
		stringPort = fromKeyboard.nextLine();
		if (stringPort.length() == 0)
			servPort = DEFAULT_PORT;
		else
			servPort = Integer.parseInt(stringPort); // convert string to int for port number

		serverAddress = InetAddress.getByName(server);

		try { // check valid port num
			if (servPort <= 0 || servPort > 65535)
				throw new NumberFormatException();
		} catch (NumberFormatException e) {
			System.out.println("Illegal port number 0, " + servPort);
			return;
		}
		
		// data to server, start handshake
		socket = new Socket(serverAddress, servPort);  
		
		TicTacData data=null; 
		System.out.print("Please Enter your name: ");
		name = fromKeyboard.nextLine();

		// send name across to server
		data = new TicTacData();
		data.setName(name);
		data.setMessage(msg);
		sendInfoToServer(socket, data); // first to send name

		// Get the HELO!!
		data = recieveFeedbackFromServer(socket);
		
		// If only player 1 is in... then client = player 1, so 'make a move' as the youngsters say
		if (!data.arePlayersJoined()) {
			// get first msg from server, which is the initial board
			// and the first move request
			data.printMessage();
			board = data.getBoard();
			board.printBoard();

			index = getIndex(board, fromKeyboard, data, true);
			userNumber = getNumber(data, fromKeyboard, true);
			
			data = clientInput(data, true, index, userNumber);
			sendInfoToServer(socket, data);
			data = recieveFeedbackFromServer(socket);
			while (!data.arePlayersJoined()) {
				data.printMessage();
				data = recieveFeedbackFromServer(socket);	// If player 2 didn't join yet, wait for them
			}
			data.printMessage();
			data = recieveFeedbackFromServer(socket);		// 1 more for the board response (made first move, server confirms)
		} else {	// Deal with intro messages for player 2
			data = recieveFeedbackFromServer(socket);
		}
		
		while(playing){
			if (!data.playAgain && data.gameEnd) {
				data.printMessage();
				playing = false;
				break;
			}
			if (!data.playMove || data.gameEnd) {
				board = data.getBoard();
				board.printBoard();
			}
			data.printMessage();
			
			// A thread just for a little custom loading animation. I spent more time on this than I should have.
			Thread loading = new Thread() {
				public void run() {
					while (true) {
						String turn = "Waiting for opponent";

						int NUM_DOTS = 8;
						// Loading dots
						try {
							Thread.sleep(500);
							for (int i=0; i<NUM_DOTS; i++) {
								System.out.print(".");
								Thread.sleep(500);
							}
						} catch (InterruptedException e) {
							// We don't care if it's interrupted, we will just stop it.
							return;
						}
						
						// Delete and write blank over it
						String delete = "", clear = "";
						for (int c=0; c<turn.length()+NUM_DOTS; c++) {
							delete+="\b";
							clear+=" ";
						}
						System.out.print(delete);
						System.out.print(clear);
						System.out.print(delete);
						System.out.print(turn);
					}
				}
			}; 

			while (!data.playMove) {	// Wait until we get the green light to play a move (waiting on other player)
				System.out.print("Waiting for opponent");

				// Will get an IllegalThreadStateException if we try to run the same thread multiple times at once
				if (!loading.isAlive())
					loading.start();
				
				data = recieveFeedbackFromServer(socket);
				System.out.println();
				data.printMessage();
			}
			loading.interrupt();
			
			boolean isOdd = data.playerOdd;  // are we odd or even?

			//check if opponent won, client won or there is a tie
			if (data.player1Won || data.draw || data.player2Won){
				//print out what server said and set new message
				String again = fromKeyboard.nextLine();
				
				if (again.toUpperCase().equals("Y")){
					data.playAgain = true;
					data.setMessage("Let's play again.");
					System.out.println("Waiting for opponent (1/2)");
				} 
				if (again.toUpperCase().equals("N")){
					data.playAgain = false;
					//tell server end
					data.setMessage("LET'S END THE GAME");
					playing = false;
				} 
			} else {
				board = data.getBoard();
				board.printBoard();
				index = getIndex(board, fromKeyboard, data, isOdd);
				userNumber = getNumber(data, fromKeyboard, isOdd);
				data = clientInput(data, isOdd, index, userNumber); //set client guess into appropriate cell
				data.setMessage("I have sent my move");
			}

			sendInfoToServer(socket, data); //either next move or quit play or play again
			//get response
			data = recieveFeedbackFromServer(socket); //next move or new game
		}
        socket.close();
	}
	
	private static int getIndex(gameBoard board, Scanner in, TicTacData data, boolean isOdd) {
		boolean validSlot = false;
		int index = -1;
		while (!validSlot) {
			System.out.println("Choose your two digit index in the form \"##\" from the given list, or type \"help\" to see the indices and \"numbers\" to see your available numbers: ");
			board.printSlots();
			String input = in.nextLine();
			while (input.equals("help") || input.equals("numbers")) {
				while (input.equals("help")) {
					System.out.println("\n---- Board Indices ----\n");
					System.out.println(board.getBoardIndices());
					System.out.println("-----------------------");
					System.out.println("Choose your two digit index in the form \"##\" from the given list, or type \"help\" to see the indices: ");
					board.printSlots();
					input = in.nextLine();
				}

				while (input.equals("numbers")) {
					System.out.println("Valid number(s) you have remaining: ");
					data.printRemain(isOdd);
					System.out.println("-----------------------");
					board.printBoard();
					board.printSlots();
					System.out.println("Choose your two digit index in the form \"##\" from the given list, or type \"help\" to see the indices: ");
					input = in.nextLine();
				}
			}
			index = Integer.parseInt(input);
			index = ((int) (index / 10) - 1) * 10 + index % 10 - 1;
			if (board.validSlot(index))
				validSlot = true;
			else
				System.out.println("Bad cell, try again");
		}
		return index;
	}

	private static int getNumber(TicTacData data, Scanner in, boolean isOdd) {
		boolean validNumber = false;
		int number = -1;
		while (!validNumber) {
			System.out.println("Please choose a number from the given list. Valid number(s) you have remaining: ");
			data.printRemain(isOdd); // player1 is opposite of player2
			number = Integer.parseInt(in.nextLine());
			if (data.validRemain(isOdd, number))
				validNumber = true;
			else
				System.out.println("Bad number, try again");
		}
		return number;
	}
	
	public static void sendInfoToServer(Socket clntSock, TicTacData toSend) throws IOException {
		try {
			OutputStream os = clntSock.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(toSend);

		} catch (EOFException e) { // needed to catch when client is done
			System.out.println("in Send EOFException: goodbye client");
			clntSock.close(); // Close the socket. We are done with this client!
		} catch (IOException e) {
			System.out.println("in Send IOException: goodbye client at");
			clntSock.close(); // this requires the throws IOException
		}
	}

	public static TicTacData recieveFeedbackFromServer(Socket clntSock) throws IOException {
		
		// client object
		TicTacData fromServer = null;

		try {
			InputStream is = clntSock.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			fromServer = (TicTacData) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (EOFException e) { // needed to catch when client is done
			System.out.println("in receive EOF: goodbye  " + name);
			clntSock.close(); // Close the socket. We are done with this client!
		} catch (IOException e) {
			System.out.println("in receive IO: goodbye " + name);
			clntSock.close(); // this requires the throws IOException
		}
		return fromServer;
	}
	public static TicTacData clientInput(TicTacData data, boolean oddeven, int index, int guess){
		data.getBoard().update((int) index / 10, index % 10, guess);
		if (oddeven) {
			data.getOddRemaining().remove(data.getOddRemaining().indexOf(guess));
		} else {
			data.getEvenRemaining().remove(data.getEvenRemaining().indexOf(guess));
		}
		return data;
	}

}
