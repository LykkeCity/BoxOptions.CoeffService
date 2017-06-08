package com.lykke.box.options.algo;


import com.lykke.box.options.daos.Price;

class BoxPricing {

    long tCurrent, tStart, tEnd;
    double upperK, lowerK;
    double startPrice;
    double frontHit, bellowHit, aboveHit;
    private final long MS_PER_YEAR = 31536000000L;
    public double marginHit;
    public double marginMiss;
    double maxPayoutCoeff;
    double bookingFee;

    BoxPricing(long startTime, long endTime, double upperStrike, double lowerStrike, Price price, double marginHit, double marginMiss, double maxPayoutCoeff, double bookingFee){
        if( startTime < endTime ){
            tStart = startTime;
            tEnd = endTime;
        }else{ // start and end time incorrectly specified
            tStart = startTime;
            tEnd = startTime + 1;
            System.out.println("Start and end time incorrectly specified");
        }
        if( lowerStrike < upperStrike ){
            lowerK = lowerStrike;
            upperK = upperStrike;
        }else{ // lower and upper strike incorrectly specified
            lowerK = lowerStrike;
            upperK = lowerStrike + 0.0001;
            System.out.println("Lower and upper strike incorrectly specified");
        }
        startPrice = price.midPrice();
        tCurrent = price.getTime();
        this.marginHit = marginHit;
        this.marginMiss = marginMiss;
        this.maxPayoutCoeff = maxPayoutCoeff;
        this.bookingFee = bookingFee;
    }

    double CumNorm(double x){ // cumulative distribution function of normal distribution N(0,1)
        // protect against overflow
        if( x > 6.0 )
            return 1.0;
        if( x < -6.0 )
            return 0.0;

        double b1 = 0.31938153;
        double b2 = -0.356563782;
        double b3 = 1.781477937;
        double b4 = -1.821255978;
        double b5 = 1.330274429;
        double p = 0.2316419;
        double c2 = 0.3989423;

        double a = Math.abs(x);
        double t = 1.0 / (1.0 + a*p);
        double b = c2*Math.exp((-x)*(x/2.0));
        double n = ((((b5*t + b4)*t + b3)*t + b2)*t + b1)*t;
        n = 1.0 - b*n;

        if ( x < 0.0 )
            n = 1.0 - n;

        return n;
    }

    double AmericanOneTouch(double sigma, double r, double deltaT, boolean downAndIn, double strike, double price){ // probability of hitting an American down/up-and-in option
        double eta = (downAndIn ? 1.0 : -1.0);
        double mu = (r - 0.5 * sigma * sigma) / (sigma * sigma);
        double lambda = Math.sqrt(mu * mu + 2.0 * r / (sigma * sigma));
        double Z = Math.log(strike / price) / (sigma * Math.sqrt(deltaT)) + lambda * sigma * Math.sqrt(deltaT);
        double V = Math.pow(strike / price, (mu + lambda)) * CumNorm(eta * Z)
                + Math.pow(strike / price, (mu - lambda)) * CumNorm(eta * Z - 2.0 * eta * lambda * sigma * Math.sqrt(deltaT));
        return V;
    }

    double density(double startPrice, double sigma, double r, double deltaTs, double x){ // probability density of log-normal distribution log N(0,1)
        return Math.exp(-Math.pow(Math.log(x/startPrice) - (r - 0.5*Math.pow(sigma, 2.0)) * deltaTs, 2.0) / (2.0*sigma*sigma*deltaTs)) / (Math.sqrt(2.0 * Math.PI * deltaTs) * sigma * x);
    }



    /**********************************************************************
     * Integrate f from a to b using Simpson's rule.
     * Increase N for more precision.
     **********************************************************************/
    public double integrateLykke(double startPrice, double downLimit, double upLimit, boolean downAndIn, double deltaT, double deltaTs, double sigma, double r, double strike) {
        int N = 1000;                    // precision parameter
        double h = (upLimit - downLimit) / (N - 1);     // step size

        // 1/3 terms
        double sum = 1.0 / 3.0 * (density(startPrice, sigma, r, deltaTs, downLimit) * AmericanOneTouch(sigma, r, deltaT, downAndIn, strike, downLimit)
                + density(startPrice, sigma, r, deltaTs, upLimit) * AmericanOneTouch(sigma, r, deltaT, downAndIn, strike, upLimit));

        // 4/3 terms
        for( int i = 1; i < N - 1; i += 2 ){
            double x = downLimit + h * i;
            sum += 4.0 / 3.0 * (density(startPrice, sigma, r, deltaTs, x) * AmericanOneTouch(sigma, r, deltaT, downAndIn, strike, x));
        }

        // 2/3 terms
        for( int i = 2; i < N - 1; i += 2 ){
            double x = downLimit + h * i;
            sum += 2.0 / 3.0 * (density(startPrice, sigma, r, deltaTs, x) * AmericanOneTouch(sigma, r, deltaT, downAndIn, strike, x));
        }

        return sum * h;
    }


    double volSmile(double moneyness){
        return Math.pow((moneyness - 1) * 500, 2) + 1;
    }


    double prob(double r, double volatility){ // computes analytically the probability of hitting specified box
        double deltaT = this.tEnd - this.tStart;
        double deltaTs = this.tStart - this.tCurrent;
        deltaT /= MS_PER_YEAR;
        deltaTs /= MS_PER_YEAR;
        double averStrike = (upperK + lowerK) / 2.0;
        double moneyness = startPrice / averStrike;
        double scaledVolat = volatility * volSmile(moneyness);
        double upValue = (Math.log(upperK / startPrice) - (r - 0.5 * Math.pow(scaledVolat, 2.0)) * deltaTs) / (scaledVolat * Math.sqrt(deltaTs));
        double downValue = (Math.log(lowerK / startPrice) - (r - 0.5 * Math.pow(scaledVolat, 2.0)) * deltaTs) / (scaledVolat * Math.sqrt(deltaTs));
        frontHit = CumNorm(upValue) - CumNorm(downValue);
        bellowHit = integrateLykke(startPrice, lowerK - 5.0 * scaledVolat * Math.sqrt(deltaTs), lowerK, false, deltaT, deltaTs, scaledVolat, r, lowerK);
        aboveHit = integrateLykke(startPrice, upperK, upperK + 5.0 * scaledVolat * Math.sqrt(deltaTs), true, deltaT, deltaTs, scaledVolat, r, upperK);
        if (frontHit + bellowHit + aboveHit > 1.0){
            return 1.0;
        }
        return (frontHit + bellowHit + aboveHit);
    }


    double[] getCoefficients(double r, double annualVolatility){
        double[] coefficients = new double[2];
        if (annualVolatility < 0.005){
            coefficients[0] = 1.0;
            coefficients[1] = 1.0;
            return coefficients;
        } else {
            double probToHit = prob(r, annualVolatility);
            double cHit = (marginHit) / (0.5 - marginHit);
            double cMiss = (marginMiss) / (0.5 - marginMiss);
            double payoutCoefHit = (1 + cHit * probToHit) / ((1 + cHit) * probToHit);
            double payoutCoefMiss = (1 + cMiss * (1 - probToHit)) / ((1 + cMiss) * (1 -probToHit));

            payoutCoefHit = Math.min(payoutCoefHit - bookingFee, maxPayoutCoeff);
            payoutCoefMiss = Math.min(payoutCoefMiss - bookingFee, maxPayoutCoeff);

            coefficients[0] = Math.max(payoutCoefHit, 1.0);
            coefficients[1] = Math.max(payoutCoefMiss, 1.0);

            return coefficients;
        }
    }
}