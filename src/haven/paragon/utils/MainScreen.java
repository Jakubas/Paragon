package haven.paragon.utils;

import haven.Coord;
import haven.FlowerMenu;
import haven.GItem;
import haven.GameUI.DraggedItem;
import haven.Gob;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static haven.paragon.utils.UtilsSetup.*;

public class MainScreen {
	public Gob getNearestObject(String... string) {
		return findMapObject(60, 0, 0, string);
	}

	public Gob getNearestObject(int radius, String... string) {
		return findMapObject(radius, 0, 0, string);
	}
    
	 public Gob findMapObject(int radius, int x, int y, String... names) {
        Coord my = player().rc;
        Coord offset = new Coord(x, y).mul(11);
        my = my.add(offset);
        double min = radius*11;
        Gob nearest = null;
        synchronized (ui().sess.glob.oc) {
            for (Gob gob :ui().sess.glob.oc) {
                double dist = gob.rc.dist(my);
                if (dist < min) {
                    boolean matches = false;
                    for (String name : names) {
                        if (gob.isName(name)) {
                            matches = true;
                            break;
                        }
                    }
                    if (matches) {
                        min = dist;
                        nearest = gob;
                    }
                }
            }
        }
        return nearest;
    }
	 
	 public Set<Gob> findMapObjects(int radius, int x, int y, String... names) {
		 Coord my = player().rc;
        Coord offset = new Coord(x, y).mul(11);
        my = my.add(offset);
        double min = radius*11;
        Set<Gob> gobs = new HashSet<Gob>();
        synchronized (ui().sess.glob.oc) {
            for (Gob gob :ui().sess.glob.oc) {
            	double dist = gob.rc.dist(my);
            	if (dist < min) {
            		for (String name : names) {
	                    if (gob.isName(name)) {
	                        gobs.add(gob);
	                        break;
	                    }
	                }
            	}
            }
        }
        return gobs;
	 }
	 
	 //gets map objects sorted by distance from player
	 public Set<Gob> findMapObjectsSorted(int radius, int x, int y, String... names) {
		 Coord my = player().rc;
        Coord offset = new Coord(x, y).mul(11);
        my = my.add(offset);
        double min = radius*11;
        new TreeSet<Gob>(new Comparator<Gob>(){
			@Override
			public int compare(Gob a, Gob b) {
				return (int) (player().coord().dist(a.coord()) - player().coord().dist(b.coord()));
			}
        }); 
        SortedSet<Gob> gobs = new TreeSet<Gob>();
        synchronized (ui().sess.glob.oc) {
            for (Gob gob :ui().sess.glob.oc) {
            	double dist = gob.rc.dist(my);
            	if (dist < min) {
            		for (String name : names) {
	                    if (gob.isName(name)) {
	                        gobs.add(gob);
	                        break;
	                    }
	                }
            	}
            }
        }
        return gobs;
	 }
	 
    public boolean waitForProgressBar(int timeout) {
    	while (!isProgressBar() && timeout > 0) {
    		sleep(10);
    		timeout -= 10;
    	}
    	return isProgressBar();
    }
    
    public boolean isProgressBar() {
    	return ui().sess.glob.gui.prog >= 0;
    }
    
	public boolean farm(Gob crop) {
		movement.doClickObj(crop, 3, 0);
		if (!flowerMenu.waitForFlowerMenu(300)) return false;
		FlowerMenu menu =ui().root.findchild(FlowerMenu.class);
        for (FlowerMenu.Petal opt : menu.opts) {
            if (opt.name.equals("Harvest")) {
                menu.choose(opt);
                menu.destroy();
                waitForProgressBar(PING_TIMEOUT*3);
                while (crop.exists() && isProgressBar()) {
                	sleep(10);
                };
                return true;
            }
        }
		return false;
	}
	
    public boolean isItemInHand() {
        if(ui().sess.glob.gui.hand.size() > 0)
        	return true;
        return false;
    }
    
    public GItem getItemInHand() {
        for (DraggedItem item :ui().sess.glob.gui.hand)
        	return item.item;
        return null;
    }
}
