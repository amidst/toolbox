/**
 ******************* ISSUE LIST **************************
 *
 * 1. Change 0 & T by Past & Present
 *
 * ********************************************************
 */

package eu.amidst.core.modelstructure.dynamics.impl;


import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.modelstructure.dynamics.DynamicBayesianNetwork;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.dynamics.DynamicModelHeader;
import eu.amidst.core.header.Variable;


/**
 * Created by afa on 03/07/14.
 */
public class DynamicBayesNetworkImpl implements DynamicBayesianNetwork {
    private ParentSet[] parentSetTime0;
    private ParentSet[] parentSetTimeT;
    private Distribution[] distributionsTime0;
    private Distribution[] distributionsTimeT;
    private DynamicModelHeader modelHeader;

    public DynamicBayesNetworkImpl(DynamicModelHeader modelHeader){

    }


    @Override
    public void initDistributions() {

    }

    @Override
    public int getNumberOfNodes() {
        return this.modelHeader.getNumberOfVars();
    }

    @Override
    public DynamicModelHeader getDynamicModelHeader() {
        return this.modelHeader;
    }

    @Override
    public Variable getVariableById(int varID) {
        return this.modelHeader.getVariableById(varID);
    }

    @Override
    public Variable getTemporalCloneById(int varID) {
        return this.modelHeader.getTemporalCloneById(varID);
    }

    @Override
    public Variable getTemporalCloneFromVariable(Variable variable) {
        return this.modelHeader.getTemporalCloneFromVariable(variable);
    }


    @Override
    public ParentSet getParentSetTimeT(int varID) {
        return this.parentSetTimeT[varID];
    }

    @Override
    public Distribution getDistributionTimeT(int varId) {
        return this.distributionsTimeT[varId];
    }


    @Override
    public ParentSet getParentSetTime0(int varID) {
        return this.parentSetTime0[varID];
    }

    @Override
    public Distribution getDistributionTime0(int varId) {
        return this.distributionsTime0[varId];
    }

}