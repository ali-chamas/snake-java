package snake_client_part2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/*****************************************************************/
/***************            SnakeClient           ****************/
/*****************************************************************/
/* Two-player client. On connect, the server tells us whether we are
   P1 or P2 via a WELCOME message. Then we wait for the player to
   press Enter, send START, and begin rendering. */

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

            /*** First message from the server tells us our player id.
                 Format: "WELCOME P1" or "WELCOME P2" ***/
            String welcome = in.readLine();
            System.out.println("Server says: " + welcome);
            String playerId = welcome.split(" ")[1];

            /*** Wait for the player to be ready ***/
            Scanner keyboard = new Scanner(System.in);
            System.out.println("");
            System.out.println("==============================================");
            System.out.println("  You are " + playerId + ".");
            System.out.println("  Click inside THIS window, then press Enter.");
            System.out.println("  (Game starts once BOTH players are ready.)");
            System.out.println("==============================================");
            keyboard.nextLine();
            out.println("START");

            /*** Start the render thread ***/
            RenderThread renderer = new RenderThread(in, playerId);
            renderer.start();

            System.out.println("Controls: w=UP s=DOWN a=LEFT d=RIGHT q=QUIT");

            /*** Main loop reads keys and sends MOVE commands ***/
            while (keyboard.hasNextLine()) {
                String input = keyboard.nextLine().trim().toLowerCase();
                if (input.equals("w"))      out.println("MOVE UP");
                else if (input.equals("s")) out.println("MOVE DOWN");
                else if (input.equals("a")) out.println("MOVE LEFT");
                else if (input.equals("d")) out.println("MOVE RIGHT");
                else if (input.equals("q")) break;
            }

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
   Each frame shows both snakes:
       P1: head = '1', body = 'x'
       P2: head = '2', body = 'o'
       Food = 'F' */

class RenderThread extends Thread {

    private BufferedReader in;
    private String myPlayerId;
    private static final int GRID_SIZE = 20;

    public RenderThread(BufferedReader r, String myId) {
        in = r;
        myPlayerId = myId;
    }

    public void run() {
        try {
            String state;
            while ((state = in.readLine()) != null) {
                renderState(state);
                if (state.contains("STATUS OVER")) {
                    showWinner(state);
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Lost connection to server.");
        }
    }

    /*** Print the game-over message from this player's point of view ***/
    private void showWinner(String state) {
        String[] tokens = state.split(" ");
        String winner = "";
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equals("WINNER")) {
                winner = tokens[i + 1];
                break;
            }
        }
        System.out.println();
        if (winner.equals("DRAW")) {
            System.out.println("*** GAME OVER - DRAW! ***");
        } else if (winner.equals(myPlayerId)) {
            System.out.println("*** GAME OVER - YOU WIN! ***");
        } else {
            System.out.println("*** GAME OVER - " + winner + " WINS, you lose! ***");
        }
        System.out.println("(press q + Enter to quit)");
    }

    /*** Parse a state line and print the grid ***/
    private void renderState(String state) {
        // 1. Empty grid
        char[][] grid = new char[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = '.';
            }
        }

        // 2. Parse the tokens
        // Format: SNAKE P1 (r,c)... SNAKE P2 (r,c)... FOOD (r,c) SCORE P1=n P2=n STATUS ...
        String[] tokens = state.split(" ");
        int p1Score = 0, p2Score = 0;
        String status = "";
        String currentSnake = "";
        boolean isHead = false;

        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.equals("SNAKE")) {
                i++;
                currentSnake = tokens[i];
                isHead = true;
            } else if (t.equals("FOOD")) {
                currentSnake = "";
                i++;
                int[] xy = parseCoord(tokens[i]);
                grid[xy[0]][xy[1]] = 'F';
            } else if (t.equals("SCORE")) {
                currentSnake = "";
                i++;
                p1Score = Integer.parseInt(tokens[i].split("=")[1]);
                i++;
                p2Score = Integer.parseInt(tokens[i].split("=")[1]);
            } else if (t.equals("STATUS")) {
                currentSnake = "";
                i++;
                status = tokens[i];
            } else if (!currentSnake.equals("") && t.startsWith("(")) {
                int[] xy = parseCoord(t);
                if (currentSnake.equals("P1")) {
                    grid[xy[0]][xy[1]] = isHead ? '1' : 'x';
                } else {
                    grid[xy[0]][xy[1]] = isHead ? '2' : 'o';
                }
                isHead = false;
            }
        }

        // 3. Clear screen and print
        System.out.print("\033[H\033[2J");
        System.out.flush();

        System.out.println("You are " + myPlayerId);
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println();
        }
        System.out.println("Legend: 1=P1 head, x=P1 body, 2=P2 head, o=P2 body, F=food");
        System.out.println("Score:  P1=" + p1Score + "   P2=" + p2Score + "    Status: " + status);
        System.out.println("Controls: w=UP s=DOWN a=LEFT d=RIGHT q=QUIT");
    }

    private int[] parseCoord(String s) {
        String inner = s.substring(1, s.length() - 1);
        String[] parts = inner.split(",");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
}