package haven.paragon.automations;

import haven.Coord;
import haven.GItem;
import haven.HavenPanel;
import haven.Inventory;
import haven.WItem;
import haven.Widget;
import haven.UI.UIException;
import haven.Window;
import haven.paragon.utils.UtilsSetup;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortCupboard implements Runnable {
	private Inventory inv;
	
	public SortCupboard(Inventory i) {
		inv = i;
	}

	public List<WItem> getInvItems() {
		List<WItem> invItems = inv.getAllItems();
		System.out.println(invItems.size());
		Collections.sort(invItems, new Comparator<WItem>() {
			@Override
			public int compare(WItem o1, WItem o2) {
				int o1x = o1.c.x;
				int o1y = o1.c.y;
				int o2x = o2.c.x;
				int o2y = o2.c.y;
				if (o1y < o2y) {
					return -1;
				} else if (o1y > o2y) {
					return 1;
				} else if (o1x < o2x) {
					return -1;
				} else if (o1x > o2x) {
					return 1;
				}
				return 0;
			}
		});
		return invItems;
	}
	
	public void selectionSort() {
		List<WItem> invItems = getInvItems();
		
		for(int i = 0; i < invItems.size()-1; i++) {
			int min = i;
			for (int j = i+1; j < invItems.size(); j++) {
				if (invItems.get(j).item.quality().avg > invItems.get(min).item.quality().avg) {
					min = j;
				}
			}
			if (min != i) {
				WItem wItem = invItems.get(i);
				WItem wItem2 = invItems.get(min);
				Coord wItemPrev = wItem.c.add(Inventory.sqsz.div(2)).div(Inventory.invsq.sz()); 
				System.out.println(wItem.item.getname());
				try {
					wItem.item.wdgmsg("take", new Coord(0, 0));
				} catch(UIException e) {
					System.out.println("restarting");
					UtilsSetup.sleep(25);
					Window window = HavenPanel.lui.sess.glob.gui.getwnd("Cupboard");
					window.sortWindowInv();
					return;
				}
				inv.wdgmsg("drop", wItem2.c.add(Inventory.sqsz.div(2)).div(Inventory.invsq.sz()));
				inv.wdgmsg("drop", wItemPrev);
				Collections.swap(invItems, min, i);
			}
		}
		System.out.println("\n\n\n\1");
		for (WItem item : getInvItems()) {
			System.out.println(item.c);
		}
		System.out.println("2");
		for (WItem item : invItems) {
			System.out.println(item.c);
		}
//		System.out.println(getInvItems());
//		if (!getInvItems().equals(invItems)){
//			selectionSort();
//		}
    }

	@Override
	public void run() {
		selectionSort();
	}
}
