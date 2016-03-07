package haven.automation;


import haven.*;

public class AddCoalToSmelter {
    private GameUI gui;
    private Gob smelter;
    private int count;
    private static final int TIMEOUT = 1500;
    private static final int HAND_DELAY = 8;

    public AddCoalToSmelter(GameUI gui, int count) {
        this.gui = gui;
        this.count = count + 1;
    }

    public void fuel() {
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                Resource res = gob.getres();
                if (res != null && res.name.contains("smelter")) {
                    if (smelter == null)
                        smelter = gob;
                    else if (gob.rc.dist(gui.map.player().rc) < smelter.rc.dist(gui.map.player().rc))
                        smelter = gob;
                }
            }
        }

        WItem coal = gui.maininv.getitem("Coal");
        if (coal == null) {
            coal = gui.maininv.getitem("Black Coal");
            if (coal == null) {
                gui.error("No coal found in the inventory");
                return;
            }
        }

        coal.item.wdgmsg("take", new Coord(coal.item.sz.x / 2, coal.item.sz.y / 2));
        int timeout = 0;
        while (gui.hand.isEmpty()) {
            timeout += HAND_DELAY;
            if (timeout >= TIMEOUT) {
                gui.error("No coal found in the inventory");
                return;
            }
            try {
                Thread.sleep(HAND_DELAY);
            } catch (InterruptedException e) {
                return;
            }
        }

        for (; count > 0; count--) {
            gui.map.wdgmsg("itemact", Coord.z, smelter.rc, count == 1 ? 0 : 1, 0, (int) smelter.id, smelter.rc, 0, -1);
            timeout = 0;
            while (true) {
                WItem newcoal = gui.vhand;
                if (newcoal != coal) {
                    coal = newcoal;
                    break;
                }

                timeout += HAND_DELAY;
                if (timeout >= TIMEOUT) {
                    gui.error("Not enough coal. Need to add " + count + " more.");
                    return;
                }
                try {
                    Thread.sleep(HAND_DELAY);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
