package eu.amidst.core.distribution;

import eu.amidst.core.header.Assignment;
import eu.amidst.core.header.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 04/11/14.
 */
public class Normal_MultinomialParents implements ConditionalDistribution {

    private Variable var;
    private List<Variable> parents;

    private Normal[] probabilities; //Not sure about the name. Perhaps "distribution" is better?


    /**
     * vasgs
     *
     * @param var
     * @param parents
     */

    public Normal_MultinomialParents(Variable var, ArrayList<Variable> parents) {
        this.var = var;
        this.parents = (List<Variable>)parents.clone();
/*
        this.parents = new ArrayList<Variable>();
        for (Variable v: parents){
            this.parents.add(v);
        }
*/
        //Initialize the distribution uniformly for each configuration of the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(parents);

        this.probabilities = new Normal[size];
        for (int i=0;i<size;i++) {
            this.probabilities[i] = new Normal(var);
        }

    }



    public Normal getNormal(Assignment parentsAssignment) {
        int position =  MultinomialIndex.getIndexFromVariableAssignment(this.parents,parentsAssignment);
        return probabilities[position];
    }


    public void setNormal(int position, Normal normalDistribution) {
        this.probabilities[position] = normalDistribution;
    }

    public void setNormal(Assignment parentsAssignment, Normal normalDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentsAssignment);
        this.setNormal(position,normalDistribution);
    }
    @Override
    public List<Variable> getConditioningVariables() {
        return parents;
    }

    @Override
    public double getProbability(double value, Assignment parentsAssignment) {

       return this.getNormal(parentsAssignment).getProbability(value);
    }

    @Override
    public double getLogProbability(double value, Assignment parentsAssignment) {

        return this.getNormal(parentsAssignment).getLogProbability(value);
    }

    @Override
    public Variable getVariable() {
        return var;
    }
}
