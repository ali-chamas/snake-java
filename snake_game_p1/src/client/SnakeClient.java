
package client;

/**
 *
 * @author alichamas
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// Client for the snake game (1 player)
public class SnakeClient {

    public static void main(String[] args) {
        String serverHostname = new String("127.0.0.1");

        try {
            Socket socket = new Socket(serverHostname, 9090);
            System.out.println("Connected to server at " + serverHostname);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // wait for the player to press Enter, then tell the server we are ready
            Scanner keyboard = new Scanner(System.in);
            System.out.println("");
            System.out.println("Press Enter to start the game.");
            keyboard.nextLine();
            out.println("START");

            // start the thread that prints the grid
            RenderThread renderer = new RenderThread(in);
            renderer.start();

            System.out.println("Controls: w=UP s=DOWN a=LEFT d=RIGHT q=QUIT");

            // read keys and send MOVE commands
            while (keyboard.hasNextLine()) {
                String input = keyboard.nextLine().trim().toLowerCase();
                if (input.equals("w")) out.println("MOVE UP");
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

// reads state messages from the server and prints the grid
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
                printGrid(state);
                if (state.contains("STATUS OVER")) {
                    System.out.println("*** GAME OVER *** (press q + Enter to quit)");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Lost connection to server.");
        }
    }

    // parse the state message and print the grid
    private void printGrid(String state) {
        // build an empty grid
        char[][] grid = new char[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = '.';
            }
        }

        // parse the message tokens
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
                if (isHead) grid[xy[0]][xy[1]] = '1';
                else grid[xy[0]][xy[1]] = 'x';
                isHead = false;
            }
        }

        // print a few blank lines to push the old grid up
        for (int i = 0; i < 5; i++) System.out.println();

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println();
        }
        System.out.println("Score: " + score + "  Status: " + status);
        System.out.println("Controls: w=UP s=DOWN a=LEFT d=RIGHT q=QUIT");
    }

    // parse "(5,10)" into the array [5, 10]
    private int[] parseCoord(String s) {
        String inner = s.substring(1, s.length() - 1);
        String[] parts = inner.split(",");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
}

