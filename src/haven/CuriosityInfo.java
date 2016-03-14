package haven;

import static haven.paragon.utils.UtilsSetup.ui;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

public class CuriosityInfo {
    public static final CuriosityInfo empty = new CuriosityInfo(-1, -1);
    private static final HashMap<String, CuriosityInfo> infos;

    public final int time; // in seconds
    public final int slots;

    static {
        infos = new HashMap<String, CuriosityInfo>();
//		File file = new File("patrolpath.txt");
//		String line = "";
		
		try {
        	InputStream is = Resource.class.getResourceAsStream("curio.config");
        	BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        	String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length > 2) {
                    String resName = parts[0];
                    Integer slots = Integer.parseInt(parts[1]);
                    int time = parseTime(parts[2]);
                    infos.put(resName, new CuriosityInfo(time, slots));
                }
            }
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private CuriosityInfo(int time, int slots) {
        this.time = time;
        this.slots = slots;
    }

    public String getFormattedTime() {
        return String.format("%02d:%02d:%02d", time / 3600, (time / 60) % 60, time % 60);
    }

    public static CuriosityInfo get(String resName) {
        return infos.get(resName);
    }

    private static int parseTime(String str) throws Exception {
        String[] parts = str.split(":");
        if (parts.length > 2) {
            int hours = Integer.parseInt(parts[0]);
            int mins = Integer.parseInt(parts[1]);
            int secs = Integer.parseInt(parts[2]);
            return secs + mins * 60 + hours * 60 * 60;
        }
        throw new Exception("Invalid time format");
    }
}