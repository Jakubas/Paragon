package haven.paragon.utils;

import static haven.paragon.utils.UtilsSetup.*;
import haven.FlowerMenu;

//might refactor this class and move methods to haven.FlowerMenu
public class FlowerMenuUtils {
	
	
	//waits for the options menu to appear, returns false if times out
    public boolean waitForFlowerMenu(int timeout) {
    	FlowerMenu menu = null;
    	while (menu == null && timeout > 0) {
    		menu = ui().root.findchild(FlowerMenu.class);
    		sleep(50);
    		timeout -= 50;
    	}
		return (menu != null);
    }
}
