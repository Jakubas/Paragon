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


import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static haven.GItem.Quality.AVG_MODE_ARITHMETIC;
import static haven.GItem.Quality.AVG_MODE_GEOMETRIC;
import static haven.GItem.Quality.AVG_MODE_QUADRATIC;

public class OptWnd extends Window {
    public static final int VERTICAL_MARGIN = 10;
    public static final int HORIZONTAL_MARGIN = 5;
    public static final int VERTICAL_AUDIO_MARGIN = 5;
    public final Panel main, video, audio, display, map, general, combat, control, uis, quality;
    public Panel current;

    public void chpanel(Panel p) {
        if (current != null)
            current.hide();
        (current = p).show();
    }

    public class PButton extends Button {
        public final Panel tgt;
        public final int key;

        public PButton(int w, String title, int key, Panel tgt) {
            super(w, title);
            this.tgt = tgt;
            this.key = key;
        }

        public void click() {
            chpanel(tgt);
        }

        public boolean type(char key, java.awt.event.KeyEvent ev) {
            if ((this.key != -1) && (key == this.key)) {
                click();
                return (true);
            }
            return (false);
        }
    }

    public class Panel extends Widget {
        public Panel() {
            visible = false;
            c = Coord.z;
        }
    }

    public class VideoPanel extends Panel {
        public VideoPanel(Panel back) {
            super();
            add(new PButton(200, "Back", 27, back), new Coord(270, 360));
            resize(new Coord(740, 400));
        }

        public class CPanel extends Widget {
            public final GLSettings cf;

            public CPanel(GLSettings gcf) {
                this.cf = gcf;
                final WidgetVerticalAppender appender = new WidgetVerticalAppender(this);
                appender.setVerticalMargin(VERTICAL_MARGIN);
                appender.setHorizontalMargin(HORIZONTAL_MARGIN);
                appender.add(new CheckBox("Per-fragment lighting") {
                    {
                        a = cf.flight.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.flight.set(true);
                            } catch (GLSettings.SettingException e) {
                                getparent(GameUI.class).error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.flight.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new CheckBox("Render shadows") {
                    {
                        a = cf.lshadow.val;
                    }

                    public void set(boolean val) {
                        if (val) {
                            try {
                                cf.lshadow.set(true);
                            } catch (GLSettings.SettingException e) {
                                getparent(GameUI.class).error(e.getMessage());
                                return;
                            }
                        } else {
                            cf.lshadow.set(false);
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new CheckBox("Antialiasing") {
                    {
                        a = cf.fsaa.val;
                    }

                    public void set(boolean val) {
                        try {
                            cf.fsaa.set(val);
                        } catch (GLSettings.SettingException e) {
                            getparent(GameUI.class).error(e.getMessage());
                            return;
                        }
                        a = val;
                        cf.dirty = true;
                    }
                });
                appender.add(new Label("Anisotropic filtering"));
                if (cf.anisotex.max() <= 1) {
                    appender.add(new Label("(Not supported)"));
                } else {
                    final Label dpy = new Label("");
                    appender.addRow(
                            new HSlider(160, (int) (cf.anisotex.min() * 2), (int) (cf.anisotex.max() * 2), (int) (cf.anisotex.val * 2)) {
                                protected void added() {
                                    dpy();
                                }

                                void dpy() {
                                    if (val < 2)
                                        dpy.settext("Off");
                                    else
                                        dpy.settext(String.format("%.1f\u00d7", (val / 2.0)));
                                }

                                public void changed() {
                                    try {
                                        cf.anisotex.set(val / 2.0f);
                                    } catch (GLSettings.SettingException e) {
                                        getparent(GameUI.class).error(e.getMessage());
                                        return;
                                    }
                                    dpy();
                                    cf.dirty = true;
                                }
                            },
                            dpy);
                }
                appender.add(new CheckBox("Disable biome tile transitions (requires logout)") {
                    {
                        a = Config.disabletiletrans;
                    }
                    public void set(boolean val) {
                        Config.disabletiletrans = val;
                        Utils.setprefb("disabletiletrans", val);
                        a = val;
                    }
                });
                appender.add(new CheckBox("Disable flavor objects including ambient sounds") {
                    {
                        a = Config.hideflocomplete;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hideflocomplete", val);
                        Config.hideflocomplete = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Hide flavor objects but keep sounds (requires logout)") {
                    {
                        a = Config.hideflovisual;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hideflovisual", val);
                        Config.hideflovisual = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Show weather") {
                    {
                        a = Config.showweather;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("showweather", val);
                        Config.showweather = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Simple crops (req. logout)") {
                    {
                        a = Config.simplecrops;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("simplecrops", val);
                        Config.simplecrops = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Simple foragables (req. logout)") {
                    {
                        a = Config.simpleforage;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("simpleforage", val);
                        Config.simpleforage = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Hide crops") {
                    {
                        a = Config.hidecrops;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("hidecrops", val);
                        Config.hidecrops = val;
                        a = val;
                    }
                });
                appender.add(new CheckBox("Show FPS") {
                    {
                        a = Config.showfps;
                    }

                    public void set(boolean val) {
                        Utils.setprefb("showfps", val);
                        Config.showfps = val;
                        a = val;
                    }
                });

                add(new Label("Disable animations (req. restart):"), new Coord(550, 0));
                CheckListbox animlist = new CheckListbox(180, 18) {
                    protected void itemclick(CheckListboxItem itm, int button) {
                        super.itemclick(itm, button);

                        String[] selected = getselected();
                        Utils.setprefsa("disableanim", selected);

                        Config.disableanimSet.clear();
                        for (String selname : selected) {
                            for (Pair<String, String> selpair : Config.disableanim) {
                                if (selpair.a.equals(selname)) {
                                    Config.disableanimSet.add(selpair.b);
                                    break;
                                }
                            }
                        }
                    }
                };

                for (Pair<String, String> obj : Config.disableanim) {
                    boolean selected = false;
                    if (Config.disableanimSet.contains(obj.b))
                        selected = true;
                    animlist.items.add(new CheckListboxItem(obj.a, selected));
                }
                add(animlist, new Coord(550, 15));

                pack();
            }
        }

        private CPanel curcf = null;

        public void draw(GOut g) {
            if ((curcf == null) || (g.gc.pref != curcf.cf)) {
                if (curcf != null)
                    curcf.destroy();
                curcf = add(new CPanel(g.gc.pref), Coord.z);
            }
            super.draw(g);
        }
    }

    public OptWnd(boolean gopts) {
        super(new Coord(740, 400), "Options", true);

        main = add(new Panel());
        video = add(new VideoPanel(main));
        audio = add(new Panel());
        display = add(new Panel());
        map = add(new Panel());
        general = add(new Panel());
        combat = add(new Panel());
        control = add(new Panel());
        uis = add(new Panel());
        quality = add(new Panel());

        initMain(gopts);
        initAudio();
        initDisplay();
        initMap();
        initGeneral();
        initCombat();
        initControl();
        initUis();
        initQuality();

        chpanel(main);
    }

    private void initMain(boolean gopts) {
        main.add(new PButton(200, "Video settings", 'v', video), new Coord(0, 0));
        main.add(new PButton(200, "Audio settings", 'a', audio), new Coord(0, 30));
        main.add(new PButton(200, "Display settings", 'd', display), new Coord(0, 60));
        main.add(new PButton(200, "Map settings", 'm', map), new Coord(0, 90));
        main.add(new PButton(200, "General settings", 'g', general), new Coord(210, 0));
        main.add(new PButton(200, "Combat settings", 'c', combat), new Coord(210, 30));
        main.add(new PButton(200, "Control settings", 'k', control), new Coord(210, 60));
        main.add(new PButton(200, "UI settings", 'u', uis), new Coord(210, 90));
        main.add(new PButton(200, "Quality settings", 'q', quality), new Coord(420, 0));

        if (gopts) {
            main.add(new Button(200, "Switch character") {
                public void click() {
                    GameUI gui = gameui();
                    gui.act("lo", "cs");
                    if (gui != null & gui.map != null)
                        gui.map.canceltasks();
                }
            }, new Coord(270, 300));
            main.add(new Button(200, "Log out") {
                public void click() {
                    GameUI gui = gameui();
                    gui.act("lo");
                    if (gui != null & gui.map != null)
                        gui.map.canceltasks();
                }
            }, new Coord(270, 330));
        }
        main.add(new Button(200, "Close") {
            public void click() {
                OptWnd.this.hide();
            }
        }, new Coord(270, 360));
        main.pack();
    }

    private void initAudio() {
        initAudioFirstColumn();
        initAudioSecondColumn();
        audio.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        audio.pack();
    }

    private void initAudioFirstColumn() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(audio);
        appender.setVerticalMargin(0);
        appender.add(new Label("Master audio volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, (int) (Audio.volume * 1000)) {
            public void changed() {
                Audio.setvolume(val / 1000.0);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("In-game event volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (ui.audio.pos.volume * 1000);
            }

            public void changed() {
                ui.audio.pos.setvolume(val / 1000.0);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Ambient volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (ui.audio.amb.volume * 1000);
            }

            public void changed() {
                ui.audio.amb.setvolume(val / 1000.0);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on unknown players") {
            {
                a = Config.alarmunknown;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmunknown", val);
                Config.alarmunknown = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int)(Config.alarmunknownvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmunknownvol = vol;
                Utils.setprefd("alarmunknownvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on red players") {
            {
                a = Config.alarmred;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmred", val);
                Config.alarmred = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmredvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmredvol = vol;
                Utils.setprefd("alarmredvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Timers alarm volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.timersalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.timersalarmvol = vol;
                Utils.setprefd("timersalarmvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on new private/party chat") {
            {
                a = Config.chatalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("chatalarm", val);
                Config.chatalarm = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.chatalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.chatalarmvol = vol;
                Utils.setprefd("chatalarmvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm when curio finishes") {
            {
                a = Config.studyalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("studyalarm", val);
                Config.studyalarm = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.studyalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.studyalarmvol = vol;
                Utils.setprefd("studyalarmvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm when pony power < 10%") {
            {
                a = Config.ponyalarm;
            }

            public void set(boolean val) {
                Utils.setprefb("ponyalarm", val);
                Config.ponyalarm = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.ponyalarmvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.ponyalarmvol = vol;
                Utils.setprefd("ponyalarmvol", vol);
            }
        });
    }

    private void initAudioSecondColumn() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(audio);
        appender.setX(350);
        appender.setVerticalMargin(0);
        appender.add(new Label("'Chip' sound volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxchipvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxchipvol = vol;
                Utils.setprefd("sfxchipvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Quern sound volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxquernvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxquernvol = vol;
                Utils.setprefd("sfxquernvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("'Whip' sound volume"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxwhipvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxwhipvol = vol;
                Utils.setprefd("sfxwhipvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on rare curios (bluebells, glimmers, ...)") {
            {
                a = Config.alarmonforagables;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmonforagables", val);
                Config.alarmonforagables = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmonforagablesvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmonforagablesvol = vol;
                Utils.setprefd("alarmonforagablesvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on bears & lynx") {
            {
                a = Config.alarmbears;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmbears", val);
                Config.alarmbears = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmbearsvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmbearsvol = vol;
                Utils.setprefd("alarmbearsvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new Label("Fireplace sound volume (req. restart)"));
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.sfxfirevol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.sfxfirevol = vol;
                Utils.setprefd("sfxfirevol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on trolls") {
            {
                a = Config.alarmtroll;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmtroll", val);
                Config.alarmtroll = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmtrollvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmtrollvol = vol;
                Utils.setprefd("alarmtrollvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on mammoths") {
            {
                a = Config.alarmmammoth;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmmammoth", val);
                Config.alarmmammoth = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmmammothvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmmammothvol = vol;
                Utils.setprefd("alarmmammothvol", vol);
            }
        });
        appender.setVerticalMargin(0);
        appender.add(new CheckBox("Alarm on battering rams and catapults") {
            {
                a = Config.alarmbram;
            }

            public void set(boolean val) {
                Utils.setprefb("alarmbram", val);
                Config.alarmbram = val;
                a = val;
            }
        });
        appender.setVerticalMargin(VERTICAL_AUDIO_MARGIN);
        appender.add(new HSlider(200, 0, 1000, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = (int) (Config.alarmbramvol * 1000);
            }

            public void changed() {
                double vol = val / 1000.0;
                Config.alarmbramvol = vol;
                Utils.setprefd("alarmbramvol", vol);
            }
        });
    }

    private void initDisplay() {
        initDisplayFirstColumn();
        initDisplaySecondColumn();
        display.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        display.pack();
    }

    private void initDisplayFirstColumn() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(display);
        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.add(new CheckBox("Display kin names") {
            {
                a = Config.showkinnames;
            }

            public void set(boolean val) {
                Utils.setprefb("showkinnames", val);
                Config.showkinnames = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display item completion as progress bar") {
            {
                a = Config.itemmeterbar;
            }

            public void set(boolean val) {
                Utils.setprefb("itemmeterbar", val);
                Config.itemmeterbar = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display item completion as percentage") {
            {
                a = Config.itempercentage;
            }

            public void set(boolean val) {
                Utils.setprefb("itempercentage", val);
                Config.itempercentage = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show hourglass percentage") {
            {
                a = Config.showprogressperc;
            }

            public void set(boolean val) {
                Utils.setprefb("showprogressperc", val);
                Config.showprogressperc = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show attributes & softcap values in craft window") {
            {
                a = Config.showcraftcap;
            }

            public void set(boolean val) {
                Utils.setprefb("showcraftcap", val);
                Config.showcraftcap = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show objects health") {
            {
                a = Config.showgobhp;
            }

            public void set(boolean val) {
                Utils.setprefb("showgobhp", val);
                Config.showgobhp = val;
                a = val;

                GameUI gui = gameui();
                if (gui != null && gui.map != null) {
                    if (val)
                        gui.map.addHealthSprites();
                    else
                        gui.map.removeCustomSprites(Sprite.GOB_HEALTH_ID);
                }
            }
        });
        appender.add(new CheckBox("Show player paths") {
            {
                a = Config.showplayerpaths;
            }

            public void set(boolean val) {
                Utils.setprefb("showplayerpaths", val);
                Config.showplayerpaths = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show animal paths") {
            {
                a = Config.showanimalpaths;
            }

            public void set(boolean val) {
                Utils.setprefb("showanimalpaths", val);
                Config.showanimalpaths = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show study remaining time") {
            {
                a = Config.showstudylefttime;
            }

            public void set(boolean val) {
                Utils.setprefb("showstudylefttime", val);
                Config.showstudylefttime = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show contents bars for buckets/flasks") {
            {
                a = Config.showcontentsbars;
            }

            public void set(boolean val) {
                Utils.setprefb("showcontentsbars", val);
                Config.showcontentsbars = val;
                a = val;
            }
        });
    }

    private void initDisplaySecondColumn() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(display);
        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setX(400);
        appender.add(new CheckBox("Show wear bars") {
            {
                a = Config.showwearbars;
            }

            public void set(boolean val) {
                Utils.setprefb("showwearbars", val);
                Config.showwearbars = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show animal radius") {
            {
                a = Config.showanimalrad;
            }

            public void set(boolean val) {
                Utils.setprefb("showanimalrad", val);
                Config.showanimalrad = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight empty/finished drying frames") {
            {
                a = Config.showdframestatus;
            }

            public void set(boolean val) {
                Utils.setprefb("showdframestatus", val);
                Config.showdframestatus = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Draw circles around party members") {
            {
                a = Config.partycircles;
            }

            public void set(boolean val) {
                Utils.setprefb("partycircles", val);
                Config.partycircles = val;
                a = val;
            }
        });
    }

    private void initMap() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(map);

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new CheckBox("Save map tiles to disk") {
            {
                a = Config.savemmap;
            }

            public void set(boolean val) {
                Utils.setprefb("savemmap", val);
                Config.savemmap = val;
                MapGridSave.mgs = null;
                a = val;
            }
        });

        map.add(new Label("Show boulders:"), new Coord(180, 0));
        map.add(new Label("Show bushes:"), new Coord(325, 0));
        map.add(new Label("Show trees:"), new Coord(470, 0));
        map.add(new Label("Hide icons:"), new Coord(615, 0));

        map.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        map.pack();
    }

    private void initGeneral() {
        initGeneralFirstColumn();
        initGeneralSecondColumn();
        general.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        general.pack();
    }

    private void initGeneralFirstColumn() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(general);

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new CheckBox("Save chat logs to disk") {
            {
                a = Config.chatsave;
            }

            public void set(boolean val) {
                Utils.setprefb("chatsave", val);
                Config.chatsave = val;
                a = val;
                if (!val && Config.chatlog != null) {
                    try {
                        Config.chatlog.close();
                        Config.chatlog = null;
                    } catch (Exception e) {
                    }
                }
            }
        });
        appender.add(new CheckBox("Show timestamps in chats") {
            {
                a = Config.chattimestamp;
            }

            public void set(boolean val) {
                Utils.setprefb("chattimestamp", val);
                Config.chattimestamp = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Notify when kin comes online") {
            {
                a = Config.notifykinonline;
            }

            public void set(boolean val) {
                Utils.setprefb("notifykinonline", val);
                Config.notifykinonline = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Auto hearth") {
            {
                a = Config.autohearth;
            }

            public void set(boolean val) {
                Utils.setprefb("autohearth", val);
                Config.autohearth = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Auto logout on unknown/red players") {
            {
                a = Config.autologout;
            }

            public void set(boolean val) {
                Utils.setprefb("autologout", val);
                Config.autologout = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Print server time to System log") {
            {
                a = Config.servertimesyslog;
            }

            public void set(boolean val) {
                Utils.setprefb("servertimesyslog", val);
                Config.servertimesyslog = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Automatically select 'Pick' action") {
            {
                a = Config.autopick;
            }

            public void set(boolean val) {
                Utils.setprefb("autopick", val);
                Config.autopick = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Automatically select 'Harvest' action") {
            {
                a = Config.autoharvest;
            }

            public void set(boolean val) {
                Utils.setprefb("autoharvest", val);
                Config.autoharvest = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Automatically select 'Eat' action") {
            {
                a = Config.autoeat;
            }

            public void set(boolean val) {
                Utils.setprefb("autoeat", val);
                Config.autoeat = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Automatically select 'Split' action") {
            {
                a = Config.autosplit;
            }

            public void set(boolean val) {
                Utils.setprefb("autosplit", val);
                Config.autosplit = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Run on login") {
            {
                a = Config.runonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("runonlogin", val);
                Config.runonlogin = val;
                a = val;
            }
        });
    }

    private void initGeneralSecondColumn() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(general);

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);
        appender.setX(310);

        appender.add(new CheckBox("Show server time") {
            {
                a = Config.showservertime;
            }

            public void set(boolean val) {
                Utils.setprefb("showservertime", val);
                Config.showservertime = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable tracking on login") {
            {
                a = Config.enabletracking;
            }

            public void set(boolean val) {
                Utils.setprefb("enabletracking", val);
                Config.enabletracking = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable criminal acts on login") {
            {
                a = Config.enablecrime;
            }

            public void set(boolean val) {
                Utils.setprefb("enablecrime", val);
                Config.enablecrime = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Select System log on login") {
            {
                a = Config.syslogonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("syslogonlogin", val);
                Config.syslogonlogin = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Auto-miner: drop mined ore") {
            {
                a = Config.dropore;
            }

            public void set(boolean val) {
                Utils.setprefb("dropore", val);
                Config.dropore = val;
                a = val;
            }
        });
    }

    private void initCombat() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(combat);

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new CheckBox("Display damage received by opponents") {
            {
                a = Config.showdmgop;
            }

            public void set(boolean val) {
                Utils.setprefb("showdmgop", val);
                Config.showdmgop = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display damage received by me") {
            {
                a = Config.showdmgmy;
            }

            public void set(boolean val) {
                Utils.setprefb("showdmgmy", val);
                Config.showdmgmy = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Highlight current opponent") {
            {
                a = Config.hlightcuropp;
            }

            public void set(boolean val) {
                Utils.setprefb("hlightcuropp", val);
                Config.hlightcuropp = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Aggro closest unknown/red player on Tab key") {
            {
                a = Config.agroclosest;
            }

            public void set(boolean val) {
                Utils.setprefb("agroclosest", val);
                Config.agroclosest = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Display cooldown time") {
            {
                a = Config.showcooldown;
            }

            public void set(boolean val) {
                Utils.setprefb("showcooldown", val);
                Config.showcooldown = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show arrow vectors") {
            {
                a = Config.showarchvector;
            }

            public void set(boolean val) {
                Utils.setprefb("showarchvector", val);
                Config.showarchvector = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show attack cooldown delta") {
            {
                a = Config.showcddelta;
            }

            public void set(boolean val) {
                Utils.setprefb("showcddelta", val);
                Config.showcddelta = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disallow aggroing of party/village members & non-red kins") {
            {
                a = Config.donotaggrofriends;
            }

            public void set(boolean val) {
                Utils.setprefb("donotaggrofriends", val);
                Config.donotaggrofriends = val;
                a = val;
            }
        });

        combat.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        combat.pack();
    }

    private void initControl() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(control);

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.add(new CheckBox("Free camera rotation") {
            {
                a = Config.camfree;
            }

            public void set(boolean val) {
                Utils.setprefb("camfree", val);
                Config.camfree = val;
                a = val;
            }
        });
        appender.addRow(
                new Label("Bad camera scrolling sensitivity"),
                new HSlider(50, 0, 50, 0) {
                    protected void attach(UI ui) {
                        super.attach(ui);
                        val = Config.badcamsensitivity;
                    }
                    public void changed() {
                        Config.badcamsensitivity = val;
                        Utils.setprefi("badcamsensitivity", val);
                    }
                });
        appender.add(new CheckBox("Minimap: use MMB to drag & L/RMB to move") {
            {
                a = Config.alternmapctrls;
            }

            public void set(boolean val) {
                Utils.setprefb("alternmapctrls", val);
                Config.alternmapctrls = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Use French (AZERTY) keyboard layout") {
            {
                a = Config.userazerty;
            }

            public void set(boolean val) {
                Utils.setprefb("userazerty", val);
                Config.userazerty = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Reverse bad camera MMB x-axis") {
            {
                a = Config.reversebadcamx;
            }

            public void set(boolean val) {
                Utils.setprefb("reversebadcamx", val);
                Config.reversebadcamx = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Reverse bad camera MMB y-axis") {
            {
                a = Config.reversebadcamy;
            }

            public void set(boolean val) {
                Utils.setprefb("reversebadcamy", val);
                Config.reversebadcamy = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Force hardware cursor (req. restart)") {
            {
                a = Config.hwcursor;
            }

            public void set(boolean val) {
                Utils.setprefb("hwcursor", val);
                Config.hwcursor = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable dropping items over water (overridable with Ctrl)") {
            {
                a = Config.nodropping;
            }

            public void set(boolean val) {
                Utils.setprefb("nodropping", val);
                Config.nodropping = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Enable full zoom-out in Ortho cam") {
            {
                a = Config.enableorthofullzoom;
            }

            public void set(boolean val) {
                Utils.setprefb("enableorthofullzoom", val);
                Config.enableorthofullzoom = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Disable hotkey (tilde/back-quote key) for drinking") {
            {
                a = Config.disabledrinkhotkey;
            }

            public void set(boolean val) {
                Utils.setprefb("disabledrinkhotkey", val);
                Config.disabledrinkhotkey = val;
                a = val;
            }
        });

        control.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        control.pack();
    }

    private void initUis() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(uis);

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);

        appender.addRow(new Label("Language (req. restart):"), langDropdown());
        appender.add(new CheckBox("Show quick hand slots") {
            {
                a = Config.quickslots;
            }

            public void set(boolean val) {
                Utils.setprefb("quickslots", val);
                Config.quickslots = val;
                a = val;

                try {
                    Widget qs = ((GameUI) parent.parent.parent).quickslots;
                    if (qs != null) {
                        if (val)
                            qs.show();
                        else
                            qs.hide();
                    }
                } catch (ClassCastException e) { // in case we are at the login screen
                }
            }
        });
        appender.add(new CheckBox("Show F-key toolbar") {
            {
                a = Config.fbelt;
            }

            public void set(boolean val) {
                Utils.setprefb("fbelt", val);
                Config.fbelt = val;
                a = val;
                GameUI gui = gameui();
                if (gui != null) {
                    FBelt fbelt = gui.fbelt;
                    if (fbelt != null) {
                        if (val)
                            fbelt.show();
                        else
                            fbelt.hide();
                    }
                }
            }
        });
        appender.add(new CheckBox("Hide extensions menu (req. restart)") {
            {
                a = Config.hidexmenu;
            }

            public void set(boolean val) {
                Utils.setprefb("hidexmenu", val);
                Config.hidexmenu = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Show inventory on login") {
            {
                a = Config.showinvonlogin;
            }

            public void set(boolean val) {
                Utils.setprefb("showinvonlogin", val);
                Config.showinvonlogin = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Hide quests panel") {
            {
                a = Config.noquests;
            }

            public void set(boolean val) {
                Utils.setprefb("noquests", val);
                Config.noquests = val;
                try {
                    if (val)
                        gameui().questpanel.hide();
                    else
                        gameui().questpanel.show();
                } catch (NullPointerException npe) { // ignored
                }
                a = val;
            }
        });
        appender.add(new CheckBox("Show Craft/Build history toolbar") {
            {
                a = Config.histbelt;
            }

            public void set(boolean val) {
                Utils.setprefb("histbelt", val);
                Config.histbelt = val;
                a = val;
                GameUI gui = gameui();
                if (gui != null) {
                    CraftHistoryBelt histbelt = gui.histbelt;
                    if (histbelt != null) {
                        if (val)
                            histbelt.show();
                        else
                            histbelt.hide();
                    }
                }
            }
        });
        appender.add(new CheckBox("Instant flower menus") {
            {
                a = Config.instantflowermenu;
            }

            public void set(boolean val) {
                Utils.setprefb("instantflowermenu", val);
                Config.instantflowermenu = val;
                a = val;
            }
        });
        //appender.addRow(new Label("Interface font size (req. restart):"), makeFontSizeGlobalDropdown());
        //appender.addRow(new Label("Button font size (req. restart):"), makeFontSizeButtonDropdown());
        //appender.addRow(new Label("Window title font size (req. restart):"), makeFontSizeWndCapDropdown());
        appender.addRow(new Label("Chat font size (req. restart):"), makeFontSizeChatDropdown());

        Button resetWndBtn = new Button(220, "Reset Windows (req. logout)") {
            @Override
            public void click() {
                for (String wndcap : Window.persistentwnds)
                    Utils.delpref(wndcap + "_c");
                Utils.delpref("mmapc");
                Utils.delpref("mmapwndsz");
                Utils.delpref("mmapsz");
                Utils.delpref("quickslotsc");
                Utils.delpref("chatsz");
                Utils.delpref("chatvis");
                Utils.delpref("gui-bl-visible");
                Utils.delpref("gui-br-visible");
                Utils.delpref("gui-ul-visible");
                Utils.delpref("gui-ur-visible");
                Utils.delpref("gui-um-visible");
                Utils.delpref("menu-visible");
                Utils.delpref("fbelt_c");
                Utils.delpref("fbelt_vertical");
            }
        };
        uis.add(resetWndBtn, new Coord(740 / 2 - resetWndBtn.sz.x / 2 , 320));
        uis.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        uis.pack();
    }

    private void initQuality() {
        final WidgetVerticalAppender appender = new WidgetVerticalAppender(quality);
        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.setHorizontalMargin(HORIZONTAL_MARGIN);
        appender.add(new CheckBox("Show item quality") {
            {
                a = Config.showquality;
            }

            public void set(boolean val) {
                Utils.setprefb("showquality", val);
                Config.showquality = val;
                a = val;
            }
        });

        Label highest = new Label("Highest");
        Label avgESV = new Label("Avg E/S/V");
        Label all = new Label("All");
        Label avgSV = new Label("Avg S/V");
        Label lowest = new Label("Lowest");

        appender.setVerticalMargin(0);
        appender.addRow(highest, avgESV, all, avgSV, lowest);

        final int showQualityWidth = HorizontalAligner.apply(Arrays.asList(highest, avgESV, all, avgSV, lowest), HORIZONTAL_MARGIN);

        appender.setVerticalMargin(VERTICAL_MARGIN);
        appender.add(new HSlider(showQualityWidth, 0, 4, 0) {
            protected void attach(UI ui) {
                super.attach(ui);
                val = Config.showqualitymode;
            }
            public void changed() {
                Config.showqualitymode = val;
                Utils.setprefi("showqualitymode", val);
            }
        });
        appender.add(new CheckBox("Show LP gain multiplier for curios") {
            {
                a = Config.showlpgainmult;
            }

            public void set(boolean val) {
                Utils.setprefb("showlpgainmult", val);
                Config.showlpgainmult = val;
                a = val;
            }
        });

        appender.addRow(new Label("Calculate Avg as (req. logout):"), avgQModeDropdown());

        appender.add(new CheckBox("Round item quality to a whole number") {
            {
                a = Config.qualitywhole;
            }

            public void set(boolean val) {
                Utils.setprefb("qualitywhole", val);
                Config.qualitywhole = val;
                a = val;
            }
        });
        appender.add(new CheckBox("Draw background for quality values") {
            {
                a = Config.qualitybg;
            }

            public void set(boolean val) {
                Utils.setprefb("qualitybg", val);
                Config.qualitybg = val;
                a = val;
            }
        });

        quality.add(new PButton(200, "Back", 27, main), new Coord(270, 360));
        quality.pack();
    }

    private Dropbox<Locale> langDropdown() {
        List<Locale> languages = enumerateLanguages();
        List<String> values = languages.stream().map(x -> x.getDisplayName()).collect(Collectors.toList());
        return new Dropbox<Locale>(10, values) {
            {
                super.change(new Locale(Resource.language));
            }

            @Override
            protected Locale listitem(int i) {
                return languages.get(i);
            }

            @Override
            protected int listitems() {
                return languages.size();
            }

            @Override
            protected void drawitem(GOut g, Locale item, int i) {
                g.text(item.getDisplayName(), Coord.z);
            }

            @Override
            public void change(Locale item) {
                super.change(item);
                Resource.language = item.toString();
                Utils.setpref("language", item.toString());
            }
        };
    }

    private List<Locale> enumerateLanguages() {
        Set<Locale> languages = new HashSet<>();
        languages.add(new Locale("en"));

        Enumeration<URL> en;
        try {
            en = this.getClass().getClassLoader().getResources("l10n");
            if (en.hasMoreElements()) {
                URL url = en.nextElement();
                JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
                try (JarFile jar = urlcon.getJarFile()) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        // we assume that if tooltip localization exists then the rest exist as well
                        // up to dev to make sure that it's true
                        if (name.startsWith("l10n/" + Resource.BUNDLE_TOOLTIP))
                            languages.add(new Locale(name.substring(13, 15)));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<Locale>(languages);
    }

    private static final Pair[] avgQModes = new Pair[]{
            new Pair<>(Resource.getLocString(Resource.BUNDLE_LABEL, "Quadratic"), AVG_MODE_QUADRATIC),
            new Pair<>(Resource.getLocString(Resource.BUNDLE_LABEL, "Geometric"), AVG_MODE_GEOMETRIC),
            new Pair<>(Resource.getLocString(Resource.BUNDLE_LABEL, "Arithmetic"), AVG_MODE_ARITHMETIC)
    };

    @SuppressWarnings("unchecked")
    private Dropbox<Pair<String, Integer>> avgQModeDropdown() {
        List<String> values = Arrays.stream(avgQModes).map(x -> x.a.toString()).collect(Collectors.toList());
        Dropbox<Pair<String, Integer>> modes = new Dropbox<Pair<String, Integer>>(avgQModes.length, values) {
            @Override
            protected Pair<String, Integer> listitem(int i) {
                return avgQModes[i];
            }

            @Override
            protected int listitems() {
                return avgQModes.length;
            }

            @Override
            protected void drawitem(GOut g, Pair<String, Integer> item, int i) {
                g.text(item.a, Coord.z);
            }

            @Override
            public void change(Pair<String, Integer> item) {
                super.change(item);
                Config.avgmode = item.b;
                Utils.setprefi("avgmode", item.b);
            }
        };
        modes.change(avgQModes[Config.avgmode]);
        return modes;
    }

    private static final List<Integer> fontSize = Arrays.asList(10, 11, 12, 13, 14, 15, 16);

    private Dropbox<Integer> makeFontSizeGlobalDropdown() {
        List<String> values = fontSize.stream().map(x -> x.toString()).collect(Collectors.toList());
        return new Dropbox<Integer>(fontSize.size(), values) {
            {
                super.change(Config.fontsizeglobal);
            }

            @Override
            protected Integer listitem(int i) {
                return fontSize.get(i);
            }

            @Override
            protected int listitems() {
                return fontSize.size();
            }

            @Override
            protected void drawitem(GOut g, Integer item, int i) {
                g.text(item.toString(), Coord.z);
            }

            @Override
            public void change(Integer item) {
                super.change(item);
                Config.fontsizeglobal = item;
                Utils.setprefi("fontsizeglobal", item);
            }
        };
    }

    private Dropbox<Integer> makeFontSizeButtonDropdown() {
        List<String> values = fontSize.stream().map(x -> x.toString()).collect(Collectors.toList());
        return new Dropbox<Integer>(fontSize.size(), values) {
            {
                super.change(Config.fontsizebutton);
            }

            @Override
            protected Integer listitem(int i) {
                return fontSize.get(i);
            }

            @Override
            protected int listitems() {
                return fontSize.size();
            }

            @Override
            protected void drawitem(GOut g, Integer item, int i) {
                g.text(item.toString(), Coord.z);
            }

            @Override
            public void change(Integer item) {
                super.change(item);
                Config.fontsizebutton = item;
                Utils.setprefi("fontsizebutton", item);
            }
        };
    }

    private Dropbox<Integer> makeFontSizeWndCapDropdown() {
        List<String> values = fontSize.stream().map(x -> x.toString()).collect(Collectors.toList());
        return new Dropbox<Integer>(fontSize.size(), values) {
            {
                super.change(Config.fontsizewndcap);
            }

            @Override
            protected Integer listitem(int i) {
                return fontSize.get(i);
            }

            @Override
            protected int listitems() {
                return fontSize.size();
            }

            @Override
            protected void drawitem(GOut g, Integer item, int i) {
                g.text(item.toString(), Coord.z);
            }

            @Override
            public void change(Integer item) {
                super.change(item);
                Config.fontsizewndcap = item;
                Utils.setprefi("fontsizewndcap", item);
            }
        };
    }

    private Dropbox<Integer> makeFontSizeChatDropdown() {
        List<String> values = fontSize.stream().map(x -> x.toString()).collect(Collectors.toList());
        return new Dropbox<Integer>(fontSize.size(), values) {
            {
                super.change(Config.fontsizechat);
            }

            @Override
            protected Integer listitem(int i) {
                return fontSize.get(i);
            }

            @Override
            protected int listitems() {
                return fontSize.size();
            }

            @Override
            protected void drawitem(GOut g, Integer item, int i) {
                g.text(item.toString(), Coord.z);
            }

            @Override
            public void change(Integer item) {
                super.change(item);
                Config.fontsizechat = item;
                Utils.setprefi("fontsizechat", item);
            }
        };
    }


    public OptWnd() {
        this(true);
    }

    public void setMapSettings() {
        final String charname = gameui().chrid;

         CheckListbox boulderlist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.boulderssel = getselected();
                Utils.setprefsa("boulderssel_" + charname, Config.boulderssel);
            }
        };
        for (String boulder : Config.boulders) {
            boolean selected = false;
            if (Config.boulderssel != null) {
                for (String sboulder : Config.boulderssel) {
                    if (sboulder.equals(boulder)) {
                        selected = true;
                        break;
                    }
                }
            }
            boulderlist.items.add(new CheckListboxItem(boulder, selected));
        }
        map.add(boulderlist, new Coord(180, 15));


        CheckListbox bushlist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.bushessel = getselected();
                Utils.setprefsa("bushessel_" + charname, Config.bushessel);
            }
        };
        for (String bush : Config.bushes) {
            boolean selected = false;
            if (Config.bushessel != null) {
                for (String sbush : Config.bushessel) {
                    if (sbush.equals(bush)) {
                        selected = true;
                        break;
                    }
                }
            }
            bushlist.items.add(new CheckListboxItem(bush, selected));
        }
        map.add(bushlist, new Coord(325, 15));

        CheckListbox treelist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.treessel = getselected();
                Utils.setprefsa("treessel_" + charname, Config.treessel);
            }
        };
        for (String tree : Config.trees) {
            boolean selected = false;
            if (Config.treessel != null) {
                for (String stree : Config.treessel) {
                    if (stree.equals(tree)) {
                        selected = true;
                        break;
                    }
                }
            }
            treelist.items.add(new CheckListboxItem(tree, selected));
        }
        map.add(treelist, new Coord(470, 15));

        CheckListbox iconslist = new CheckListbox(130, 18) {
            protected void itemclick(CheckListboxItem itm, int button) {
                super.itemclick(itm, button);
                Config.iconssel = getselected();
                Utils.setprefsa("iconssel_" + charname, Config.iconssel);
            }
        };
        for (String icon : Config.icons) {
            boolean selected = false;
            if (Config.iconssel != null) {
                for (String sicon : Config.iconssel) {
                    if (sicon.equals(icon)) {
                        selected = true;
                        break;
                    }
                }
            }
            iconslist.items.add(new CheckListboxItem(icon, selected));
        }
        map.add(iconslist, new Coord(615, 15));
        map.pack();
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (msg == "close")) {
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void show() {
        chpanel(main);
        super.show();
    }
}
