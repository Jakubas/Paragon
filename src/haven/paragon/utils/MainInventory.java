package haven.paragon.utils;

import static haven.paragon.utils.UtilsSetup.*;
import haven.WItem;

import java.util.List;

public class MainInventory {
	
	
    public void drink(int threshold) {
    	ui.sess.glob.gui.maininv.drink(threshold);
    	mainScreen.waitForProgressBar(PING_TIMEOUT);
    	while(mainScreen.isProgressBar()) {
    		sleep(100);
    	}
    }
    
    public int size() {
    	return ui.sess.glob.gui.maininv.isz.x * ui.sess.glob.gui.maininv.isz.y;
    }
    
    public boolean isFull() {
    	return (ui.sess.glob.gui.maininv.wmap.size() >= size());
    }
    
    public void dropIdentical(String objName) {
    	List<WItem> items = ui.sess.glob.gui.maininv.getitems(objName);
    	if (items.isEmpty()) return;
    	WItem item = items.get(0);
    	item.item.wdgmsg("drop-identical", item.item);
    }
}
