/**
 ******************* ISSUE LIST **************************************************************************************
 *
 * 1. Make SufficientStatics an static class to avoid the creation of an object in each call to getSuffStatistics();
 * 2. Make naturalParameters and momentParameters statics?
 * *******************************************************************************************************************
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.utils.Vector;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public abstract class EF_Distribution {
    /**
     * The variable of the distribution
     */
    protected Variable var = null;

    protected NaturalParameters naturalParameters;

    protected MomentParameters momentParameters;

    /**
     * Gets the variable of the distribution
     *
     * @return A <code>Variable</code> object.
     */
    public final Variable getVariable() {

        return this.var;
    }

    public final void setVariable(Variable var_){
        if (this.var!=null && (this.var.getDistributionType()!=var_.getDistributionType()))
            throw new IllegalArgumentException("Variable does not have equal distribution type or state space");

        this.var = var_;
    }

    public final NaturalParameters getNaturalParameters() {

        return this.naturalParameters;
    }


    public final MomentParameters getMomentParameters() {

        return this.momentParameters;
    }

    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters=parameters;//.copy(parameters);
        this.updateMomentFromNaturalParameters();
    }

    public void setMomentParameters(SufficientStatistics parameters) {
        this.momentParameters=(MomentParameters)parameters;
        this.updateNaturalFromMomentParameters();
    }

    public void setMomentParameters(MomentParameters parameters) {
        this.momentParameters=parameters;// .copy(parameters);
        this.updateNaturalFromMomentParameters();
    }

    public abstract void updateNaturalFromMomentParameters();

    public abstract void updateMomentFromNaturalParameters();

    public abstract SufficientStatistics getSufficientStatistics(Assignment data);

    public abstract int sizeOfSufficientStatistics();

    public abstract double computeLogBaseMeasure(Assignment dataInstance);

    public abstract double computeLogNormalizer();

    public double computeProbabilityOf(Assignment dataInstance){
        return Math.exp(this.computeLogProbabilityOf(dataInstance));
    }

    //TODO: the logbasemeasure and the lognormalizer are positives or negatives terms (Andres)
    public double computeLogProbabilityOf(Assignment dataInstance){
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(dataInstance)) + this.computeLogBaseMeasure(dataInstance) - this.computeLogNormalizer();
    }

    public abstract Vector createZeroedVector();

    public MomentParameters createZeroedMomentParameters(){
        return (MomentParameters)this.createZeroedVector();
    }

    public SufficientStatistics createZeroedSufficientStatistics(){
        return (SufficientStatistics)this.createZeroedVector();
    }

    public NaturalParameters createZeroedNaturalParameters(){
        return (NaturalParameters)this.createZeroedVector();
    }


}
