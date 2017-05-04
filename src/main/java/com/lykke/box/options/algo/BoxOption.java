package com.lykke.box.options.algo;

/**
 * Created by author.
 */
public class BoxOption {
    transient long startsInMS;
    transient long lenInMS;
    transient double relatUpStrike;
    transient double relatBotStrike;
    private double hitCoeff;
    private double missCoeff;


    BoxOption(long startsInMS, long lenInMS, double relatUpStrike, double relatBotStrike){
        this.startsInMS = startsInMS;
        this.lenInMS = lenInMS;
        this.relatUpStrike = relatUpStrike;
        this.relatBotStrike = relatBotStrike;
    }


    public BoxOption cloneBoxOption(){
        BoxOption boxOption = new BoxOption(startsInMS, lenInMS, relatUpStrike, relatBotStrike);
        boxOption.hitCoeff = this.hitCoeff;
        boxOption.missCoeff = this.missCoeff;
        boxOption.relatUpStrike = this.relatUpStrike;
        boxOption.relatBotStrike = this.relatBotStrike;
        return boxOption;
    }


    public void setCoefficients(double[] coefficients){
        this.hitCoeff = coefficients[0];
        this.missCoeff = coefficients[1];

    }

    public double getHitCoeff(){
        return hitCoeff;
    }

    public double getMissCoeff(){
        return missCoeff;
    }
}
