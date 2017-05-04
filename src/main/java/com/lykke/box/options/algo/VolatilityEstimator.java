package com.lykke.box.options.algo;

import com.lykke.box.options.daos.Price;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.List;


public class VolatilityEstimator {

    public Runner runner;
    public List<Double> activityDistribution;
    public List<Price> historicPrices; // need it only for initial estimation
    private long timeOfBar;
    public double periodsPerYear;
    public final long msInWeek = 604800000L;
    public int nBarStartTime; // in which activity bar is the current price
    public int nBarEndOfOpt; // in which activity bar is the end of the option
    public long movingWindow;
    public double prevVolat;
    public double volat;
    public boolean initialized;

    VolatilityEstimator(List<Double> activityDistribution, List<Price> historicPrices, double delta, long movingWindow, double periodsPerYear){
        runner = new Runner(delta, delta, -1);
        this.activityDistribution = activityDistribution;
        this.historicPrices = historicPrices;
        this.movingWindow = movingWindow;
        this.periodsPerYear = periodsPerYear;
        timeOfBar = msInWeek / activityDistribution.size(); // divide number of ms per week to the number of bars
        initialized = false;
    }

    private double computeAnnualVolat(List<Price> historicPrices){
        for (Price aPrice : historicPrices){
            runner.run(aPrice);
        }
        double sqrtVar = runner.computeSqrtVar();
        return Math.sqrt((sqrtVar * periodsPerYear));
    }

    private double computeAverageActivity(Price currentPrice, long optEndsInMs){ // average activity for a given period of time

        long msFromMonday = msFromMonday(currentPrice.getTime());
        int firstBar = (int) (msFromMonday / timeOfBar);
        int lastBar = (int)((msFromMonday + optEndsInMs) / timeOfBar);
        nBarStartTime = firstBar;
        nBarEndOfOpt = lastBar;
        double sumActivity = 0.0;
        for (int iBar = firstBar; iBar <= lastBar; iBar++){
            sumActivity += activityDistribution.get(iBar % activityDistribution.size()); // will iterate the array if index is too big
        }
        return sumActivity / (double) (lastBar - firstBar + 1);
    }


    private long msFromMonday(long currentTime){
        DateTime dateTime = new DateTime(currentTime);
        DateTime mondayThisWeek = dateTime.withDayOfWeek(DateTimeConstants.MONDAY); // thus we get date of a Monday
        mondayThisWeek = mondayThisWeek.withTimeAtStartOfDay(); // to come to the very beginning of the Monday
        return currentTime - mondayThisWeek.getMillis();
    }


    public double run(List<Price> newPrices, Price currentPrice, long optEndsInMs){
        if (!initialized){
            initialized = true;
            double annualVolat = computeAnnualVolat(historicPrices);
            double coeff = computeAverageActivity(currentPrice, optEndsInMs);
            volat = annualVolat * coeff;
            prevVolat = volat;
            return volat;

        } else {
            for (Price aPrice : newPrices){
               runner.run(aPrice);
            }
            while ((runner.numberDC != 0) && (runner.timesDC.getFirst() < currentPrice.getTime() - movingWindow)){
                runner.timesDC.removeFirst();
                runner.osLengths.removeFirst();
                runner.numberDC -= 1;
            }
            double updatedSqrtVar = runner.computeSqrtVar();
            double annualVolat = Math.sqrt((updatedSqrtVar * periodsPerYear));
            double coeff = computeAverageActivity(currentPrice, optEndsInMs);
            prevVolat = volat;
            volat = annualVolat * coeff;
            return volat;
        }
    }

}
