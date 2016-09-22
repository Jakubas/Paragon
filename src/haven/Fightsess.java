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

import java.text.DecimalFormat;
import java.util.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

public class Fightsess extends Widget {
    public static final Tex cdframe = Resource.loadtex("gfx/hud/combat/cool");
    public static final Tex actframe = Buff.frame;
    public static final Coord actframeo = Buff.imgoff;
    public static final Tex indframe = Resource.loadtex("gfx/hud/combat/indframe");
    public static final Coord indframeo = (indframe.sz().sub(32, 32)).div(2);
    public static final Tex useframe = Resource.loadtex("gfx/hud/combat/lastframe");
    public static final Coord useframeo = (useframe.sz().sub(32, 32)).div(2);
    private static final Text.Foundry cdfndr = new Text.Foundry(Text.serif, 18).aa(true);
    private static final Color cdclrpos = new Color(128, 128, 255);
    private static final Color cdclrneg = new Color(239, 41, 41);
    public static final int actpitch = 50;
    public final Indir<Resource>[] actions;
    public final boolean[] dyn;
    public int use = -1, useb = -1;
    public Coord pcc;
    public int pho;
    private final Fightview fv;
    private static final DecimalFormat cdfmt = new DecimalFormat("#.#");
    private static final Map<Long, Tex> cdvalues = new HashMap<Long, Tex>(7);

    private static final Map<String, Integer> atkcds = new HashMap<String, Integer>(9){{
        put("Chop", 50);
        put("Cleave", 80);
        put("Haymaker", 60);
        put("Kick", 45);
        put("Knock Its Teeth Out", 35);
        put("Left Hook", 40);
        put("Low Blow", 30);
        put("Punch", 30);
        put("Sting", 35);
    }};

    @RName("fsess")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            int nact = (Integer) args[0];
            return (new Fightsess(nact, parent.getparent(GameUI.class).fv));
        }
    }

    @SuppressWarnings("unchecked")
    public Fightsess(int nact, Fightview fv) {
        this.fv = fv;
        pho = -40;
        this.actions = (Indir<Resource>[]) new Indir[nact];
        this.dyn = new boolean[nact];
    }

    public void presize() {
        resize(parent.sz);
        pcc = sz.div(2);
    }

    protected void added() {
        presize();
    }

    private void updatepos() {
        MapView map;
        Gob pl;
        if (((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null) || (pl.sc == null))
            return;
        pcc = pl.sc;
        pho = (int) (pl.sczu.mul(20f).y) - 20;
    }

    private static final Resource tgtfx = Resource.local().loadwait("gfx/hud/combat/trgtarw");
    private final Map<Pair<Long, Resource>, Sprite> cfx = new CacheMap<Pair<Long, Resource>, Sprite>();
    private final Collection<Sprite> curfx = new ArrayList<Sprite>();

    private void fxon(long gobid, Resource fx) {
	MapView map = getparent(GameUI.class).map;
	Gob gob = ui.sess.glob.oc.getgob(gobid);
	if((map == null) || (gob == null))
	    return;
	Pair<Long, Resource> id = new Pair<Long, Resource>(gobid, fx);
	Sprite spr = cfx.get(id);
	if(spr == null)
	    cfx.put(id, spr = Sprite.create(null, fx, Message.nil));
	map.drawadd(gob.loc.apply(spr));
	curfx.add(spr);
    }

    public void tick(double dt) {
	for(Sprite spr : curfx)
	    spr.tick((int)(dt * 1000));
	curfx.clear();
    }

    private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 18, new Color(128, 128, 255)).aa(true), 1, 1, new Color(48, 48, 96));
    private final Text.UText<?> ip = new Text.UText<Integer>(ipf) {
        public String text(Integer v) {
            return ("IP: " + v);
        }

        public Integer value() {
            return (fv.current.ip);
        }
    };
    private final Text.UText<?> oip = new Text.UText<Integer>(ipf) {
        public String text(Integer v) {
            return ("IP: " + v);
        }

        public Integer value() {
            return (fv.current.oip);
        }
    };

    private static Coord actc(int i) {
        int rl = 5;
        return(new Coord((actpitch * (i % rl)) - (((rl - 1) * actpitch) / 2), 125 + ((i / rl) * actpitch)));
    }

    private static final Coord cmc = new Coord(0, 67);
    private static final Coord usec1 = new Coord(-65, 67);
    private static final Coord usec2 = new Coord(65, 67);
    private Indir<Resource> lastact1 = null, lastact2 = null;
    private Text lastacttip1 = null, lastacttip2 = null;

    public void draw(GOut g) {
        updatepos();
        double now = System.currentTimeMillis() / 1000.0;

        for (Buff buff : fv.buffs.children(Buff.class))
            buff.draw(g.reclip(pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
        if (fv.current != null) {
            for (Buff buff : fv.current.buffs.children(Buff.class))
                buff.draw(g.reclip(pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y), buff.sz));

            g.aimage(ip.get().tex(), pcc.add(-75, 0), 1, 0.5);
            g.aimage(oip.get().tex(), pcc.add(75, 0), 0, 0.5);

	        if(fv.lsrel.size() > 1)
		        fxon(fv.current.gobid, tgtfx);

            if (Config.showcddelta && fv.current != null) {
                Tex cdtex = cdvalues.get(fv.current.gobid);
                if (cdtex != null)
                    g.aimage(cdtex, pcc.add(0, 175), 0.5, 1);
            }
        }

        {
            Coord cdc = pcc.add(cmc);
            if (now < fv.atkct) {
                double cd = fv.atkct - now;
                double a = (now - fv.atkcs) / (fv.atkct - fv.atkcs);
                g.chcolor(255, 0, 128, 224);
                g.fellipse(cdc, new Coord(24, 24), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
                g.chcolor();
                if (Config.showcooldown)
                    g.atextstroked(cdfmt.format(cd), pcc.add(0, 27), 0.5, 0.5, Color.WHITE, Color.BLACK);
            }
            g.image(cdframe, cdc.sub(cdframe.sz().div(2)));
        }
        try {
            Indir<Resource> lastact = fv.lastact;
            if (lastact != this.lastact1) {
                this.lastact1 = lastact;
                this.lastacttip1 = null;
            }
            long lastuse = fv.lastuse;
            if (lastact != null) {
                Tex ut = lastact.get().layer(Resource.imgc).tex();
                Coord useul = pcc.add(usec1).sub(ut.sz().div(2));
                g.image(ut, useul);
                g.image(useframe, useul.sub(useframeo));
                double a = now - (lastuse / 1000.0);
                if (a < 1) {
                    Coord off = new Coord((int) (a * ut.sz().x / 2), (int) (a * ut.sz().y / 2));
                    g.chcolor(255, 255, 255, (int) (255 * (1 - a)));
                    g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
                    g.chcolor();
                }
            }
        } catch (Loading l) {
        }

        if (fv.current != null) {
            try {
                Indir<Resource> lastact = fv.current.lastact;
                if(lastact != this.lastact2) {
                    this.lastact2 = lastact;
                    this.lastacttip2 = null;
                }
                long lastuse = fv.current.lastuse;
                if(lastact != null) {
                    Tex ut = lastact.get().layer(Resource.imgc).tex();
                    Coord useul = pcc.add(usec2).sub(ut.sz().div(2));
                    g.image(ut, useul);
                    g.image(useframe, useul.sub(useframeo));
                    double a = now - (lastuse / 1000.0);
                    if(a < 1) {
                        Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
                        g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
                        g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
                        g.chcolor();
                    }
                }
            } catch(Loading l) {
            }
        }

        for (int i = 0; i < actions.length; i++) {
            Coord ca = pcc.add(actc(i));
            Indir<Resource> act = actions[i];
            try {
                if (act != null) {
                    Tex img = act.get().layer(Resource.imgc).tex();
                    ca = ca.sub(img.sz().div(2));
                    g.image(img, ca);
                    if (i == use) {
                        g.image(indframe, ca.sub(indframeo));
                    } else {
                        g.image(actframe, ca.sub(actframeo));
                    }
                }
            } catch (Loading l) {
            }
            ca.x += actpitch;
        }
    }

    private Widget prevtt = null;
    private Text acttip = null;
    public static final String[] keytips = {"1", "2", "3", "4", "5", "Shift+1", "Shift+2", "Shift+3", "Shift+4", "Shift+5"};
    public Object tooltip(Coord c, Widget prev) {
        for (Buff buff : fv.buffs.children(Buff.class)) {
            Coord dc = pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y);
            if (c.isect(dc, buff.sz)) {
                Object ret = buff.tooltip(c.sub(dc), prevtt);
                if (ret != null) {
                    prevtt = buff;
                    return (ret);
                }
            }
        }
        if (fv.current != null) {
            for (Buff buff : fv.current.buffs.children(Buff.class)) {
                Coord dc = pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y);
                if (c.isect(dc, buff.sz)) {
                    Object ret = buff.tooltip(c.sub(dc), prevtt);
                    if (ret != null) {
                        prevtt = buff;
                        return (ret);
                    }
                }
            }
        }
        final int rl = 5;
        for (int i = 0; i < actions.length; i++) {
            Coord ca = pcc.add(actc(i));
            Indir<Resource> act = actions[i];
            try {
                if (act != null) {
                    Tex img = act.get().layer(Resource.imgc).tex();
                    ca = ca.sub(img.sz().div(2));
                    if (c.isect(ca, img.sz())) {
                        if (dyn[i])
                            return ("Combat discovery");
                        String tip = act.get().layer(Resource.tooltip).t + " ($b{$col[255,128,0]{" + keytips[i] + "}})";
                        if((acttip == null) || !acttip.text.equals(tip))
                            acttip = RichText.render(tip, -1);
                        return(acttip);
                    }
                }
            } catch (Loading l) {
            }
            ca.x += actpitch;
        }

        try {
            Indir<Resource> lastact = this.lastact1;
            if(lastact != null) {
                Coord usesz = lastact.get().layer(Resource.imgc).sz;
                Coord lac = pcc.add(usec1);
                if(c.isect(lac.sub(usesz.div(2)), usesz)) {
                    if(lastacttip1 == null)
                        lastacttip1 = Text.render(lastact.get().layer(Resource.tooltip).t);
                    return(lastacttip1);
                }
            }
        } catch(Loading l) {}
        try {
            Indir<Resource> lastact = this.lastact2;
            if(lastact != null) {
                Coord usesz = lastact.get().layer(Resource.imgc).sz;
                Coord lac = pcc.add(usec2);
                if(c.isect(lac.sub(usesz.div(2)), usesz)) {
                    if(lastacttip2 == null)
                        lastacttip2 = Text.render(lastact.get().layer(Resource.tooltip).t);
                    return(lastacttip2);
                }
            }
        } catch(Loading l) {}
        return (null);
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "act") {
            int n = (Integer) args[0];
            if (args.length > 1) {
                Indir<Resource> res = ui.sess.getres((Integer) args[1]);
                actions[n] = res;
                dyn[n] = ((Integer) args[2]) != 0;
            } else {
                actions[n] = null;
            }
        } else if (msg == "use") {
            this.use = (Integer) args[0];
        } else if (msg == "used") {
            Indir<Resource> act = actions[(Integer) args[0]];
            try {
                if (act != null) {
                    Resource.Tooltip tt = act.get().layer(Resource.Tooltip.class);
                    if (tt != null) {
                        Integer cd = atkcds.get(tt.t);
                        if (cd != null && fv.current != null) {
                            double inc = fv.atkcd - cd;
                            int cddelta = -(int) (inc / (double) cd * 100);
                            cdvalues.put(fv.current.gobid, Text.renderstroked(cddelta + " %", cddelta < 0 ? cdclrneg : cdclrpos, Color.BLACK, cdfndr).tex());
                        }
                    }
                }
            } catch (Loading l) {
            }
        } else {
            super.uimsg(msg, args);
        }
    }

    public boolean globtype(char key, KeyEvent ev) {
        if ((key == 0) && (ev.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) == 0) {
            int n = -1;
            switch(ev.getKeyCode()) {
                case KeyEvent.VK_1: n = 0; break;
                case KeyEvent.VK_2: n = 1; break;
                case KeyEvent.VK_3: n = 2; break;
                case KeyEvent.VK_4: n = 3; break;
                case KeyEvent.VK_5: n = 4; break;
            }
            if((n >= 0) && ((ev.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0))
                n += 5;
            if((n >= 0) && (n < actions.length)) {
                wdgmsg("use", n);
                return(true);
            }
        } else if((key == 9) && ((ev.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)) {
            Fightview.Relation cur = fv.current;
            if(cur != null) {
                fv.lsrel.remove(cur);
                fv.lsrel.addLast(cur);
            }
            fv.wdgmsg("bump", (int)fv.lsrel.get(0).gobid);
            return(true);
        }
        return(super.globtype(key, ev));
    }
}
