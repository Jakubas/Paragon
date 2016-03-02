package haven.paragon.utils;

import haven.Gob;
import haven.HavenPanel;
import haven.UI;

public class UtilsSetup {
	public static FlowerMenuUtils flowerMenu = new FlowerMenuUtils();
	public static MainScreen mainScreen = new MainScreen();
	public static Movement	movement = new Movement();
	public static InventoryUtils inventory = new InventoryUtils();		
	
	public static final int PING_TIMEOUT = 1000;
	public static UI ui = HavenPanel.lui;

	public static Gob player() {
		return ui.sess.glob.gui.map.player();
	}
	
	public static boolean sleep(int timeout) {
		try {
			Thread.sleep(timeout);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
}
