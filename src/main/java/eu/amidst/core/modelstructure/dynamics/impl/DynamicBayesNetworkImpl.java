package eu.amidst.core.modelstructure.dynamics.impl;


import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.modelstructure.dynamics.DynamicBayesianNetwork;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.dynamics.DynamicModelHeader;
import eu.amidst.core.header.dynamics.DynamicVariable;


/**
 * Created by afa on 03/07/14.
 */
public class DynamicBayesNetworkImpl implements DynamicBayesianNetwork {
    private ParentSet[][] parentSetTime0;
    private ParentSet[] parentSetTimeT;
    private Distribution[][] estimatorTime0;
    private Distribution[] estimatorTimeT;
    private DynamicModelHeader modelHeader;

    public DynamicBayesNetworkImpl(DynamicModelHeader modelHeader){

    }


    @Override
    public int getMarkovOrder() {
        return this.modelHeader.getMarkovOrder();
    }

    @Override
    public void initEstimators() {

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
    public DynamicVariable getVariableById(int varID) {
        return this.modelHeader.getVariableById(varID);
    }

    @Override
    public DynamicVariable getVariableByTimeId(int varTimeID) {
        return this.getVariableByTimeId(varTimeID);
    }

    @Override
    public ParentSet getParentSetTimeT(int varID) {
        return this.parentSetTimeT[varID];
    }

    @Override
    public Distribution getEstimatorTimeT(int varId) {
        return this.estimatorTimeT[varId];
    }

    @Override
    public ParentSet getParentSetTime0(int varID) {
        return this.parentSetTime0[0][varID];
    }

    @Override
    public Distribution getEstimatorTime0(int varId) {
        return this.estimatorTime0[0][varId];
    }

    @Override
    public ParentSet getParentSetTime0(int varID, int initTime) {
        return this.parentSetTime0[initTime][varID];
    }

    @Override
    public Distribution getEstimatorTime0(int varId, int initTime) {
        return this.estimatorTime0[initTime][varId];
    }

}