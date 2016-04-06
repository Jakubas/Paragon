package haven.paragon.automations;

import haven.Button;
import haven.Coord;
import haven.Gob;
import haven.Widget;
import haven.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import static haven.paragon.utils.UtilsSetup.*;

public class Farm implements Runnable {
	
	private volatile boolean interrupted = false;
	
	
	@Override
	public void run() {
		try {
			Widget w = ui().sess.glob.gui.add(new CloseWindow(), 600, 300);
			Gob crop = mainScreen.getNearestObject("terobjs/plants/");
			String cropName = crop.getres().name;
			ArrayList<Gob> cropList = getFarmingGobList(cropName);
			farmer(cropList,  cropName);
			w.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void farmer(ArrayList<Gob> cropList, String cropName) {
		while (!interrupted) {
			Gob crop = findCrop(cropName);
			if (crop == null)
				return;
			mainInventory.drink(41);
			mainScreen.farm(crop);
			mainInventory.dropIdenticalPartial("Seed", "seed", "Carrot", "Beetroot");
		}
	}
	
	public Gob findCrop(String cropName) {
		Gob crop = null;
		if ((crop = mainScreen.getObjectAt(player().rc.add(0, 11), cropName)) != null) {
		} else if ((crop = mainScreen.getObjectAt(player().rc.add(11, 0), cropName)) != null) {
		} else if ((crop = mainScreen.getObjectAt(player().rc.add(0, -11), cropName)) != null) {
		} else if ((crop = mainScreen.getObjectAt(player().rc.add(-11, 0), cropName)) != null) {
		} else {
			crop = mainScreen.getNearestObject(10, cropName);
		}
		return crop;
	}
	
	public ArrayList<Gob> getFarmingGobList(String cropName) {
		Set<Gob> crops = mainScreen.findMapObjects(50, 0, 0, cropName);
		ArrayList<Gob> cropList = new ArrayList<Gob>();
		cropList.addAll(crops);
		return cropList;
	}
	
	private class CloseWindow extends Window {
        public CloseWindow() {
            super(Coord.z, "Harvester");
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
