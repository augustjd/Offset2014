package offset.sim;

// general utilities
import java.io.*;
import java.util.List;
import java.util.*;

import javax.tools.*;

import java.util.concurrent.*;
import java.net.URL;

// gui utility
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

enum PType {
    PTYPE_noowner,
    PTYPE_player0,
    PTYPE_player1
}

public class Offset
{
    static String ROOT_DIR = "offset";

    // recompile .class file?
    static boolean recompile = true;
    
    // print more details?
    static boolean verbose = true;

    // Step by step trace
    static boolean trace = true;

    // enable gui
    static boolean gui = true;
    static int counter = 0;

    // default parameters
    static int MAX_TICKS = 10000;
    static int size =32;
    static private Point[] grid = new Point[size*size];
    
    static Player player0;
    static Player player1;
    
    static Pair p0;
    static Pair p1;
    //static FileOutputStream out;
    static PrintWriter writer;
    static ArrayList<ArrayList> history = new ArrayList<ArrayList>();
    
	// list files below a certain directory
	// can filter those having a specific extension constraint
    //
	static List <File> directoryFiles(String path, String extension) {
		List <File> allFiles = new ArrayList <File> ();
		allFiles.add(new File(path));
		int index = 0;
		while (index != allFiles.size()) {
			File currentFile = allFiles.get(index);
			if (currentFile.isDirectory()) {
				allFiles.remove(index);
				for (File newFile : currentFile.listFiles())
					allFiles.add(newFile);
			} else if (!currentFile.getPath().endsWith(extension))
				allFiles.remove(index);
			else index++;
		}
		return allFiles;
	}

  	// compile and load players dynamically
    //
	static Player loadPlayer(String group, Pair pr, int id) {
        try {
            // get tools
            URL url = Offset.class.getProtectionDomain().getCodeSource().getLocation();
            // use the customized reloader, ensure clearing all static information
            ClassLoader loader = new ClassReloader(url, Offset.class.getClassLoader());
            if (loader == null) throw new Exception("Cannot load class loader");
            JavaCompiler compiler = null;
            StandardJavaFileManager fileManager = null;
            // get separator
            String sep = File.separator;
            // load players
            // search for compiled files
            File classFile = new File(ROOT_DIR + sep + group + sep + "Player.class");
            System.err.println(classFile.getAbsolutePath());
            if (!classFile.exists() || recompile) {
                // delete all class files
                List <File> classFiles = directoryFiles(ROOT_DIR + sep + group, ".class");
                System.err.print("Deleting " + classFiles.size() + " class files...   ");
                for (File file : classFiles)
                    file.delete();
                System.err.println("OK");
                if (compiler == null) compiler = ToolProvider.getSystemJavaCompiler();
                if (compiler == null) throw new Exception("Cannot load compiler");
                if (fileManager == null) fileManager = compiler.getStandardFileManager(null, null, null);
                if (fileManager == null) throw new Exception("Cannot load file manager");
                // compile all files
                List <File> javaFiles = directoryFiles(ROOT_DIR + sep + group, ".java");
                System.err.print("Compiling " + javaFiles.size() + " source files...   ");
                Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(javaFiles);
                boolean ok = compiler.getTask(null, fileManager, null, null, null, units).call();
                if (!ok) throw new Exception("Compile error");
                System.err.println("OK");
            }
            // load class
            System.err.print("Loading player class...   ");
            Class playerClass = loader.loadClass(ROOT_DIR + "." + group + ".Player");
            System.err.println("OK");
            // set name of player and append on list
            Class[] cArg = new Class[2]; //Our constructor has 3 arguments
            cArg[0] = Pair.class; //First argument is of *object* type Long
            cArg[1] = int.class; //Second argument is of *object* type String
            
           
            Player player = (Player) playerClass.getDeclaredConstructor(cArg).newInstance(pr, id);
            if (player == null)
                throw new Exception("Load error");
            else
                return player;
            	
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }

	}


    // generate a random Pair given a integer d
    static Pair randomPair(int d) {
        Pair pr = new Pair();
        // generate [0-50)
        
        pr.x = random.nextInt(d);
        pr.y = d-pr.x;
        return pr;
    }

    
    void playgui() {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    OffsetUI ui  = new OffsetUI();
                    ui.createAndShowGUI();
                }
            });
    }


    class OffsetUI extends JPanel implements ActionListener {
        int FRAME_SIZE = 800;
        int FIELD_SIZE = 600;
        JFrame f;
        FieldPanel field;
        JButton next;
        JButton next10;
        JButton next50;
        JLabel label;
        JLabel label0;

        public OffsetUI() {
            setPreferredSize(new Dimension(FRAME_SIZE, FRAME_SIZE));
            setOpaque(false);
        }

        public void init() {}

        private boolean performOnce() {
            if (tick > MAX_TICKS) {
                label.setText("Time out!!!");
                label.setVisible(true);
                // print error message
                System.err.println("[ERROR] The player is time out!");
                next.setEnabled(false);
                return false;
            }
           else if (endOfGame()) {
                label.setText("Finishes in " + tick + " ticks!");
                label.setVisible(true);
                // print success message
                int scr0, scr1;
                scr0 = calculatescore(0);
                scr1 = calculatescore(1);
                label0.setText("score for player0  is "+scr0+" score for player1 is "+scr1);
                label0.setVisible(true);
                System.err.println("[SUCCESS] The player achieves the goal in " + tick + " ticks.");
                next.setEnabled(false);
                return false;
            }
            else {
                playStep();
                return true;
            }
        }
        
        public void actionPerformed(ActionEvent e) {
            int steps = 0;

            if (e.getSource() == next)
                steps = 1;
            else if (e.getSource() == next10)
                steps = 10;
            else if (e.getSource() == next50)
                steps = 50;

            for (int i = 0; i < steps; ++i) {
                if (!performOnce())
                    break;
            }

            repaint();
        }


        public void createAndShowGUI()
        {
            this.setLayout(null);

            f = new JFrame("Offset");
            field = new FieldPanel(1.0 * FIELD_SIZE / dimension);
            next = new JButton("Next"); 
            next.addActionListener(this);
            next.setBounds(0, 0, 100, 50);
            next10 = new JButton("Next10"); 
            next10.addActionListener(this);
            next10.setBounds(100, 0, 100, 50);
            next50 = new JButton("Next50"); 
            next50.addActionListener(this);
            next50.setBounds(200, 0, 100, 50);

            label = new JLabel();
            label0 = new JLabel();
            label.setVisible(false);
            label.setBounds(0, 60, 200, 50);
            label.setFont(new Font("Arial", Font.PLAIN, 15));

            label0.setVisible(false);
            label0.setBounds(250, 60, 500, 50);
            label.setFont(new Font("Arial", Font.PLAIN, 15));
            
            field.setBounds(100, 100, FIELD_SIZE + 50, FIELD_SIZE + 50);

            this.add(next);
            this.add(next10);
            this.add(next50);
            this.add(label);
            this.add(label0);
            this.add(field);

            f.add(this);

            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setVisible(true);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
        }

    }

    class FieldPanel extends JPanel {
        double PSIZE = 10;
        double s;
        BasicStroke stroke = new BasicStroke(2.0f);
        double ox = 10.0;
        double oy = 10.0;

        public FieldPanel(double scale) {
            setOpaque(false);
            s = scale;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(stroke);

            // draw 2D rectangle
            double x_in = (dimension*s-ox)/size;
            double y_in = (dimension*s-oy)/size;
           // g2.draw(new Rectangle2D.Double(ox,oy,ox+x_in,oy+y_in));
            for (int i=0; i<size; i++) {
            	
            for (int j=0; j<size; j++) {
            	g2.draw(new Rectangle2D.Double(ox+x_in*i,oy+y_in*j,x_in,y_in));
            }
            
            }
            for (int i = 0; i < size*size; ++i) {
            	drawPoint(g2, grid[i]);
            }
        }
        
        public void drawPoint(Graphics2D g2, Point p) {
        	StringBuilder sb = new StringBuilder();
            sb.append("");
            sb.append(p.value);
            String strI = sb.toString();
            double x_in = (dimension*s-ox)/size;
            double y_in = (dimension*s-oy)/size;
        	if (p.owner == -1) {
                g2.setPaint(Color.BLUE);
        	}
            else if (p.owner == 0) {
                g2.setPaint(Color.magenta);
            }
            else {
                g2.setPaint(Color.GREEN);
            }
        	if (p.change) {
        		//System.out.println("haha, we should change background color now");
        		g2.fillRect((int)(ox+p.x*x_in), (int)(oy+p.y*y_in), (int)(x_in), (int)(y_in));
        		g2.setPaint(Color.WHITE);
        		p.change = false;
        	}
            
         //   Ellipse2D e = new Ellipse2D.Double(p.x*s-PSIZE/2+ox, p.y*s-PSIZE/2+oy, PSIZE, PSIZE);
          //  g2.setStroke(stroke);
            //g2.draw(e);9
            g2.drawString(strI, (int)(11+p.x*x_in), (int)(p.y*y_in+25));
        }

       
    }
    


    // update the current point according to the offsets
    void update(movePair movepr, int playerID) {
    	if (movepr.move) {
    	Point src = movepr.x;
    	Point target = movepr.y;
        target.value = target.value+src.value;
        src.value = 0;
        target.owner = playerID;
        src.owner = -1;
        src.change = true;
        target.change = true;
    	}
    }
    public int calculatescore(int id) {
    	int score =0;
    	for (int i=0; i<size; i++) {
    		for (int j =0; j<size; j++) {
    			if (grid[i*size+j].owner ==id) {
    				score = score+grid[i*size+j].value;
    			}
    		}
    	}
    	return score;
    }
 
    boolean validateMove(movePair movepr, Pair pr) {
    	
    	Point src = movepr.x;
    	Point target = movepr.y;
    	boolean rightposition = false;
    	if (Math.abs(target.x-src.x)==Math.abs(pr.x) && Math.abs(target.y-src.y)==Math.abs(pr.y)) {
    		rightposition = true;
    	}
    	if (Math.abs(target.x-src.x)==Math.abs(pr.y) && Math.abs(target.y-src.y)==Math.abs(pr.x)) {
    		rightposition = true;
    	}
        if (rightposition  && src.value == target.value && src.value>0) {
        	return true;
        }
        else {
        	return false;
        }
    }

    // detect whether the player has achieved the requirement
    boolean endOfGame() {
            if (counter >=2) {
            	System.out.println("end of the game!");
            	return true;
            	
            }
            else {
            	return false;
            }

    }

 /*   static Point[] copyPointArray(Point[] points) {
        Point[] npoints = new Point[points.length];
        for (int p = 0; p < points.length; ++p)
            npoints[p] = new Point(points[p]);

        return npoints;
    }

*/
    void playStep() {
        tick++;  
        movePair next;
        int currentplayer;
        Pair currentPr;
        
        
        if (tick % 2 == 1) {
        	next = player0.move(grid, p0, history);
        	currentPr = p0;
        	currentplayer = 0;
        	counter = 0;
        }
        else {
        	next = player1.move(grid, p1, history);
        	currentPr = p1;
        	currentplayer =1;
        }
        //System.out.println(next.move);
        if (next.move) {
        if (validateMove(next, currentPr)) {
        	writer.printf("(%d, %b, (%d, %d), (%d, %d), %d)\n", currentplayer, next.move, next.x.x, next.x.y, next.y.x, next.y.y, next.x.value*2);
        	writer.flush();
        	ArrayList record = new ArrayList();
        	record.add(currentplayer);
        	record.add(next);
        	history.add(record);
        	update(next, currentplayer);
        	//pairPrint(next);
        }
        else {
        	System.out.println("[ERROR] Invalid move, let the player stay.");
        }
        }
        else {
        	if (nomove(currentPr)) {
        		System.out.printf("%d player no move\n", currentplayer);
        		counter = counter+1;
        	}
        	else {
        		System.err.printf("Player %d still have valid movie, but it gives up", currentplayer);
        	}
        }
        
    }

    void play() {
        while (tick <= MAX_TICKS) {
            if (endOfGame()) break;
            playStep();
        }
        
        if (tick > MAX_TICKS) {
            // Time out
            System.err.println("[ERROR] The player is time out!");
        }
        else {
            // Achieve the goal
            System.err.println("[SUCCESS] The player achieves the goal in " + tick + " ticks.");
        }
    }

   void init() {
	   
       for (int i=0; i<size; i++) {
    	   for (int j=0; j<size; j++) {
    		   grid[i*size+j] = new Point(i, j, 1, -1);
    	   }
       }
    }
   
   void pairPrint(movePair movepr) {
	   System.out.printf("src is (%d, %d) = %d", movepr.x.x, movepr.x.y, movepr.x.value);
	   System.out.printf("target is (%d, %d) = %d \n", movepr.y.x, movepr.y.y, movepr.y.value);
	   
   }
   boolean nomove(Pair pr) {
	   for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				for (int i_pr=0; i_pr<size; i_pr++) {
				for (int j_pr=0; j_pr <size; j_pr++) {
					movePair movepr = new movePair(false, grid[i*size+j], grid[size*i_pr+j_pr]);
					//movepr.x = grid[i*size+j];
				//	movepr.y = grid[size*i_pr+j_pr];
					if (validateMove(movepr, pr)) {
						return false;
					}
				}
				}
			}
	   }
	   return true;
   }
    
	public static void main(String[] args) throws Exception
	{
        // game parameters
        String group0 = null;
        String group1 = null;
        String output = null;
        int d = 0;
        if (args.length > 0)
             d = Integer.parseInt(args[0]);
        if (args.length > 1)
            group0 = args[1];
        if (args.length > 2)
            group1 = args[2];
        if (args.length >3)
        	output = args[3];
        
        // create game
       
		writer = new PrintWriter(output, "UTF-8");
        Offset game = new Offset();
        game.init();
        p0=randomPair(d);
        p1=randomPair(d);
        while (p0.x==p1.x || p0.y == p1.x) {
        	p1=randomPair(d);
        }
        System.out.printf("Pair 1 is (%d, %d)", p0.x, p0.y);
        System.out.printf("Pair 2 is (%d, %d)", p1.x, p1.y);
        player0 = loadPlayer(group0, p0, 0);
        player1 = loadPlayer(group1, p1, 1);
        // init game
        
        // play game
        //if (gui) {
            game.playgui();
       // }
       // else {
         //   game.play();
       // }
       
    }        

    int tick = 0;

    static double dimension = 100.0; // dimension of the map
    static Random random = new Random();
}
