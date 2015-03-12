package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;

import java.util.Random;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public abstract class EF_UnivariateDistribution extends EF_Distribution {

    public abstract double computeLogBaseMeasure(double val);

    public abstract SufficientStatistics getSufficientStatistics(double val);

    public abstract Vector getExpectedParameters();

    public double computeProbabilityOf(double val){
        return Math.exp(this.computeLogProbabilityOf(val));
    }

    public double computeLogProbabilityOf(double val){
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(val)) + this.computeLogBaseMeasure(val) + this.computeLogNormalizer();
    }

    @Override
    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters = parameters;//.copy(parameters);
        this.fixNumericalInstability();
        this.updateMomentFromNaturalParameters();
    }

    public abstract void fixNumericalInstability();

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data){
        return this.getSufficientStatistics(data.getValue(this.getVariable()));
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance){
        return this.computeLogBaseMeasure(dataInstance.getValue(this.getVariable()));
    }

    public abstract EF_UnivariateDistribution deepCopy();

    public abstract EF_UnivariateDistribution randomInitialization(Random rand);

    public <E extends UnivariateDistribution> E toUnivariateDistribution(){
        throw new UnsupportedOperationException("This EF distribution is not convertible to standard form");
    }
}
