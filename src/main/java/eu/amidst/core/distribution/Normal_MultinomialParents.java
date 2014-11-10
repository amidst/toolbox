package eu.amidst.core.distribution;

import eu.amidst.core.header.Assignment;
import eu.amidst.core.header.Variable;
import eu.amidst.core.utils.MultinomialIndex;
import java.util.List;

/**
 * <h2>This class implements a conditional distribution of a normal variable given a set of multinomial parents.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-4
 */
public class Normal_MultinomialParents implements ConditionalDistribution {

    /**
     * The variable of the distribution
     */
    private Variable var;

    /**
     * The list of parent variables
     */
    private List<Variable> parents;

    /**
     * An array of normal distribution, one for each assignment of the multinomial parents
     */
    private Normal[] distribution;

<<<<<<< HEAD
=======
    /**
     * The class constructor.
     * @param var The variable of the distribution.
     * @param parents The set of parent variables.
     */
>>>>>>> 4e21783053c330e69f7bba2d39ec4b654d045040
    public Normal_MultinomialParents(Variable var, List<Variable> parents) {
        this.var = var;
        this.parents = parents;

        //Initialize the distribution uniformly for each configuration of the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(parents);

        this.distribution = new Normal[size];
        for (int i = 0; i < size; i++) {
            this.distribution[i] = new Normal(var);
        }
    }

    /**
     * Gets the corresponding univariate normal distribution after conditioning the distribution to a multinomial
     * parent assignment.
     * @param parentsAssignment An <code>Assignment</code> for the parents.
     * @return A <code>Normal</code> object with the univariate distribution.
     */
    public Normal getNormal(Assignment parentsAssignment) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentsAssignment);
        return distribution[position];
    }

    /**
     * Sets a <code>Normal</code> distribution in a given position in the array of distributions.
     * @param position The position in which the distribution is set.
     * @param normalDistribution The <code>Normal</code> distribution to be set.
     */
    public void setNormal(int position, Normal normalDistribution) {
        this.distribution[position] = normalDistribution;
    }

    /**
     * Sets a <code>Multinomial</code> distribution in a position in the array of distributions determined by a given
     * parents assignment.
     * @param parentsAssignment An <code>Assignment</code> for the parents.
     * @param normalDistribution The <code>Normal</code> distribution to be set.
     */
    public void setNormal(Assignment parentsAssignment, Normal normalDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentsAssignment);
        this.setNormal(position, normalDistribution);
    }

    /**
     * Gets the set of conditioning variables.
     * @return A <code>List</code> with the conditioning variables.
     */
    @Override
    public List<Variable> getConditioningVariables() {
        return parents;
    }

    /**
     * Evaluates the resulting univariate density function in a point after conditioning the distribution to a
     * given parent <code>Assignment</code>.
     * @param value A <code>double</code> value of the variable to be evaluated.
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> with the corresponding density value.
     */
    @Override
    public double getProbability(double value, Assignment parentAssignment) {

        return this.getNormal(parentAssignment).getProbability(value);
    }

    /**
     * Computes the logarithm of the evaluated density function in a point after conditioning the distribution to a
     * given parent <code>Assignment</code>.
     * @param value A <code>double</code> value of the variable to be evaluated.
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> with the logarithm of the corresponding density value.
     */
    @Override
    public double getLogProbability(double value, Assignment parentAssignment) {

        return this.getNormal(parentAssignment).getLogProbability(value);
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
