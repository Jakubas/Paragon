package haven;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusWdg extends Widget {
    private static ThreadGroup tg = new ThreadGroup("StatusUpdaterThreadGroup");
    private static final String statusupdaterthreadname = "StatusUpdater";

    private static final Tex hearthlingsplayingdef = Text.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Players: %s"), "?"), Color.WHITE).tex();
    private static final Tex pingtimedef = Text.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Ping: %s ms"), "?"), Color.WHITE).tex();
    private Tex hearthlingsplaying = hearthlingsplayingdef;
    private Tex pingtime = pingtimedef;

    private final static Pattern pattern = Config.iswindows ?
            Pattern.compile(".+?=32 .+?=(\\d+).*? TTL=.+") :    // Reply from 87.245.198.59: bytes=32 time=2ms TTL=53
            Pattern.compile(".+?time=(\\d+\\.?\\d*) ms");       // 64 bytes from ansgar.seatribe.se (213.239.201.139): icmp_seq=1 ttl=47 time=71.4 ms


    public StatusWdg() {
        synchronized (StatusWdg.class) {
            tg.interrupt();
            startupdaterthread();
        }
    }

    private void updatehearthlingscount() {
        String hearthlingscount = "?";

        String mainpagecontent = geturlcontent("http://www.havenandhearth.com/portal");
        if (!mainpagecontent.isEmpty())
            hearthlingscount = getstringbetween(mainpagecontent, "There are", "hearthlings playing").trim();

        synchronized (StatusWdg.class) {
            hearthlingsplaying = Text.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Players: %s"), hearthlingscount), Color.WHITE).tex();
        }
    }

    private void updatepingtime() {
        String ping = "?";

        java.util.List<String> command = new ArrayList<>();
        command.add("ping");
        command.add(Config.iswindows ? "-n" : "-c");
        command.add("1");
        command.add("game.havenandhearth.com");

        BufferedReader standardOutput = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            standardOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String output = "";
            String line;
            while ((line = standardOutput.readLine()) != null) {
                output += line;
            }

            Matcher matcher = pattern.matcher(output);
            while (matcher.find()) {
                ping = matcher.group(1);
            }
        } catch (IOException ex) {
            // NOP
        } finally {
            if (standardOutput != null)
                try {
                    standardOutput.close();
                } catch (IOException e) { // ignored
                }
        }

        if (ping.isEmpty())
            ping = "?";

        synchronized (this) {
            pingtime = Text.render(String.format(Resource.getLocString(Resource.BUNDLE_LABEL, "Ping: %s ms"), ping), Color.WHITE).tex();
        }
    }

    private void startupdaterthread() {
        Thread statusupdaterthread = new Thread(tg, new Runnable() {
            public void run() {
                CookieHandler.setDefault(new CookieManager());

                while (true) {
                    if (visible) {
                        updatehearthlingscount();
                        if (Thread.interrupted())
                            return;

                        updatepingtime();
                        if (Thread.interrupted())
                            return;
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        }, statusupdaterthreadname);
        statusupdaterthread.start();
    }

    private static String getstringbetween(String input, String leftdelimiter, String rightdelimiter) {
        int leftdelimiterposition = input.indexOf(leftdelimiter);
        if (leftdelimiterposition == -1)
            return "";

        int rightdelimiterposition = input.indexOf(rightdelimiter);
        if (rightdelimiterposition == -1)
            return "";

        return input.substring(leftdelimiterposition + leftdelimiter.length(), rightdelimiterposition);
    }

    private String geturlcontent(String url) {
        URL url_;
        BufferedReader br = null;
        String urlcontent = "";

        try {
            url_ = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)url_.openConnection();
            InputStream is = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {
                urlcontent += line;
            }
        } catch (SocketException se) {
            // don't print socket exceptions when network is unreachable to prevent console spamming on bad connections
            if (!se.getMessage().equals("Network is unreachable"))
                se.printStackTrace();
            return "";
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            return "";
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "";
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException ioe) {
                // NOP
            }
        }
        return urlcontent;
    }

    @Override
    public void draw(GOut g) {
        synchronized (StatusWdg.class) {
            g.image(hearthlingsplaying, Coord.z);
            g.image(pingtime, new Coord(0, hearthlingsplaying.sz().y));

            int w = hearthlingsplaying.sz().x;
            if (pingtime.sz().x > w)
                w = pingtime.sz().x;
            this.sz = new Coord(w,  hearthlingsplaying.sz().y + pingtime.sz().y);
        }
    }

    @Override
    public void hide() {
        hearthlingsplaying = hearthlingsplayingdef;
        pingtime = pingtimedef;
        super.hide();
    }
}
