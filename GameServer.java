import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
	static final int DEFAULT_PORT = 5000;
	public static void main(String[] args) throws Exception {
		// local data
		int servPort;
		String serverPort = null;
		Scanner fromKeyboard = new Scanner(System.in);
		System.out.print("Enter port, or press return to use default:");

		if ((serverPort = fromKeyboard.nextLine()).length() == 0)
			servPort = DEFAULT_PORT;
		else
			servPort = Integer.parseInt(serverPort);	
		
        ServerSocket listener = null;
        //check to see if listener can be made
        try {
			listener = new ServerSocket(servPort);
		} catch (IOException e) {
			System.err.println("Could not listen on port: "+servPort);
			System.exit(-1);
		}

		List<Socket> list = new CopyOnWriteArrayList<>();
		List<String> names = new CopyOnWriteArrayList<>();
        System.out.println("server waiting...");
        
        try {
            while (true) {
				// Create the game instance
				gameThread game = new gameThread(null, null, list);
				int players = 0;
				while (players < 2) {
					Socket clntSocket = listener.accept();
					list.add(clntSocket);
					waitLobby gameQueue = new waitLobby(clntSocket, list, names, game);
					Thread t = new Thread(gameQueue); 
					// System.out.println("Lobby-" + t.getName()+ " started.");
					t.start();
					players++;
					if (players == 1) {
						Thread r = new Thread(game);
						System.out.println("gameThread (" + r.getName() + ") started.");
						r.start();
					}
				}
            }
        } finally {
            listener.close();
        }
        
	}
	
	public static void printListofClients(List<Socket> list) {
		for(int i = 0; i < list.size(); i++){
			System.out.println(list.get(i));
		}
	}
}
