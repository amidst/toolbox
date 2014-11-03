package eu.amidst.core.distribution;

import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.potential.Potential;

/**
 * Created by afa on 02/07/14.
 */
public interface Distribution {

    Potential getRestrictedPotentialExceptFor(DataInstance instance, int varID);

    Potential getRestrictedPotential(DataInstance instance);

    public double getProbability(DataInstance data);

    public void setExpectationParameters(ExpectationParameters ss);

    public ExpectationParameters getExpectationParameters();

    public interface ExpectationParameters {
        public double[] getExpectationParameters();

    }

    //public double[] getNaturalParameters();

    //public double[] getStandardParameters();

    //public void setStandardParameters(double[] par);
  }
