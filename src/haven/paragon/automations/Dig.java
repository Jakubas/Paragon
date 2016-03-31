package haven.paragon.automations;

import static haven.paragon.utils.UtilsSetup.*;
import haven.Button;
import haven.Coord;
import haven.Widget;
import haven.Window;

public class Dig implements Runnable {
	
	private volatile boolean interrupted = false;
    
	@Override
	public void run() {
		ui().sess.glob.gui.add(new CloseWindow(), 600, 300);
		mainInventory.dropIdentical("Soil");
		mainInventory.dropIdentical("Earthworm");
		mainInventory.dropIdentical("Acre Clay");
		while(!interrupted) {
			mainInventory.drink(80);
			
			if (!mainInventory.isFull()) {
				ui().sess.glob.gui.menu.wdgmsg("act", new Object[]{"dig"});
				sleep(PING_TIMEOUT);
				movement.clickCoord(player().coord());
			}
			
			sleep(PING_TIMEOUT);
			while(!mainInventory.isFull() && mainScreen.isProgressBar()) {
				if (interrupted)
					return;
				sleep(50);
			}

			mainInventory.dropIdentical("Soil");
			mainInventory.dropIdentical("Earthworm");
			mainInventory.dropIdentical("Acre Clay");
	    	Coord me = player().rc;
	    	Coord coord = new Coord(me.x+11, me.y);
	    	movement.clickCoord(coord);
		}
	}
	
	private class CloseWindow extends Window {
        public CloseWindow() {
            super(Coord.z, "Digs Tubers and Clay");
            add(new Button(120, "Stop", false) {
				public void click() {
                	interrupted = true;
                	parent.destroy();
                }
            });
            pack();
        }
        public void wdgmsg(Widget sender, String msg, Object... args) {
            interrupted = true;
            destroy();
        }
	}
}
