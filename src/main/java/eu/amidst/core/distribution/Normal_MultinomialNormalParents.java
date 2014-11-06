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

import eu.amidst.core.database.statics.readers.DistType;
import eu.amidst.core.header.Assignment;
import eu.amidst.core.header.Variable;
import eu.amidst.core.utils.MultinomialIndex;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 04/11/14.
 */
public class Normal_MultinomialNormalParents implements ConditionalDistribution {

    // This class can't implement the interface ConditionalDistribution because methods
    // getConditioningVariables, getProbability and getLogProbability don't have the same arguments.
    // Solution: remove the interface ConditionalDistribution?

    private Variable var;
    private List<Variable> multinomialParents;
    private List<Variable> normalParents;
    private List<Variable> parents;

    private CLG[] distribution;


    public Normal_MultinomialNormalParents (Variable var, List<Variable> parents) {
        this.var = var;
        this.multinomialParents = new ArrayList<Variable>();
        this.normalParents = new ArrayList<Variable>();

        for (Variable v:parents){
            if (v.getDistributionType().compareTo(DistType.MULTINOMIAL)==0){
                this.multinomialParents.add(var);
            }else{
                this.normalParents.add(var);
            }
        }

        //Initialize the distribution with a CLG(var, normalParents) for each configuration of the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);
        this.distribution = new CLG[size];
        for (int i=0;i<size;i++) {
            this.distribution[i] = new CLG(var, normalParents);
        }
    }

    public List<Variable> getConditioningVariables() {
        return parents;
    }

    /**
     * Gets a CLG distribution restricted to an assignment over a set of Multinomial parents. Let X and Y two sets of
     * Normal variables, and Z a set of Multinomial. Then this method computes f(X|Y,Z=z).
     * @param assignment An assignment over a set of parents. For generality reasons, apart from the Multinomial
     *                   parents, the assignment contains values for the Normal parents as well (although they are
     *                   not used in this case).
     * @return a CLG distribution restricted to the assignment given as argument.
     */
    public CLG getCLG(Assignment assignment) {
        int position =  MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, assignment);
        return distribution[position];
    }

    /**
     * Sets a CLG distribution to the array of distributions in a position.
     * @param position The position in which the CLG distribution is set.
     * @param CLG_distribution A CLG distribution.
     */
    public void setCLG (int position, CLG CLG_distribution){
        this.distribution[position] = CLG_distribution;
    }

    /**
     * Sets a CLG distribution to the array of distributions in the position determined by an Assignment. Note that the
     * Assignment contains values for the Normal parents as well (although they are not used in this case).
     * @param assignment
     * @param CLG_Distribution
     */
    public void setCLG(Assignment assignment, CLG CLG_Distribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, assignment);
        this.setCLG(position, CLG_Distribution);
    }

    public double getProbability(double value, Assignment assignment) {
        return getCLG(assignment).getProbability(value, assignment);
    }

    public double getLogProbability(double value, Assignment assignment) {
        return getCLG(assignment).getLogProbability(value, assignment);
    }

    public Variable getVariable() {
        return var;
    }
}
