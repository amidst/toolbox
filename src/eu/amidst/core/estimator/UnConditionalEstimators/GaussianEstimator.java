package eu.amidst.core.estimator.UnConditionalEstimators;

import eu.amidst.core.database.statics.DataInstance;
import eu.amidst.core.estimator.Estimator;
import eu.amidst.core.potential.Potential;

/**
 * Created by afa on 03/07/14.
 */
public class GaussianEstimator implements Estimator {
    private double mean;
    private double sd;
    private double sumSquaredValues;
    private double sumValues;
    private double intervalWidth;

    @Override
    public Potential getRestrictedPotential(DataInstance instance) {
        return null;
    }

    @Override
    public double[] getSufficientStatistics(DataInstance instance) {
        return new double[0];
    }

    @Override
    public double[] getExpectedSufficientStatistics(DataInstance instance, Potential pot) {
        return new double[0];
    }

    @Override
    public void setExpectationParameters(double[] ss) {

    }

    @Override
    public double[] getExpectationParameters() {
        return new double[0];
    }

    @Override
    public double getProbability(DataInstance data) {
        return 0;
    }
}
