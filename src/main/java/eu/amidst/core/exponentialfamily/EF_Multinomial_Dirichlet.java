package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Map;

/**
 * Created by ana@cs.aau.dk on 27/02/15.
 */
public class EF_Multinomial_Dirichlet extends EF_ConditionalDistribution{


    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        return 0;
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
}
