package eu.amidst.core.models;


import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

import java.util.List;


/**
 * <h2>This class implements a dynamic Bayesian network.</h2>
 *
 * @author afalvarez@ual.es, andres@cs.aau.dk & ana@cs.aau.dk
 * @version 1.0
 * @since 2014-07-3
 *
 */
public class DynamicBayesianNetwork{


    /**
     * It contains the distributions for all variables at time 0.
     */
    private ConditionalDistribution[] distributionsTime0;

    /**
     * It contains the distributions for all variables at time T.
     */
    private ConditionalDistribution[] distributionsTimeT;

    private DynamicDAG dynamicDAG;

    /**
     * The private class constructor
     * @param dynamicDAG_
     */
    private DynamicBayesianNetwork(DynamicDAG dynamicDAG_){
        dynamicDAG = dynamicDAG_;
        this.initializeDistributions();
    }

    /**
     * The class public constructor, as a factory pattern
     * @param dynamicDAG_
     * @return A <code>DynamicBayesianNetwork</code> with the given list of variables
     */
    public static DynamicBayesianNetwork newDynamicBayesianNetwork(DynamicDAG dynamicDAG_){
        return new DynamicBayesianNetwork(dynamicDAG_);
    }


    /**
     * Initialize the Distributions of the variables based on their <code>StateSpaceType</code>
     */
    private void initializeDistributions() {
        //Parents should have been assigned before calling this method (from dynamicmodelling.models)

        this.distributionsTime0 = new ConditionalDistribution[this.getDynamicVariables().getNumberOfVars()];
        this.distributionsTimeT = new ConditionalDistribution[this.getDynamicVariables().getNumberOfVars()];

        for (Variable var : this.getDynamicVariables().getListOfDynamicVariables()) {
            int varID = var.getVarID();

            /* Distributions at time t */
            this.distributionsTimeT[varID] = DistributionBuilder.newDistribution(var, this.dynamicDAG.getParentSetTimeT(var).getParents());
            this.dynamicDAG.getParentSetTimeT(var).blockParents();

            /* Distributions at time 0 */
            this.distributionsTime0[varID] = DistributionBuilder.newDistribution(var, this.dynamicDAG.getParentSetTime0(var).getParents());
            this.dynamicDAG.getParentSetTime0(var).blockParents();
        }
    }

    public int getNumberOfDynamicVars() {
        return this.getDynamicVariables().getNumberOfVars();
    }

    public DynamicVariables getDynamicVariables() {
        return this.dynamicDAG.getDynamicVariables();
    }

    /*
    public Variable getVariableById(int varID) {
        return this.getListOfDynamicVariables().getVariableById(varID);
    }

    public Variable getTemporalCloneById(int varID) {
        return this.getListOfDynamicVariables().getTemporalCloneById(varID);
    }

    public Variable getTemporalCloneFromVariable(Variable variable) {
        return this.getListOfDynamicVariables().getTemporalCloneFromVariable(variable);
    }
    */


    public <E extends ConditionalDistribution> E getDistributionTimeT(Variable var) {
        return (E) this.distributionsTimeT[var.getVarID()];
    }

    public <E extends ConditionalDistribution> E getDistributionTime0(Variable var) {
        return (E) this.distributionsTime0[var.getVarID()];
    }

    public void setDistributionsTimeT(Variable var, ConditionalDistribution distribution){
        this.distributionsTimeT[var.getVarID()] = distribution;
    }

    public void setDistributionsTime0(Variable var, ConditionalDistribution distribution){
        this.distributionsTime0[var.getVarID()] = distribution;
    }

    public DynamicDAG getDynamicDAG (){
        return this.dynamicDAG;
    }

    public int getNumberOfVars() {
        return this.getDynamicVariables().getNumberOfVars();
    }

    public List<Variable> getListOfDynamicVariables() {
        return this.getDynamicVariables().getListOfDynamicVariables();
    }
}
