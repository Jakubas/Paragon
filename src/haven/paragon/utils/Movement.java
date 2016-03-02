package haven.paragon.utils;

import haven.Coord;
import haven.Gob;
import haven.Moving;
import java.util.Random;
import static haven.paragon.utils.UtilsSetup.*;


public class Movement {
	public void moveToObject(Gob gob) {
    	double dist = gob.rc.dist(player().rc);
    	while (dist > 0) {
    		leftClickObj(gob);
    		dist = gob.rc.dist(player().rc);
    		if (dist > 0) clickInRandomDirection();
    	}
	}
	
	public void moveToCoord(Coord coord) {
    	double dist = coord.dist(player().rc);
    	while (dist > 0) {
    		clickCoord(coord);
    		dist = coord.dist(player().rc);
    		if (dist > 0) {
    			clickInRandomDirection();
    		}
    	}
    }
	
	public void clickCoord(Coord coord) {
		ui.sess.glob.gui.map.wdgmsg("click", Coord.z, coord, 1, 0, 0);
		waitForMovement(PING_TIMEOUT);
		while (isMoving());
	}
	
	//@button 1 - left mouse button, 2 - middle mouse button, 3 - right mouse button
	//@mod don't remember mod values... need to doc
	public void doClickObj(Gob gob, int button, int mod) {
		ui.sess.glob.gui.map.wdgmsg("click", Coord.z, gob.rc, button, 0, mod, (int)gob.id, gob.rc, 0, -1);
	}
	
	public void leftClickObjOffset(Gob gob, int xOffset, int yOffset) {
		ui.sess.glob.gui.map.wdgmsg("click", Coord.z, new Coord(gob.rc.x+xOffset, gob.rc.y+yOffset), 1, 0, 0, (int)gob.id, gob.rc, 0, -1);
		waitForMovement(PING_TIMEOUT);
		while (isMoving()) {
			sleep(100);
		}
	}
	
	public void leftClickObj(Gob gob) {
		ui.sess.glob.gui.map.wdgmsg("click", Coord.z, gob.rc, 1, 0, 0, (int)gob.id, gob.rc, 0, -1);
		//Hack, wait 200ms to account for delay between clicking and starting movement
		waitForMovement(PING_TIMEOUT);
		while (isMoving()) {
			sleep(100);
		}
	}
	
	//1/6th  chance of walking North, South, West, East
	//1/12th chance of going NW, NE, SW, SE
    public void clickInRandomDirection() {
    	Gob me = player();
    	Random random = new Random();
    	int xOffset = 0;
    	int yOffset = 0;
    	int offsetSize = 15;
    	switch (random.nextInt(3)) {
	    	case 0: xOffset = offsetSize; break;
	    	case 1: yOffset = offsetSize; break;
	    	case 2: xOffset = offsetSize; yOffset = offsetSize; break;
    	}
    	switch (random.nextInt(4)) {
    		case 0: leftClickObjOffset(me,  xOffset,  yOffset); break;
    		case 1: leftClickObjOffset(me, -xOffset,  yOffset); break;
    		case 2: leftClickObjOffset(me,  xOffset, -yOffset); break;
    		case 3: leftClickObjOffset(me, -xOffset, -yOffset); break;
    	}
    }
    
	//returns true if movement was started before timeout
    public boolean waitForMovement(int timeout) {
    	while (!isMoving() && timeout > 0) {
    		sleep(50);
    		timeout -= 50;
    	}
    	return isMoving();
    }
    
	public boolean isMoving() {
		Gob me = player();
		if (me == null) return false;
		Moving m = me.getattr(Moving.class);
		return (m != null);
	}
}
