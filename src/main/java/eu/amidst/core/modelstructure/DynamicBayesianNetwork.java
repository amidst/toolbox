package eu.amidst.core.modelstructure;


import eu.amidst.core.header.DistType;
import eu.amidst.core.distribution.*;
import eu.amidst.core.header.DynamicModelHeader;
import eu.amidst.core.header.Variable;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by afa on 03/07/14.
 */
public class DynamicBayesianNetwork{
    private ParentSet[] parentSetTime0;
    private ParentSet[] parentSetTimeT;
    private Distribution[] distributionsTime0;
    private Distribution[] distributionsTimeT;
    private DynamicModelHeader modelHeader;

    /**
     *
     * @param modelHeader
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

    public static DynamicBayesianNetwork newDynamicBayesianNetwork(DynamicModelHeader modelHeader){
        return new DynamicBayesianNetwork(modelHeader);
    }


    public void initializeDistributions() {
        //Parents should have been assigned before calling this method (from dynamicmodelling.models)

        for (Variable var : modelHeader.getVariables()) {
            int varID = var.getVarID();
            /* Distributions at time t */
            this.distributionsTimeT[varID] = DistributionBuilder.newDistribution(var, parentSetTimeT[varID].getParents());

            /* Distributions at time 0 */
            this.distributionsTime0[varID] = DistributionBuilder.newDistribution(var, parentSetTime0[varID].getParents());
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
