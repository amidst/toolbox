package eu.amidst.core.distribution;

import eu.amidst.core.header.statics.Assignment;
import eu.amidst.core.header.statics.Variable;
import eu.amidst.core.utils.MultinomialIndex;


import java.util.List;

/**
 * Created by afa on 04/11/14.
 */
public class Normal_MultinomialNormalParents {//implements ConditionalDistribution {

    // This class can't implement the interface ConditionalDistribution because methods
    // getConditioningVariables, getProbability and getLogProbability don't have the same arguments.
    // Solution: remove the interface ConditionalDistribution?

    private Variable var;
    private List<Variable> multinomialParents;
    private List<Variable> normalParents;

    private CLG[] distribution;


    public Normal_MultinomialNormalParents (Variable var, List<Variable> multinomialParents, List<Variable> normalParents) {
        this.var = var;
        this.multinomialParents = multinomialParents;
        this.normalParents = normalParents;

        //Initialize the distribution with a CLG(var, normalParents) for each configuration of the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);
        this.distribution = new CLG[size];
        for (int i=0;i<size;i++) {
            this.distribution[i] = new CLG(var, normalParents);
        }
    }

    public List<Variable> getMultinomialParents() {
        return multinomialParents;
    }

    public List<Variable> getNormalParents() {
        return normalParents;
    }

    public CLG getCLG(Assignment multinomialParentsAssignment) {
        int position =  MultinomialIndex.getIndexFromVariableAssignment(multinomialParentsAssignment);
        return distribution[position];
    }

    public void setCLG (int position, CLG CLG_distribution){
        this.distribution[position] = CLG_distribution;
    }

    public void setCLG(Assignment multinomialParentsAssignment, CLG CLG_Distribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(multinomialParentsAssignment);
        this.setCLG(position, CLG_Distribution);
    }

    public double getProbability(double value, Assignment multinomialParentsAssignment, Assignment normalParentsAssignment) {
        return getCLG(multinomialParentsAssignment).getProbability(value, normalParentsAssignment);
    }

    public double getLogProbability(double value, Assignment multinomialParentsAssignment, Assignment normalParentsAssignment) {
        return getCLG(multinomialParentsAssignment).getLogProbability(value, normalParentsAssignment);
    }

    public Variable getVariable() {
        return var;
    }
}
