
package server;

/**
 *
 * @author alichamas
 */
import java.util.Random;

// game state for 2 players
public class GameState {

    public static final int GRID_SIZE = 20;

    private Snake p1;
    private Snake p2;
    private int[] food;
    private boolean gameOver;
    private String winner;
    private Random rand;

    public GameState() {
        rand = new Random();
        p1 = new Snake("P1", 10, 4, "RIGHT");
        p2 = new Snake("P2", 10, 16, "LEFT");
        gameOver = false;
        winner = "";
        placeFood();
    }

    private void placeFood() {
        while (true) {
            int r = rand.nextInt(GRID_SIZE);
            int c = rand.nextInt(GRID_SIZE);
            if (!onSnake(p1, r, c) && !onSnake(p2, r, c)) {
                food = new int[]{r, c};
                return;
            }
        }
    }

    // is cell (r,c) on the body of snake s?
    private boolean onSnake(Snake s, int r, int c) {
        for (int i = 0; i < s.body.size(); i++) {
            int[] seg = s.body.get(i);
            if (seg[0] == r && seg[1] == c) return true;
        }
        return false;
    }

    public synchronized void setDirection(String playerId, String newDir) {
        Snake s;
        if (playerId.equals("P1")) s = p1;
        else s = p2;

        if (s.dead) return;

        // don't allow going backwards
        if (newDir.equals("UP") && s.direction.equals("DOWN")) return;
        if (newDir.equals("DOWN") && s.direction.equals("UP")) return;
        if (newDir.equals("LEFT") && s.direction.equals("RIGHT")) return;
        if (newDir.equals("RIGHT") && s.direction.equals("LEFT")) return;
        s.direction = newDir;
    }

    public synchronized void tick() {
        if (gameOver) return;

        moveSnake(p1);
        moveSnake(p2);

        // check if a snake hit the other snake
        if (!p1.dead) {
            int[] h = p1.body.getFirst();
            if (onSnake(p2, h[0], h[1])) p1.dead = true;
        }
        if (!p2.dead) {
            int[] h = p2.body.getFirst();
            if (onSnake(p1, h[0], h[1])) p2.dead = true;
        }

        // check game over and decide the winner
        if (p1.dead && p2.dead) {
            gameOver = true;
            winner = "DRAW";
        } else if (p1.dead) {
            gameOver = true;
            winner = "P2";
        } else if (p2.dead) {
            gameOver = true;
            winner = "P1";
        }
    }

    // move one snake by one cell
    private void moveSnake(Snake s) {
        if (s.dead) return;

        int[] head = s.body.getFirst();
        int newRow = head[0];
        int newCol = head[1];
        if (s.direction.equals("UP")) newRow--;
        if (s.direction.equals("DOWN")) newRow++;
        if (s.direction.equals("LEFT")) newCol--;
        if (s.direction.equals("RIGHT")) newCol++;

        // wall?
        if (newRow < 0 || newRow >= GRID_SIZE || newCol < 0 || newCol >= GRID_SIZE) {
            s.dead = true;
            return;
        }

        // self?
        if (onSnake(s, newRow, newCol)) {
            s.dead = true;
            return;
        }

        s.body.addFirst(new int[]{newRow, newCol});

        // food?
        if (newRow == food[0] && newCol == food[1]) {
            s.score++;
            placeFood();
        } else {
            s.body.removeLast();
        }
    }

    public synchronized String getStateString() {
        String s = "SNAKE P1";
        for (int i = 0; i < p1.body.size(); i++) {
            int[] seg = p1.body.get(i);
            s = s + " (" + seg[0] + "," + seg[1] + ")";
        }
        s = s + " SNAKE P2";
        for (int i = 0; i < p2.body.size(); i++) {
            int[] seg = p2.body.get(i);
            s = s + " (" + seg[0] + "," + seg[1] + ")";
        }
        s = s + " FOOD (" + food[0] + "," + food[1] + ")";
        s = s + " SCORE P1=" + p1.score + " P2=" + p2.score;
        if (gameOver) s = s + " STATUS OVER WINNER " + winner;
        else s = s + " STATUS RUNNING";
        return s;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }
}
