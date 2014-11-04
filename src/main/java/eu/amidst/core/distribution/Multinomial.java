package eu.amidst.core.distribution;

import eu.amidst.core.header.statics.Variable;

/**
 * Created by afa on 03/11/14.
 */
public class Multinomial implements UnivariateDistribution {

    private Variable var;

    private double[] counts;
    private double sumCounts;

    //Is this an attribute or is only computed from counts when necessary?
    private double[] probabilities;


    public Multinomial (Variable var) {
        this.var = var;
    }

    public double[] getCounts () {
        return counts;
    }
    public double getSumCounts() {
        return sumCounts;
    }

    @Override
    public double getLogProbability(double value) {
        return (Math.log(probabilities[(int)value]));
    }

    @Override
    public double getProbability(double value) {
        return (probabilities[(int)value]);
    }

    @Override
    public Variable getVariable() {
        return var;
    }

    @Override
    public void updateCounts(double value) {
        counts[(int)value] += 1;
        sumCounts += 1;
    }

}
