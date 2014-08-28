package eu.amidst.core.StaticBayesianNetwork.impl;

import eu.amidst.core.Estimators.Estimator;
import eu.amidst.core.headers.StaticModelHeader;
import eu.amidst.core.headers.StaticDataHeader;
import eu.amidst.core.StaticBayesianNetwork.BayesianNetwork;
import eu.amidst.core.StaticBayesianNetwork.ParentSet;
import eu.amidst.core.headers.Variable;


/**
 * Created by afa on 02/07/14.
 */
public class BayesianNetworkImpl implements BayesianNetwork {
    private ParentSet[] parents;
    private Estimator[] estimators;
    private StaticModelHeader modelHeader;
    private StaticDataHeader dataHeader;

    public BayesianNetworkImpl(StaticModelHeader modelHeader){

    }
    public void learnParameters() {
    }

    @Override
    public ParentSet getParentSet(int varID) {
        return null;
    }

    @Override
    public void initEstimators() {

    }

    @Override
    public Estimator getEstimator(int varId) {
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
