package snake_server_part2;

import java.util.LinkedList;

/*****************************************************************/
/***************              Snake             ******************/
/*****************************************************************/
/* Represents one snake in the game. Each player has one Snake.
   This class is just data; the rules live in GameState. */

public class Snake {

    public String id;                  // "P1" or "P2"
    public LinkedList<int[]> body;     // segments, head at index 0
    public String direction;           // requested direction
    public String lastTickDirection;   // direction used last tick (for reversal check)
    public int score;
    public boolean dead;

    public Snake(String id, int startRow, int startCol, String startDir) {
        this.id = id;
        body = new LinkedList<int[]>();

        // Place 3 segments. Head at (startRow, startCol), body trails behind.
        body.add(new int[]{startRow, startCol});  // head
        if (startDir.equals("RIGHT")) {
            body.add(new int[]{startRow, startCol - 1});
            body.add(new int[]{startRow, startCol - 2});
        } else if (startDir.equals("LEFT")) {
            body.add(new int[]{startRow, startCol + 1});
            body.add(new int[]{startRow, startCol + 2});
        }

        direction = startDir;
        lastTickDirection = startDir;
        score = 0;
        dead = false;
    }
}