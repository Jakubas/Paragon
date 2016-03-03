package haven.pathfinder;


import haven.*;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Pathfinder implements Runnable {
    private OCache oc;
    private MCache map;
    private MapView mv;
    private Coord dest;
    public boolean terminate = false;
    public boolean moveinterupted = false;
    private int meshid;
    private int clickb;
    private Gob gob;
    private String action;
    private int count = -100;
    private int step = -200;
    public Coord mc;

    public Pathfinder(MapView mv, Coord dest, String action) {
        this.dest = dest;
        this.action = action;
        this.oc = mv.glob.oc;
        this.map = mv.glob.map;
        this.mv = mv;
    }

    public Pathfinder(MapView mv, Coord dest, Gob gob, int meshid, int clickb, String action) {
        this.dest = dest;
        this.meshid = meshid;
        this.clickb = clickb;
        this.gob = gob;
        this.action = action;
        this.oc = mv.glob.oc;
        this.map = mv.glob.map;
        this.mv = mv;
    }

    private final Set<PFListener> listeners = new CopyOnWriteArraySet<PFListener>();
    public final void addListener(final PFListener listener) {
        listeners.add(listener);
    }

    public final void removeListener(final PFListener listener) {
        listeners.remove(listener);
    }

    private final void notifyListeners() {
        for (PFListener listener : listeners) {
            listener.pfDone(this);
        }
    }

    @Override
    public void run() {
        do {
            moveinterupted = false;
            pathfind(mv.player().rc);
        } while (moveinterupted && !terminate);

        notifyListeners();
    }

    public void pathfind(Coord src) {
        long starttotal = System.nanoTime();
        haven.pathfinder.Map m = new haven.pathfinder.Map(src, dest, map);

        long start = System.nanoTime();
        synchronized (oc) {
            for (Gob gob : oc) {
                if (gob.isplayer())
                    continue;
                // need to exclude destination gob so it won't get into TO candidates list
                if (this.gob != null && this.gob.id == gob.id)
                    continue;
                m.addGob(gob);
            }
        }

        // exclude any bounding boxes overlapping the destination gob
        if (this.gob != null)
            m.excludeGob(this.gob);

        System.out.println("      Gobs Processing: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        Iterable<Edge> path = m.main();
        System.out.println("--------------- Total: " + (double) (System.nanoTime() - starttotal) / 1000000.0 + " ms.");

        m.dbgdump();

        Iterator<Edge> it = path.iterator();
        while (it.hasNext() && !moveinterupted && !terminate) {
            count = -100;
            step = -200;

            Edge e = it.next();

            mc = new Coord(src.x + e.dest.x - Map.origin, src.y + e.dest.y - Map.origin);

            if (action != null && !it.hasNext())
                mv.gameui().menu.wdgmsg("act", new Object[]{action});

            if (gob != null && !it.hasNext())
                mv.wdgmsg("click", gob.sc, mc, clickb, 0, 0, (int) gob.id, gob.rc, 0, meshid);
            else
                mv.wdgmsg("click", Coord.z, mc, 1, 0);

            boolean done = false;
            synchronized (Pathfinder.class) {
                done = step < count - 1;
            }

            int c = 0;
            while (!moveinterupted && !terminate && done && !mv.player().rc.equals(mc)) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    return;
                }

                // when right clicking gobs, char will try to navigate towards gob's rc
                // however he will be blocked by gob's bounding box.
                // no step/count notification will be send by the server for last segment
                // and player's position won't match mc either
                // therefore we just wait for two sleeping cycles
                if (gob != null && !it.hasNext()) {
                    if (System.currentTimeMillis() - lastmsg > 400)
                        break;
                }

                synchronized (Pathfinder.class) {
                    done = step < count - 1;
                }
            }
        }

        terminate = true;
    }

    public void moveStop(int step) {
        moveinterupted = true;
        lastmsg = System.currentTimeMillis();
    }

    private long lastmsg;
    public void moveStep(int step) {
        this.step = step;
        lastmsg = System.currentTimeMillis();
    }

    public void moveCount(int count) {
        this.count = count;
        lastmsg = System.currentTimeMillis();
    }
}
