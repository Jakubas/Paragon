package haven.paragon.utils;

import java.awt.Color;

import haven.GameUI;
import haven.Gob;
import haven.HavenPanel;
import haven.UI;

public class UtilsSetup {
	public static FlowerMenuUtils flowerMenu = new FlowerMenuUtils();
	public static MainScreen mainScreen = new MainScreen();
	public static Movement	movement = new Movement();
	public static MainInventory mainInventory = new MainInventory();		
	
	public static final int PING_TIMEOUT = 1500;

	public static UI ui() {
		return HavenPanel.lui;
	}
	
	public static Gob player() {
		return ui().sess.glob.gui.map.player();
	}
	
	public static boolean sleep(int timeout) {
		try {
			Thread.sleep(timeout);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
    public static void sysMsg(String msg, Color color ) {
    	ui().root.findchild(GameUI.class).info(msg,color);
    }
}
