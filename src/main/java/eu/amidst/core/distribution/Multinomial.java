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