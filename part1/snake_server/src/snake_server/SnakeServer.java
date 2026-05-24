/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snake_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/*****************************************************************/
/***************    SnakeServer (Part 1: single player)    *******/
/*****************************************************************/
/* The server owns the game state and runs the game loop.
   It accepts ONE client (the player) and then:
     - Spawns a game loop thread that ticks every 200 ms and pushes the
       updated state to the client.
     - Uses its main thread to read MOVE commands coming from the client.
*/

public class SnakeServer {

    public static void main(String[] args) {
        /*** Create the shared game state ***/
        GameState game = new GameState();

        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try {
            /*** Create the welcoming socket on port 9090 ***/
            serverSocket = new ServerSocket(9090);
            System.out.println("Server is on, waiting for a player to connect...");

            /*** Block until a client connects ***/
            clientSocket = serverSocket.accept();
            System.out.println("Player connected from: " + clientSocket.getInetAddress());

            /*** Create the input/output streams ***/
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            /*** Wait for the client to confirm the player is ready.
                 This avoids the game running before the player has had time
                 to focus the client window and start typing commands. ***/
            System.out.println("Waiting for player to press Enter to start...");
            String startMsg = in.readLine();
            System.out.println("Received: " + startMsg + " -> game starting!");

            /*** Now start the game loop thread: it sends state every 200ms ***/
            GameLoopThread loop = new GameLoopThread(game, out);
            loop.start();

            /*** Main thread reads commands from the client until disconnect
                 or game over ***/
            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Received: " + command);
                handleCommand(command, game);
                if (game.isGameOver()) break;
            }

            /*** Cleanup ***/
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

    /*** Translate a client command into a game state update ***/
    private static void handleCommand(String command, GameState game) {
        // Commands are "MOVE UP", "MOVE DOWN", "MOVE LEFT", "MOVE RIGHT"
        if (command.startsWith("MOVE ")) {
            String dir = command.substring(5);
            game.setDirection(dir);
        }
    }
}

/*****************************************************************/
/***************         GameLoopThread          *****************/
/*****************************************************************/
/* This thread runs the game tick. Every 200 ms it moves the snake one
   cell and pushes the updated state string to the client.
   It stops when the game is over or when the connection to the client
   has been lost (PrintWriter.checkError() == true). */

class GameLoopThread extends Thread {

    private GameState game;
    private PrintWriter out;

    public GameLoopThread(GameState g, PrintWriter o) {
        game = g;
        out = o;
    }

    public void run() {
        try {
            while (!game.isGameOver() && !out.checkError()) {
                game.tick();                       // move the snake one cell
                out.println(game.getStateString()); // send the new state
                Thread.sleep(600);                 // tick rate: 600 ms per move (slow enough for line-buffered input)
            }
            // Send one last state so the client sees STATUS OVER
            out.println(game.getStateString());
            System.out.println("Game loop ended. Final score: ");
        } catch (InterruptedException e) {
            System.err.println("Game loop interrupted.");
        }
    }
}