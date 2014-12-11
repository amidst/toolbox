package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;

/**
 * Created by ana@cs.aau.dk on 08/12/14.
 */
public class Normal_NormalParents extends EF_ConditionalDistribution  {


    public Normal_NormalParents(Variable var_, List<Variable> parents_) {

        this.var = var_;
        this.parents = parents_;

        if (var_.getDistributionType()!= DistType.GAUSSIAN)
            throw new UnsupportedOperationException("Creating a Normal|Normal EF distribution for a non-gaussian child variable.");

        for (Variable v : parents) {
            if (v.getDistributionType() != DistType.GAUSSIAN)
                throw new UnsupportedOperationException("Creating a Normal|Normal EF distribution for a non-gaussian parent variable.");
        }

    }

    @Override
    public void updateNaturalFromMomentParameters() {

    }

    @Override
    public void updateMomentFromNaturalParameters() {

    }

    @Override
    public SufficientStatistics getSufficientStatistics(DataInstance data) {
        return null;
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 0;
    }

    @Override
    public double computeLogBaseMeasure(DataInstance dataInstance) {
        return 0;
    }

    @Override
    public double computeLogNormalizer() {
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
}
