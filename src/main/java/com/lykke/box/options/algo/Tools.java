package com.lykke.box.options.algo;

import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

/**
 * Created by author.
 */
public class Tools {

    public static final long MS_IN_WEEK = 604800000L;

    public Tools(){}

    /**
     * The method checks of the given currentTime is between two points: timeWeekendStarts and timeWeekendEnds.
     * @param timeWeekendStarts
     * @param timeWeekendEnds
     * @param currentTime
     * @return true of false
     */
    public static boolean checkIfNowIsWeekend(long timeWeekendStarts, long timeWeekendEnds, long currentTime){
        long currentTimeFromMonday = findTimeFromLastMonday(currentTime);
        boolean nowIsWeekend = false;
        if (currentTimeFromMonday >= timeWeekendStarts && currentTimeFromMonday <= timeWeekendEnds){
            nowIsWeekend = true;
        }
        return nowIsWeekend;
    }


    /**
     * This is the simplest version of the function which returns a set of dates used as tipping points of the
     * historical data set given to the VolatilityEstimator constructor.
     * !!!!WARNING!!!!
     * The method can handle only a movingWindow which has length less then length of a trading week. Not more then 4
     * days.
     * @param timeWeekendStarts is time in milliseconds of the beginning of the trading break
     * @param timeWeekendEnds is time in milliseconds of the end of the trading break
     * @param movingWindow is period of time in milliseconds of the desired data set
     * @return an ArrayList<Long> of dates in Long. For example, a list {date1, date2, date3, date4} means that we should
     * prepare last of historical prices FROM the date1 TO date2 AND FROM date3 TO date4. Thus, the final set of date
     * will consist of two periods.
     */
    public static ArrayList<Long> findDatesOfInputData(long timeWeekendStarts, long timeWeekendEnds, long movingWindow){
        ArrayList<Long> listTimes = new ArrayList<>();
        long currentTime = DateTime.now().getMillis();
        long tradingWeekLen = timeWeekendStarts + (MS_IN_WEEK - timeWeekendEnds);
        long weekendLen = MS_IN_WEEK - tradingWeekLen;
        long timeFromEndWeekendToNextWeek = MS_IN_WEEK - timeWeekendEnds;
        long currentTimeFromMonday = findTimeFromLastMonday(currentTime);
        long timeOfBeginningOfTheWeekend = currentTime - (currentTimeFromMonday - timeWeekendStarts);
        if (checkIfNowIsWeekend(timeWeekendStarts, timeWeekendEnds, currentTime)){ // if we are already in the weekend period, we simply take a movingWindow before the weekend
            listTimes.add(timeOfBeginningOfTheWeekend - movingWindow);
            listTimes.add(timeOfBeginningOfTheWeekend);
            return listTimes;
        } else {
            if (currentTimeFromMonday + timeFromEndWeekendToNextWeek >= movingWindow){ // if we have enough time from the current moment to the end of the previous weekend
                listTimes.add(currentTime - movingWindow);
                listTimes.add(currentTime);
                return listTimes;
            } else {
                long restOfTime = movingWindow - (currentTimeFromMonday + timeFromEndWeekendToNextWeek);
                listTimes.add(currentTime - (currentTimeFromMonday + timeFromEndWeekendToNextWeek) - weekendLen - restOfTime);
                listTimes.add(currentTime - (currentTimeFromMonday + timeFromEndWeekendToNextWeek) - weekendLen);

                listTimes.add(currentTime - (currentTimeFromMonday + timeFromEndWeekendToNextWeek));
                listTimes.add(currentTime);
                return listTimes;
            }
        }
    }

    /**
     * The method measures the distance in milliseconds of the given currentTime from the previous Monday
     * @param currentTime is input parameter, in milliseconds
     * @return the distance in milliseconds of the given currentTime from the previous Monday
     */
    public static long findTimeFromLastMonday(long currentTime){
        DateTime dateTime = new DateTime(currentTime);
        DateTime mondayThisWeek = dateTime.withDayOfWeek(DateTimeConstants.MONDAY); // thus we get date of a Monday
        mondayThisWeek = mondayThisWeek.withTimeAtStartOfDay(); // to come to the very beginning of the Monday
        return currentTime - mondayThisWeek.getMillis();
    }

}
