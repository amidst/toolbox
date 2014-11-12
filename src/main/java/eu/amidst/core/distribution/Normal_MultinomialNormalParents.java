/**
 ******************* ISSUES **************************
 *
 * 1. CODING: - this.multinomialParents or multinomialParents? Common criteria.
 *
 *             - methods are ordered? alphabetically?
 *
 *
 * ***************************************************
 */


package eu.amidst.core.distribution;

import eu.amidst.core.header.DistType;
import eu.amidst.core.header.Assignment;
import eu.amidst.core.header.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <h2>This class implements a conditional distribution of a normal variable given a set of multinomial and normal
 * parents.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-4
 */

public class Normal_MultinomialNormalParents implements ConditionalDistribution {

    /**
     * The variable of the distribution
     */
    private Variable var;

    /**
     * The list of multinomial parents
     */
    private List<Variable> multinomialParents;

    /**
     * The list of normal parents
     */
    private List<Variable> normalParents;

    /**
     * The set of parents
     */
    private List<Variable> parents;

    /**
     * An array of <code>Normal_NormalParents</code> objects, one for each configuration of the multinomial parents. These objects are
     * ordered according to the criteria implemented in class utils.MultinomialIndex
     */
    private Normal_NormalParents[] distribution;


    /**
     * The class constructor.
     * @param var The variable of the distribution.
     * @param parents The set of parent variables.
     */
    public Normal_MultinomialNormalParents(Variable var, List<Variable> parents) {

        this.var = var;
        this.multinomialParents = new ArrayList<Variable>();
        this.normalParents = new ArrayList<Variable>();
        this.parents = parents;


        for (Variable v : parents) {
            if (v.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                this.multinomialParents.add(var);
            } else {
                this.normalParents.add(var);
            }
        }

        //Initialize the distribution with a Normal_NormalParents(var, normalParents) for each configuration of the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);
        this.distribution = new Normal_NormalParents[size];
        for (int i = 0; i < size; i++) {
            this.distribution[i] = new Normal_NormalParents(var, normalParents);
        }

        //Make them unmodifiable
        this.multinomialParents = Collections.unmodifiableList(this.multinomialParents);
        this.normalParents = Collections.unmodifiableList(this.normalParents);
        this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Gets the set of conditioning variables.
     * @return A <code>List</code> with the conditioning variables.
     */
    public List<Variable> getConditioningVariables() {
        return parents;
    }

    /**
     * Gets a <code>Normal_NormalParentsDistribution</code> distribution conditioned to an assignment over a set of
     * Multinomial parents. Let X and Y two sets of Normal variables, and Z a set of Multinomial. Then this method
     * computes f(X|Y,Z=z).
     * @param assignment An assignment over a set of parents. For generality reasons, apart from the Multinomial
     *                   parents, the assignment contains values for the Normal parents as well (although they are
     *                   not used in this case).
     * @return a <code>Normal_NormalParentsDistribution</code> distribution conditioned to the assignment given as
     * argument.
     */
    public Normal_NormalParents getNormal_NormalParentsDistribution(Assignment assignment) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, assignment);
        return distribution[position];
    }

    /**
     * Sets a <code>Normal_NormalParents</code> distribution to a given position in the array of distributions.
     * @param position The position in which the distribution is set.
     * @param distribution A <code>Normal_NormalParents</code> distribution.
     */
    public void setNormal_NormalParentsDistribution(int position, Normal_NormalParents distribution) {
        this.distribution[position] = distribution;
    }

    /**
     * Sets a <code>Normal_NormalParents</code> distribution to the array of distributions in a position determined by
     * an given <code>Assignment</code>. Note that this assignment contains values for the Normal parents as well
     * (although they are not used in this case).
     * @param assignment An <code>Assignment</code> for the parents variables.
     * @param distribution A <code>Normal_NormalParents</code> distribution.
     */
    public void setNormal_NormalParentsDistribution(Assignment assignment, Normal_NormalParents distribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, assignment);
        this.setNormal_NormalParentsDistribution(position, distribution);
    }

    /**
     * Evaluates the resulting univariate density function in a point after restricting the distribution to a
     * given parent <code>Assignment</code>.
     * @param value A <code>double</code> value of the variable to be evaluated.
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> with the corresponding density value.
     */
    public double getProbability(double value, Assignment parentAssignment) {
        return getNormal_NormalParentsDistribution(parentAssignment).getProbability(value, parentAssignment);
    }

    /**
     * Computes the logarithm of the evaluated density function in a point after restricting the distribution to a
     * given parent <code>Assignment</code>.
     * @param value A <code>double</code> value of the variable to be evaluated.
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> with the logarithm of the corresponding density value.
     */
    public double getLogProbability(double value, Assignment parentAssignment) {
        return getNormal_NormalParentsDistribution(parentAssignment).getLogProbability(value, parentAssignment);
    }

    /**
     * Gets the variable of the distribution.
     * @return A <code>Variable</code> object.
     */
    public Variable getVariable() {
        return var;
    }
}
