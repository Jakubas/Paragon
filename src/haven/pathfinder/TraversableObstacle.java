package haven.pathfinder;

import haven.Coord;

import java.util.HashMap;

public class TraversableObstacle {
    public int gcx;
    public int gcy;
    public Coord wa, wb, wc, wd;
    public Coord clra, clrb, clrc, clrd;
    public int radius;
    public HashMap<Integer, Utils.MinMax> raster;

    public TraversableObstacle(int gcx, int gcy, Coord wa, Coord wb, Coord wc, Coord wd,
                               Coord clra, Coord clrb, Coord clrc, Coord clrd, int radius, HashMap<Integer, Utils.MinMax> raster) {
        this.gcx = gcx;
        this.gcy = gcy;
        this.wa = wa;
        this.wb = wb;
        this.wc = wc;
        this.wd = wd;
        this.clra = clra;
        this.clrb = clrb;
        this.clrc = clrc;
        this.clrd = clrd;
        this.radius = radius;
        this.raster = raster;
    }
}