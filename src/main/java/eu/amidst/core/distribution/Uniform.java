package eu.amidst.core.distribution;

import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 23/11/14.
 */
public class Uniform extends UnivariateDistribution {

    public Uniform(Variable var_) {
        this.var = var_;
    }

    @Override
    public double getLogProbability(double value) {
        return 0;
    }

    @Override
    public int getNumberOfFreeParameters() {
        //Discrete uniform needs 1 parameter (number of states of the variable)
        //Continuous uniform needs 2 parameters (minimum and maximum interval)
        return 0; //????
    }

    public String label(){ return "Uniform"; }
}
