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


package eu.amidst.core.distribution;

import eu.amidst.core.header.Assignment;
import eu.amidst.core.header.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.Collections;
import java.util.List;

/**
 * <h2>This class implements a conditional distribution of a multinomial variable given a set of multinomial parents.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-4
 */
public class Multinomial_MultinomialParents implements ConditionalDistribution {

    /**
     * The variable of the distribution
     */
    private Variable var;

    /**
     * The list of parents of the variable
     */
    private List<Variable> parents;

    /**
     * An array of <code>Multinomial</code> objects, one for each configuration of the parents. These objects are ordered
     * according to the criteria implemented in class utils.MultinomialIndex
     */
    private Multinomial[] probabilities;

    /**
     * The class constructor.
     * @param var The variable of the distribution.
     * @param parents The set of parents of the variable.
     */
    public Multinomial_MultinomialParents(Variable var, List<Variable> parents) {

        this.var = var;
        this.parents = parents;

        // Computes the size of the array of probabilities as the number of possible assignments for the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(this.parents);

        // Initialize the distribution uniformly for each configuration of the parents.
        this.probabilities = new Multinomial[size];
        for (int i = 0; i < size; i++) {
            this.probabilities[i] = new Multinomial(var);
        }


        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);

    }

    /**
     * Sets a <code>Multinomial</code> distribution in a given position in the array of probabilities.
     * @param position The position in which the distribution is set.
     * @param multinomialDistribution A <code>Multinomial</code> object.
     */
    public void setMultinomial(int position, Multinomial multinomialDistribution) {
        this.probabilities[position] = multinomialDistribution;
    }

    /**
     * Sets a <code>Multinomial</code> distribution in a position in the array of probabilities determined by a given
     * parents assignment.
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @param multinomialDistribution A <code>Multinomial</code> object.
     */
    public void setMultinomial(Assignment parentAssignment, Multinomial multinomialDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentAssignment);
        this.setMultinomial(position, multinomialDistribution);
    }

    /**
     * Gets the <code>Multinomial</code> distribution for given a parents assignment.
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @return A <code>Multinomial</code> object.
     */
    public Multinomial getMultinomial(Assignment parentAssignment) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentAssignment);
        return probabilities[position];
    }

    /**
     * Gets the set of conditioning variables.
     * @return A <code>unmodifiable List</code> with the conditioning variables.
     */
    @Override
    public List<Variable> getConditioningVariables() {
        return parents;
    }

    /**
     * Computes the probability of the variable for a given state and a parent assignment.
     * @param value The position of the variable state in the array of probabilities (within the corresponding
     *              Multinomial object after conditioning to the parent assignment).
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> value with the probability.
     */
    @Override
    public double getProbability(double value, Assignment parentAssignment) {
        return this.getMultinomial(parentAssignment).getProbability(value);
    }

    /**
     * Computes the logarithm of the probability of the variable for a given state and a parent assignment.
     * @param value The position of the variable state in the array of probabilities (within the corresponding
     *              Multinomial object after conditioning to the parent assignment).
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> value with the logarithm of the probability.
     */
    @Override
    public double getLogProbability(double value, Assignment parentAssignment) {
        return this.getMultinomial(parentAssignment).getLogProbability(value);
    }

    /**
     * Gets the variable of the distribution.
     * @return A <code>Variable</code> object.
     */
    @Override
    public Variable getVariable() {
        return var;
    }
}
