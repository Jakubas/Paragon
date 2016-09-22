/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;

public class Fightview extends Widget {
    static Tex bg = Resource.loadtex("gfx/hud/bosq");
    static int height = 5;
    static int ymarg = 5;
    static int width = 165;
    static Coord avasz = new Coord(27, 27);
    static Coord cavac = new Coord(width - Avaview.dasz.x - 10, 10);
    static Coord cgivec = new Coord(cavac.x - 35, cavac.y);
    static Coord cpursc = new Coord(cavac.x - 75, cgivec.y + 35);
    public LinkedList<Relation> lsrel = new LinkedList<Relation>();
    private List<Long> rotationlist = new ArrayList<Long>();
    public Relation current = null;
    public Indir<Resource> blk, batk, iatk;
    public double atkcs, atkct;
    public Indir<Resource> lastact = null;
    public long lastuse = 0;
    public int atkcd;
    private GiveButton curgive;
    private Avaview curava;
    private Button curpurs;
    public final Bufflist buffs = add(new Bufflist());
    private static final Gob.Overlay curol = new Gob.Overlay(new FightCurrentOpp());
    {
        buffs.hide();
    }

    public class Relation {
        public final long gobid;
        public final Avaview ava;
        public final GiveButton give;
        public final Button purs;
        public final Bufflist buffs = add(new Bufflist());
        {
            buffs.hide();
        }
        public int ip, oip;
        public Indir<Resource> lastact = null;
        public long lastuse = 0;

        public Relation(long gobid) {
            this.gobid = gobid;
            add(this.ava = new Avaview(avasz, gobid, "avacam"));
            add(this.give = new GiveButton(0, new Coord(15, 15)));
            add(this.purs = new Button(70, "Pursue"));
        }

        public void give(int state) {
            if (this == current)
                curgive.state = state;
            this.give.state = state;
        }

        public void show(boolean state) {
            ava.show(state);
            give.show(state);
            purs.show(state);
        }

        public void remove() {
            ui.destroy(ava);
            ui.destroy(give);
            ui.destroy(purs);
        }

        public void use(Indir<Resource> act) {
            lastact = act;
            lastuse = System.currentTimeMillis();
        }
    }

    public void use(Indir<Resource> act) {
        lastact = act;
        lastuse = System.currentTimeMillis();
    }

    @RName("frv")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            return (new Fightview());
        }
    }

    public Fightview() {
        super(new Coord(width, (bg.sz().y + ymarg) * height));
    }

    public void addchild(Widget child, Object... args) {
        if (args[0].equals("buff")) {
            Widget p;
            if (args[1] == null)
                p = buffs;
            else
                p = getrel((Integer) args[1]).buffs;
            p.addchild(child);
        } else {
            super.addchild(child, args);
        }
    }

    private void setcur(Relation rel) {
        if ((current == null) && (rel != null)) {
            add(curgive = new GiveButton(0), cgivec);
            add(curava = new Avaview(Avaview.dasz, rel.gobid, "avacam"), cavac);
            add(curpurs = new Button(70, "Pursue"), cpursc);
            curgive.state = rel.give.state;
        } else if ((current != null) && (rel == null)) {
            ui.destroy(curgive);
            ui.destroy(curava);
            ui.destroy(curpurs);
            curgive = null;
            curava = null;
            curpurs = null;
        } else if ((current != null) && (rel != null)) {
            curgive.state = rel.give.state;
            curava.avagob = rel.gobid;
        }
        current = rel;

        if (Config.hlightcuropp) {
            if (current != null) {
                Gob curgob = ui.sess.glob.oc.getgob(current.gobid);
                if (curgob != null && !curgob.ols.contains(curol))
                    curgob.ols.add(curol);
            }
            for (Relation r : lsrel) {
                Gob relgob = ui.sess.glob.oc.getgob(r.gobid);
                if (relgob != null && r != rel)
                    relgob.ols.remove(curol);
            }
        }
    }

    public void destroy() {
        setcur(null);
        super.destroy();
    }

    public void draw(GOut g) {
        int y = 10;
        if (curava != null)
            y = curava.c.y + curava.sz.y + 10;
        int x = width - bg.sz().x - 10;
        for (Relation rel : lsrel) {
            if (rel == current) {
                rel.show(false);
                continue;
            }
            g.image(bg, new Coord(x, y));
            rel.ava.c = new Coord(x + 25, ((bg.sz().y - rel.ava.sz.y) / 2) + y);
            rel.give.c = new Coord(x + 5, 4 + y);
            rel.purs.c = new Coord(rel.ava.c.x + rel.ava.sz.x + 5, 4 + y);
            rel.show(true);
            y += bg.sz().y + ymarg;
        }
        super.draw(g);
    }

    public static class Notfound extends RuntimeException {
        public final long id;

        public Notfound(long id) {
            super("No relation for Gob ID " + id + " found");
            this.id = id;
        }
    }

    private Relation getrel(long gobid) {
        for (Relation rel : lsrel) {
            if (rel.gobid == gobid)
                return (rel);
        }
        throw (new Notfound(gobid));
    }

    public void rotateopp() {
        if (lsrel.size() <= 1)
            return;

        for (int i = 0; i < rotationlist.size(); i++) {
            try {
                if (rotationlist.get(i) == current.gobid) {
                    long nxtid = rotationlist.get(i + 1 == rotationlist.size() ? 0 : i + 1);
                    OCache oc = ui.sess.glob.oc;
                    synchronized (oc) {
                        for (Gob gob : oc) {
                            if (gob.id == nxtid) {
                                GameUI gui = gameui();
                                gui.menu.wdgmsg("act", new Object[]{"aggro"});
                                gui.map.wdgmsg("click", gob.sc, Coord.z, 1, 0, 0, (int) gob.id, gob.rc, 0, 0);
                                Gob pl = gui.map.player();
                                gui.map.wdgmsg("click", pl.sc, pl.rc, 3, 0);
                                return;
                            }
                        }
                    }
                    return;
                }
            } catch (IndexOutOfBoundsException e) { // ignored
            }
        }
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == curava) {
            wdgmsg("click", (int) current.gobid, args[0]);
            return;
        } else if (sender == curgive) {
            wdgmsg("give", (int) current.gobid, args[0]);
            return;
        } else if (sender == curpurs) {
            wdgmsg("prs", (int) current.gobid);
            return;
        }
        for (Relation rel : lsrel) {
            if (sender == rel.ava) {
                wdgmsg("click", (int) rel.gobid, args[0]);
                return;
            } else if (sender == rel.give) {
                wdgmsg("give", (int) rel.gobid, args[0]);
                return;
            } else if (sender == rel.purs) {
                wdgmsg("prs", (int) rel.gobid);
                return;
            }
        }
        super.wdgmsg(sender, msg, args);
    }

    private Indir<Resource> n2r(int num) {
        if (num < 0)
            return (null);
        return (ui.sess.getres(num));
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "new") {
            Relation rel = new Relation((Integer) args[0]);
            rel.give((Integer) args[1]);
            rel.ip = (Integer) args[2];
            rel.oip = (Integer) args[3];
            lsrel.addFirst(rel);
            rotationlist.add(rel.gobid);
            ui.sess.glob.oc.isfight = true;
            return;
        } else if (msg == "del") {
            Relation rel = getrel((Integer) args[0]);
            OCache oc = ui.sess.glob.oc;
            oc.removedmgoverlay(rel.gobid);
            if (Config.hlightcuropp) {
                Gob relgob = ui.sess.glob.oc.getgob(rel.gobid);
                if (relgob != null)
                    relgob.ols.remove(curol);
            }
            rel.remove();
            lsrel.remove(rel);
            rotationlist.remove(rel.gobid);
            if (lsrel.size() == 0) {
                oc.removedmgoverlay(MapView.plgob);
                oc.isfight = false;
            }
            if (rel == current)
                setcur(null);
            return;
        } else if (msg == "upd") {
            Relation rel = getrel((Integer) args[0]);
            rel.give((Integer) args[1]);
            rel.ip = (Integer) args[2];
            rel.oip = (Integer) args[3];
            return;
        } else if(msg == "used") {
            use((args[0] == null)?null:ui.sess.getres((Integer)args[0]));
            return;
        } else if(msg == "ruse") {
            Relation rel = getrel((Integer)args[0]);
            rel.use((args[1] == null)?null:ui.sess.getres((Integer)args[1]));
            return;
        } else if (msg == "cur") {
            try {
                Relation rel = getrel((Integer) args[0]);
                lsrel.remove(rel);
                lsrel.addFirst(rel);
                setcur(rel);
            } catch (Notfound e) {
                setcur(null);
            }
            return;
        } else if (msg == "atkc") {
            atkcd = (Integer) args[0];
            atkcs = System.currentTimeMillis() / 1000.0;
            atkct = atkcs + (atkcd * 0.06);
            return;
        } else if (msg == "blk") {
            blk = n2r((Integer) args[0]);
            return;
        } else if (msg == "atk") {
            batk = n2r((Integer) args[0]);
            iatk = n2r((Integer) args[1]);
            return;
        }
        super.uimsg(msg, args);
    }
}
