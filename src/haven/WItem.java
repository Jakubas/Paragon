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

import java.awt.Color;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;

import static haven.Inventory.sqsz;

public class WItem extends Widget implements DTarget, Comparable<WItem> {
    public static final Resource missing = Resource.local().loadwait("gfx/invobjs/missing");
    private static final Resource studyalarmsfx = Resource.local().loadwait("sfx/study");
    public final GItem item;
    private Resource cspr = null;
    private Message csdt = Message.nil;
    public static final Color famountclr = new Color(24, 116, 205);
    private static final Color qualitybg = new Color(20, 20, 20, 255 - Config.qualitybgtransparency);
    public static final Color[] wearclr = new Color[]{
            new Color(233, 0, 14), new Color(218, 128, 87), new Color(246, 233, 87), new Color(145, 225, 60)
    };

    public WItem(GItem item) {
        super(sqsz);
        this.item = item;
    }

    public void drawmain(GOut g, GSprite spr) {
        spr.draw(g);
    }

    public static BufferedImage shorttip(List<ItemInfo> info) {
        return (ItemInfo.shorttip(info));
    }

    public static BufferedImage longtip(GItem item, List<ItemInfo> info) {
        BufferedImage img = ItemInfo.longtip(info);
        Resource.Pagina pg = item.res.get().layer(Resource.pagina);
        if (pg != null)
            img = ItemInfo.catimgs(0, img, RichText.render("\n" + pg.text, 200).img);
        return (img);
    }

    public BufferedImage longtip(List<ItemInfo> info) {
        return (longtip(item, info));
    }

    public class ItemTip implements Indir<Tex> {
        private final TexI tex;

        public ItemTip(BufferedImage img) {
            if (img == null)
                throw (new Loading());
            tex = new TexI(img);
        }

        public GItem item() {
            return (item);
        }

        public Tex get() {
            return (tex);
        }
    }

    public class ShortTip extends ItemTip {
        public ShortTip(List<ItemInfo> info) {
            super(shorttip(info));
        }
    }

    public class LongTip extends ItemTip {
        public LongTip(List<ItemInfo> info) {
            super(longtip(info));
        }
    }

    private long hoverstart;
    private ItemTip shorttip = null, longtip = null;
    private List<ItemInfo> ttinfo = null;

    public Object tooltip(Coord c, Widget prev) {
        long now = System.currentTimeMillis();
        if (prev == this) {
        } else if (prev instanceof WItem) {
            long ps = ((WItem) prev).hoverstart;
            if (now - ps < 1000)
                hoverstart = now;
            else
                hoverstart = ps;
        } else {
            hoverstart = now;
        }
        try {
            List<ItemInfo> info = item.info();
            if (info.size() < 1)
                return (null);
            if (info != ttinfo) {
                shorttip = longtip = null;
                ttinfo = info;
            }
            if (now - hoverstart < 1000) {
                if (shorttip == null)
                    shorttip = new ShortTip(info);
                return (shorttip);
            } else {
                if (longtip == null)
                    longtip = new LongTip(info);
                return (longtip);
            }
        } catch (Loading e) {
            return ("...");
        }
    }

    public volatile static int cacheseq = 0;

    public abstract class AttrCache<T> {
        private List<ItemInfo> forinfo = null;
        public T save = null;
        private int forseq = -1;

        public T get() {
            try {
                List<ItemInfo> info = item.info();
                if ((cacheseq != forseq) || (info != forinfo)) {
                    save = find(info);
                    forinfo = info;
                    forseq = cacheseq;
                }
            } catch (Loading e) {
                return (null);
            }
            return (save);
        }

        protected abstract T find(List<ItemInfo> info);
    }

    public final AttrCache<Color> olcol = new AttrCache<Color>() {
        protected Color find(List<ItemInfo> info) {
            Color ret = null;
            for (ItemInfo inf : info) {
                if (inf instanceof GItem.ColorInfo) {
                    Color c = ((GItem.ColorInfo) inf).olcol();
                    if (c != null)
                        ret = (ret == null) ? c : Utils.preblend(ret, c);
                }
            }
            return (ret);
        }
    };

    public final AttrCache<Tex> itemnum = new AttrCache<Tex>() {
        protected Tex find(List<ItemInfo> info) {
            GItem.NumberInfo ninf = ItemInfo.find(GItem.NumberInfo.class, info);
            if (ninf == null) return (null);
            return (new TexI(Utils.outline2(Text.render(Integer.toString(ninf.itemnum()), Color.WHITE).img, Utils.contrast(Color.WHITE))));
        }
    };

    private GSprite lspr = null;

    public void tick(double dt) {
    /* XXX: This is ugly and there should be a better way to
     * ensure the resizing happens as it should, but I can't think
	 * of one yet. */
        GSprite spr = item.spr();
        if ((spr != null) && (spr != lspr)) {
            Coord sz = new Coord(spr.sz());
            if ((sz.x % sqsz.x) != 0)
                sz.x = sqsz.x * ((sz.x / sqsz.x) + 1);
            if ((sz.y % sqsz.y) != 0)
                sz.y = sqsz.y * ((sz.y / sqsz.y) + 1);
            resize(sz);
            lspr = spr;
        }
    }

    public void draw(GOut g) {
        GSprite spr = item.spr();
        if (spr != null) {
            Coord sz = spr.sz();
            g.defstate();
            if (olcol.get() != null)
                g.usestate(new ColorMask(olcol.get()));
            drawmain(g, spr);
            g.defstate();
            if (item.num >= 0) {
                g.atext(Integer.toString(item.num), sz, 1, 1, Text.numfnd);
            } else if (itemnum.get() != null) {
                g.aimage(itemnum.get(), new Coord(sz.x, 0), 1, 0);
            }
            if (item.meter > 0) {
                if (Config.itemmeterbar) {
                    g.chcolor(220, 60, 60, 255);
                    g.frect(Coord.z, new Coord((int) (sz.x / (100 / (double) item.meter)), 4));
                    g.chcolor();
                } else if (!Config.itempercentage) {
                    double a = ((double) item.meter) / 100.0;
                    g.chcolor(255, 255, 255, 64);
                    Coord half = sz.div(2);
                    g.prect(half, half.inv(), half, a * Math.PI * 2);
                    g.chcolor();
                }
            }

            GItem.Quality quality = item.quality();
            if (Config.showquality) {
                if (quality != null && quality.max != 0) {
                    Coord btm = new Coord(0, sz.y - 12);
                    if (Config.showqualitymode == 2) {
                        if (Config.qualitybg) {
                            g.chcolor(qualitybg);
                            g.frect(new Coord(0, sz.y - 32), quality.etex.sz().add(1, -1));
                            g.frect(new Coord(0, sz.y - 22), quality.stex.sz().add(1, -1));
                            g.frect(btm, quality.vtex.sz().add(1, -1));
                            g.chcolor();
                        }
                        g.image(quality.etex, new Coord(0, sz.y - 32));
                        g.image(quality.stex, new Coord(0, sz.y - 22));
                        g.image(quality.vtex, btm);
                    } else if (quality.curio && Config.showlpgainmult) {
                        if (Config.qualitybg) {
                            g.chcolor(qualitybg);
                            g.frect(new Coord(0, sz.y - 12), quality.lpgaintex.sz().add(1, -1));
                            g.chcolor();
                        }
                        g.image(quality.lpgaintex, btm);
                    } else if (Config.showqualitymode == 0) {
                        if (Config.qualitybg) {
                            g.chcolor(qualitybg);
                            g.frect(new Coord(0, sz.y - 12), quality.maxtex.sz().add(1, -1));
                            g.chcolor();
                        }
                        g.image(quality.maxtex, btm);
                    } else if (Config.showqualitymode == 1) {
                        Tex t = Config.qualitywhole ? quality.avgwholetex : quality.avgtex;
                        if (Config.qualitybg) {
                            g.chcolor(qualitybg);
                            g.frect(btm, t.sz().add(1, -1));
                            g.chcolor();
                        }
                        g.image(t, btm);
                    } else if (Config.showqualitymode == 3) {
                        Tex t = Config.qualitywhole ? quality.avgsvwholetex : quality.avgsvtex;
                        if (Config.qualitybg) {
                            g.chcolor(qualitybg);
                            g.frect(btm, t.sz().add(1, -1));
                            g.chcolor();
                        }
                        g.image(t, btm);
                    } else {
                        if (Config.qualitybg) {
                            g.chcolor(qualitybg);
                            g.frect(btm, quality.mintex.sz().add(1, -1));
                            g.chcolor();
                        }
                        g.image(quality.mintex, btm);
                    }
                }
            }

            boolean studylefttimedisplayed = false;
            if (Config.showstudylefttime && quality != null && quality.curio && item.meter > 0) {
                if (item.timelefttex == null) {
                    item.updatetimelefttex();
                }

                if (item.timelefttex != null) {
                    g.image(item.timelefttex, Coord.z);
                    studylefttimedisplayed = true;
                }
            }

            if (!studylefttimedisplayed && item.meter > 0 && Config.itempercentage && item.metertex != null) {
                g.image(item.metertex, Coord.z);
            }

            if (Config.showcontentsbars) {
                ItemInfo.Contents cnt = item.getcontents();
                if (cnt != null && cnt.content > 0)
                    drawamountbar(g, cnt.content, cnt.isseeds);
            }

            if (Config.showwearbars) {
                try {
                    for (ItemInfo info : item.info()) {
                        if (info.getClass().getName().equals("Wear")) {
                            double d = (Integer) info.getClass().getDeclaredField("d").get(info);
                            double m = (Integer) info.getClass().getDeclaredField("m").get(info);
                            double p = (m - d) / m;
                            int h = (int) (p * (double) sz.y);
                            g.chcolor(wearclr[p == 1.0 ? 3 : (int) (p / 0.25)]);
                            g.frect(new Coord(sz.x - 3, sz.y - h), new Coord(3, h));
                            g.chcolor();
                            // NOTE: apparently identically named class "Wear" with no namespace is used
                            // for both the wear and armor class info... Y U DO DIS LOFTAR X(
                            // We need to break here once we found first "Wear" (it will always come before the armor class.)
                            // otherwise it would generate exception on second "Wear" class and we don't want to do that
                            // in drawing routine.
                            break;
                        }
                    }
                } catch (Exception e) { // fail silently if info is not ready
                }
            }
        } else {
            g.image(missing.layer(Resource.imgc).tex(), Coord.z, sz);
        }
    }

    private void drawamountbar(GOut g, double content, boolean isseeds) {
        double capacity;
        String name = item.getname();
        if (name.contains("Waterskin"))
            capacity = 3.0D;
        else if (name.contains("Bucket"))
            capacity = isseeds ? 1000D : 10.0D;
        else if (name.contains("Waterflask"))
            capacity = 2.0D;
        else
            return;

        int h = (int) (content / capacity * sz.y) - 1;
        if (h < 0)
            return;

        g.chcolor(famountclr);
        g.frect(new Coord(sz.x - 4, sz.y - h - 1), new Coord(3, h));
        g.chcolor();
    }

    private void openwebpage(String url) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URL(url).toURI());
            } catch (Exception e) {
                // NOP
            }
        }
    }

    public boolean mousedown(Coord c, int btn) {
        if (btn == 1) {
            if (ui.modctrl && ui.modmeta)
                wdgmsg("drop-identical", this.item);
            else if (ui.modctrl && ui.modshift) {
                String name = ItemInfo.find(ItemInfo.Name.class, item.info()).str.text;
                name = name.replace(' ', '_');
                if (!Resource.language.equals("en")) {
                    int i = name.indexOf('(');
                    if (i > 0)
                        name = name.substring(i + 1, name.length() - 1);
                }
                String url = String.format("http://ringofbrodgar.com/wiki/%s", name);
                openwebpage(url);
            } else if (ui.modshift && !ui.modmeta)
                item.wdgmsg("transfer", c);
            else if (ui.modctrl)
                item.wdgmsg("drop", c);
            else if (ui.modmeta)
                wdgmsg("transfer-identical", this.item);
            else
                item.wdgmsg("take", c);
            return (true);
        } else if (btn == 3) {
            if (ui.modmeta && !(parent instanceof Equipory))
                wdgmsg("transfer-identical-asc", this.item);
            else
                item.wdgmsg("iact", c, ui.modflags());
            return (true);
        }
        return (false);
    }

    public boolean drop(Coord cc, Coord ul) {
        return (false);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
        item.wdgmsg("itemact", ui.modflags());
        return (true);
    }

    public void destroy() {
        super.destroy();
        Curiosity ci = null;
        if (parent instanceof Inventory && parent.parent instanceof Tabs.Tab) {
        try {
            ci = ItemInfo.find(Curiosity.class, item.info());
            if (ci != null && item.meter >= 99) {
                Resource.Tooltip tt = item.resource().layer(Resource.Tooltip.class);
                if (tt != null)
                    gameui().syslog.append(tt.t + " LP: " + ci.exp, Color.LIGHT_GRAY);

                if (Config.autostudy) {
                    Window invwnd = gameui().getwnd("Inventory");
                    ArrayList<Window> cupboards = gameui().getwnds("Cupboard");
                    Resource res = item.resource();
                    if (res != null) {
                        if (!replacecurio(invwnd, res)) {
                        	for (int i = 0; i < cupboards.size(); i++) {
                        		if (replacecurio(cupboards.get(i), res))
                        			return;
                        	}
                        }
                    }
                }
            }
        } catch (Loading l) {
        }

            if (Config.studyalarm && ci != null && item.meter >= 99)
                Audio.play(studyalarmsfx, Config.studyalarmvol);
        }
    }

    private boolean replacecurio(Window wnd, Resource res) {
        try {
            for (Widget invwdg = wnd.lchild; invwdg != null; invwdg = invwdg.prev) {
                if (invwdg instanceof Inventory) {
                    Inventory inv = (Inventory) invwdg;
                    for (Widget witm = inv.lchild; witm != null; witm = witm.prev) {
                        if (witm instanceof WItem) {
                            GItem ngitm = ((WItem) witm).item;
                            Resource nres = ngitm.resource();
                            if (nres != null && nres.name.equals(res.name)) {
                                ngitm.wdgmsg("take", witm.c);
                                ((Inventory) parent).drop(Coord.z, c);
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }
        } catch (Exception e) { // ignored
        }
        return false;
    }

	@Override
	public int compareTo(WItem wItem) {
		return (int) (this.item.quality().avg - wItem.item.quality().avg);
	}
}
