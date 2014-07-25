package eu.amidst.core.Estimators.UnConditionalEstimators;

import eu.amidst.core.Estimators.Estimator;
import eu.amidst.core.Potential.Potential;
import eu.amidst.core.StaticDataBase.DataInstance;

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
    public void updateSufficientStatistics(DataInstance instance) {

    }

    @Override
    public void updateExpectedSufficientStatistics(DataInstance instance, Potential pot) {

    }

    @Override
    public Potential getRestrictedPotential(DataInstance instance) {
        return null;
    }
}
