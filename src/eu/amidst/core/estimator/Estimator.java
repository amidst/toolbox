package eu.amidst.core.estimator;

import eu.amidst.core.database.statics.DataInstance;
import eu.amidst.core.potential.Potential;

/**
 * Created by afa on 02/07/14.
 */
public interface Estimator {

    Potential getRestrictedPotential(DataInstance instance);

    public double[] getSufficientStatistics(DataInstance instance);

    public double[] getExpectedSufficientStatistics(DataInstance instance, Potential pot);

    public void setExpectationParameters(double[] ss);

    public double[] getExpectationParameters();

    public double getProbability(DataInstance data);

    //public double[] getNaturalParameters();

    //public double[] getStandardParameters();

    //public void setStandardParameters(double[] par);
}
