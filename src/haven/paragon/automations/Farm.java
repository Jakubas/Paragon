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
    private Widget window;
	
	
	@Override
	public void run() {
		try {
			window = ui.sess.glob.gui.add(new CloseWindow(), 600, 300);
			Gob crop = mainScreen.getNearestObject("terobjs/plants/");
			String cropName = crop.getres().name;
			ArrayList<Gob> cropList = getFarmingGobList(cropName);
			farmer(cropList,  cropName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void farmer(ArrayList<Gob> cropList, String cropName) {
		
		while (!cropList.isEmpty() && !interrupted) {
			Collections.sort(cropList);
			Gob crop = cropList.get(0);
			movement.moveToObject(crop);
			mainScreen.farm(crop);
			cropList.remove(0);
		}
		System.out.println("Finished farming :");
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
                	window.destroy();
                }
            });
            pack();
        }
	}
}
