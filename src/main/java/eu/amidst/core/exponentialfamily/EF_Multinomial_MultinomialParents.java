/**
 * ****************** ISSUE LIST ******************************
 *
 *
 *
 * 1. getConditioningVariables change to getParentsVariables()
 *
 *
 *
 * **********************************************************
 */


package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.header.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.Collections;
import java.util.List;

public class EF_Multinomial_MultinomialParents extends EF_ConditionalDistribution {


    private EF_Multinomial[] probabilities;


    public EF_Multinomial_MultinomialParents(Variable var, List<Variable> parents) {

        this.var = var;
        this.parents = parents;

        // Computes the size of the array of probabilities as the number of possible assignments for the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(this.parents);

        // Initialize the distribution uniformly for each configuration of the parents.
        this.probabilities = new EF_Multinomial[size];
        for (int i = 0; i < size; i++) {
            this.probabilities[i] = new EF_Multinomial(var);
        }

        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);

    }

    public void setEF_Multinomial(int position, EF_Multinomial ef_multinomial) {
        this.probabilities[position] = ef_multinomial;
    }

    public EF_Multinomial getEF_Multinomial(DataInstance dataInstancet) {
        int position = MultinomialIndex.getIndexFromDataInstance(this.parents, dataInstancet);
        return probabilities[position];
    }

    @Override
    public double getLogConditionalProbability(DataInstance dataInstance) {
        double value = dataInstance.getValue(this.var);
        return this.getEF_Multinomial(dataInstance).getLogProbability(value);
    }

    @Override
    public EF_UnivariateDistribution getEFUnivariateByInstantiatingTo(DataInstance instance) {
        return this.getEF_Multinomial(instance);
    }

    @Override
    public SufficientStatistics getSufficientStatistics(DataInstance instance) {
        return null;
    }

}
