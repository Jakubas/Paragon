package paragon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javafx.scene.shape.MoveTo;
import jdk.nashorn.internal.runtime.FindProperty;
import haven.FlowerMenu;
import haven.Gob;
import haven.UI;

public class Farm {
	
	Utils utils;
	private UI ui;
	
	public Farm(UI ui) {
		this.ui = ui;
		utils = new Utils(ui);
		Gob crop = utils.getNearestObject("terobjs/plants/");
		String cropName = crop.getres().name;
		farmer(cropName);
	}
	
	public void farmer(String cropName) {
		Set<Gob> crops = utils.findMapObjects(50, 0, 0, cropName);
		//remove all crops west and directly north of the player
		Iterator<Gob> it = crops.iterator();
		while (it.hasNext()) {
			Gob crop= it.next();
			int cx = crop.coordX();
			int cy = crop.coordY();
			int px = utils.player().coordX();
			int py = utils.player().coordY();
			if (cx < px || cx == px && cy < py) {
				crops.remove(crop);
			}
		}

		ArrayList<Gob> cropsList = new ArrayList<Gob>();
		cropsList.addAll(crops);
	
		Comparator<Gob> cmp = new Comparator<Gob>() {
			@Override
			public int compare(Gob a, Gob b) {
				return (int) (utils.player().coord().dist(a.coord()) - utils.player().coord().dist(b.coord()));
			}
        };
		
		while (!cropsList.isEmpty()) {
			cropsList.sort(cmp);
			Gob crop = cropsList.get(0);
			utils.moveToObject(crop);
			utils.farm(crop);
		}
		System.out.println("Finished farming :" + cropName.lastIndexOf('/'));
	}
}
