package eu.amidst.core.Estimators;

import eu.amidst.core.Potential.Potential;
import eu.amidst.core.StaticDataBase.DataInstance;

/**
 * Created by afa on 02/07/14.
 */
public interface Estimator {
    void updateSufficientStatistics(DataInstance instance);

    void updateExpectedSufficientStatistics(DataInstance instance, Potential pot);

    Potential getRestrictedPotential(DataInstance instance);
}
