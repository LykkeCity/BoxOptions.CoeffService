package com.lykke.box.options.algo;

import com.lykke.box.options.daos.Price;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;


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
    public boolean addedWeekend; // shows whether we've already added a weekend to the list of IE or not

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
            timeWeekendEnds = 586800000L;
        } else {
            timeWeekendStarts = timeWeekendEnds = -1L;
        }
        addedWeekend = false;
        DateTimeZone.setDefault(DateTimeZone.UTC); // it is an important field: without this the algorithm will
        // interpret time like my local time. https://stackoverflow.com/questions/9397715/defaulting-date-time-zone-to-utc-for-jodatimes-datetime

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

    private void initialize(List<Price> historicalPrices){
        Price previousPrice = historicalPrices.get(0);
        long weekendLength = timeWeekendEnds - timeWeekendStarts;
        for (Price currentPrice : historicalPrices){
            if (currentPrice.getTime() - previousPrice.getTime() > weekendLength / 2){ // to catch a weekend gap. At least half, to be sure.
                runner.addTimeToIEs(timeWeekendEnds - timeWeekendStarts);
            }
            runner.run(currentPrice);
            previousPrice = currentPrice.clonePrice();
        }
    }


    public double run(List<Price> newPrices, Price currentPrice, long optEndsInMs){
        if (!initialized){
            initialized = true;
            initialize(historicPrices); // just initializes all runners and finds historical IEs
            prevVolat = volat = 0.0; // this part prevents wrong payouts on weekends
            return volat;

        } else {
            if (Tools.checkIfNowIsWeekend(timeWeekendStarts, timeWeekendEnds, currentPrice.getTime())){
                if (!addedWeekend){
                    runner.addTimeToIEs(timeWeekendEnds - timeWeekendStarts);
                    addedWeekend = true;
                }
                prevVolat = volat;
                volat = 0.0;
            } else {
                for (Price aPrice : newPrices){
                    runner.run(aPrice);
                }
                double updatedSqrtVar = runner.computeTotalSqrtVar();
//                double annualVolat = Math.sqrt((updatedSqrtVar * periodsPerYear));
                double annualVolat = 0.25;
                double coeff = computeAverageFutureActivity(currentPrice, optEndsInMs);
                prevVolat = volat;
                volat = annualVolat * coeff;
                addedWeekend = false;
            }
            return volat;
        }
    }



}
