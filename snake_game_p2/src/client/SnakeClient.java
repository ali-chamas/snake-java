
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

// client for the 2-player snake game
public class SnakeClient {

    public static void main(String[] args) {
        String serverHostname = new String("127.0.0.1");

        try {
            Socket socket = new Socket(serverHostname, 9090);
            System.out.println("Connected to server at " + serverHostname);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // server tells us if we are P1 or P2
            String welcome = in.readLine();
            System.out.println("Server says: " + welcome);
            String playerId = welcome.split(" ")[1];

            // wait for player to press Enter, then tell the server we are ready
            Scanner keyboard = new Scanner(System.in);
            System.out.println("");
            System.out.println("You are " + playerId);
            System.out.println("Press Enter when you are ready.");
            keyboard.nextLine();
            out.println("START");

            // start the thread that prints the grid
            RenderThread renderer = new RenderThread(in, playerId);
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

// reads state from the server and prints the grid
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
                printGrid(state);
                if (state.contains("STATUS OVER")) {
                    showWinner(state);
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Lost connection to server.");
        }
    }

    // show who won
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
    }

    // parse the state message and print the grid
    private void printGrid(String state) {
        // empty grid
        char[][] grid = new char[GRID_SIZE][GRID_SIZE];
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = '.';
            }
        }

        // parse the message tokens
        String[] tokens = state.split(" ");
        int p1Score = 0;
        int p2Score = 0;
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
                    if (isHead) grid[xy[0]][xy[1]] = '1';
                    else grid[xy[0]][xy[1]] = 'x';
                } else {
                    if (isHead) grid[xy[0]][xy[1]] = '2';
                    else grid[xy[0]][xy[1]] = 'o';
                }
                isHead = false;
            }
        }

        // print blank lines to push old grid up
        for (int i = 0; i < 5; i++) System.out.println();

        System.out.println("You are " + myPlayerId);
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                System.out.print(grid[r][c] + " ");
            }
            System.out.println();
        }
        System.out.println("Legend: 1=P1 head, x=P1 body, 2=P2 head, o=P2 body, F=food");
        System.out.println("Score: P1=" + p1Score + "  P2=" + p2Score + "  Status: " + status);
        System.out.println("Controls: w=UP s=DOWN a=LEFT d=RIGHT q=QUIT");
    }

    // parse "(5,10)" into [5, 10]
    private int[] parseCoord(String s) {
        String inner = s.substring(1, s.length() - 1);
        String[] parts = inner.split(",");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
}
