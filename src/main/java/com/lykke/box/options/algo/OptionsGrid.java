package com.lykke.box.options.algo;

import com.lykke.box.options.daos.Price;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by author.
 */
public class OptionsGrid {

    private final long MS_PER_YEAR = 31536000000L;
    long timeToFirstOption;
    long optionLen;
    double priceSize;
    int nPriceIndexes;
    int nTimeIndexes;
    double marginHit;
    double marginMiss;
    double maxPayoutCoeff;
    double bookingFee;
    public BoxOption[][] optionsGrid;
    VolatilityEstimator[] volatilityEstimators;



    OptionsGrid(long timeToFirstOption, long optionLen, double priceSize, int nPriceIndexes, int nTimeIndexes,  double marginHit, double marginMiss, double maxPayoutCoeff, double bookingFee){

        this.timeToFirstOption = timeToFirstOption;
        this.optionLen = optionLen;
        this.priceSize = priceSize;
        this.nPriceIndexes = nPriceIndexes;
        this.nTimeIndexes = nTimeIndexes;
        this.marginHit = marginHit;
        this.marginMiss = marginMiss;
        this.maxPayoutCoeff = maxPayoutCoeff;
        this.bookingFee = bookingFee;
        optionsGrid = new BoxOption[nTimeIndexes][nPriceIndexes];
        volatilityEstimators = new VolatilityEstimator[nTimeIndexes];

    }


    public void initiateGrid(List<Double> activityDistribution, List<Price> historicPrices, double delta, long movingWindow, Price price){

        double minRelatBottomStrike = - (priceSize * nPriceIndexes / 2.0);
        double minRelatUpperStrike = minRelatBottomStrike + priceSize;
        for (int i = 0; i < nTimeIndexes; i++){
            long optStartsInMs = timeToFirstOption + i * optionLen;
            long optEndsInMs = optStartsInMs + optionLen;
            volatilityEstimators[i] = new VolatilityEstimator(activityDistribution, historicPrices, delta, movingWindow, (MS_PER_YEAR / (double) movingWindow));
            volatilityEstimators[i].run(new ArrayList<>(), price, optEndsInMs);
            for (int j = 0; j < nPriceIndexes; j++){
                optionsGrid[i][j] = new BoxOption(optStartsInMs, optEndsInMs, minRelatUpperStrike + j * priceSize, minRelatBottomStrike + j * priceSize);
                BoxPricing boxPricing = new BoxPricing(price.getTime() + optStartsInMs, price.getTime() + optEndsInMs, optionsGrid[i][j].relatUpStrike + price.midPrice(), optionsGrid[i][j].relatBotStrike + price.midPrice(), price, marginHit, marginMiss, maxPayoutCoeff, bookingFee);
                optionsGrid[i][j].setCoefficients(boxPricing.getCoefficients(0, volatilityEstimators[i].volat));
            }
        }
    }


    public void updateCoefficients(List<Price> newPrices, Price price){
        for (int i = 0; i < nTimeIndexes; i++){
            double volatility = volatilityEstimators[i].run(newPrices, price, optionsGrid[i][0].startsInMS + optionsGrid[i][0].lenInMS);
            if (volatility != volatilityEstimators[i].prevVolat){
                for (int j = 0; j < nPriceIndexes; j++){
                    BoxPricing boxPricing = new BoxPricing(price.getTime() + optionsGrid[i][j].startsInMS, price.getTime() + optionsGrid[i][j].startsInMS + optionsGrid[i][j].lenInMS, optionsGrid[i][j].relatUpStrike + price.midPrice(), optionsGrid[i][j].relatBotStrike + price.midPrice(), price, marginHit, marginMiss, maxPayoutCoeff, bookingFee);
                    optionsGrid[i][j].setCoefficients(boxPricing.getCoefficients(0, volatilityEstimators[i].volat));
                }
            }
        }
    }



}
