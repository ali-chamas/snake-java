
package server;

/**
 *
 * @author alichamas
 */
import java.util.LinkedList;
import java.util.Random;

// Holds the game state: snake, food, score, direction
public class GameState {

    public static final int GRID_SIZE = 20;

    private LinkedList<int[]> snake;   // list of [row, col], head at index 0
    private int[] food;
    private String direction;
    private int score;
    private boolean gameOver;
    private Random rand;

    public GameState() {
        rand = new Random();
        snake = new LinkedList<int[]>();
        snake.add(new int[]{10, 10}); // head
        snake.add(new int[]{10, 9});
        snake.add(new int[]{10, 8});
        direction = "RIGHT";
        score = 0;
        gameOver = false;
        placeFood();
    }

    // pick a random cell that is not on the snake
    private void placeFood() {
        while (true) {
            int r = rand.nextInt(GRID_SIZE);
            int c = rand.nextInt(GRID_SIZE);
            boolean onSnake = false;
            for (int i = 0; i < snake.size(); i++) {
                int[] seg = snake.get(i);
                if (seg[0] == r && seg[1] == c) {
                    onSnake = true;
                    break;
                }
            }
            if (!onSnake) {
                food = new int[]{r, c};
                return;
            }
        }
    }

    public synchronized void setDirection(String newDir) {
        // don't allow going backwards into yourself
        if (newDir.equals("UP") && direction.equals("DOWN")) return;
        if (newDir.equals("DOWN") && direction.equals("UP")) return;
        if (newDir.equals("LEFT") && direction.equals("RIGHT")) return;
        if (newDir.equals("RIGHT") && direction.equals("LEFT")) return;
        direction = newDir;
    }

    // move the snake one step
    public synchronized void tick() {
        if (gameOver) return;

        int[] head = snake.getFirst();
        int newRow = head[0];
        int newCol = head[1];

        if (direction.equals("UP")) newRow--;
        if (direction.equals("DOWN")) newRow++;
        if (direction.equals("LEFT")) newCol--;
        if (direction.equals("RIGHT")) newCol++;

        // hit the wall?
        if (newRow < 0 || newRow >= GRID_SIZE || newCol < 0 || newCol >= GRID_SIZE) {
            gameOver = true;
            return;
        }

        // hit itself?
        for (int i = 0; i < snake.size(); i++) {
            int[] seg = snake.get(i);
            if (seg[0] == newRow && seg[1] == newCol) {
                gameOver = true;
                return;
            }
        }

        // add new head
        snake.addFirst(new int[]{newRow, newCol});

        // ate food?
        if (newRow == food[0] && newCol == food[1]) {
            score++;
            placeFood();
        } else {
            snake.removeLast(); // didn't eat, remove tail
        }
    }

    // build the message to send to the client
    public synchronized String getStateString() {
        String s = "SNAKE";
        for (int i = 0; i < snake.size(); i++) {
            int[] seg = snake.get(i);
            s = s + " (" + seg[0] + "," + seg[1] + ")";
        }
        s = s + " FOOD (" + food[0] + "," + food[1] + ")";
        s = s + " SCORE " + score;
        if (gameOver) s = s + " STATUS OVER";
        else s = s + " STATUS RUNNING";
        return s;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }
}