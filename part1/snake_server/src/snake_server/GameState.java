package snake_server;

import java.util.LinkedList;
import java.util.Random;

/*****************************************************************/
/***************            GameState           ******************/
/*****************************************************************/
/* This class holds the state of the game: snake position, food position,
   direction, score, and game-over flag. All methods that modify or read
   the state are synchronized because they are called from two different
   threads on the server: the game loop thread and the thread that reads
   client commands. */

public class GameState {

    /*** Grid dimensions (20x20 as in the project description) ***/
    public static final int GRID_SIZE = 20;

    /*** Snake body: each int[] is a [row, col] cell. Head is at index 0. ***/
    private LinkedList<int[]> snake;

    /*** Food position [row, col] ***/
    private int[] food;

    /*** Direction requested by the player ("UP", "DOWN", "LEFT", "RIGHT") ***/
    private String direction;

    /*** The direction that was actually used on the last tick. We keep this
         separately to avoid 180-degree reversals when the player presses two
         keys between ticks. ***/
    private String lastTickDirection;

    /*** Number of food items eaten ***/
    private int score;

    /*** True once the snake has crashed ***/
    private boolean gameOver;

    /*** Used to place food randomly ***/
    private Random rand;

    /*** Constructor: place the snake at the center going right, place first food ***/
    public GameState() {
        rand = new Random();
        snake = new LinkedList<int[]>();
        // Initial snake: 3 segments in the middle of the grid
        snake.add(new int[]{10, 10});   // head
        snake.add(new int[]{10, 9});
        snake.add(new int[]{10, 8});    // tail
        direction = "RIGHT";
        lastTickDirection = "RIGHT";
        score = 0;
        gameOver = false;
        placeFood();
    }

    /*** Place a new food at a random empty cell ***/
    private void placeFood() {
        while (true) {
            int r = rand.nextInt(GRID_SIZE);
            int c = rand.nextInt(GRID_SIZE);
            // Make sure the food does not land on the snake body
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

    /*** Update the direction, but ignore 180-degree reversals ***/
    public synchronized void setDirection(String newDir) {
        // Reject reversals against the LAST APPLIED direction. This avoids the
        // classic Snake bug where pressing two keys quickly (e.g. UP then LEFT
        // while moving RIGHT) lets the snake reverse into itself.
        if (newDir.equals("UP")    && lastTickDirection.equals("DOWN"))  return;
        if (newDir.equals("DOWN")  && lastTickDirection.equals("UP"))    return;
        if (newDir.equals("LEFT")  && lastTickDirection.equals("RIGHT")) return;
        if (newDir.equals("RIGHT") && lastTickDirection.equals("LEFT"))  return;
        direction = newDir;
    }

    /*** Advance the snake one cell in the current direction.
         Checks for wall, self, and food collisions. ***/
    public synchronized void tick() {
        if (gameOver) return;

        // Compute the new head position
        int[] head = snake.getFirst();
        int newRow = head[0];
        int newCol = head[1];

        if (direction.equals("UP"))    newRow--;
        if (direction.equals("DOWN"))  newRow++;
        if (direction.equals("LEFT"))  newCol--;
        if (direction.equals("RIGHT")) newCol++;

        // 1. Wall collision
        if (newRow < 0 || newRow >= GRID_SIZE || newCol < 0 || newCol >= GRID_SIZE) {
            gameOver = true;
            return;
        }

        // 2. Self collision
        for (int i = 0; i < snake.size(); i++) {
            int[] seg = snake.get(i);
            if (seg[0] == newRow && seg[1] == newCol) {
                gameOver = true;
                return;
            }
        }

        // 3. Move: add the new head
        snake.addFirst(new int[]{newRow, newCol});

        // 4. Food collision: grow (don't remove the tail) and spawn new food
        if (newRow == food[0] && newCol == food[1]) {
            score++;
            placeFood();
        } else {
            // No food eaten: remove the tail so the snake keeps its length
            snake.removeLast();
        }

        // Remember the direction we just used
        lastTickDirection = direction;
    }

    /*** Build the state message that gets sent to the client.
         Format: SNAKE (r,c) (r,c) ... FOOD (r,c) SCORE n STATUS RUNNING|OVER ***/
    public synchronized String getStateString() {
        String s = "SNAKE";
        for (int i = 0; i < snake.size(); i++) {
            int[] seg = snake.get(i);
            s = s + " (" + seg[0] + "," + seg[1] + ")";
        }
        s = s + " FOOD (" + food[0] + "," + food[1] + ")";
        s = s + " SCORE " + score;
        s = s + " STATUS " + (gameOver ? "OVER" : "RUNNING");
        return s;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }
}
