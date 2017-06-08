package com.lykke.box.options.algo;

import com.lykke.box.options.daos.Price;
import java.util.LinkedList;

public class Runner {
    public double prevDC;
    public double extreme;
    public double deltaUp;  // 1% == 0.01
    public double deltaDown;
    public int type; // means the latest observed direction
    public boolean initalized;
    public int numberDC;
    public LinkedList<Long> timesDC;
    public LinkedList<Double> osLengths;
    public double avSqrtVar;
    public double osL;
    private LinkedList<IE> ieLinkedList;
    private long movingWindow;


    Runner(double deltaUp, double deltaDown, int type, long movingWindow){
        this.type = type; this.deltaUp = deltaUp; this.deltaDown = deltaDown; initalized = false; numberDC = 0;
        timesDC = new LinkedList<>();
        osLengths = new LinkedList<>();
        ieLinkedList = new LinkedList<>();
        this.movingWindow = movingWindow;
    }


    public int run(Price aPrice){

        if (!initalized){
            initalized = true;
            timesDC.add(aPrice.getTime());
            osL = 0.0;
            osLengths.add(0.0);
            numberDC += 1;
            if (type == -1){
                extreme = aPrice.getAsk();
                prevDC = aPrice.getAsk();
            }
            else if (type == 1){
                extreme = aPrice.getBid();
                prevDC = aPrice.getBid();
            }
            double sqrtVar = computeSqrtVar(osL, deltaUp);
            IE ie = new IE(type, aPrice.getTime(), prevDC, osL, sqrtVar);
            ieLinkedList.add(ie);
            return type;
        } else {

            if (type == -1){
                if (aPrice.getAsk() < extreme){
                    extreme = aPrice.getAsk();
                    return 0;

                } else if (Math.log(aPrice.getBid() / extreme) >= deltaUp){
                    osL = Math.abs(Math.log(extreme / prevDC));
                    osLengths.add(osL);
                    timesDC.add(aPrice.getTime());
                    prevDC = aPrice.getBid();
                    extreme = aPrice.getBid();
                    type = 1;
                    numberDC += 1;
                    double sqrtVar = computeSqrtVar(osL, deltaDown);
                    IE ie = new IE(type, aPrice.getTime(), aPrice.getBid(), osL, sqrtVar);
                    ieLinkedList.add(ie);
                    removeOldIEsIfAny(aPrice.getTime());
                    return 1;
                }

            }
            else if (type == 1){
                if (aPrice.getBid() > extreme){
                    extreme = aPrice.getBid();
                    return 0;

                } else if (-Math.log(aPrice.getAsk() / extreme) >= deltaDown){
                    osL = Math.abs(Math.log(extreme / prevDC));
                    osLengths.add(osL);
                    timesDC.add(aPrice.getTime());
                    prevDC = aPrice.getAsk();
                    extreme = aPrice.getAsk();
                    type = -1;
                    numberDC += 1;
                    double sqrtVar = computeSqrtVar(osL, deltaUp);
                    IE ie = new IE(type, aPrice.getTime(), aPrice.getAsk(), osL, sqrtVar);
                    ieLinkedList.add(ie);
                    removeOldIEsIfAny(aPrice.getTime());
                    return -1;
                }
            }
        }

        return 0;
    }


    /**
     * Computes squared variability of one overshoot
     * @param osL is the size of an overshoot
     * @param delta is the size of the relevant threshold
     * @return squared variability of the overshoot
     */
    private double computeSqrtVar(double osL, double delta){
        return Math.pow(osL - delta, 2);
    }

    /**
     * This part is needed to have IEs with correct prices after weekends.
     * @param time
     */
    public void addTimeToIEs(long time){
        for (IE ie : ieLinkedList){
            ie.setTime(ie.getTime() + time);
        }
    }

    /**
     * The methods compute summ of all squared variabilities of overshoots in the ieLinkedList
     * @return
     */
    public double computeTotalSqrtVar(){
        double totalSqrtVar = 0;
        for (IE ie : ieLinkedList){
            totalSqrtVar += ie.getSqrtOsDeviation();
        }
        return totalSqrtVar;
    }

    /**
     * The method should remove old IEs from the list
     * @param currentTime is really the current time
     */
    private void removeOldIEsIfAny(long currentTime){
        while (ieLinkedList.size() != 0 && currentTime - ieLinkedList.getFirst().getTime() > movingWindow){
            ieLinkedList.removeFirst();
        }
    }
}