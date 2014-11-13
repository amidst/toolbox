package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.utils.Vector;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public abstract class EF_UnivariateDistribution extends EF_Distribution {

    protected NaturalParameters naturalParameters;

    protected MomentParameters momentParameters;

    public NaturalParameters getNaturalParameters(){
        return this.naturalParameters;
    }

    public void setNaturalParameters(NaturalParameters parameters){
        this.naturalParameters=parameters;
        this.setMomentParameters(this.getMomentFromNaturalParameters(parameters));
    }

    public MomentParameters getMomentParameters(){
        return this.momentParameters;
    }

    public void setMomentParameters(MomentParameters parameters){
        this.momentParameters=parameters;
        this.setNaturalParameters(this.getNaturalFromMomentParameters(parameters));
    }

    public abstract double computeLogBaseMeasure(double val);

    public abstract double computeLogNormalizer(NaturalParameters parameters);

    public double getProbability(double val){
        return Math.exp(this.getLogProbability(val));
    }

    public double getLogProbability(double val){
        return Vector.dotProduct(this.naturalParameters,this.getSufficientStatistics(val)) + this.computeLogBaseMeasure(val) + this.computeLogNormalizer(this.naturalParameters);
    }

    public SufficientStatistics getSufficientStatistics(DataInstance data){
        return this.getSufficientStatistics(data.getValue(this.getVariable()));
    }

    public abstract SufficientStatistics getSufficientStatistics(double val);

    public abstract NaturalParameters getNaturalFromMomentParameters(MomentParameters momentParameters);

    public abstract MomentParameters getMomentFromNaturalParameters(NaturalParameters naturalParameters);

}
