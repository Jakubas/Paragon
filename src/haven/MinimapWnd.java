package haven;

import java.awt.event.KeyEvent;

public class MinimapWnd extends Window {

    private Widget mmap;
    private  IButton pclaim, vclaim;
    private boolean minimized;
    private Coord szr;

    public MinimapWnd(Coord sz, Widget mmap, IButton pclaim, IButton vclaim) {
        super(sz, "Map");
        this.mmap = mmap;
        this.pclaim = pclaim;
        this.vclaim = vclaim;
        this.c = Coord.z;
    }

    public boolean mousedown(Coord c, int button) {
        if(!minimized) {
            parent.setfocus(this);
            raise();
        }
        return super.mousedown(c, button);
    }

    public boolean mouseup(Coord c, int button) {
        if (ismousegrab()) {
            Utils.setprefc("mmapc", this.c);
        }
        super.mouseup(c, button);
        return (true);
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
        } else {
            mmap.show();
            pclaim.show();
            vclaim.show();
        }

        if (minimized) {
            szr = asz;
            resize(new Coord(asz.x, 0));
        } else {
            resize(szr);
        }
    }
}