package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.utils.Vector;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public abstract class EF_UnivariateDistribution extends EF_Distribution {

    public abstract double computeLogBaseMeasure(double val);

    public abstract SufficientStatistics getSufficientStatistics(double val);

    public double computeProbabilityOf(double val){
        return Math.exp(this.computeLogProbabilityOf(val));
    }

    public double computeLogProbabilityOf(double val){
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(val)) + this.computeLogBaseMeasure(val) + this.computeLogNormalizer();
    }

    public SufficientStatistics getSufficientStatistics(DataInstance data){
        return this.getSufficientStatistics(data.getValue(this.getVariable()));
    }

    public double computeLogBaseMeasure(DataInstance dataInstance){
        return this.computeLogBaseMeasure(dataInstance.getValue(this.getVariable()));
    }
}
