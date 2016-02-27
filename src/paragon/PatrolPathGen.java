package paragon;

import haven.Button;
import haven.Coord;
import haven.UI;
import haven.Widget;
import haven.Window;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PatrolPathGen implements Runnable {
	
	private volatile boolean interrupted = false;
    private Widget closeWindow;
    private Widget coordWindow;
	Utils utils;
	
	
	public PatrolPathGen (UI ui) {
		utils = new Utils(ui);
	}
	
	@Override
	public void run() {
		prevCoord = utils.player().rc;
		coordWindow = utils.ui.sess.glob.gui.add(new CoordWindow(), 450, 300);
		closeWindow = utils.ui.sess.glob.gui.add(new CloseWindow(), 450, 380);
		
		File writeFile = new File("patrolpath.txt");
		try {
			writer = new BufferedWriter(new FileWriter(writeFile, false));
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (!interrupted) {
			utils.sleep(50);
		}
	}
	
	
	private int totalX = 0;
	private int totalY = 0;   
	private Coord prevCoord;
	BufferedWriter writer = null;
	File writeFile = null;

	private class CoordWindow extends Window {
        public CoordWindow() {
            super(Coord.z, "Patrol Path Generator");
            add(new Button(120, "Add Coord", false) {
                public void click() {
                	int x = utils.player().rc.x - prevCoord.x;
                	int y = utils.player().rc.y - prevCoord.y;
                	totalX += x;
                	totalY += y;
                	System.out.println("new Coord(" + totalX + ", " + totalY + "), ");
                	prevCoord = utils.player().rc;
                	try {          
                		writer.write("(" + x + "," + y + ")");
                		writer.flush();
                	} catch (Exception e) {
                		e.printStackTrace();
                	}
                }
            });
            pack();
        }
	}
	
	private class CloseWindow extends Window {
        public CloseWindow() {
            super(Coord.z, "Patrol Path Generator");
            add(new Button(120, "Stop", false) {
				public void click() {
                	interrupted = true;
                	closeWindow.destroy();
                	coordWindow.destroy();
                }
            });
            pack();
        }
	}
}
