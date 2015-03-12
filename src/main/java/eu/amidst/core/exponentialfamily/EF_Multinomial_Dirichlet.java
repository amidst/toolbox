package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.BaseDistribution_MultinomialParents;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.CheckVariablesOrder;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 *
 * //TODO suff. stats for first form to be implemented (if required)
 * Created by ana@cs.aau.dk on 27/02/15.
 */
public class EF_Multinomial_Dirichlet extends EF_ConditionalLearningDistribution{


    Variable dirichletVariable;
    int nOfStates;

    public EF_Multinomial_Dirichlet(Variable var, Variable dirichletVariable) {

        if (!var.isMultinomial()) {
            throw new UnsupportedOperationException("Creating a Multinomial_Dirichlet EF distribution for a non-multinomial variable.");
        }
        if (!dirichletVariable.isDirichletParameter()) {
            throw new UnsupportedOperationException("Creating a Multinomial_Dirichlet EF distribution with a non-dirichlet variable.");
        }

        this.var=var;
        nOfStates = var.getNumberOfStates();
        this.dirichletVariable = dirichletVariable;
        this.parents = new ArrayList<>();
        this.parents.add(dirichletVariable);

        this.parametersParentVariables = new ArrayList();
        this.parametersParentVariables.add(dirichletVariable);

        this.parents = CheckVariablesOrder.orderListOfVariables(this.parents);
        this.parametersParentVariables = CheckVariablesOrder.orderListOfVariables(this.parametersParentVariables);

    }

    /**
     * Of the second form (message from all parents to X variable). Needed to calculate the lower bound.
     *
     * @param momentParents
     * @return
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {

        return 0.0;
    }

    /**
     * Of the second form (message from all parents to X variable).
     * @param momentParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {

        NaturalParameters naturalParameters = new ArrayVector(this.nOfStates);
        naturalParameters.copy(momentParents.get(this.dirichletVariable));

        return naturalParameters;
    }

    /**
     * It is the message to one node to its parent @param parent, taking into account the suff. stat. if it is observed
     * or the moment parameters if not, and incorporating the message (with moment param.) received from all co-parents.
     * (Third form EF equations).
     *
     * @param parent
     * @param momentChildCoParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {

        NaturalParameters naturalParameters = new ArrayVector(this.nOfStates);

        naturalParameters.copy(momentChildCoParents.get(this.var));

        return naturalParameters;
    }

    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("No Implemented. This method is no really needed");
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
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
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    @Override
    public Vector createZeroedVector() {
        throw new UnsupportedOperationException("No Implemented. EF_Multinomial_Dirichlet distribution should only be used for learning, use EF_Multinomial for inference.");
    }

    @Override
    public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedParameters) {
        Multinomial multinomial = new Multinomial(this.getVariable());

        Vector vector = expectedParameters.get(this.dirichletVariable);

        for (int i = 0; i < this.nOfStates; i++) {
            multinomial.setProbabilityOfState(i,vector.get(i));

        }
        return new BaseDistribution_MultinomialParents<Multinomial>(new ArrayList(), Arrays.asList(multinomial));
    }
}
