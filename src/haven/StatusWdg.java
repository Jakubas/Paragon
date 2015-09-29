package haven;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusWdg extends Widget {

    public static String username;
    public static String pass;

    private static ThreadGroup tg = new ThreadGroup("StatusUpdaterThreadGroup");
    private String statusupdaterthreadname = "StatusUpdater";

    private String hearthlingsplaying = "?";
    private String pingtime = "?";
    private String accountstatus = "?";

    public StatusWdg() {
        synchronized (StatusWdg.class) {
            Thread[] activethreads = new Thread[tg.activeCount()];
            int count = tg.enumerate(activethreads);
            for (int i = 0; i < count; i++) {
                if (activethreads[i].getName().equals(this.statusupdaterthreadname)) {
                    activethreads[i].interrupt();
                    try {
                        activethreads[i].join();
                    } catch (InterruptedException ex) {
                        // NOP
                    }
                }
            }

            startupdaterthread();
        }
    }

    private String removehtmltags(String input) {
        return input.replaceAll("\\<[^>]*>", "");
    }

    private void updatehearthlingscount() {
        String hearthlingscount = "?";

        String mainpagecontent = geturlcontent("http://www.havenandhearth.com/portal/");
        if (!mainpagecontent.isEmpty())
            hearthlingscount = getstringbetween(mainpagecontent, "There are", "hearthlings playing").trim();

        if (hearthlingscount.isEmpty())
            hearthlingscount = "?";

        synchronized (StatusWdg.class) {
            hearthlingsplaying = hearthlingscount;
        }
    }

    private void updatepingtime() {
        String ping = "?";

        java.util.List<String> command = new ArrayList<>();
        command.add("ping");
        command.add("-n");
        command.add("1");
        command.add("game.havenandhearth.com");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            BufferedReader standardOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String output = "";
            String line;
            while ((line = standardOutput.readLine()) != null) {
                output += line;
            }

            // Reply from 87.245.198.59: bytes=32 time=2ms TTL=53
            // Ответ от 213.180.204.3: число байт=32 время=12мс TTL=55
            Pattern pattern = Pattern.compile(".+?=32 .+?=(\\d+).*? TTL=.+");
            Matcher matcher = pattern.matcher(output);
            while (matcher.find()) {
                ping = matcher.group(1);
            }
        } catch (IOException ex) {
            // NOP
        }

        if (ping.isEmpty()) {
            ping = "?";
        }

        synchronized (this) {
            pingtime = ping;
        }
    }

    private boolean mklogin() {
        try {
            URL url = new URL("http://www.havenandhearth.com/portal/sec/login");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            Map<String,Object> params = new LinkedHashMap<>();
            params.put("username", username);
            params.put("password", pass);

            StringBuilder postdata = new StringBuilder();
            for (Map.Entry<String,Object> param : params.entrySet()) {
                if (postdata.length() != 0)
                    postdata.append('&');
                postdata.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postdata.append('=');
                postdata.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postdata.toString().length()));

            conn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(postdata.toString());
            wr.flush();
            wr.close();

            int responsecode = conn.getResponseCode();
            if (responsecode != HttpURLConnection.HTTP_OK)
                return false;
        } catch (IOException ex) {
            return false;
        }

        return true;
    }

    private void updateaccountstatus() {
        String status = "?";

        int retriescount = 0;
        while (retriescount < 2) {
            String profilepagecontent = geturlcontent("http://www.havenandhearth.com/portal/profile");
            status = removehtmltags(getstringbetween(profilepagecontent, "Account status:", "(All times")).trim();
            if (status.isEmpty()) {
                mklogin();
                ++retriescount;
                continue;
            }

            break;
        }

        if (status.isEmpty())
            status = "?";

        synchronized (StatusWdg.class) {
            accountstatus = status;
        }
    }

    private void startupdaterthread() {
        Thread statusupdaterthread = new Thread(tg, new Runnable() {
            public void run() {
                CookieHandler.setDefault(new CookieManager());

                mklogin();
                if (Thread.interrupted())
                    return;

                while (true) {
                    updatehearthlingscount();
                    if (Thread.interrupted())
                        return;

                    updatepingtime();
                    if (Thread.interrupted())
                        return;

                    updateaccountstatus();
                    if (Thread.interrupted())
                        return;

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        }, this.statusupdaterthreadname);
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
        InputStream is = null;
        BufferedReader br;
        String urlcontent = "";

        try {
            url_ = new URL(url);
            is = url_.openStream();
            br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {
                urlcontent += line;
            }
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            return "";
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "";
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe) {
                // NOP
            }
        }

        return urlcontent;
    }

    @Override
    public void draw(GOut g) {
        synchronized (StatusWdg.class) {
            java.util.List<Tex> texturesToDisplay = new ArrayList<>();

            Text herthlingsplayingtext = Text.render(String.format("Players: %s", this.hearthlingsplaying), Color.WHITE);
            texturesToDisplay.add(herthlingsplayingtext.tex());

            Text pingtimetext = Text.render(String.format("Ping: %s ms", this.pingtime), Color.WHITE);
            texturesToDisplay.add(pingtimetext.tex());

            Text accountstatustext = Text.render(String.format("Account status: %s", this.accountstatus), Color.WHITE);
            texturesToDisplay.add(accountstatustext.tex());

            int requiredwidth = 0;
            int requiredheight = 0;
            int y = 0;
            for (Tex textodisplay : texturesToDisplay) {
                g.image(textodisplay, new Coord(0, y));

                if (textodisplay.sz().x > requiredwidth)
                    requiredwidth = textodisplay.sz().x;
                y += textodisplay.sz().y;
            }

            requiredheight = y;
            this.sz = new Coord(requiredwidth, requiredheight);
        }
    }
}
