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


    Runner(double deltaUp, double deltaDown, int type){
        this.type = type; this.deltaUp = deltaUp; this.deltaDown = deltaDown; initalized = false; numberDC = 0;
        timesDC = new LinkedList<>();
        osLengths = new LinkedList<>();
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
                    return -1;
                }
            }
        }

        return 0;
    }


    public double computeSqrtVar(){
        avSqrtVar = 0.0;
        if (numberDC == 0){
            return avSqrtVar;
        } else {
            for (double osL : osLengths){
                avSqrtVar +=  Math.pow(osL - deltaUp, 2);
            }
            return avSqrtVar;
        }
    }



}
