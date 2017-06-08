package com.lykke.box.options.algo;

/**
 * Created by author.
 *
 * This class is built to hold information about an intrinsic event. It can be either a directional-change intrinsic
 * event (IE), or an overshoot IE.
 */
public class IE {

    private int type; // is a type of the IE: +1 or -1 for DC IEs, +2 and -2 for an overshoot IE
    private long time; // is when the IE happened
    private double level; // is the price level at which the IE happened
    private double osL; // is overshoot length, in fraction of the previous DC price
    private double sqrtOsDeviation; // is the squared overshoot deviation, (w(d) - d)^2

    public IE(int type, long time, double level, double osL, double sqrtOsDeviation){
        this.type = type;
        this.time = time;
        this.level = level;
        this.osL = osL;
        this.sqrtOsDeviation = sqrtOsDeviation;
    }

    public int getType() {
        return type;
    }

    public long getTime() {
        return time;
    }

    public double getLevel() {
        return level;
    }

    public double getOsL() {
        return osL;
    }

    public double getSqrtOsDeviation(){
        return sqrtOsDeviation;
    }

    public void setTime(long time){
        this.time = time;
    }
}
