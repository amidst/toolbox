package eu.amidst.core.modelstructure;


import eu.amidst.core.distribution.*;
import eu.amidst.core.header.DynamicModelHeader;
import eu.amidst.core.header.Variable;

import java.util.*;


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
     * It contains a pointer to the modelHeader (list of variables).
     */
    private DynamicModelHeader modelHeader;

    /**
     * The private class constructor
     * @param modelHeader The modelHeader or list of variables
     */
    private DynamicBayesianNetwork(DynamicModelHeader modelHeader){
        this.modelHeader = modelHeader;
        this.parentSetTime0 = new ParentSet[modelHeader.getNumberOfVars()];
        this.parentSetTimeT = new ParentSet[modelHeader.getNumberOfVars()];
        this.distributionsTime0 = new Distribution[modelHeader.getNumberOfVars()];
        this.distributionsTimeT = new Distribution[modelHeader.getNumberOfVars()];
        for (int i=0;i<modelHeader.getNumberOfVars();i++) {
            parentSetTime0[i] = ParentSet.newParentSet();
            parentSetTimeT[i] = ParentSet.newParentSet();
        }
    }

    /**
     * The class public constructor, as a factory pattern
     * @param modelHeader The modelHeader or list of variables
     * @return A <code>DynamicBayesianNetwork</code> with the given header (list of variables)
     */
    public static DynamicBayesianNetwork newDynamicBayesianNetwork(DynamicModelHeader modelHeader){
        return new DynamicBayesianNetwork(modelHeader);
    }


    /**
     * Initialize the Distributions of the variables based on their <code>StateSpaceType</code>
     */
    public void initializeDistributions() {
        //Parents should have been assigned before calling this method (from dynamicmodelling.models)

        for (Variable var : modelHeader.getVariables()) {
            int varID = var.getVarID();

            /* Distributions at time t */
            this.distributionsTimeT[varID] = DistributionBuilder.newDistribution(var, parentSetTimeT[varID].getParents());
            parentSetTimeT[varID].blockParents((ArrayList)Collections.unmodifiableList(parentSetTimeT[varID].getParents()));

            /* Distributions at time 0 */
            this.distributionsTime0[varID] = DistributionBuilder.newDistribution(var, parentSetTime0[varID].getParents());
            parentSetTime0[varID].blockParents((ArrayList)Collections.unmodifiableList(parentSetTime0[varID].getParents()));
        }
    }

    /* Methods accessing the variables in the modelHeader*/
    public int getNumberOfNodes() {
        return this.modelHeader.getNumberOfVars();
    }

    public DynamicModelHeader getDynamicModelHeader() {
        return this.modelHeader;
    }

    public Variable getVariableById(int varID) {
        return this.modelHeader.getVariableById(varID);
    }

    public Variable getTemporalCloneById(int varID) {
        return this.modelHeader.getTemporalCloneById(varID);
    }

    public Variable getTemporalCloneFromVariable(Variable variable) {
        return this.modelHeader.getTemporalCloneFromVariable(variable);
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
