package eu.amidst.core.distribution;

import eu.amidst.core.header.statics.Variable;

/**
 * Created by afa on 03/11/14.
 */
public class Normal implements UnivariateDistribution {

    private double mean;

    // sd or precision??? Both are redundant
    private double sd;
    private double precision;

    private double sumX;
    private double sumX2;
    private Variable var;


    public Normal(Variable var) {
        this.var = var;
        this.mean = 0;
        this.sd = 1;
    }

    public double getMean() {
        return mean;
    }

    public double getPrecision() {
        return precision;
    }

    public double getSd() {
        return sd;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }
    public void setSd(double sd) {
        this.sd = sd;
    }

    @Override
    public Variable getVariable() {
        return var;
    }

    @Override
    public double getLogProbability (double x) {
        return(Math.log(1/(sd * Math.sqrt(2 * Math.PI))) - 0.5 * Math.pow(((x-mean)/sd),2));
    }

    @Override
    public double getProbability(double x) {
        return(1/(sd * Math.sqrt(2 * Math.PI)) * Math.exp(-0.5 * Math.pow(((x-mean)/sd),2)));
    }

    @Override
    public void updateCounts(double data) {
        this.sumX = sumX + data;
        this.sumX2 = sumX2 + data*data;
    }


}
