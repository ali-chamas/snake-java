package snake_server_part2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*****************************************************************/
/***************    SnakeServer (Part 2: 2 players)     **********/
/*****************************************************************/
/* The server now accepts TWO clients. For each one it spawns a
   ClientHandler thread (the doctor's threads-exercise pattern).
   Once both players have sent START, it launches the game loop
   thread which ticks the world and broadcasts state to both. */

public class SnakeServer {

    public static void main(String[] args) {
        GameState game = new GameState();

        try {
            /*** Create the welcoming socket on port 9090 ***/
            ServerSocket serverSocket = new ServerSocket(9090);
            System.out.println("Server is on, waiting for 2 players to connect...");

            /*** Accept the first client and label it P1 ***/
            Socket sock1 = serverSocket.accept();
            System.out.println("P1 connected from " + sock1.getInetAddress());
            ClientHandler h1 = new ClientHandler(sock1, game, "P1");

            /*** Accept the second client and label it P2 ***/
            Socket sock2 = serverSocket.accept();
            System.out.println("P2 connected from " + sock2.getInetAddress());
            ClientHandler h2 = new ClientHandler(sock2, game, "P2");

            /*** Wait until BOTH players have sent START.
                 Each handler flips its ready flag in its run loop. ***/
            System.out.println("Waiting for both players to press Enter...");
            while (!h1.ready || !h2.ready) {
                Thread.sleep(100);
            }
            System.out.println("Both ready - game starting!");

            /*** Launch the game loop ***/
            GameLoopThread loop = new GameLoopThread(game, h1, h2);
            loop.start();

            /*** Wait for the game loop to finish before closing ***/
            loop.join();

            /*** Cleanup ***/
            sock1.close();
            sock2.close();
            serverSocket.close();
            System.out.println("Server closed.");

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("Server interrupted: " + e.getMessage());
        }
    }
}

/*****************************************************************/
/***************         GameLoopThread          *****************/
/*****************************************************************/
/* Ticks the game every 600 ms and broadcasts the state string to
   BOTH client handlers. Stops when the game is over. */

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
            /*** Send one last state so both clients see STATUS OVER + winner ***/
            String finalState = game.getStateString();
            h1.out.println(finalState);
            h2.out.println(finalState);
            System.out.println("Game loop ended.");
        } catch (InterruptedException e) {
            System.err.println("Game loop interrupted.");
        }
    }
}