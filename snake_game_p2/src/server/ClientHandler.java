
package server;

/**
 *
 * @author alichamas
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// one thread per connected player
public class ClientHandler extends Thread {

    private Socket clientSocket;
    private GameState game;
    private String playerId;          // "P1" or "P2"
    private BufferedReader in;
    public PrintWriter out;
    public boolean ready;

    public ClientHandler(Socket socket, GameState game, String playerId) {
        this.clientSocket = socket;
        this.game = game;
        this.playerId = playerId;
        this.ready = false;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            // tell the client which player it is
            out.println("WELCOME " + playerId);
        } catch (IOException e) {
            System.err.println("Handler error: " + e.getMessage());
        }
        start();
    }

    public void run() {
        System.out.println("Handler for " + playerId + " started");
        try {
            String command;
            while ((command = in.readLine()) != null) {
                if (command.equals("START")) {
                    ready = true;
                    System.out.println(playerId + " is ready");
                } else if (command.startsWith("MOVE ")) {
                    String dir = command.substring(5);
                    game.setDirection(playerId, dir);
                    System.out.println("Received from " + playerId + ": " + command);
                }
                if (game.isGameOver()) break;
            }
        } catch (IOException e) {
            System.err.println(playerId + " disconnected");
        }
        System.out.println("Handler for " + playerId + " ending");
    }
}

