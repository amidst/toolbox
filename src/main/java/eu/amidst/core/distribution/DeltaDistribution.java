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
    public double[] getParameters() {
        return new double[1];
    }

    @Override
    public int getNumberOfParameters() {
        return 1;
    }

    @Override
    public String label() {
        return "Delta of " + this.deltaValue;
    }

    @Override
    public void randomInitialization(Random random) {

    }

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.DeltaDistribution"))
            return this.equalDist((DeltaDistribution)dist,threshold);
        return false;
    }

    @Override
    //TODO
    public String toString() {
        return null;
    }

    public boolean equalDist(DeltaDistribution dist, double threshold) {
        if (dist.getVariable()!=dist.getVariable())
            return false;

        if (deltaValue!=dist.getDeltaValue())
            return false;

        return true;
    }
}
