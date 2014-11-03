package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.potential.Potential;

/**
 * Created by afa on 09/10/14.
 */
public interface ExponentialFamilyDistribution extends Distribution {

    public SufficientStatistics getSufficientStatistics(DataInstance instance);

    public SufficientStatistics getExpectedSufficientStatistics(DataInstance instance, Potential pot);

    //Declare abstract because different distributions implements different methods.
    public abstract class SufficientStatistics {

    }
}
