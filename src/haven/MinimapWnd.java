package haven;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import static haven.PUtils.blurmask2;
import static haven.PUtils.rasterimg;

public class MinimapWnd extends Widget implements DTarget {
    public static final Tex bg = Resource.loadtex("gfx/hud/wnd/lg/bg");
    public static final Tex cl = Resource.loadtex("gfx/hud/wndmap/lg/cl");
    public static final Tex tm = Resource.loadtex("gfx/hud/wnd/lg/tm");
    public static final Tex tr = Resource.loadtex("gfx/hud/wnd/lg/tr");
    public static final Tex lm = Resource.loadtex("gfx/hud/wndmap/lg/lm");
    public static final Tex rm = Resource.loadtex("gfx/hud/wnd/lg/rm");
    public static final Tex bl = Resource.loadtex("gfx/hud/wndmap/lg/bl");
    public static final Tex bm = Resource.loadtex("gfx/hud/wnd/lg/bm");
    public static final Tex br = Resource.loadtex("gfx/hud/wndmap/lg/br");
    public static final Coord tlm = new Coord(18, 30), brm = new Coord(13, 22);
    private final Widget mmap;
    private final MapView map;
    private IButton pclaim, vclaim, center, viewdist, grid;
    private ToggleButton lock;
    private boolean minimized;
    private Coord szr;
    private boolean resizing;
    private Coord doff;
    private static final Coord minsz = new Coord(110, 130), maxsz = new Coord(700, 700);
    public static final BufferedImage ctex = Resource.loadimg("gfx/hud/fonttex");
    public static final Text.Furnace cf = new Text.Imager(new PUtils.TexFurn(new Text.Foundry(Text.sans, 14).aa(true), ctex)) {
        protected BufferedImage proc(Text text) {
            return (rasterimg(blurmask2(text.img.getRaster(), 1, 1, Color.BLACK)));
        }
    };

    private static final BufferedImage[] cbtni = new BufferedImage[]{
            Resource.loadimg("gfx/hud/wndmap/lg/cbtnu"),
            Resource.loadimg("gfx/hud/wndmap/lg/cbtnh")};
    public final Coord tlo, rbo, mrgn;
    public final IButton cbtn;
    public boolean dt = false;
    public Text cap;
    public Coord wsz, ctl, csz, atl, asz;
    public int cmw;
    private UI.Grab dm = null;

    public MinimapWnd(Coord sz, MapView _map) {
        this.tlo = Coord.z;
        this.rbo = Coord.z;
        this.mrgn = Coord.z;
        cbtn = add(new IButton(cbtni[0], cbtni[1]));
        resize(sz);
        setfocustab(true);
        this.mmap = new LocalMiniMap(Utils.getprefc("mmapsz", new Coord(290, 271)), _map);
        this.map = _map;
        this.c = Coord.z;

        pclaim = new IButton("gfx/hud/lbtn-vil", "", "-d", "-h") {
            {tooltip = Text.render("Display personal claims");}
            public void click() {
                if((map != null) && !map.visol(0))
                    map.enol(0, 1);
                else
                    map.disol(0, 1);
            }
        };
        vclaim = new IButton("gfx/hud/lbtn-claim", "", "-d", "-h") {
            {tooltip = Text.render("Display village claims");}
            public void click() {
                if((map != null) && !map.visol(2))
                    map.enol(2, 3);
                else
                    map.disol(2, 3);
            }
        };
        center = new IButton("gfx/hud/center", "", "", "") {
            {tooltip = Text.render("Center the map on player");}
            public void click() {
                ((LocalMiniMap)mmap).center();
            }
        };
        lock = new ToggleButton("gfx/hud/lock", "gfx/hud/lockd", Config.maplocked) {
            { tooltip = Text.render("Lock map dragging"); }
            public void click() {
                Config.maplocked = !Config.maplocked;
                Utils.setprefb("maplocked", Config.maplocked);
            }
        };
        viewdist = new IButton("gfx/hud/viewdist", "", "", "") {
            {tooltip = Text.render("Show view distance box");}
            public void click() {
                Config.mapshowviewdist = !Config.mapshowviewdist;
                Utils.setprefb("mapshowviewdist", Config.mapshowviewdist);
            }
        };
        grid = new IButton("gfx/hud/grid", "", "", "") {
            {tooltip = Text.render("Show map grid");}
            public void click() {
                Config.mapshowgrid = !Config.mapshowgrid;
                Utils.setprefb("maplocked", Config.mapshowgrid);
            }
        };

        add(mmap, 1, 31);
        add(pclaim, 4, -3);
        add(vclaim, 4, -8);
        add(center, 50, -8);
        add(lock, 110, 5);
        add(viewdist, 99, -8);
        add(grid, 126, -8);
        pack();
    }

    protected void added() {
        parent.setfocus(this);
    }

    public void cdraw(GOut g) {
    }

    private void drawframe(GOut g) {
        Coord mdo, cbr;
        g.image(cl, tlo);
        mdo = tlo.add(cl.sz().x, 0);
        cbr = tlo.add(wsz.add(-tr.sz().x, tm.sz().y));
        for (; mdo.x < cbr.x; mdo.x += tm.sz().x)
            g.image(tm, mdo, Coord.z, cbr);
        g.image(tr, tlo.add(wsz.x - tr.sz().x, 0));

        mdo = tlo.add(0, cl.sz().y);
        cbr = tlo.add(lm.sz().x, wsz.y - bl.sz().y);
        for (; mdo.y < cbr.y; mdo.y += lm.sz().y)
            g.image(lm, mdo, Coord.z, cbr);

        mdo = tlo.add(wsz.x - rm.sz().x, tr.sz().y);
        cbr = tlo.add(wsz.x, wsz.y - br.sz().y);
        for (; mdo.y < cbr.y; mdo.y += rm.sz().y)
            g.image(rm, mdo, Coord.z, cbr);

        g.image(bl, tlo.add(0, wsz.y - bl.sz().y));
        mdo = tlo.add(bl.sz().x, wsz.y - bm.sz().y);
        cbr = tlo.add(wsz.x - br.sz().x, wsz.y);
        for (; mdo.x < cbr.x; mdo.x += bm.sz().x)
            g.image(bm, mdo, Coord.z, cbr);
        g.image(br, tlo.add(wsz.sub(br.sz())));
    }

    public void draw(GOut g) {
        Coord bgc = new Coord();
        for (bgc.y = ctl.y; bgc.y < ctl.y + csz.y; bgc.y += bg.sz().y) {
            for (bgc.x = ctl.x; bgc.x < ctl.x + csz.x; bgc.x += bg.sz().x)
                g.image(bg, bgc, ctl, csz);
        }
        cdraw(g.reclip(atl, asz));
        drawframe(g);
        super.draw(g);
    }

    public Coord contentsz() {
        Coord max = new Coord(0, 0);
        for (Widget wdg = child; wdg != null; wdg = wdg.next) {
            if (wdg == cbtn)
                continue;
            if (!wdg.visible)
                continue;
            Coord br = wdg.c.add(wdg.sz);
            if (br.x > max.x)
                max.x = br.x;
            if (br.y > max.y)
                max.y = br.y;
        }
        return (max);
    }

    public void resize(Coord sz) {
        asz = sz;
        csz = asz.add(mrgn.mul(2));
        wsz = csz.add(tlm).add(brm);
        this.sz = wsz.add(tlo).add(rbo);
        ctl = tlo.add(tlm);
        atl = ctl.add(mrgn);
        cmw = (cap == null) ? 0 : (cap.sz().x);
        cmw = Math.max(cmw, wsz.x / 4);
        cbtn.c = xlate(tlo.add(wsz.x - cbtn.sz.x, 0), false);
        for (Widget ch = child; ch != null; ch = ch.next)
            ch.presize();
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "pack") {
            pack();
        } else if (msg == "dt") {
            dt = (Integer) args[0] != 0;
        } else if (msg == "cap") {
            return;
        } else {
            super.uimsg(msg, args);
        }
    }

    public Coord xlate(Coord c, boolean in) {
        if (in)
            return (c.add(atl));
        else
            return (c.sub(atl));
    }

    public boolean mousedown(Coord c, int button) {
        if (!minimized && c.x > sz.x-40 && c.y > sz.y-30) {
            doff = c;
            dm = ui.grabmouse(this);
            resizing = true;
            return true;
        }

        if (!minimized) {
            parent.setfocus(this);
            raise();
        }

        if (super.mousedown(c, button)) {
            parent.setfocus(this);
            raise();
            return true;
        }

        if (c.isect(ctl, csz)) {
            if (button == 1) {
                dm = ui.grabmouse(this);
                doff = c;
            }
            parent.setfocus(this);
            raise();
            return true;
        }
        return false;
    }

    public void mousemove(Coord c) {
        if (resizing && dm != null) {
            Coord d = c.sub(doff);
            doff = c;
            mmap.sz.x = Math.min(Math.max(mmap.sz.x + d.x, minsz.x), maxsz.x);
            mmap.sz.y = Math.min(Math.max(mmap.sz.y + d.y, minsz.y), maxsz.y);
            pack();
            Utils.setprefc("mmapwndsz", sz);
            Utils.setprefc("mmapsz", mmap.sz);
        } else {
            if (dm != null)
                this.c = this.c.add(c.add(doff.inv()));
            else
                super.mousemove(c);
        }
    }

    public boolean mouseup(Coord c, int button) {
        resizing = false;

        if (dm != null) {
            Utils.setprefc("mmapc", this.c);
            dm.remove();
            dm = null;
        } else {
            return super.mouseup(c, button);
        }
        return true;
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if(sender == cbtn) {
            minimize();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public boolean type(char key, KeyEvent ev) {
        if(key == KeyEvent.VK_ESCAPE) {
            wdgmsg(cbtn, "click");
            return(true);
        }
        return(super.type(key, ev));
    }

    private void minimize() {
        minimized = !minimized;
        if (minimized) {
            mmap.hide();
            pclaim.hide();
            vclaim.hide();
            center.hide();
            lock.hide();
            viewdist.hide();
            grid.hide();
        } else {
            mmap.show();
            pclaim.show();
            vclaim.show();
            center.show();
            lock.show();
            viewdist.show();
            grid.show();
        }

        if (minimized) {
            szr = asz;
            resize(new Coord(asz.x, 24));
        } else {
            resize(szr);
        }
    }

    public boolean drop(Coord cc, Coord ul) {
        if (dt) {
            wdgmsg("drop", cc);
            return (true);
        }
        return (false);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        return (false);
    }
}