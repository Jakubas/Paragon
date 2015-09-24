package haven;


import java.awt.*;

public class QuickSlotsWdg extends Widget implements DTarget {
    private static final Color brdr = new Color(28, 35, 24);
    private static final Color bg = new Color(40, 50, 34);
    private static final Coord slotsz = new Coord(44, 44);
    private static final Coord itemsz = new Coord(42, 42);
    private static final Coord lc = Coord.z;
    private static final Coord rc = new Coord(50, 0);
    private UI.Grab dragging;
    private Coord dc;

    public QuickSlotsWdg() {
        super(new Coord(44 + 44 + 6, 44));

    }

    @Override
    public void draw(GOut g) {
        Equipory e = getequipory();
        if (e != null) {
            g.chcolor(brdr);
            g.rect(lc, slotsz);
            g.chcolor(bg);
            g.frect(lc.add(1, 1), itemsz);
            WItem left = e.quickslots[6];
            if (left != null)
                drawitem(g.reclipl(lc.add(6, 6), g.sz), left);

            g.chcolor(brdr);
            g.rect(rc, slotsz);
            g.chcolor(bg);
            g.frect(rc.add(1, 1), itemsz);
            WItem right = e.quickslots[7];
            if (right != null)
                drawitem(g.reclipl(rc.add(6, 6), g.sz), right);
        }
    }

    private void drawitem(GOut g, WItem witem) {
        GItem item = witem.item;
        GSprite spr = item.spr();
        if (spr != null) {
            Coord sz = spr.sz();
            g.defstate();
            witem.drawmain(g, spr);
            g.defstate();
            if (item.num >= 0)
                g.atext(Integer.toString(item.num), sz, 1, 1);
            else if (witem.itemnum.get() != null)
                g.aimage(witem.itemnum.get(), sz, 1, 1);
        } else {
            g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, sz);
        }
    }

    @Override
    public boolean drop(Coord cc, Coord ul) {
        Equipory e = getequipory();
        if (e != null) {
            e.wdgmsg("drop", cc.x <= 47 ? 6 : 7);
            return true;
        }
        return false;
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        Equipory e = getequipory();
        if (e != null) {
            WItem w = e.quickslots[cc.x <= 47 ? 6 : 7];
            if (w != null) {
                return w.iteminteract(cc, ul);
            }
        }
        return false;
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if (button == 1 && c.x > 44 && c.x < 50) {
            dragging = ui.grabmouse(this);
            dc = c;
            return true;
        }
        dragging = null;
        Equipory e = getequipory();
        if (e != null) {
            WItem w = e.quickslots[c.x <= 47 ? 6 : 7];
            if (w != null) {
                w.mousedown(new Coord(w.sz.x / 2, w.sz.y / 2), button);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        if (dragging != null) {
            dragging.remove();
            dragging = null;
            Utils.setprefc("quickslotsc", this.c);
            return true;
        }
        return super.mouseup(c, button);
    }

    @Override
    public void mousemove(Coord c) {
        if (dragging != null) {
            this.c = this.c.add(c.x, c.y).sub(dc);
            return;
        }
        super.mousemove(c);
    }

    private Equipory getequipory() {
        Window e = ((GameUI) parent).equwnd;
        if (e != null) {
            for (Widget w = e.lchild; w != null; w = w.prev) {
                if (w instanceof Equipory)
                    return (Equipory) w;
            }
        }
        return null;
    }
}