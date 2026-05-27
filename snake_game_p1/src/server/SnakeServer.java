
package server;

/**
 *
 * @author alichamas
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

// Server for the snake game (1 player)
public class SnakeServer {

    public static void main(String[] args) {
        GameState game = new GameState();
        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try {
            serverSocket = new ServerSocket(9090);
            System.out.println("Server is on, waiting for a player to connect...");

            clientSocket = serverSocket.accept();
            System.out.println("Player connected from: " + clientSocket.getInetAddress());

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // wait for the client to say they are ready
            System.out.println("Waiting for player to press Enter to start...");
            String startMsg = in.readLine();
            System.out.println("Received: " + startMsg + " -> game starting!");

            // start the thread that ticks the game
            GameLoopThread loop = new GameLoopThread(game, out);
            loop.start();

            // read MOVE commands from the client
            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Received: " + command);
                if (command.startsWith("MOVE ")) {
                    String dir = command.substring(5);
                    game.setDirection(dir);
                }
                if (game.isGameOver()) break;
            }

            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
            System.out.println("Server closed.");

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            System.exit(1);
        }
    }
}

// sends the game state to the client every 600 ms
class GameLoopThread extends Thread {

    private GameState game;
    private PrintWriter out;

    public GameLoopThread(GameState g, PrintWriter o) {
        game = g;
        out = o;
    }

    public void run() {
        try {
            while (!game.isGameOver()) {
                game.tick();
                out.println(game.getStateString());
                Thread.sleep(600);
            }
            // send one last state so the client sees GAME OVER
            out.println(game.getStateString());
        } catch (InterruptedException e) {
            System.err.println("Game loop interrupted.");
        }
    }
}
