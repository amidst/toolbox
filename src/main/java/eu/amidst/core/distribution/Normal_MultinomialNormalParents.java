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

import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
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

public class Normal_MultinomialNormalParents extends ConditionalDistribution{


    /**
     * The list of multinomial parents
     */
    private List<Variable> multinomialParents;

    /**
     * The list of normal parents
     */
    private List<Variable> normalParents;

    /**
     * An array of <code>Normal_NormalParents</code> objects, one for each configuration of the multinomial parents. These objects are
     * ordered according to the criteria implemented in class utils.MultinomialIndex
     */
    private Normal_NormalParents[] distribution;


    /**
     * The class constructor.
     * @param var1 The variable of the distribution.
     * @param parents1 The set of parent variables.
     */
    public Normal_MultinomialNormalParents(Variable var1, List<Variable> parents1) {

        this.var = var1;
        this.multinomialParents = new ArrayList<Variable>();
        this.normalParents = new ArrayList<Variable>();
        this.parents = parents1;

        for (Variable parent : parents) {

            if (parent.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                this.multinomialParents.add(parent);
            } else {
                this.normalParents.add(parent);
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
    public Normal_NormalParents getNormal_NormalParentsDistribution(int i) {
        return distribution[i];

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


    @Override
    public int getNumberOfFreeParameters() {
        int n=0;
        for(Normal_NormalParents dist:this.getDistribution()){
            n+= dist.getNumberOfFreeParameters();
        }
        return n;
    }

    /**
     * Computes the logarithm of the evaluated density function in a point after restricting the distribution to a
     * given parent <code>Assignment</code>.
     * @param assignment An <code>Assignment</code>
     * @return A <code>double</code> with the logarithm of the corresponding density value.
     */
    public double getLogConditionalProbability(Assignment assignment) {
        return getNormal_NormalParentsDistribution(assignment).getLogConditionalProbability(assignment);
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getNormal_NormalParentsDistribution(assignment).getNormal(assignment);
    }

    public List<Variable> getMultinomialParents() {
        return multinomialParents;
    }


    public List<Variable> getNormalParents() {
        return normalParents;
    }

    public Normal_NormalParents[] getDistribution() {
        return distribution;
    }

    public String label(){
        return "Normal|Multinomial,Normal";
    }

    public int getNumberOfParentAssignments(){
        return this.distribution.length;
    }



    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("");
        for (int i = 0; i < getNumberOfParentAssignments(); i++) {
            str.append(this.getNormal_NormalParentsDistribution(i).toString()+"\n");
        }

        return str.toString();
    }

}
