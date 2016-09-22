package haven;

public class WidgetDummy extends Widget {

    public WidgetDummy() {
        super(Coord.z);
        super.visible = false;
    }

    @Override
    public void uimsg(String msg, Object... args) {
        if (msg == "cancel") {
            ui.destroy(WidgetDummy.this);
            return;
        }
        super.uimsg(msg, args);
    }


    @Override
    public boolean mousedown(Coord c, int button) {
        return false;
    }

    @Override
    public void draw(GOut g) {
        return;
    }

    @Override
    public boolean keydown(java.awt.event.KeyEvent ev) {
        return false;
    }

    @Override
    public boolean type(char key, java.awt.event.KeyEvent ev) {
        return false;
    }
}
