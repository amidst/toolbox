package eu.amidst.core.distribution.exponentialfamily;


import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.potential.Potential;

/**
 * Created by afa on 03/07/14.
 */
public class MultinomialDistribution implements ExponentialFamilyDistribution {

    @Override
    public Potential getRestrictedPotentialExceptFor(DataInstance instance, int varID) {
        return null;
    }

    @Override
    public Potential getRestrictedPotential(DataInstance instance) {
        return null;
    }

    @Override
    public SufficientStatistics getSufficientStatistics(DataInstance instance) {
        return null;
    }

    @Override
    public SufficientStatistics getExpectedSufficientStatistics(DataInstance instance, Potential pot) {
        return null;
    }

    @Override
    public void setExpectationParameters(ExponentialFamilyDistribution.ExpectationParameters ss) {

    }

    @Override
    public ExpectationParameters getExpectationParameters() {
        return null;
    }

    @Override
    public double getProbability(DataInstance data) {
        return 0;
    }

    public class SufficientStatistics implements ExponentialFamilyDistribution.SufficientStatistics {
        private double[] counts;
        private double sumCounts;

    }
    public class ExpectationParameters implements ExponentialFamilyDistribution.ExpectationParameters{

    }
}
