package snake_server_part2;

import java.util.Random;

/*****************************************************************/
/***************            GameState           ******************/
/*****************************************************************/
/* Two-player version. Holds two snakes and one food.
   All methods are synchronized because three threads touch this object:
   the game loop thread and two ClientHandler threads. */

public class GameState {

    public static final int GRID_SIZE = 20;

    private Snake p1;
    private Snake p2;
    private int[] food;
    private boolean gameOver;
    private String winner;  // "P1", "P2", "DRAW", or "" while running
    private Random rand;

    public GameState() {
        rand = new Random();
        // P1 starts on the left, moving right
        p1 = new Snake("P1", 10, 4, "RIGHT");
        // P2 starts on the right, moving left
        p2 = new Snake("P2", 10, 16, "LEFT");
        gameOver = false;
        winner = "";
        placeFood();
    }

    /*** Place a food at a random cell not occupied by either snake ***/
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

    /*** Helper: is cell (r,c) on the body of snake s? ***/
    private boolean onSnake(Snake s, int r, int c) {
        for (int i = 0; i < s.body.size(); i++) {
            int[] seg = s.body.get(i);
            if (seg[0] == r && seg[1] == c) return true;
        }
        return false;
    }

    /*** Set the direction of P1 or P2, rejecting 180-degree reversals ***/
    public synchronized void setDirection(String playerId, String newDir) {
        Snake s = playerId.equals("P1") ? p1 : p2;
        if (s.dead) return;
        if (newDir.equals("UP")    && s.lastTickDirection.equals("DOWN"))  return;
        if (newDir.equals("DOWN")  && s.lastTickDirection.equals("UP"))    return;
        if (newDir.equals("LEFT")  && s.lastTickDirection.equals("RIGHT")) return;
        if (newDir.equals("RIGHT") && s.lastTickDirection.equals("LEFT"))  return;
        s.direction = newDir;
    }

    /*** Move both snakes one step, check all collisions, declare a winner ***/
    public synchronized void tick() {
        if (gameOver) return;

        // 1. Move each snake (wall + self + food collision checked inside)
        tickSnake(p1);
        tickSnake(p2);

        // 2. Snake-vs-snake: head of one hits the body of the other
        boolean p1HitsP2 = !p1.dead && onSnake(p2, p1.body.getFirst()[0], p1.body.getFirst()[1]);
        boolean p2HitsP1 = !p2.dead && onSnake(p1, p2.body.getFirst()[0], p2.body.getFirst()[1]);
        if (p1HitsP2) p1.dead = true;
        if (p2HitsP1) p2.dead = true;

        // 3. Decide game over
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

    /*** Move a single snake one cell, check wall / self / food collisions ***/
    private void tickSnake(Snake s) {
        if (s.dead) return;

        int[] head = s.body.getFirst();
        int newRow = head[0];
        int newCol = head[1];
        if (s.direction.equals("UP"))    newRow--;
        if (s.direction.equals("DOWN"))  newRow++;
        if (s.direction.equals("LEFT"))  newCol--;
        if (s.direction.equals("RIGHT")) newCol++;

        // Wall collision
        if (newRow < 0 || newRow >= GRID_SIZE || newCol < 0 || newCol >= GRID_SIZE) {
            s.dead = true;
            return;
        }

        // Self collision
        if (onSnake(s, newRow, newCol)) {
            s.dead = true;
            return;
        }

        // Move
        s.body.addFirst(new int[]{newRow, newCol});
        if (newRow == food[0] && newCol == food[1]) {
            s.score++;
            placeFood();
        } else {
            s.body.removeLast();
        }

        s.lastTickDirection = s.direction;
    }

    /*** Build the state string sent to BOTH clients each tick.
         Format: SNAKE P1 (r,c) ... SNAKE P2 (r,c) ... FOOD (r,c)
                 SCORE P1=n P2=n  STATUS RUNNING | STATUS OVER WINNER X ***/
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
        if (gameOver) {
            s = s + " STATUS OVER WINNER " + winner;
        } else {
            s = s + " STATUS RUNNING";
        }
        return s;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }
}