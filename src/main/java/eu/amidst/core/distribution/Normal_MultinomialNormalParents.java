package eu.amidst.core.distribution;

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
            v.getDistributionKind();
            if (){
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

    public CLG getCLG(Assignment assignment) {
        int position =  MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, assignment);
        return distribution[position];
    }

    public void setCLG (int position, CLG CLG_distribution){
        this.distribution[position] = CLG_distribution;
    }

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
