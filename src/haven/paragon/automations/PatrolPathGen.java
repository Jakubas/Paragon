package haven.paragon.automations;

import haven.Button;
import haven.Coord;
import haven.Widget;
import haven.Window;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static haven.paragon.utils.UtilsSetup.*;

public class PatrolPathGen implements Runnable {
	
	private volatile boolean interrupted = false;
    private Widget coordWindow;
	
	@Override
	public void run() {
		prevCoord = player().rc;
		coordWindow = ui.sess.glob.gui.add(new CoordWindow(), 450, 300);
		ui.sess.glob.gui.add(new CloseWindow(), 450, 380);
		
		File writeFile = new File("patrolpath.txt");
		try {
			writer = new BufferedWriter(new FileWriter(writeFile, false));
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (!interrupted) {
			sleep(50);
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
                	int x = player().rc.x - prevCoord.x;
                	int y = player().rc.y - prevCoord.y;
                	totalX += x;
                	totalY += y;
                	System.out.println("new Coord(" + totalX + ", " + totalY + "), ");
                	prevCoord = player().rc;
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
        public void wdgmsg(Widget sender, String msg, Object... args) {
            interrupted = true;
            destroy();
        }
	}
	
	private class CloseWindow extends Window {
        public CloseWindow() {
            super(Coord.z, "Patrol Path Generator");
            add(new Button(120, "Stop", false) {
				public void click() {
                	interrupted = true;
                	parent.destroy();
                	coordWindow.destroy();
                }
            });
            pack();
        }
        public void wdgmsg(Widget sender, String msg, Object... args) {
            interrupted = true;
            destroy();
            coordWindow.destroy();
        }
	}
}
