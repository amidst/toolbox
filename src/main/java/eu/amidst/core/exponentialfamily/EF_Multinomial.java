package eu.amidst.core.exponentialfamily;

import eu.amidst.core.header.Variable;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public class EF_Multinomial extends EF_UnivariateDistribution {

    /**
     * The class constructor.
     * @param var The variable of the distribution.
     */
    public EF_Multinomial(Variable var) {

    }


    @Override
    public double computeLogBaseMeasure(double val) {
        return 0;
    }

    @Override
    public double computeLogNormalizer(NaturalParameters parameters) {
        return 0;
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        return null;
    }

    @Override
    public NaturalParameters getNaturalFromMomentParameters(MomentParameters momentParameters) {
        return null;
    }

    @Override
    public MomentParameters getMomentFromNaturalParameters(NaturalParameters naturalParameters) {
        return null;
    }
}
