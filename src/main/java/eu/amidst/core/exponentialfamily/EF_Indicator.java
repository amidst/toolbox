package eu.amidst.core.exponentialfamily;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;

/**
 * Created by andresmasegosa on 13/01/15.
 */
public class EF_Indicator extends EF_ConditionalDistribution{

    Variable indicatorVar;
    EF_ConditionalDistribution conditionalDist;


    public EF_Indicator(Variable var_, Variable indicatorVar_, List<Variable> parents_, EF_ConditionalDistribution conditionalDist_){
        this.var=var_;
        this.indicatorVar = indicatorVar_;
        this.parents=parents_;
        this.conditionalDist=conditionalDist_;
    }

    public double computeLogProbabilityOf(DataInstance dataInstance){
        if (dataInstance.getValue(this.indicatorVar)==0){
            return 0.0;
        }else {
            return this.naturalParameters.dotProduct(this.getSufficientStatistics(dataInstance)) + this.computeLogBaseMeasure(dataInstance) - this.computeLogNormalizer();
        }
    }

    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("No Implemented. This method is no really needed");
    }

    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        return 0;
    }

    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        return null;
    }

    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        return null;
    }
    @Override
    public <E extends ConditionalDistribution> E toConditionalDistribution() {
        return null;
    }

    @Override
    public void updateNaturalFromMomentParameters() {

    }

    @Override
    public void updateMomentFromNaturalParameters() {

    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        return null;
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 0;
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return 0;
    }

    @Override
    public double computeLogNormalizer() {
        return 0;
    }

    @Override
    public Vector createZeroedVector() {
        return null;
    }

    static class IndicatorVector implements SufficientStatistics, MomentParameters, NaturalParameters {

        double indicator;
        Vector baseVector;

        IndicatorVector(Vector baseVector_){
            this.baseVector=baseVector_;
        }

        public double getIndicator() {
            return indicator;
        }

        public Vector getBaseVector() {
            return baseVector;
        }

        @Override
        public double get(int i) {
            return 0;
        }

        @Override
        public void set(int i, double val) {

        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void sum(Vector vector) {

        }

        @Override
        public void copy(Vector vector) {

        }

        @Override
        public void divideBy(double val) {

        }

        @Override
        public double dotProduct(Vector vec) {
            return 0;
        }
    }
}
