package paragon;

import haven.Button;
import haven.Coord;
import haven.Inventory;
import haven.UI;
import haven.Widget;
import haven.Window;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PatrolPathGen implements Runnable {
	
	Utils utils;
	
	
	public PatrolPathGen (UI ui) {
		utils = new Utils(ui);
	}
	
	@Override
	public void run() {
		prevCoord = utils.player().rc;
		utils.ui.sess.glob.gui.add(new StatusWindow(), 450, 300);
		
		File writeFile = new File("patrolpath.txt");
		try {
			writer = new BufferedWriter(new FileWriter(writeFile, false));
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (!Thread.currentThread().isInterrupted()) {
			utils.sleep(50);
		}
	}
	
	
	private int totalX = 0;
	private int totalY = 0;   
	private Coord prevCoord;
	BufferedWriter writer = null;
	File writeFile = null;

	private class StatusWindow extends Window {
        public StatusWindow() {
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
}
