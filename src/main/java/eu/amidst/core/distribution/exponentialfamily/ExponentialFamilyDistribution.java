package eu.amidst.core.distribution.exponentialfamily;

import eu.amidst.core.database.statics.DataInstance;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.potential.Potential;

/**
 * Created by afa on 09/10/14.
 */
public interface ExponentialFamilyDistribution extends Distribution {

    public double[] getSufficientStatistics(DataInstance instance);

    public double[] getExpectedSufficientStatistics(DataInstance instance, Potential pot);

    public void setExpectationParameters(double[] ss);

    public double[] getExpectationParameters();

}
