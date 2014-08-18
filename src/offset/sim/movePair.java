package offset.sim;

import offset.sim.movePair;

public class movePair {

	public boolean move;
    public Point x;
    public Point y;

    public movePair() {  }

    public movePair(boolean flag, Point xx, Point yy) {
        move = flag;
        x = xx;
        y = yy;
    }
}