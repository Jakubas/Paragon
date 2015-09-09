package haven;


import java.util.List;

public class TimersWnd extends Window {
    public final GameUI gui;
    public static final int width = 460;

    public TimersWnd(final GameUI gui) {
        super(Coord.z, "Timers");
        this.gui = gui;

        Button btna = new Button(50, "Add") {
            public void click() {
                parent.parent.add(new TimerEditWnd("Create New Timer", gui), new Coord(gui.sz.x / 2 - 200, gui.sz.y / 2 - 200));
            }
        };
        add(btna, new Coord(20, 10));

        CheckBox chkalarm = new CheckBox("Sound Alarm") {
            {
                a = Config.timersalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("timersalarm", val);
                Config.timersalarm = val;
                a = val;
            }
        };
        add(chkalarm, new Coord(350, 15));


        List<TimerWdg> timers = Glob.timersThread.getall();
        for (int i = 0; i < timers.size(); i++) {
            TimerWdg timer = timers.get(i);
            // add(timer, new Coord(20, tsy + (i * 24) + (i*TimerWdg.vspace)));
        }

        resize(width, timers.size() * TimerWdg.height + 60);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            reqdestroy();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public boolean type(char key, java.awt.event.KeyEvent ev) {
        if (key == 27) {
            reqdestroy();
            return true;
        }
        return super.type(key, ev);
    }
}
