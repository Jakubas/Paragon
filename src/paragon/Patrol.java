package paragon;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import haven.Coord;
import haven.Gob;
import haven.KinInfo;
import haven.Loading;
import haven.OCache;
import haven.Resource;
import haven.UI;

public class Patrol implements Runnable {

	private volatile boolean interrupted = false;
	private Utils utils;
	
	public Patrol(UI ui) {
		utils = new Utils(ui);
	}
	
	@Override
	public void run() {
		
		//load coordinates
		Coord[] patrolPath = new Coord[]{new Coord (-20,0), new Coord(20,0)};
		if (patrolPath.length == 0)
			System.out.println("Please create a path with the PatrolCoord bot");
		
	    for(int i = 0; i < patrolPath.length; i++) {
	    	if (interrupted) {
	    		return;
	    	}
	    	Coord coord = patrolPath[i];
	    	Gob pl = utils.player();
			moveToCoordAndPatrol(new Coord(pl.coordX()+coord.x, pl.coordY()+coord.y));
			if (patrolPath.length == i-1) {
				//reset path
				System.out.println("reset");
				i = 0;
			}
	    }
	}
	
	public void moveToCoordAndPatrol(Coord coord) {
    	double dist = coord.dist(utils.player().rc);
    	while (dist > 0) {
    		clickCoordAndPatrol(coord);
    		dist = coord.dist(utils.player().rc);
    		if (dist > 0) {
    			utils.clickInRandomDirection();
    		}
    	}
    }
	
	public void clickCoordAndPatrol(Coord coord) {
		utils.ui.sess.glob.gui.map.wdgmsg("click", Coord.z, coord, 1, 0, 0);
		utils.waitForMovement(Utils.PING_TIMEOUT);
		while (utils.isMoving()) {
			if (isUnknownPlayer()) {
				playWarningSound();
			}
			utils.sleep(100);
		}
	}
	
	//need your own sound, put it in sounds/alarm.ogg of the build folder
	public void playWarningSound() {
		AudioInputStream audioInputStream;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(new File("sounds/alarm.wav").getAbsoluteFile());
	        Clip clip = AudioSystem.getClip();
	        clip.open(audioInputStream);
	        clip.start();
	        //wait for sound to finish playing
	        utils.sleep(2000);
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isUnknownPlayer() {
        OCache oc = utils.ui.sess.glob.oc;
        synchronized (oc) {
            Gob gobcls = null;
            double gobclsdist = Double.MAX_VALUE;
            for (Gob gob : oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null && "body".equals(res.basename()) && gob.id != utils.player().id) {
                        KinInfo kininfo = gob.getattr(KinInfo.class);
                        if (kininfo == null || kininfo.group == 2) {
                            double dist = utils.player().rc.dist(gob.rc);
                            if (dist < gobclsdist) {
                                gobcls = gob;
                                gobclsdist = dist;
                            }
                        }
                    }
                } catch (Loading l) {
                }
            }
            return gobcls != null;
        }
    }
	
	public void cancel() {
	  interrupted = true;   
	}
}
