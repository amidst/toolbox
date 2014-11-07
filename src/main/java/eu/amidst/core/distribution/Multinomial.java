/**
 ******************* ISSUE LIST **************************
 *
 * 1. In general, should we clone attributes in the constructor to avoid bad uses of input variables later on?
 *
 * 2. How are we going to update the probabilities? Value by value? Or directly with the whole set of probabilities? or both?
 * Two methods are included: setProbabilities(double[] probabilities) and setProbabilityAt(int index, double value)
 *
 * ********************************************************
 */
package eu.amidst.core.distribution;

import eu.amidst.core.header.Variable;


/**
 * Created by afa on 03/11/14.
 */
public class Multinomial implements UnivariateDistribution {

    private Variable var;
    private double[] probabilities;


    public Multinomial (Variable var) {

        this.var = var;

        this.probabilities = new double[var.getNumberOfStates()];

        for (int i=0;i<var.getNumberOfStates();i++){
            this.probabilities[i]=1.0/var.getNumberOfStates();
        }
    }

    public void setProbabilities(double[] probabilities) {
        this.probabilities = probabilities;
    }

    public void setProbabilityAt(int index, double value) {
        this.probabilities[index] = value;
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

}