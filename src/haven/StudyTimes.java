package haven;

import java.io.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class StudyTimes {
    public static void updatestudytimes() {
        Document doc;
        while (true) {
            try {
                doc = Jsoup.connect("http://ringofbrodgar.com/wiki/Curiosity").get();
                break;
            } catch (IOException ioe) {
                try {
                    // Give some time to fix possible internet connection's errors
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }

        try {
            Element allcuriostable = doc.select("table").get(0);
            Elements rows = allcuriostable.select("tr");
            for (int i = 1; i < rows.size(); ++i) {
                try {
                    Element row = rows.get(i);
                    Elements cols = row.select("td");

                    String name = cols.get(0).text();
                    String studytime = cols.get(3).text();
                    if (name.isEmpty() || studytime.isEmpty()) {
                        continue;
                    }

                    Utils.setprefd("curio." + name.toLowerCase(), Double.valueOf(studytime));
                } catch (Exception ex) {
                    continue;
                }
            }
        } catch (Exception ex) {
            // Invalid page received
            return;
        }
    }

    public static double getstudytime(String curioname) {
        return Utils.getprefd("curio." + curioname.toLowerCase(), 0.0);
    }
}
