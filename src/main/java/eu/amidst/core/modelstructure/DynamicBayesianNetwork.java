package eu.amidst.core.modelstructure;


import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;


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
     * It contains the ParentSets for all variables at time 0.
     */
    private ParentSet[] parentSetTime0;

    /**
    * It contains the ParentSets for all variables at time T.
    */
    private ParentSet[] parentSetTimeT;

    /**
     * It contains the distributions for all variables at time 0.
     */
    private Distribution[] distributionsTime0;

    /**
     * It contains the distributions for all variables at time T.
     */
    private Distribution[] distributionsTimeT;

    /**
     * It contains a pointer to the variables (list of variables).
     */
    private DynamicVariables variables;

    /**
     * The private class constructor
     * @param variables The variables or list of variables
     */
    private DynamicBayesianNetwork(DynamicVariables variables){
        this.variables = variables;
        this.parentSetTime0 = new ParentSet[variables.getNumberOfVars()];
        this.parentSetTimeT = new ParentSet[variables.getNumberOfVars()];
        this.distributionsTime0 = new Distribution[variables.getNumberOfVars()];
        this.distributionsTimeT = new Distribution[variables.getNumberOfVars()];
        for (int i=0;i<variables.getNumberOfVars();i++) {
            parentSetTime0[i] = ParentSet.newParentSet();
            parentSetTimeT[i] = ParentSet.newParentSet();
        }
    }

    /**
     * The class public constructor, as a factory pattern
     * @param variables The variables or list of variables
     * @return A <code>DynamicBayesianNetwork</code> with the given list of variables
     */
    public static DynamicBayesianNetwork newDynamicBayesianNetwork(DynamicVariables variables){
        return new DynamicBayesianNetwork(variables);
    }


    /**
     * Initialize the Distributions of the variables based on their <code>StateSpaceType</code>
     */
    public void initializeDistributions() {
        //Parents should have been assigned before calling this method (from dynamicmodelling.models)

        for (Variable var : variables.getDynamicVariables()) {
            int varID = var.getVarID();

            /* Distributions at time t */
            this.distributionsTimeT[varID] = DistributionBuilder.newDistribution(var, parentSetTimeT[varID].getParents());
            parentSetTimeT[varID].blockParents();

            /* Distributions at time 0 */
            this.distributionsTime0[varID] = DistributionBuilder.newDistribution(var, parentSetTime0[varID].getParents());
            parentSetTime0[varID].blockParents();
        }
    }

    /* Methods accessing the variables in the variables*/
    public int getNumberOfNodes() {
        return this.variables.getNumberOfVars();
    }

    public DynamicVariables getDynamicVariables() {
        return this.variables;
    }

    public Variable getVariableById(int varID) {
        return this.variables.getVariableById(varID);
    }

    public Variable getTemporalCloneById(int varID) {
        return this.variables.getTemporalCloneById(varID);
    }

    public Variable getTemporalCloneFromVariable(Variable variable) {
        return this.variables.getTemporalCloneFromVariable(variable);
    }

    /* Methods accessing structure at time T*/
    public ParentSet getParentSetTimeT(Variable var) {
        return this.parentSetTimeT[var.getVarID()];
    }

    public Distribution getDistributionTimeT(Variable var) {
        return this.distributionsTimeT[var.getVarID()];
    }


    /* Methods accessing structure at time 0*/
    public ParentSet getParentSetTime0(Variable var) {
        return this.parentSetTime0[var.getVarID()];
    }

    public Distribution getDistributionTime0(Variable var) {
        return this.distributionsTime0[var.getVarID()];
    }

}
