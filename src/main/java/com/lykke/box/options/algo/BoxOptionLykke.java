package com.lykke.box.options.algo;

import com.lykke.box.options.daos.Price;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BoxOptionLykke {

    public static final double DELTA = 0.00018; // threshold to compute activity, 1% == 0.01
//    public static final double DELTA = 0.00033; // threshold to compute activity, 1% == 0.01
    public static final String activityFileName = "Activities/newEURCHF10min_0_00018_weekendYES.txt"; // weekly activity file
//    public static final String activityFileName = "Activities/volatActivity_EURJPY_10min_0.00033_2017-05-23_01-04-48.txt"; // weekly activity file
    public static final long TIME_STEP = 40000L; // how often we recalculate prices, in ms
    public static final long MOVING_WINDOW = 86400000L; // in ms (604800000L for 1 week, 86400000L for a day)
    public static final double MARGIN_HIT = 0.05; // 0.10 means 10%, must be less then 0.50
    public static final double MARGIN_MISS = 0.05;
    public static final double MAX_PAYOUT_COEFF = 10;

    public static final double BOOKING_FEE = 0.01;


    public static void main(String[] args){

        ArrayList<Price> historicPrices = new ArrayList<>(); // contains all ticks of the chosen historical interval
        ArrayList<Double> activityDistrib = readActivityFile(activityFileName); // contains weekly activity, average of which is normalized on 1

        /**********************************************************************
         * Here we create a set of ticks for the chosen period. It will be
         * used to compute the initial realized annual volatility.
         * In real case one should use real prices.
         **********************************************************************/
//        String testDataFile = "D:/Data/EURUSD_UTC_Ticks_Bid_2016.02.02_2017.01.31.csv";
        String testDataFile = "D:/Data/EURCHF_UTC_Ticks_Bid_2016.02.02_2017.01.31.csv";
        String dateFormat = "yyyy.MM.dd HH:mm:ss.SSS";
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(testDataFile));
            String line = bufferedReader.readLine(); // header
            line = bufferedReader.readLine();
            Price price = lineToPrice(line, dateFormat);
            long finalTime = price.getTime() + MOVING_WINDOW;
            historicPrices.add(price);
            boolean notEnd = true;
            while (notEnd){
                price = lineToPrice(bufferedReader.readLine(), dateFormat);
                if (price.getTime() > finalTime){
                    notEnd = false;
                } else {
                    historicPrices.add(price);
                }
            }



            String dateString = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date()); // date for the filename
            String fileName = "box1_" + dateString + ".csv";
            PrintWriter writer = new PrintWriter("Results/" + fileName, "UTF-8");


            /**********************************************************************
             * Here we have the main code:
             **********************************************************************/

            double boxVertSize = 0.000055;
            long timeToFirstOption = 4000L; // in ms
            long boxLen = 7000L; // in ms
            int nPriceIndexes = 11;
            int nTimeIndexes = 11;
            boolean hasWeekend = false;

            Price currentPrice = historicPrices.get(historicPrices.size() - 1);
            long startTime = currentPrice.getTime();

            OptionsGrid optionsGrid = new OptionsGrid(timeToFirstOption, boxLen, boxVertSize, nPriceIndexes, nTimeIndexes, MARGIN_HIT, MARGIN_MISS, MAX_PAYOUT_COEFF, BOOKING_FEE, hasWeekend);
            optionsGrid.initiateGrid(activityDistrib, historicPrices, DELTA, MOVING_WINDOW, currentPrice);

            writer.println(optionsGrid.getHeadString());



            // we'll do 1 Month of experiments, 2592000000L ms (or 1 Week, 604800000L)
            for (long ms = 0; ms < 604800000L; ms += TIME_STEP){

                long currentTime = startTime + ms;


                ArrayList<Price> newPrices = new ArrayList<>();
                while (price.getTime() <= currentTime){ // if we observe a new price (prices) within the given time step
                    newPrices.add(price);
                    price = lineToPrice(bufferedReader.readLine(), dateFormat);
                }
                currentPrice = new Price(startTime + ms, currentPrice.getBid(), currentPrice.getAsk());

                optionsGrid.updateCoefficients(newPrices, currentPrice);


                if (ms % (TIME_STEP * 400) == 0){
                    writer.println(optionsGrid.getPayoutsString(currentTime, currentPrice.midPrice()));
                    System.out.println(new Date(currentTime));
                }

            }

            writer.close();
            System.out.println("File is saved as:\n" + fileName);


        } catch (Exception ex){
            ex.printStackTrace();
        }

    }




    /**********************************************************************
     * Auxiliary functions
     **********************************************************************/

    public static Price lineToPrice(String line, String dateFormat){
        String lineData[] = line.split(",");
        return (new Price(stringToDate(lineData[0], dateFormat).getTime(), Double.parseDouble(lineData[2]), Double.parseDouble(lineData[1])));
    }

    public static ArrayList<Double> readActivityFile(String fileName){
        ArrayList <Double> activity = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = bufferedReader.readLine()) != null){
                activity.add(Double.parseDouble(line));
            }
            bufferedReader.close();

        } catch (Exception ex){
            ex.printStackTrace();
        }

        return activity;
    }

    public static Date stringToDate(String inputStringDate, String dateFormat){
        DateFormat formatDate = new SimpleDateFormat(dateFormat, Locale.US);
        try {
            Date date = formatDate.parse(inputStringDate);
            return date;
        } catch (ParseException e){
            e.printStackTrace();
            return null;
        }
    }

}
