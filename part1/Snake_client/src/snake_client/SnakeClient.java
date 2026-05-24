/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package snake_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/*****************************************************************/
/***************            SnakeClient           ****************/
/*****************************************************************/
/* The client connects to the server, sends MOVE commands typed by the
   user, and displays the game grid received from the server.
   Two threads are needed:
     - the MAIN thread reads keystrokes (w/a/s/d/q) from the keyboard
     - the RENDER thread reads state messages from the server and prints
       the grid
*/

public class SnakeClient {

    public static void main(String[] args) {
        String serverHostname = new String("127.0.0.1");
        Socket socket = null;

        try {
            /*** Connect to the server ***/
            socket = new Socket(serverHostname, 9090);
            System.out.println("Connected to server at " + serverHostname);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            /*** Wait for the player to be ready BEFORE starting the render
                 thread, so the screen does not start scrolling away the prompt. ***/
            Scanner keyboard = new Scanner(System.in);
            System.out.println("");
            System.out.println("==============================================");
            System.out.println("  Click inside THIS output window to focus it,");
            System.out.println("  then press Enter to start the game.");
            System.out.println("==============================================");
            keyboard.nextLine();         // wait for Enter
            out.println("START");        // tell the server we are ready

            /*** Now start the render thread: it reads state updates and prints the grid ***/
            RenderThread renderer = new RenderThread(in);
            renderer.start();

            /*** Main thread reads keystrokes and sends MOVE commands ***/
            System.out.println("Controls: w=UP, s=DOWN, a=LEFT, d=RIGHT, q=QUIT");
            System.out.println("(Type the letter and press Enter)");

            while (keyboard.hasNextLine()) {
                String input = keyboard.nextLine().trim().toLowerCase();
                if (input.equals("w"))      out.println("MOVE UP");
                else if (input.equals("s")) out.println("MOVE DOWN");
                else if (input.equals("a")) out.println("MOVE LEFT");
                else if (input.equals("d")) out.println("MOVE RIGHT");
                else if (input.equals("q")) break;
            }

            /*** Cleanup ***/
            in.close();
            out.close();
            socket.close();
            keyboard.close();

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}

/*****************************************************************/
/***************           RenderThread           ****************/
/*****************************************************************/
/* Reads state messages from the server and prints the grid.
   Format expected: SNAKE (r,c) (r,c) ... FOOD (r,c) SCORE n STATUS s */

class RenderThread extends Thread {

    private BufferedReader in;
    private static final int GRID_SIZE = 20;

    public RenderThread(BufferedReader r) {
        in = r;
    }

    public void run() {
        try {
            String state;
            while ((state = in.readLine()) != null) {
                renderState(state);
                if (state.contains("STATUS OVER")) {
                    System.out.println("\n*** GAME OVER ***  (press q + Enter to quit)");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Lost connection to server.");
        }
    }

    /*** Parse a state line and print the grid ***/
    private void renderState(String state) {
        // 1. Build an empty grid full of '.'
        char[][] grid = new char[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = '.';
            }
        }

        // 2. Parse the tokens
        String[] tokens = state.split(" ");
        int score = 0;
        String status = "";
        boolean parsingSnake = false;
        boolean isHead = true;

        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.equals("SNAKE")) {
                parsingSnake = true;
                isHead = true;
            } else if (t.equals("FOOD")) {
                parsingSnake = false;
                i++;
                int[] xy = parseCoord(tokens[i]);
                grid[xy[0]][xy[1]] = 'F';
            } else if (t.equals("SCORE")) {
                i++;
                score = Integer.parseInt(tokens[i]);
            } else if (t.equals("STATUS")) {
                i++;
                status = tokens[i];
            } else if (parsingSnake && t.startsWith("(")) {
                int[] xy = parseCoord(t);
                grid[xy[0]][xy[1]] = isHead ? '1' : 'x';
                isHead = false;
            }
        }

        // 3. Clear the screen and print the grid (ANSI escape codes)
        System.out.print("\033[H\033[2J");
        System.out.flush();

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println();
        }
        System.out.println("Score: " + score + "    Status: " + status);
        System.out.println("Controls: w=UP s=DOWN a=LEFT d=RIGHT q=QUIT");
    }

    /*** Parse a coordinate like "(5,10)" into the int array [5, 10] ***/
    private int[] parseCoord(String s) {
        // Strip the parentheses, then split on the comma
        String inner = s.substring(1, s.length() - 1);
        String[] parts = inner.split(",");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
}