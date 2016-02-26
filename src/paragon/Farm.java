package paragon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import haven.Gob;
import haven.UI;

public class Farm implements Runnable {
	
	Utils utils;
	private UI ui;
	
	public Farm(UI ui) {
		this.ui = ui;
		utils = new Utils(ui);
	}
	
	@Override
	public void run() {
		try {
			Gob crop = utils.getNearestObject("terobjs/plants/");
			String cropName = crop.getres().name;
			ArrayList<Gob> cropList = getFarmingGobList(cropName);
			farmer(cropList,  cropName);
		} catch (Exception e) {
			//for debugging
			e.printStackTrace();
		}
	}
	
	public void farmer(ArrayList<Gob> cropList, String cropName) {
		
		while (!cropList.isEmpty()) {
			Collections.sort(cropList);
			Gob crop = cropList.get(0);
			utils.moveToObject(crop);
			utils.farm(crop);
			cropList.remove(0);
		}
		System.out.println("Finished farming :");
	}
	
	public ArrayList<Gob> getFarmingGobList(String cropName) {
		Set<Gob> crops = utils.findMapObjects(50, 0, 0, cropName);
		ArrayList<Gob> cropList = new ArrayList<Gob>();
		cropList.addAll(crops);
		return cropList;
	}
}
