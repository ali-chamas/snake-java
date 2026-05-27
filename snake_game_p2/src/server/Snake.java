
package server;

/**
 *
 * @author alichamas
 */
import java.util.LinkedList;

// one snake. each player has one of these.
public class Snake {

    public String id;                  // "P1" or "P2"
    public LinkedList<int[]> body;     // list of [row, col], head at index 0
    public String direction;
    public int score;
    public boolean dead;

    public Snake(String id, int startRow, int startCol, String startDir) {
        this.id = id;
        body = new LinkedList<int[]>();

        body.add(new int[]{startRow, startCol}); // head
        if (startDir.equals("RIGHT")) {
            body.add(new int[]{startRow, startCol - 1});
            body.add(new int[]{startRow, startCol - 2});
        } else if (startDir.equals("LEFT")) {
            body.add(new int[]{startRow, startCol + 1});
            body.add(new int[]{startRow, startCol + 2});
        }

        direction = startDir;
        score = 0;
        dead = false;
    }
}