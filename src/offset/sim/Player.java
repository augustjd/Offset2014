package offset.sim;

import offset.sim.Point;

public abstract class Player {
    public int id; // id of the piper, 1,2,3...npiper
    public Pair pr;
    
    public Player(Pair prin, int idin) {
    	this.pr = prin;
    	this.id = idin;
    }
    
    
    public abstract void init() ;
    
    // Return: the next position
    // my position: pipers[id-1]
    public abstract movePair move(Point[] grid, Pair pr); // positions of the rats

}