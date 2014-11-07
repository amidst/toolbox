package eu.amidst.core.distribution;

import eu.amidst.core.header.Assignment;
import eu.amidst.core.header.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.List;

/**
 * Created by afa on 04/11/14.
 */
public class Multinomial_MultinomialParents implements ConditionalDistribution {


    private Variable var;

    private List<Variable> parents;

    private Multinomial[] probabilities;

    public Multinomial_MultinomialParents(Variable var, List<Variable> parents) {

        this.var = var;
        this.parents = parents;

        //Initialize the distribution uniformly for each configuration of the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(parents);
        this.probabilities = new Multinomial[size];
        for (int i=0;i<size;i++) {
            this.probabilities[i] = new Multinomial(var);
        }
    }

    public void setMultinomial(int position, Multinomial multinomialDistribution) {
        this.probabilities[position] = multinomialDistribution;
    }

    public void setMultinomial(Assignment parentsAssignment, Multinomial multinomialDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents,parentsAssignment);
        this.setMultinomial(position,multinomialDistribution);
    }

    public Multinomial getMultinomial(Assignment parentsAssignment) {

        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents,parentsAssignment);

        return probabilities[position];
    }


    @Override
    public List<Variable> getConditioningVariables() {
        return parents;
    }

    @Override
    public double getLogProbability(double value, Assignment parentsAssignment) {

        return this.getMultinomial(parentsAssignment).getLogProbability(value);
    }

    @Override
    public double getProbability(double value, Assignment parentsAssignment) {

       return this.getMultinomial(parentsAssignment).getProbability(value);
    }

    @Override
    public Variable getVariable() {
        return var;
    }
}
