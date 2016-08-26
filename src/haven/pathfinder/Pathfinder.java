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
    public Coord mc;
    private int modflags;
    private int interruptedRetries = 5;

    public Pathfinder(MapView mv, Coord dest, String action) {
        this.dest = dest;
        this.action = action;
        this.oc = mv.glob.oc;
        this.map = mv.glob.map;
        this.mv = mv;
    }

    public Pathfinder(MapView mv, Coord dest, Gob gob, int meshid, int clickb, int modflags, String action) {
        this.dest = dest;
        this.meshid = meshid;
        this.clickb = clickb;
        this.gob = gob;
        this.modflags = modflags;
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
            Gob player = mv.player();
            for (Gob gob : oc) {
                if (gob.isplayer())
                    continue;
                // need to exclude destination gob so it won't get into TO candidates list
                if (this.gob != null && this.gob.id == gob.id)
                    continue;
                GobHitbox.BBox box = GobHitbox.getBBox(gob, true);
                if (box != null && isInsideBoundBox(gob.rc, gob.a, box, player.rc)) {
                    m.excludeGob(gob);
                    continue;
                }
                m.addGob(gob);
            }
        }

        // if player is located at a position occupied by a gob (can happen when starting too close to gobs)
        // move it 1 unit away from it
        if (m.isOriginBlocked()) {
            Pair<Integer, Integer> freeloc = m.getFreeLocation();

            if (freeloc == null) {
                terminate = true;
                m.dbgdump();
                return;
            }

            mc = new Coord(src.x + freeloc.a - Map.origin, src.y + freeloc.b - Map.origin);
            mv.wdgmsg("click", Coord.z, mc, 1, 0);

            // FIXME
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // need to recalculate map
            moveinterupted = true;
            m.dbgdump();
            return;
        }



        // exclude any bounding boxes overlapping the destination gob
        if (this.gob != null)
            m.excludeGob(this.gob);

        if (Map.DEBUG_TIMINGS)
            System.out.println("      Gobs Processing: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        Iterable<Edge> path = m.main();
        if (Map.DEBUG_TIMINGS)
            System.out.println("--------------- Total: " + (double) (System.nanoTime() - starttotal) / 1000000.0 + " ms.");

        m.dbgdump();

        Iterator<Edge> it = path.iterator();
        lastmsg = System.currentTimeMillis();
        while (it.hasNext() && !moveinterupted && !terminate) {
            Edge e = it.next();

            mc = new Coord(src.x + e.dest.x - Map.origin, src.y + e.dest.y - Map.origin);

            if (action != null && !it.hasNext())
                mv.gameui().menu.wdgmsg("act", new Object[]{action});

            if (gob != null && !it.hasNext())
                mv.wdgmsg("click", gob.sc, mc, clickb, modflags, 0, (int) gob.id, gob.rc, 0, meshid);
            else
                mv.wdgmsg("click", Coord.z, mc, 1, 0);

            while (!moveinterupted && !terminate && !mv.player().rc.equals(mc)) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    return;
                }

                // when right clicking gobs, char will try to navigate towards gob's rc
                // however he will be blocked by gob's bounding box.
                // no step/count notification will be send by the server for last segment
                // and player's position won't match mc either
                // therefore we just wait for a bit
                if (gob != null && !it.hasNext() && System.currentTimeMillis() - lastmsg > 500) {
                    break;
                } else if (System.currentTimeMillis() - lastmsg > 800) { // make sure we don't get stuck indefinitely
                    break;
                }
            }

            if (moveinterupted) {
                interruptedRetries--;
                if (interruptedRetries == 0)
                    terminate = true;
                m.dbgdump();
                return;
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
        lastmsg = System.currentTimeMillis();
    }

    public void moveCount(int count) {
        lastmsg = System.currentTimeMillis();
    }

    static public boolean isInsideBoundBox(Coord gobRc, double gobA, GobHitbox.BBox gobBBox, Coord point) {
        final Coordf relative = new Coordf(point.sub(gobRc)).rotate(-gobA);
        return relative.x >= gobBBox.a.x && relative.x <= gobBBox.b.x &&
               relative.y >= gobBBox.a.y && relative.y <= gobBBox.b.y;
    }
}
