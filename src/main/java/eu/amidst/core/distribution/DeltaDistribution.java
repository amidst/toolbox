package eu.amidst.core.distribution;

import eu.amidst.core.variables.Variable;

import java.util.Random;

/**
 * Created by andresmasegosa on 13/01/15.
 */
public class DeltaDistribution extends UnivariateDistribution {

    double deltaValue;

    public DeltaDistribution(Variable var1, double deltaValue1){
        this.var=var1;
        this.deltaValue=deltaValue1;
    }

    public double getDeltaValue() {
        return deltaValue;
    }

    @Override
    public double getLogProbability(double value) {
        return (deltaValue==value)? 1.0 : 0.0;
    }

    @Override
    public double sample(Random rand) {
        return deltaValue;
    }

    @Override
    public int getNumberOfFreeParameters() {
        return 1;
    }

    @Override
    public String label() {
        return "Delta of " + this.deltaValue;
    }

    @Override
    public void randomInitialization(Random random) {

    }
}
