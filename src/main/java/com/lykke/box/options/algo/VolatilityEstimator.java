package com.lykke.box.options.algo;

import com.lykke.box.options.daos.Price;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;


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
    public long timeWeekendStarts, timeWeekendEnds;
    public boolean hasWeekend;

    VolatilityEstimator(List<Double> activityDistribution, List<Price> historicPrices, double delta, long movingWindow, double periodsPerYear, boolean hasWeekend){
        runner = new Runner(delta, delta, -1, movingWindow);
        this.activityDistribution = activityDistribution;
        this.historicPrices = historicPrices;
        this.movingWindow = movingWindow;
        this.periodsPerYear = periodsPerYear;
        this.hasWeekend = hasWeekend;
        timeOfBar = msInWeek / activityDistribution.size(); // divide number of ms per week to the number of bars
        initialized = false;
        if (hasWeekend){
            timeWeekendStarts = 421200000L;
            timeWeekendEnds = 587400000L;
        } else {
            timeWeekendStarts = timeWeekendEnds = -1L;
        }

    }

    private double computeAnnualVolat(List<Price> historicPrices){
        for (Price aPrice : historicPrices){
            runner.run(aPrice);
        }
        double sqrtVar = runner.computeTotalSqrtVar();
        return Math.sqrt((sqrtVar * periodsPerYear));
    }

    private double computeAverageFutureActivity(Price currentPrice, long optEndsInMs){ // average activity for a given period of time

        long msFromMonday = msFromMonday(currentPrice.getTime());
        int firstBar = (int) (msFromMonday / timeOfBar);
        int lastBar = (int)((msFromMonday + optEndsInMs) / timeOfBar);
        nBarStartTime = firstBar;
        nBarEndOfOpt = lastBar;
        double sumActivity = 0.0;
        for (int iBar = firstBar; iBar <= lastBar; iBar++){
            sumActivity += activityDistribution.get(iBar % activityDistribution.size()); // will iterate the array if index is too big
        }
        return sumActivity / (double) (lastBar - firstBar + 1); // finds average activity till the end of the box
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
            double coeff = computeAverageFutureActivity(currentPrice, optEndsInMs);
            volat = annualVolat * coeff;
            prevVolat = volat;
            return volat;

        } else {
            for (Price aPrice : newPrices){
                runner.run(aPrice);
            }
            if (checkIfNowIsWeekend(timeWeekendStarts, timeWeekendEnds, currentPrice.getTime())){
                runner.addTimeToIEs(timeWeekendEnds - timeWeekendStarts);
                prevVolat = volat;
                volat = 0.0;
            } else {
                double updatedSqrtVar = runner.computeTotalSqrtVar();
                double annualVolat = Math.sqrt((updatedSqrtVar * periodsPerYear));
                double coeff = computeAverageFutureActivity(currentPrice, optEndsInMs);
                prevVolat = volat;
                volat = annualVolat * coeff;
            }
            return volat;
        }
    }

    private boolean checkIfNowIsWeekend(long timeWeekendStarts, long timeWeekendEnds, long currentTime){
        DateTime dateTime = new DateTime(currentTime);
        DateTime mondayThisWeek = dateTime.withDayOfWeek(DateTimeConstants.MONDAY); // thus we get date of a Monday
        mondayThisWeek = mondayThisWeek.withTimeAtStartOfDay(); // to come to the very beginning of the Monday
        long currentTimeFromMonday = currentTime - mondayThisWeek.getMillis();
        boolean nowIsWeekend = false;
        if (currentTimeFromMonday >= timeWeekendStarts && currentTimeFromMonday <= timeWeekendEnds){
            nowIsWeekend = true;
        }
        return nowIsWeekend;
    }

}
