package eu.amidst.core.distribution;

import eu.amidst.core.header.Variable;

/**
 * Created by afa on 03/11/14.
 */
public class Normal implements UnivariateDistribution {

    private double mean;

    private double sd;

    private Variable var;

    public Normal(Variable var) {
        this.var = var;
        this.mean = 0;
        this.sd = 1;
    }


    public double getMean() {
        return mean;
    }

    public double getSd() {
        return sd;
    }

    public void setMean(double mean) {
        this.mean = mean;
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
        return (-Math.log(sd) - 0.5*Math.log(2*Math.PI) - 0.5 * Math.pow(((x-mean)/sd),2));
    }

    @Override
    public double getProbability(double x) {
        return(1/(sd * Math.sqrt(2 * Math.PI)) * Math.exp(-0.5 * Math.pow(((x-mean)/sd),2)));
    }
}
