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

public class gameThread implements Runnable {

	// Volatile fields, to be modified by two separate threads.
	volatile Socket player1, player2;
	String p1Name, p2Name;
	int p1Wins = 0, p2Wins = 0, draws = 0;
	boolean inGame = true;
	List<Socket> list;

	public boolean playAgain = true;
	public boolean odd = true;

	TicTacData data;

	public gameThread(Socket player1, Socket player2, List<Socket> list) throws IOException {
		this.player1 = player1;
		this.player2 = player2;
		this.p1Name = "";
		this.p2Name = "";
		this.list = list;
	}

	public gameThread(Socket player1, Socket player2, String player1Name, String player2Name, List<Socket> list,
			boolean odds) throws IOException {
		this.player1 = player1;
		this.player2 = player2;
		this.p1Name = player1Name;
		this.p2Name = player2Name;
		this.list = list;
		this.odd = odds;
	}

	public void setPlayer1(Socket p1) {
		this.player1 = p1;
	}

	public void setPlayer2(Socket p2) {
		this.player2 = p2;
	}

	@Override
	public void run() {
		// Have to synchronize this block because trying to use this while loop to wait
		synchronized(this) {
			while (player1 == null) {
				// Wait for player 1
			}
			// Get first move from player 1
			data = new TicTacData(odd);

		}

		synchronized(this) {
			while (player1 == null || player2 == null) {
				// Wait for players to join
			}
		}

		// find out who the client is.
		SocketAddress clientAddress = player1.getRemoteSocketAddress();
		SocketAddress clientAddress2 = player2.getRemoteSocketAddress();

		System.out.printf("Running game between %s (%s) and %s (%s)\n", p1Name, clientAddress, p2Name, clientAddress2);
		gameBoard board = null;

		// Multiple games
		Socket playersTurn = player1;
		while (playAgain) {
			System.out.println((playersTurn == player1 ? p1Name : p2Name) + " is odd");
			try {
				data = new TicTacData(odd);
				data.playersJoined();

				System.out.println("Waiting for first guess");
				data = recieveGuessFromClient(playersTurn);	// First player initial move
				board = data.getBoard();
				System.out.println("First board looks like:");
				board.printBoard();
				sendFeedbackAndBroadcastMove(data, playersTurn, playersTurn == player1 ? player2 : player1, odd, player1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			inGame = true;

			playersTurn = playersTurn == player1 ? player2 : player1;
			// loop for single game
			while (true) {
				try {
					System.out.println("Waiting for client response from " + (playersTurn == player1 ? p1Name : p2Name));
					data = recieveGuessFromClient(playersTurn); // got client response			
					System.out.println("From " + (playersTurn == player1 ? p1Name : p2Name) + ": "+ data.getMessage()); // client should send new message
					board = data.getBoard();
					board.printBoard();

					//if client said bye, remove client from list of clients
					if (data.message.equals("exit") || data.message.equals("bye")) {
						list.remove(playersTurn);
					}

					//based on response from above receive
					if (!inGame && data.playAgain) { // game over & wants to play again
						System.out.println((playersTurn == player1 ? p1Name : p2Name) + " wants to play again... waiting on " + (playersTurn == player1 ? p2Name : p1Name));
						data = recieveGuessFromClient((playersTurn == player1 ? player2 : player1));
						if (data.playAgain) {	// Both want to play again
							System.out.printf("Both players want to play again. %s is odd\n", odd ? p2Name : p1Name);
							playersTurn = odd ? player2 : player1;	// If p1 was just odds, now p2 will start as odds
							odd = !odd;
							data = new TicTacData(odd);
							data.setMessage("Let's start the new game!");
							data.playAgain = true;
							data.playMove = true;
							data.playerOdd = playersTurn == player1 ? odd : !odd;
							sendFeedbackToClient(playersTurn, data);
							data.playMove = false;
							data.playerOdd = playersTurn == player1 ? !odd : odd;
							sendFeedbackToClient(playersTurn == player1 ? player2 : player1, data);
							break;
						} else {				// Only 1 player wants to play again
							System.out.println("1 player said no to play again. Broadcasting to players and then quitting");
							data.setMessage((playersTurn == player1 ? p2Name : p1Name) + " does not want to play again. Thanks for playing!");
							data.playAgain = false;
							data.gameEnd = true;
							sendFeedbackToClient(player1, data);
							sendFeedbackToClient(player2, data);
							playAgain = false;
							break;
						}
					} else {
						if (data.playAgain == false) {
							System.out.println((playersTurn == player1 ? p1Name : p2Name) + " does not want to play again.");
							data.setMessage((playersTurn == player1 ? p1Name : p2Name) + " does not want to play again. Thanks for playing!");
							sendFeedbackToClient(player1, data);
							sendFeedbackToClient(player2, data);
							playAgain = false;
							break;
						}
					}
					System.out.println("Server: keep playing.");
					// did player move result in win or draw?
					if (board.checkWin()) {
						String winner = "RJ";
						if (playersTurn == player1) {
							data.player1Won = true;
							p1Wins++;
							winner = p1Name;
						} else {
							data.player2Won = true;
							p2Wins++;
							winner = p2Name;
						}
						inGame = false;
						data.gameEnd = true;

						data.setMessage(winner + " WON! Your record so far: W-L-T = " + p1Wins + "-" + p2Wins + "-" + draws +  " Do you want to play again? enter y or n");
						sendFeedbackToClient(player1, data);
						data.setMessage(winner + " WON! Your record so far: W-L-T = " + p2Wins + "-" + p1Wins + "-" + draws +  " Do you want to play again? enter y or n");
						sendFeedbackToClient(player2, data);
						System.out.println(winner + " won");
					} else if (board.boardFull()) {
						// draw
						data.draw = true;
						data.gameEnd = true;
						inGame = false;
						draws++;
						data.setMessage("Draw! Your record so far: W-L-T = " + p1Wins + "-" + p2Wins + "-" + draws +  " Do you want to play again? enter y or n");
						sendFeedbackToClient(player1, data);
						data.setMessage("Draw! Your record so far: W-L-T = " + p1Wins + "-" + p2Wins + "-" + draws +  " Do you want to play again? enter y or n");
						sendFeedbackToClient(player2, data);
						System.out.println("Game DRAW");
					} else {	// Other player's turn
						board = data.getBoard();
						if (board.checkWin()) { 		// Check if other player won
							String winner = "RJ ;)";	// This String will get overwritten
							if (playersTurn == player1) {
								data.player1Won = true;
								p1Wins++;
								winner = p1Name;
							} else {
								data.player2Won = true;
								p2Wins++;
								winner = p2Name;
							}
							if (playersTurn == player1) {
								data.player1Won = true;
								p1Wins++;
							} else {
								data.player2Won = true;
								p2Wins++;
							}
							data.gameEnd = true;
							inGame = false;
							
							data.setMessage(winner + " WON! Your record so far: W-L-T = " + p1Wins + "-" + p2Wins + "-" + draws +  " Do you want to play again? enter y or n");
							sendFeedbackToClient(player1, data);
							data.setMessage(winner + " WON! Your record so far: W-L-T = " + p2Wins + "-" + p1Wins + "-" + draws +  " Do you want to play again? enter y or n");
							sendFeedbackToClient(player2, data);
							System.out.println(winner + " won");
						} else if (board.boardFull()) // check for draw with new move
						{
							data.draw = true;
							data.gameEnd = true;
							inGame = false;
							draws++;
							data.setMessage("Draw! Your record so far: wins=" + p1Wins + " losses=" + p2Wins + " draws=" + draws + " Do you want to play again? enter y or n");
							sendFeedbackToClient(player1, data);
							data.setMessage("Draw! Your record so far: wins=" + p2Wins + " losses=" + p1Wins + " draws=" + draws + " Do you want to play again? enter y or n");
							sendFeedbackToClient(player2, data);
							System.out.println("Server DRAW");
						} else { // Neither win nor draw, so keep playing
							data.setMessage("let's play some more");
							System.out.println("No one Won AND no DRAW");
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				// After a move is played, respond
				try {
					if (!data.gameEnd)
						sendFeedbackAndBroadcastMove(data, playersTurn, playersTurn == player1 ? player2 : player1, odd, player1);	// Sends confirmation to player 2, sends "your move" to player 1
					playersTurn = playersTurn == player1 ? player2 : player1;	// Switch who is guessing/receiving
				} catch (IOException e) {
					e.printStackTrace();
				}
			}	// 1 game

		}	// repeat games
		System.out.println("Player " + p1Name + " is gone. This thread will now terminate.");
		try {
			player1.close();
			player2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendFeedbackAndBroadcastMove(TicTacData data, Socket feedbackPlayer, Socket broadcastPlayer, boolean odds, Socket player1) throws IOException { 
		data.playMove = false;
		data.setMessage("Your move was sent");
		System.out.println("Sending move");
		sendFeedbackToClient(feedbackPlayer, data);
		data.playMove = true;
		data.setMessage("Your turn");
		// Player can play odds/evens depending on whose turn it is
		if (broadcastPlayer == player1) data.playerOdd = odds;
		else 							data.playerOdd = !odds;

		System.out.println("Broadcasting with odds = " + data.playerOdd);
		sendFeedbackToClient(broadcastPlayer, data);
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
