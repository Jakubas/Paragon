package haven.paragon.automations;

import static haven.paragon.utils.UtilsSetup.*;

import java.awt.Color;

import haven.Button;
import haven.Coord;
import haven.Gob;
import haven.Widget;
import haven.Window;

public class CatchDragonflies implements Runnable {
	
	private volatile boolean interrupted = false;
    
	@Override
	public void run() {
		ui.sess.glob.gui.add(new CloseWindow(), 600, 300);
		while(!interrupted) {
			if (inventory.isFull()) {
				sysMsg("inventory is full", Color.WHITE);
				return;
			}
			
			
			while(!inventory.isFull()) {
				if (interrupted) {
					return;
				}
				sleep(100);
				inventory.drink(80);
				Gob dragonfly = mainScreen.getNearestObject("gfx/kritter/dragonfly/dragonfly");
				if (dragonfly == null)
					continue;
				int prevSize = inventory.size();
				movement.doClickObj(dragonfly, 3, 0);
				movement.waitForMovement(PING_TIMEOUT);
				while (movement.isMoving()) {
					sleep(100);
				}
				if (prevSize == inventory.size())
					movement.clickInRandomDirection();
			}
		}
	}
	
	private class CloseWindow extends Window {
        public CloseWindow() {
            super(Coord.z, "Dragonfly Catcher");
            add(new Button(120, "Stop", false) {
				public void click() {
                	interrupted = true;
                	sysMsg("stopping dragonfly catcher", Color.WHITE);
                	parent.destroy();
                }
            });
            pack();
        }
        public void wdgmsg(Widget sender, String msg, Object... args) {
            interrupted = true;
            sysMsg("stopping dragonfly catcher", Color.WHITE);
            destroy();
        }
	}
}
