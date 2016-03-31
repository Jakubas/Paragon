package haven.paragon.utils;

import static haven.paragon.utils.UtilsSetup.*;
import haven.WItem;

import java.util.List;

public class MainInventory {
	
	
    public boolean drink(int threshold) {
    	boolean isDrinking = ui().sess.glob.gui.maininv.drink(threshold);
    	if (isDrinking) {
			mainScreen.waitForProgressBar(PING_TIMEOUT);
			while(mainScreen.isProgressBar()) {
				sleep(50);
			}
    	}
    	return isDrinking;
    }
    
    public int size() {
    	return ui().sess.glob.gui.maininv.isz.x * ui().sess.glob.gui.maininv.isz.y;
    }
    
    public boolean isFull() {
    	return (ui().sess.glob.gui.maininv.wmap.size() >= size());
    }
    
    public void dropIdentical(String... objNames) {
    	List<WItem> items = ui().sess.glob.gui.maininv.getitems(objNames);
    	if (items.isEmpty()) return;
    	WItem item = items.get(0);
    	item.item.wdgmsg("drop-identical", item.item);
    }
    
    public void dropIdenticalPartial(String... objNames) {
    	List<WItem> items = ui().sess.glob.gui.maininv.getitemsPartial(objNames);
    	if (items.isEmpty()) return;
    	WItem item = items.get(0);
    	item.item.wdgmsg("drop-identical", item.item);
    }
}
