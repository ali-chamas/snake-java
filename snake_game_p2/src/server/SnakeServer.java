
package server;

/**
 *
 * @author alichamas
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// server that accepts 2 players
public class SnakeServer {

    public static void main(String[] args) {
        GameState game = new GameState();

        try {
            ServerSocket serverSocket = new ServerSocket(9090);
            System.out.println("Server is on, waiting for 2 players to connect...");

            // accept player 1
            Socket sock1 = serverSocket.accept();
            System.out.println("P1 connected from " + sock1.getInetAddress());
            ClientHandler h1 = new ClientHandler(sock1, game, "P1");

            // accept player 2
            Socket sock2 = serverSocket.accept();
            System.out.println("P2 connected from " + sock2.getInetAddress());
            ClientHandler h2 = new ClientHandler(sock2, game, "P2");

            // wait until both players have pressed Enter
            System.out.println("Waiting for both players to press Enter...");
            while (!h1.ready || !h2.ready) {
                Thread.sleep(100);
            }
            System.out.println("Both ready - game starting!");

            // start the game loop
            GameLoopThread loop = new GameLoopThread(game, h1, h2);
            loop.start();

            // wait until the game is over
            while (!game.isGameOver()) {
                Thread.sleep(100);
            }

            // wait a bit so the last state reaches the clients
            Thread.sleep(500);

            sock1.close();
            sock2.close();
            serverSocket.close();
            System.out.println("Server closed.");

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("Server interrupted.");
        }
    }
}

// sends the game state to both players every 600 ms
class GameLoopThread extends Thread {

    private GameState game;
    private ClientHandler h1;
    private ClientHandler h2;

    public GameLoopThread(GameState g, ClientHandler h1, ClientHandler h2) {
        this.game = g;
        this.h1 = h1;
        this.h2 = h2;
    }

    public void run() {
        try {
            while (!game.isGameOver()) {
                game.tick();
                String state = game.getStateString();
                h1.out.println(state);
                h2.out.println(state);
                Thread.sleep(600);
            }
            // send final state with the winner
            String finalState = game.getStateString();
            h1.out.println(finalState);
            h2.out.println(finalState);
            System.out.println("Game loop ended.");
        } catch (InterruptedException e) {
            System.err.println("Game loop interrupted.");
        }
    }
}
