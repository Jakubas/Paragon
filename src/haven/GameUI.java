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

import haven.automation.ErrorSysMsgCallback;

import java.awt.*;
import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static haven.GItem.Quality.AVG_MODE_GEOMETRIC;
import static haven.GItem.Quality.AVG_MODE_QUADRATIC;
import static haven.Inventory.invsq;

public class GameUI extends ConsoleHost implements Console.Directory {
    public static final Text.Foundry msgfoundry = new Text.Foundry(Text.dfont, Config.fontsizeglobal * 14 / 11);
    public static final Text.Foundry progressf = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 12).aa(true);
    private static final int blpw = 142, brpw = 142;
    public final String chrid;
    public final long plid;
    private final Hidepanel ulpanel, umpanel, urpanel, brpanel, menupanel;
    public Avaview portrait;
    public MenuGrid menu;
    public MapView map;
    public Fightview fv;
    private List<Widget> meters = new LinkedList<Widget>();
    private Text lastmsg;
    private long msgtime;
    public Window invwnd, equwnd, makewnd;
    public Inventory maininv;
    public CharWnd chrwdg;
    private Widget qqview;
    public BuddyWnd buddies;
    private final Zergwnd zerg;
    public final Collection<Polity> polities = new ArrayList<Polity>();
    public HelpWnd help;
    public OptWnd opts;
    public Collection<DraggedItem> hand = new LinkedList<DraggedItem>();
    public WItem vhand;
    public ChatUI chat;
    public ChatUI.Channel syslog;
    public double prog = -1;
    private boolean afk = false;
    @SuppressWarnings("unchecked")
    public Indir<Resource>[] belt = new Indir[144];
    public Belt beltwdg = add(new NKeyBelt());
    public final Map<Integer, String> polowners = new HashMap<Integer, String>();
    public Bufflist buffs;
    public MinimapWnd minimapWnd;
    public TimersWnd timerswnd;
    public QuickSlotsWdg quickslots;
    public StatusWdg statuswindow;
    public AlignPanel questpanel;
    private boolean updhanddestroyed = false;
    public static boolean swimon = false;
    public static boolean crimeon = false;
    public static boolean trackon = false;
    private boolean crimeautotgld = false;
    private boolean trackautotgld = false;
    public FBelt fbelt;
    public CraftHistoryBelt histbelt;
    private ErrorSysMsgCallback errmsgcb;
    private static final Pattern esvMsgPattern = Pattern.compile("Essence: ([0-9]+), Substance: ([0-9]+), Vitality: ([0-9]+)");

    public abstract class Belt extends Widget {
        public Belt(Coord sz) {
            super(sz);
        }

        public void keyact(final int slot) {
            if (map != null) {
                Coord mvc = map.rootxlate(ui.mc);
                if (mvc.isect(Coord.z, map.sz)) {
                    map.delay(map.new Hittest(mvc) {
                        protected void hit(Coord pc, Coord mc, MapView.ClickInfo inf) {
                            if (inf == null)
                                GameUI.this.wdgmsg("belt", slot, 1, ui.modflags(), mc);
                            else
                                GameUI.this.wdgmsg("belt", slot, 1, ui.modflags(), mc, (int) inf.gob.id, inf.gob.rc);
                        }

                        protected void nohit(Coord pc) {
                            GameUI.this.wdgmsg("belt", slot, 1, ui.modflags());
                        }
                    });
                }
            }
        }
    }

    @RName("gameui")
    public static class $_ implements Factory {
        public Widget create(Widget parent, Object[] args) {
            String chrid = (String) args[0];
            int plid = (Integer) args[1];
            return (new GameUI(chrid, plid));
        }
    }

    public GameUI(String chrid, long plid) {
        this.chrid = chrid;
        this.plid = plid;
        setcanfocus(true);
        setfocusctl(true);
        chat = add(new ChatUI(0, 0));
        if (Utils.getprefb("chatvis", true)) {
            chat.resize(0, chat.savedh);
            chat.show();
        }
        beltwdg.raise();
        ulpanel = add(new Hidepanel("gui-ul", null, new Coord(-1, -1)));
        umpanel = add(new Hidepanel("gui-um", null, new Coord(0, -1)) {
            @Override
            public Coord base() {
                if (base != null)
                    return base.get();
                return new Coord(parent.sz.x / 2 - this.sz.x / 2, 0);
            }
        });
        urpanel = add(new Hidepanel("gui-ur", null, new Coord(1, -1)));
        brpanel = add(new Hidepanel("gui-br", null, new Coord(1, 1)) {
            public void move(double a) {
                super.move(a);
                menupanel.move();
            }
        });
        menupanel = add(new Hidepanel("menu", new Indir<Coord>() {
            public Coord get() {
                return (new Coord(GameUI.this.sz.x, Math.min(brpanel.c.y - 79, GameUI.this.sz.y - menupanel.sz.y)));
            }
        }, new Coord(1, 0)));
        menu = brpanel.add(new MenuGrid(), 20, 34);
        brpanel.add(new Img(Resource.loadtex("gfx/hud/brframe")), 0, 0);
        menupanel.add(new MainMenu(), 0, 0);
        foldbuttons();
        portrait = ulpanel.add(new Avaview(Avaview.dasz, plid, "avacam") {
            public boolean mousedown(Coord c, int button) {
                return (true);
            }
        }, new Coord(10, 10));
        buffs = ulpanel.add(new Bufflist(), new Coord(95, 65));
        umpanel.add(new Cal(), new Coord(0, 10));
        add(new Widget(new Coord(300, 40)) {
            @Override
            public void draw(GOut g) {
                if (Config.showservertime) {
                    Tex time = ui.sess.glob.servertimetex;
                    if (time != null)
                        g.image(time, new Coord(300 / 2 - time.sz().x / 2, 0));
                }
            }
        }, new Coord(HavenPanel.w / 2 - 300 / 2, umpanel.sz.y));
        syslog = chat.add(new ChatUI.Log(Resource.getLocString(Resource.BUNDLE_LABEL, "System")));
        opts = add(new OptWnd());
        opts.hide();
        zerg = add(new Zergwnd(), 187, 50);
        zerg.hide();

        timerswnd = new TimersWnd(this);
        timerswnd.hide();
        add(timerswnd, new Coord(HavenPanel.w / 2 - timerswnd.sz.x / 2, 100));

        quickslots = new QuickSlotsWdg();
        if (!Config.quickslots)
            quickslots.hide();
        add(quickslots, Utils.getprefc("quickslotsc", new Coord(430, HavenPanel.h - 160)));

        statuswindow = new StatusWdg();
        if (!Config.statuswdgvisible)
            statuswindow.hide();
        add(statuswindow, new Coord(HavenPanel.w / 2 + 80, 10));

        if (!chrid.equals("")) {
            Utils.loadprefchklist("boulderssel_" + chrid, Config.boulders);
            Utils.loadprefchklist("bushessel_" + chrid, Config.bushes);
            Utils.loadprefchklist("treessel_" + chrid, Config.trees);
            Utils.loadprefchklist("iconssel_" + chrid, Config.icons);
            opts.setMapSettings();
        }

        fbelt = new FBelt(chrid, Utils.getprefb("fbelt_vertical", true));
        fbelt.load();
        add(fbelt, Utils.getprefc("fbelt_c", new Coord(20, 200)));
        if (!Config.fbelt)
            fbelt.hide();

        histbelt = new CraftHistoryBelt(Utils.getprefb("histbelt_vertical", true));
        add(histbelt, Utils.getprefc("histbelt_c", new Coord(70, 200)));
        if (!Config.histbelt)
            histbelt.hide();
    }

    /* Ice cream */
    private final IButton[] fold_br = new IButton[4];

    private void updfold(boolean reset) {
        int br;
        if (brpanel.tvis && menupanel.tvis)
            br = 0;
        else if (brpanel.tvis && !menupanel.tvis)
            br = 1;
        else if (!brpanel.tvis && !menupanel.tvis)
            br = 2;
        else
            br = 3;
        for (int i = 0; i < fold_br.length; i++)
            fold_br[i].show(i == br);

        if (reset)
            resetui();
    }

    private void foldbuttons() {
        final Tex rdnbg = Resource.loadtex("gfx/hud/rbtn-maindwn");
        final Tex rupbg = Resource.loadtex("gfx/hud/rbtn-upbg");
        fold_br[0] = new IButton("gfx/hud/rbtn-dwn", "", "-d", "-h") {
            public void draw(GOut g) {
                g.image(rdnbg, Coord.z);
                super.draw(g);
            }

            public void click() {
                menupanel.cshow(false);
                updfold(true);
            }
        };
        fold_br[1] = new IButton("gfx/hud/rbtn-dwn", "", "-d", "-h") {
            public void draw(GOut g) {
                g.image(rdnbg, Coord.z);
                super.draw(g);
            }

            public void click() {
                brpanel.cshow(false);
                updfold(true);
            }
        };
        fold_br[2] = new IButton("gfx/hud/rbtn-up", "", "-d", "-h") {
            public void draw(GOut g) {
                g.image(rupbg, Coord.z);
                super.draw(g);
            }

            public void click() {
                menupanel.cshow(true);
                updfold(true);
            }

            public void presize() {
                this.c = parent.sz.sub(this.sz);
            }
        };
        fold_br[3] = new IButton("gfx/hud/rbtn-dwn", "", "-d", "-h") {
            public void draw(GOut g) {
                g.image(rdnbg, Coord.z);
                super.draw(g);
            }

            public void click() {
                brpanel.cshow(true);
                updfold(true);
            }
        };
        menupanel.add(fold_br[0], 0, 0);
        fold_br[0].lower();
        brpanel.adda(fold_br[1], brpanel.sz.x, 32, 1, 1);
        adda(fold_br[2], 1, 1);
        fold_br[2].lower();
        menupanel.add(fold_br[3], 0, 0);
        fold_br[3].lower();

        updfold(false);
    }

    protected void added() {
        resize(parent.sz);
        ui.cons.out = new java.io.PrintWriter(new java.io.Writer() {
            StringBuilder buf = new StringBuilder();

            public void write(char[] src, int off, int len) {
                buf.append(src, off, len);
                int p;
                while ((p = buf.indexOf("\n")) >= 0) {
                    syslog.append(buf.substring(0, p), Color.WHITE);
                    buf.delete(0, p + 1);
                }
            }

            public void close() {
            }

            public void flush() {
            }
        });
        Debug.log = ui.cons.out;
        opts.c = sz.sub(opts.sz).div(2);
    }

    public class Hidepanel extends Widget {
        public final String id;
        public final Coord g;
        public final Indir<Coord> base;
        public boolean tvis;
        private double cur;

        public Hidepanel(String id, Indir<Coord> base, Coord g) {
            this.id = id;
            this.base = base;
            this.g = g;
            cur = show(tvis = Utils.getprefb(id + "-visible", true)) ? 0 : 1;
        }

        public <T extends Widget> T add(T child) {
            super.add(child);
            pack();
            if (parent != null)
                move();
            return (child);
        }

        public Coord base() {
            if (base != null) return (base.get());
            return (new Coord((g.x > 0) ? parent.sz.x : (g.x < 0) ? 0 : (parent.sz.x / 2),
                    (g.y > 0) ? parent.sz.y : (g.y < 0) ? 0 : (parent.sz.y / 2)));
        }

        public void move(double a) {
            cur = a;
            Coord c = new Coord(base());
            if (g.x < 0)
                c.x -= (int) (sz.x * a);
            else if (g.x > 0)
                c.x -= (int) (sz.x * (1 - a));
            if (g.y < 0)
                c.y -= (int) (sz.y * a);
            else if (g.y > 0)
                c.y -= (int) (sz.y * (1 - a));
            this.c = c;
        }

        public void move() {
            move(cur);
        }

        public void presize() {
            move();
        }

        public boolean mshow(final boolean vis) {
            clearanims(Anim.class);
            if (vis)
                show();
            new NormAnim(0.25) {
                final double st = cur, f = vis ? 0 : 1;

                public void ntick(double a) {
                    if ((a == 1.0) && !vis)
                        hide();
                    move(st + (Utils.smoothstep(a) * (f - st)));
                }
            };
            tvis = vis;
            updfold(false);
            return (vis);
        }

        public boolean mshow() {
            return (mshow(Utils.getprefb(id + "-visible", true)));
        }

        public boolean cshow(boolean vis) {
            Utils.setprefb(id + "-visible", vis);
            if (vis != tvis)
                mshow(vis);
            return (vis);
        }

        public void cdestroy(Widget w) {
            parent.cdestroy(w);
        }
    }

    static class Hidewnd extends Window {
        Hidewnd(Coord sz, String cap, boolean lg) {
            super(sz, cap, lg);
        }

        Hidewnd(Coord sz, String cap) {
            super(sz, cap);
        }

        public void wdgmsg(Widget sender, String msg, Object... args) {
            if ((sender == this) && msg.equals("close")) {
                this.hide();
                return;
            }
            super.wdgmsg(sender, msg, args);
        }
    }

    static class Zergwnd extends Hidewnd {
        Tabs tabs = new Tabs(Coord.z, Coord.z, this);
        final TButton kin, pol, pol2;

        class TButton extends IButton {
            Tabs.Tab tab = null;
            final Tex inv;

            TButton(String nm, boolean g) {
                super(Resource.loadimg("gfx/hud/buttons/" + nm + "u"), Resource.loadimg("gfx/hud/buttons/" + nm + "d"));
                if (g)
                    inv = Resource.loadtex("gfx/hud/buttons/" + nm + "g");
                else
                    inv = null;
            }

            public void draw(GOut g) {
                if ((tab == null) && (inv != null))
                    g.image(inv, Coord.z);
                else
                    super.draw(g);
            }

            public void click() {
                if (tab != null) {
                    tabs.showtab(tab);
                    repack();
                }
            }
        }

        Zergwnd() {
            super(Coord.z, "Kith & Kin", true);
            kin = add(new TButton("kin", false));
            kin.tooltip = Text.render("Kin");
            pol = add(new TButton("pol", true));
            pol2 = add(new TButton("rlm", true));
        }

        private void repack() {
            tabs.indpack();
            kin.c = new Coord(0, tabs.curtab.contentsz().y + 20);
            pol.c = new Coord(kin.c.x + kin.sz.x + 10, kin.c.y);
            pol2.c = new Coord(pol.c.x + pol.sz.x + 10, pol.c.y);
            this.pack();
        }

        Tabs.Tab ntab(Widget ch, TButton btn) {
            Tabs.Tab tab = add(tabs.new Tab() {
                public void cresize(Widget ch) {
                    repack();
                }
            }, tabs.c);
            tab.add(ch, Coord.z);
            btn.tab = tab;
            repack();
            return (tab);
        }

        void dtab(TButton btn) {
            btn.tab.destroy();
            btn.tab = null;
            repack();
        }

        void addpol(Polity p) {
	        /* This isn't very nice. :( */
            TButton btn = p.cap.equals("Village")?pol:pol2;
            ntab(p, btn);
            btn.tooltip = Text.render(p.cap);
        }
    }

    public static class DraggedItem {
        public final GItem item;
        public final Coord dc;

        DraggedItem(GItem item, Coord dc) {
            this.item = item;
            this.dc = dc;
        }
    }

    private void updhand() {
        if ((hand.isEmpty() && (vhand != null)) || ((vhand != null) && !hand.contains(vhand.item))) {
            ui.destroy(vhand);
            vhand = null;
            if (ui.modshift && ui.keycode == KeyEvent.VK_Z && map.lastinterpc != null)
                updhanddestroyed = true;
        }
        if (!hand.isEmpty() && (vhand == null)) {
            DraggedItem fi = hand.iterator().next();
            vhand = add(new ItemDrag(fi.dc, fi.item));
            if (ui.modshift && ui.keycode == KeyEvent.VK_Z && updhanddestroyed) {
                map.iteminteractreplay();
                updhanddestroyed = false;
            }
        }
    }

    public void addchild(Widget child, Object... args) {
        String place = ((String) args[0]).intern();
        if (place == "mapview") {
            child.resize(sz);
            map = add((MapView) child, Coord.z);
            map.lower();
            map.glob.gui = this;
            if (minimapWnd != null)
                ui.destroy(minimapWnd);
            minimapWnd = minimap();
            if (Config.enabletracking && menu != null && !trackon) {
                menu.wdgmsg("act", new Object[]{"tracking"});
                trackautotgld = true;
            }
            if (Config.enablecrime && menu != null && !crimeon) {
                crimeautotgld = true;
                menu.wdgmsg("act", new Object[]{"crime"});
            }

            if (trackon) {
                buffs.addchild(new BuffToggle("track", Bufflist.bufftrack));
                errornosfx("Tracking is now turned on.");
            }
            if (crimeon) {
                buffs.addchild(new BuffToggle("crime", Bufflist.buffcrime));
                errornosfx("Criminal acts are now turned on.");
            }
            if (swimon) {
                buffs.addchild(new BuffToggle("swim", Bufflist.buffswim));
                errornosfx("Swimming is now turned on.");
            }
        } else if (place == "fight") {
            fv = urpanel.add((Fightview) child, 0, 0);
        } else if (place == "fsess") {
            add(child, Coord.z);
        } else if (place == "inv") {
            invwnd = new Hidewnd(Coord.z, "Inventory") {
                public void cresize(Widget ch) {
                    pack();
                }
            };
            invwnd.add(maininv = (Inventory) child, Coord.z);
            invwnd.pack();
            invwnd.show(Config.showinvonlogin);
            add(invwnd, new Coord(100, 100));
        } else if (place == "equ") {
            equwnd = new Hidewnd(Coord.z, "Equipment");
            equwnd.add(child, Coord.z);
            equwnd.pack();
            equwnd.hide();
            add(equwnd, new Coord(400, 10));
        } else if (place == "hand") {
            GItem g = add((GItem) child);
            Coord lc = (Coord) args[1];
            hand.add(new DraggedItem(g, lc));
            updhand();
        } else if (place == "chr") {
            chrwdg = add((CharWnd) child, new Coord(300, 50));
            chrwdg.hide();
        } else if (place == "craft") {
            final Widget mkwdg = child;
            makewnd = new Window(Coord.z, "Crafting", true) {
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if ((sender == this) && msg.equals("close")) {
                        mkwdg.wdgmsg("close");
                        return;
                    }
                    super.wdgmsg(sender, msg, args);
                }

                public void cdestroy(Widget w) {
                    if (w == mkwdg) {
                        ui.destroy(this);
                        makewnd = null;
                    }
                }
            };
            makewnd.add(mkwdg, Coord.z);
            makewnd.pack();
            add(makewnd, new Coord(400, 200));
        } else if (place == "buddy") {
            zerg.ntab(buddies = (BuddyWnd) child, zerg.kin);
        } else if (place == "pol") {
            Polity p = (Polity)child;
            polities.add(p);
            zerg.addpol(p);
        } else if (place == "chat") {
            ChatUI.Channel prevchannel = chat.sel;
            chat.addchild(child);
            if (prevchannel != null && chat.sel.cb == null) {
                chat.select(prevchannel);
            }
        } else if (place == "party") {
            add(child, 10, 95);
        } else if (place == "meter") {
            int x = (meters.size() % 3) * (IMeter.fsz.x + 5);
            int y = (meters.size() / 3) * (IMeter.fsz.y + 2);
            ulpanel.add(child, portrait.c.x + portrait.sz.x + 10 + x, portrait.c.y + y);
            meters.add(child);
        } else if (place == "buff") {
            buffs.addchild(child);
        } else if (place == "qq") {
            if (qqview != null)
                qqview.reqdestroy();
            final Widget cref = qqview = child;
            questpanel = new AlignPanel() {
                {
                    add(cref);
                }

                protected Coord getc() {
                    return (new Coord(10, GameUI.this.sz.y - chat.sz.y - beltwdg.sz.y - this.sz.y - 10));
                }

                public void cdestroy(Widget ch) {
                    qqview = null;
                    destroy();
                }
            };
            if (Config.noquests)
                questpanel.hide();

            add(questpanel);
        } else if (place == "misc") {
            add(child, (Coord) args[1]);
        } else {
            throw (new UI.UIException("Illegal gameui child", place, args));
        }
    }

    private MinimapWnd minimap() {
        Coord mwsz = Utils.getprefc("mmapwndsz", new Coord(290, 310));
        minimapWnd = new MinimapWnd(mwsz, map);
        add(minimapWnd, Utils.getprefc("mmapc", new Coord(10, 100)));
        return minimapWnd;
    }

    public void cdestroy(Widget w) {
        if (w instanceof GItem) {
            for (Iterator<DraggedItem> i = hand.iterator(); i.hasNext(); ) {
                DraggedItem di = i.next();
                if (di.item == w) {
                    i.remove();
                    updhand();
                }
            }
        } else if (polities.contains(w)) {
            polities.remove(w);
            zerg.dtab(zerg.pol);
        } else if (w == chrwdg) {
            chrwdg = null;
        }
        meters.remove(w);
    }

    private static final Resource.Anim progt = Resource.local().loadwait("gfx/hud/prog").layer(Resource.animc);
    private Tex curprog = null;
    private int curprogf, curprogb;

    private void drawprog(GOut g, double prog) {
        int fr = Utils.clip((int) Math.floor(prog * progt.f.length), 0, progt.f.length - 2);
        int bf = Utils.clip((int) (((prog * progt.f.length) - fr) * 255), 0, 255);
        if ((curprog == null) || (curprogf != fr) || (curprogb != bf)) {
            if (curprog != null)
                curprog.dispose();
            WritableRaster buf = PUtils.imgraster(progt.f[fr][0].sz);
            PUtils.blit(buf, progt.f[fr][0].img.getRaster(), Coord.z);
            PUtils.blendblit(buf, progt.f[fr + 1][0].img.getRaster(), Coord.z, bf);
            curprog = new TexI(PUtils.rasterimg(buf));
            curprogf = fr;
            curprogb = bf;
        }
        g.aimage(curprog, new Coord(sz.x / 2, (sz.y * 4) / 10), 0.5, 0.5);

        if (Config.showprogressperc)
            g.atextstroked((int) (prog * 100) + "%", (sz.y * 4) / 10 - curprog.sz().y / 2 + 1, Color.WHITE, Color.BLACK, progressf);
    }

    public void draw(GOut g) {
        beltwdg.c = new Coord(chat.c.x, Math.min(chat.c.y - beltwdg.sz.y + 4, sz.y - beltwdg.sz.y));
        super.draw(g);
        if (prog >= 0)
            drawprog(g, prog);
        int by = sz.y;
        if (chat.visible)
            by = Math.min(by, chat.c.y);
        if (beltwdg.visible)
            by = Math.min(by, beltwdg.c.y);
        if (cmdline != null) {
            drawcmd(g, new Coord(blpw + 10, by -= 20));
        } else if (lastmsg != null) {
            if ((System.currentTimeMillis() - msgtime) > 3000) {
                lastmsg = null;
            } else {
                g.chcolor(0, 0, 0, 192);
                g.frect(new Coord(blpw + 8, by - 22), lastmsg.sz().add(4, 4));
                g.chcolor();
                g.image(lastmsg.tex(), new Coord(blpw + 10, by -= 20));
            }
        }
        if (!chat.visible) {
            chat.drawsmall(g, new Coord(blpw + 10, by), 50);
        }
    }

    public void tick(double dt) {
        super.tick(dt);
        if (!afk && (System.currentTimeMillis() - ui.lastevent > 300000)) {
            afk = true;
            wdgmsg("afk");
        } else if (afk && (System.currentTimeMillis() - ui.lastevent < 300000)) {
            afk = false;
        }
    }

    private void togglebuff(String err, String name, Resource res) {
        if (err.endsWith("on.") && buffs.gettoggle(name) == null) {
            buffs.addchild(new BuffToggle(name, res));
            if (name.equals("swim"))
                swimon = true;
            else if (name.equals("crime"))
                crimeon = true;
            else if (name.equals("track"))
                trackon = true;
        } else if (err.endsWith("off.")) {
            BuffToggle tgl = buffs.gettoggle(name);
            if (tgl != null)
                tgl.reqdestroy();
            if (name.equals("swim"))
                swimon = true;
            else if (name.equals("crime"))
                crimeon = false;
            else if (name.equals("track"))
                trackon = false;
        }
    }

    public void uimsg(String msg, Object... args) {
        if (msg == "err") {
            String err = (String) args[0];
            if (err.startsWith("Swimming is now turned")) {
                togglebuff(err, "swim", Bufflist.buffswim);
            } else if (err.startsWith("Tracking is now turned")) {
                togglebuff(err, "track", Bufflist.bufftrack);
                if (trackautotgld) {
                    errornosfx(err);
                    trackautotgld = false;
                    return;
                }
            } else if (err.startsWith("Criminal acts are now turned")) {
                togglebuff(err, "crime", Bufflist.buffcrime);
                if (crimeautotgld) {
                    errornosfx(err);
                    crimeautotgld = false;
                    return;
                }
            }
            error(err);
        } else if (msg == "msg") {
            String text = (String) args[0];
            msg(text);
        } else if (msg == "prog") {
            if (args.length > 0)
                prog = ((Number) args[0]).doubleValue() / 100.0;
            else
                prog = -1;
        } else if (msg == "setbelt") {
            int slot = (Integer) args[0];
            if (args.length < 2) {
                belt[slot] = null;
            } else {
                belt[slot] = ui.sess.getres((Integer) args[1]);
            }
        } else if (msg == "polowner") {
            int id = (Integer)args[0];
            String o = (String)args[1];
            boolean n = ((Integer)args[2]) != 0;
            if(o != null)
                o = o.intern();
            String cur = polowners.get(id);
            if(map != null) {
                if((o != null) && (cur == null)) {
                    map.setpoltext(id, "Entering " + o);
                } else if((o == null) && (cur != null)) {
                    map.setpoltext(id, "Leaving " + cur);
                }
            }
            polowners.put(id, o);
        } else if (msg == "showhelp") {
            Indir<Resource> res = ui.sess.getres((Integer) args[0]);
            if (help == null)
                help = adda(new HelpWnd(res), 0.5, 0.5);
            else
                help.res = res;
        } else {
            super.uimsg(msg, args);
        }
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == menu) {
            wdgmsg(msg, args);
            return;
        } else if ((sender == chrwdg) && (msg == "close")) {
            chrwdg.hide();
        } else if((polities.contains(sender)) && (msg == "close")) {
            sender.hide();
        } else if ((sender == help) && (msg == "close")) {
            ui.destroy(help);
            help = null;
            return;
        }
        super.wdgmsg(sender, msg, args);
    }

    private void fitwdg(Widget wdg) {
        if (wdg.c.x < 0)
            wdg.c.x = 0;
        if (wdg.c.y < 0)
            wdg.c.y = 0;
        if (wdg.c.x + wdg.sz.x > sz.x)
            wdg.c.x = sz.x - wdg.sz.x;
        if (wdg.c.y + wdg.sz.y > sz.y)
            wdg.c.y = sz.y - wdg.sz.y;
    }

    public static class MenuButton extends IButton {
        private final int gkey;

        MenuButton(String base, int gkey, String tooltip) {
            super("gfx/hud/" + base, "", "-d", "-h");
            this.gkey = (char) gkey;
            this.tooltip = RichText.render(tooltip, 0);
        }

        public void click() {
        }

        public boolean globtype(char key, KeyEvent ev) {
            if (Config.agroclosest && key == 9)
                return super.globtype(key, ev);

            // ctrl + tab is used to rotate opponents
            if (key == 9 && ev.isControlDown())
                return true;

            if (key == gkey) {
                click();
                return (true);
            }
            return (super.globtype(key, ev));
        }
    }

    private static final Tex menubg = Resource.loadtex("gfx/hud/rbtn-bg");

    public class MainMenu extends Widget {
        public MainMenu() {
            super(menubg.sz());
            add(new MenuButton("rbtn-inv", 9, Resource.getLocString(Resource.BUNDLE_LABEL, "Inventory ($col[255,255,0]{Tab})")) {
                public void click() {
                    if ((invwnd != null) && invwnd.show(!invwnd.visible)) {
                        invwnd.raise();
                        fitwdg(invwnd);
                    }
                }
            }, 0, 0);
            add(new MenuButton("rbtn-equ", 5, Resource.getLocString(Resource.BUNDLE_LABEL, "Equipment ($col[255,255,0]{Ctrl+E})")) {
                public void click() {
                    if ((equwnd != null) && equwnd.show(!equwnd.visible)) {
                        equwnd.raise();
                        fitwdg(equwnd);
                    }
                }
            }, 0, 0);
            add(new MenuButton("rbtn-chr", 20, Resource.getLocString(Resource.BUNDLE_LABEL, "Character Sheet ($col[255,255,0]{Ctrl+T})")) {
                public void click() {
                    if ((chrwdg != null) && chrwdg.show(!chrwdg.visible)) {
                        chrwdg.raise();
                        fitwdg(chrwdg);
                    }
                }
            }, 0, 0);
            add(new MenuButton("rbtn-bud", 2, Resource.getLocString(Resource.BUNDLE_LABEL, "Kith & Kin ($col[255,255,0]{Ctrl+B})")) {
                public void click() {
                    if (zerg.show(!zerg.visible)) {
                        zerg.raise();
                        fitwdg(zerg);
                        setfocus(zerg);
                    }
                }
            }, 0, 0);
            add(new MenuButton("rbtn-opt", 15, Resource.getLocString(Resource.BUNDLE_LABEL, "Options ($col[255,255,0]{Ctrl+O})")) {
                public void click() {
                    if (opts.show(!opts.visible)) {
                        opts.raise();
                        fitwdg(opts);
                        setfocus(opts);
                    }
                }
            }, 0, 0);
        }

        public void draw(GOut g) {
            g.image(menubg, Coord.z);
            super.draw(g);
        }
    }

    public boolean globtype(char key, KeyEvent ev) {
        if (key == ':') {
            entercmd();
            return (true);
        } else if (ev.isShiftDown() && ev.getKeyCode() == KeyEvent.VK_DELETE) {
            toggleui();
            return (true);
        } else if (key == 3) {
            if (chat.visible && !chat.hasfocus) {
                setfocus(chat);
            } else {
                if (chat.targeth == 0) {
                    chat.sresize(chat.savedh);
                    setfocus(chat);
                } else {
                    chat.sresize(0);
                }
            }
            Utils.setprefb("chatvis", chat.targeth != 0);
        } else if ((key == 27) && (map != null) && !map.hasfocus) {
            setfocus(map);
            return (true);
        } else if (key == 17 /*ctrl+q*/) {
            timerswnd.show(!timerswnd.visible);
            timerswnd.raise();
            return true;
        } else if (ev.isControlDown() && ev.getKeyCode() == KeyEvent.VK_G) {
            if (map != null)
                map.togglegrid();
            return true;
        } else if (ev.isControlDown() && ev.getKeyCode() == KeyEvent.VK_M) {
            boolean curstatus = statuswindow.visible;
            statuswindow.show(!curstatus);
            Utils.setprefb("statuswdgvisible", !curstatus);
            Config.statuswdgvisible = !curstatus;
            return true;
        } else if (ev.isAltDown() && ev.getKeyCode() == KeyEvent.VK_Z) {
            quickslots.drop(QuickSlotsWdg.lc, Coord.z);
            quickslots.simulateclick(QuickSlotsWdg.lc);
            return true;
        } else if (ev.isAltDown() && ev.getKeyCode() == KeyEvent.VK_X) {
            quickslots.drop(QuickSlotsWdg.rc, Coord.z);
            quickslots.simulateclick(QuickSlotsWdg.rc);
            return true;
        } else if (ev.isAltDown() && ev.getKeyCode() == KeyEvent.VK_S) {
            HavenPanel.needtotakescreenshot = true;
            return true;
        } else if (ev.isControlDown() && ev.getKeyCode() == KeyEvent.VK_H) {
            Config.hidegobs = !Config.hidegobs;
            Utils.setprefb("hidegobs", Config.hidegobs);
            if (map != null)
                map.refreshGobsHidable();
            return true;
        } else if (ev.getKeyCode() == KeyEvent.VK_TAB && Config.agroclosest) {
            if (map != null)
                map.aggroclosest();
            return true;
        } else if (ev.isShiftDown() && ev.getKeyCode() == KeyEvent.VK_I) {
            Config.resinfo = !Config.resinfo;
            Utils.setprefb("resinfo", Config.resinfo);
            msg("Resource info on shift/shift+ctrl is now turned " + (Config.resinfo ? "on" : "off"), Color.WHITE);
            return true;
        } else if (ev.isShiftDown() && ev.getKeyCode() == KeyEvent.VK_B) {
            Config.showboundingboxes = !Config.showboundingboxes;
            Utils.setprefb("showboundingboxes", Config.showboundingboxes);
            if (map != null)
                map.refreshGobsAll();
            return true;
        } else if (ev.isControlDown() && ev.getKeyCode() == KeyEvent.VK_Z) {
            Config.pf = !Config.pf;
            msg("Pathfinding is now turned " + (Config.pf ? "on" : "off"), Color.WHITE);
            return true;
        } else if (ev.isControlDown() && ev.getKeyCode() == KeyEvent.VK_N) {
            Config.daylight = !Config.daylight;
            Utils.setprefb("daylight", Config.daylight);
        } else if (ev.isControlDown() && ev.getKeyCode() == KeyEvent.VK_P) {
            Config.showplantgrowstage = !Config.showplantgrowstage;
            Utils.setprefb("showplantgrowstage", Config.showplantgrowstage);
            if (!Config.showplantgrowstage && map != null)
                map.removeCustomSprites(Sprite.GROWTH_STAGE_ID);
            if (map != null)
                map.refreshGobsGrowthStages();
        } else if (ev.isControlDown() && ev.getKeyCode() == KeyEvent.VK_X) {
            Config.tilecenter = !Config.tilecenter;
            Utils.setprefb("tilecenter", Config.tilecenter);
            msg("Tile centering is now turned " + (Config.tilecenter ? "on." : "off."), Color.WHITE);
        } else if (ev.isControlDown() && ev.getKeyCode() == KeyEvent.VK_D) {
            Config.showminerad = !Config.showminerad;
            Utils.setprefb("showminerad", Config.showminerad);
            return true;
        } else if (ev.isShiftDown() && ev.getKeyCode() == KeyEvent.VK_D) {
            Config.showfarmrad = !Config.showfarmrad;
            Utils.setprefb("showfarmrad", Config.showfarmrad);
            return true;
        } else if (!Config.disabledrinkhotkey && (ev.getKeyCode() == KeyEvent.VK_BACK_QUOTE || (Config.iswindows && Utils.getScancode(ev) == 41))) {
            synchronized (ui.fmAutoSelName) {
                ui.fmAutoSelName = "Drink";
                ui.fmAutoTime = System.currentTimeMillis();
            }
            maininv.drink(100);
            return true;
        }

        return (super.globtype(key, ev));
    }

    public boolean mousedown(Coord c, int button) {
        return (super.mousedown(c, button));
    }

    private boolean uishowing = true;

    // TODO: toggle chat, betls, and minimap visibility as well
    public void toggleui() {
        Hidepanel[] panels = {brpanel, ulpanel, umpanel, urpanel, menupanel};
        uishowing = !uishowing;
        if (uishowing) {
            for (Hidepanel p : panels)
                p.mshow(true);
        } else {
            for (Hidepanel p : panels)
                p.mshow(false);
        }
    }

    public void resetui() {
        Hidepanel[] panels = {brpanel, ulpanel, umpanel, urpanel, menupanel};
        for (Hidepanel p : panels)
            p.cshow(p.tvis);
        uishowing = true;
    }

    public void resize(Coord sz) {
        this.sz = sz;
        chat.resize(Config.chatsz.equals(Coord.z) ? new Coord(sz.x - brpw, 111) : Config.chatsz);
        chat.move(new Coord(0, sz.y));
        if (!Utils.getprefb("chatvis", true))
            chat.sresize(0);
        if (map != null)
            map.resize(sz);
        beltwdg.c = new Coord(blpw + 10, sz.y - beltwdg.sz.y - 5);
        statuswindow.c = new Coord(HavenPanel.w / 2 + 80, 10);
        super.resize(sz);
    }

    public void presize() {
        resize(parent.sz);
    }

    public void msg(String msg, Color color, Color logcol) {
        msgtime = System.currentTimeMillis();
        msg = Resource.getLocString(Resource.BUNDLE_MSG, msg);
        lastmsg = msgfoundry.render(msg, color);
        syslog.append(msg, logcol);
        if (color == Color.WHITE)
            Audio.play(msgsfx);
    }

    public void msg(String msg, Color color) {
        msg(msg, color, color);
    }

    private static final Resource errsfx = Resource.local().loadwait("sfx/error");
    private static final Resource msgsfx = Resource.local().loadwait("sfx/msg");

    public void error(String msg) {
        msg(msg, new Color(192, 0, 0), new Color(255, 0, 0));
        Audio.play(errsfx);
        if (errmsgcb != null)
            errmsgcb.notifyErrMsg(msg);
    }

    public void errornosfx(String msg) {
        msg(msg, new Color(192, 0, 0), new Color(255, 0, 0));
    }

    public void msg(String msg) {
        Matcher m = esvMsgPattern.matcher(msg);
        if (m.find()) {
            int e = Integer.parseInt(m.group(1));
            int s = Integer.parseInt(m.group(2));
            int v = Integer.parseInt(m.group(3));

            double avg;
            switch (Config.avgmode) {
                case AVG_MODE_QUADRATIC:
                    avg =  Math.sqrt((e * e + s * s + v * v) / 3.0);
                    break;
                case AVG_MODE_GEOMETRIC:
                    avg =  Math.pow(e * s * v, 1.0 / 3.0);
                    break;
                default:
                    avg =  (e + s + v) / 3.0;
                    break;
            }
            msg += "  (Avg: " + Utils.fmt1DecPlace(avg) + ")";
        }
        msg(msg, Color.WHITE, Color.WHITE);
    }
    
    public void info(String msg, Color color) {
    	 msgtime = System.currentTimeMillis();
    	 lastmsg = msgfoundry.render(msg, color);
    	 syslog.append(msg, color);
    }

    public void act(String... args) {
        wdgmsg("act", (Object[]) args);
    }

    public void act(int mods, Coord mc, Gob gob, String... args) {
        int n = args.length;
        Object[] al = new Object[n];
        System.arraycopy(args, 0, al, 0, n);
        if (mc != null) {
            al = Utils.extend(al, al.length + 2);
            al[n++] = mods;
            al[n++] = mc;
            if (gob != null) {
                al = Utils.extend(al, al.length + 2);
                al[n++] = (int) gob.id;
                al[n++] = gob.rc;
            }
        }
        wdgmsg("act", al);
    }

    public ArrayList<Window> getwnds(String cap) {
    	ArrayList<Window> wnds = new ArrayList<Window>();
        for (Widget w = lchild; w != null; w = w.prev) {
            if (w instanceof Window) {
                Window wnd = (Window) w;
                if (wnd.cap != null && cap.equals(wnd.cap.text))
                    wnds.add(wnd);
            }
        }
        return wnds;
    }
    
    public Window getwnd(String cap) {
        for (Widget w = lchild; w != null; w = w.prev) {
            if (w instanceof Window) {
                Window wnd = (Window) w;
                if (wnd.cap != null && cap.equals(wnd.origcap))
                    return wnd;
            }
        }
        return null;
    }


    private static final int WND_WAIT_SLEEP = 8;
    public Window waitfForWnd(String cap, int timeout) {
        int t  = 0;
        while (t < timeout) {
            Window wnd = getwnd(cap);
            if (wnd != null)
                return wnd;
            t += WND_WAIT_SLEEP;
            try {
                Thread.sleep(WND_WAIT_SLEEP);
            } catch (InterruptedException e) {
                return null;
            }
        }
        return null;
    }

    public List<IMeter.Meter> getmeters(String name) {
        for (Widget meter : meters) {
            if (meter instanceof IMeter) {
                IMeter im = (IMeter) meter;
                try {
                    Resource res = im.bg.get();
                    if (res != null && res.basename().equals(name))
                        return im.meters;
                } catch (Loading l) {
                }
            }
        }
        return null;
    }

    public IMeter.Meter getmeter(String name, int midx) {
        List<IMeter.Meter> meters = getmeters(name);
        if (meters != null && midx < meters.size())
            return meters.get(midx);
        return null;
    }

    public Equipory getequipory() {
        if (equwnd != null) {
            for (Widget w = equwnd.lchild; w != null; w = w.prev) {
                if (w instanceof Equipory)
                    return (Equipory) w;
            }
        }
        return null;
    }

    private static final Tex nkeybg = Resource.loadtex("gfx/hud/hb-main");

    public class NKeyBelt extends Belt implements DTarget, DropTarget {
        public int curbelt = 0;
        final Coord pagoff = new Coord(5, 25);

        public NKeyBelt() {
            super(nkeybg.sz());
            adda(new IButton("gfx/hud/hb-btn-chat", "", "-d", "-h") {
                Tex glow;

                {
                    this.tooltip = RichText.render(Resource.getLocString(Resource.BUNDLE_LABEL, "Chat ($col[255,255,0]{Ctrl+C})"), 0);
                    glow = new TexI(PUtils.rasterimg(PUtils.blurmask(up.getRaster(), 2, 2, Color.WHITE)));
                }

                public void click() {
                    if (chat.targeth == 0) {
                        chat.sresize(chat.savedh);
                        setfocus(chat);
                    } else {
                        chat.sresize(0);
                    }
                    Utils.setprefb("chatvis", chat.targeth != 0);
                }

                public void draw(GOut g) {
                    super.draw(g);
                    Color urg = chat.urgcols[chat.urgency];
                    if (urg != null) {
                        GOut g2 = g.reclipl(new Coord(-2, -2), g.sz.add(4, 4));
                        g2.chcolor(urg.getRed(), urg.getGreen(), urg.getBlue(), 128);
                        g2.image(glow, Coord.z);
                    }
                }
            }, sz, 1, 1);
        }

        private Coord beltc(int i) {
            return (pagoff.add(((invsq.sz().x + 2) * i) + (10 * (i / 5)), 0));
        }

        private int beltslot(Coord c) {
            for (int i = 0; i < 10; i++) {
                if (c.isect(beltc(i), invsq.sz()))
                    return (i + (curbelt * 12));
            }
            return (-1);
        }

        public void draw(GOut g) {
            g.image(nkeybg, Coord.z);
            for (int i = 0; i < 10; i++) {
                int slot = i + (curbelt * 12);
                Coord c = beltc(i);
                g.image(invsq, beltc(i));
                try {
                    if (belt[slot] != null)
                        g.image(belt[slot].get().layer(Resource.imgc).tex(), c.add(1, 1));
                } catch (Loading e) {
                }
                g.chcolor(156, 180, 158, 255);
                FastText.aprintf(g, c.add(invsq.sz().sub(2, 0)), 1, 1, "%d", (i + 1) % 10);
                g.chcolor();
            }
            super.draw(g);
        }

        public boolean mousedown(Coord c, int button) {
            int slot = beltslot(c);
            if (slot != -1) {
                if (button == 1)
                    GameUI.this.wdgmsg("belt", slot, 1, ui.modflags());
                if (button == 3)
                    GameUI.this.wdgmsg("setbelt", slot, 1);
                return (true);
            }
            return (super.mousedown(c, button));
        }

        public boolean globtype(char key, KeyEvent ev) {
            if(key != 0)
                return(false);
            int c = ev.getKeyCode();
            if((c < KeyEvent.VK_0) || (c > KeyEvent.VK_9))
                return (false);

            int i = Utils.floormod(c - KeyEvent.VK_0 - 1, 10);
            boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
            if (M) {
                curbelt = i;
            } else {
                keyact(i + (curbelt * 12));
            }
            return (true);
        }

        public boolean drop(Coord c, Coord ul) {
            int slot = beltslot(c);
            if (slot != -1) {
                GameUI.this.wdgmsg("setbelt", slot, 0);
                return (true);
            }
            return (false);
        }

        public boolean iteminteract(Coord c, Coord ul) {
            return (false);
        }

        public boolean dropthing(Coord c, Object thing) {
            int slot = beltslot(c);
            if (slot != -1) {
                if (thing instanceof Resource) {
                    Resource res = (Resource) thing;
                    if (res.layer(Resource.action) != null) {
                        GameUI.this.wdgmsg("setbelt", slot, res.name);
                        return (true);
                    }
                }
            }
            return (false);
        }
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
        cmdmap.put("afk", new Console.Command() {
            public void run(Console cons, String[] args) {
                afk = true;
                wdgmsg("afk");
            }
        });
        cmdmap.put("act", new Console.Command() {
            public void run(Console cons, String[] args) {
                Object[] ad = new Object[args.length - 1];
                System.arraycopy(args, 1, ad, 0, ad.length);
                wdgmsg("act", ad);
            }
        });
        cmdmap.put("tool", new Console.Command() {
            public void run(Console cons, String[] args) {
                add(gettype(args[1]).create(GameUI.this, new Object[0]), 200, 200);
            }
        });
    }

    public Map<String, Console.Command> findcmds() {
        return (cmdmap);
    }

    public void registerErrMsg(ErrorSysMsgCallback callback) {
        this.errmsgcb = callback;
    }
}
