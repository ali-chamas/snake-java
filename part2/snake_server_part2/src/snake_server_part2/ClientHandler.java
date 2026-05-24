package snake_server_part2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*****************************************************************/
/***************          ClientHandler         ******************/
/*****************************************************************/
/* One thread per connected player. Same pattern as the doctor's
   threads exercise (Th_server): the class extends Thread, the
   constructor stores the socket and calls start(), and the
   per-client work happens inside run().

   Reads commands from the client (START, MOVE UP, etc.)
   and updates the shared GameState. */

public class ClientHandler extends Thread {

    private Socket clientSocket;
    private GameState game;
    private String playerId;          // "P1" or "P2"

    private BufferedReader in;
    public  PrintWriter out;          // public so the server can broadcast to it
    public  boolean ready;            // becomes true after the client sends START

    public ClientHandler(Socket socket, GameState game, String playerId) {
        this.clientSocket = socket;
        this.game = game;
        this.playerId = playerId;
        this.ready = false;
        try {
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            /*** Tell the client which player it is, so its UI can label itself ***/
            out.println("WELCOME " + playerId);
        } catch (IOException e) {
            System.err.println("Handler init error: " + e.getMessage());
        }
        start();   // launch the thread (same idiom as the doctor's Th_server)
    }

    public void run() {
        System.out.println("Handler for " + playerId + " started");
        try {
            String command;
            while ((command = in.readLine()) != null) {
                if (command.equals("START")) {
                    ready = true;
                    System.out.println(playerId + " is ready to play");
                } else if (command.startsWith("MOVE ")) {
                    String dir = command.substring(5);
                    game.setDirection(playerId, dir);
                    System.out.println("Received from " + playerId + ": " + command);
                }
                if (game.isGameOver()) break;
            }
        } catch (IOException e) {
            System.err.println(playerId + " disconnected: " + e.getMessage());
        }
        System.out.println("Handler for " + playerId + " ending");
    }
}