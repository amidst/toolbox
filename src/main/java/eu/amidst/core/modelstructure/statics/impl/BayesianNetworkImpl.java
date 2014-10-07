package eu.amidst.core.modelstructure.statics.impl;


import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.statics.Variable;
import eu.amidst.core.header.statics.StaticModelHeader;

/**
 * Created by afa on 02/07/14.
 */
public class BayesianNetworkImpl implements BayesianNetwork {
    private ParentSet[] parents;
    private Distribution[] estimators;
    private StaticModelHeader modelHeader;

    public BayesianNetworkImpl(StaticModelHeader modelHeader){

    }

    @Override
    public ParentSet getParentSet(int varID) {
        return null;
    }

    @Override
    public void initEstimators() {

    }

    @Override
    public Distribution getEstimator(int varId) {
        return null;
    }

    @Override
    public int getNumberOfNodes() {
        return 0;
    }

    @Override
    public Variable getVariable(int varID) {
        return null;
    }

    @Override
    public StaticModelHeader getStaticModelHeader() {
        return null;
    }
}
