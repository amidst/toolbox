package eu.amidst.core.Estimators.UnConditionalEstimators;

import eu.amidst.core.Estimators.Estimator;
import eu.amidst.core.Potential.Potential;
import eu.amidst.core.StaticDataBase.DataInstance;

/**
 * Created by afa on 03/07/14.
 */
public class MultivariateGaussianEstimator implements Estimator {
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

    private double[] means;
    private double[][] covMatrix;


}
