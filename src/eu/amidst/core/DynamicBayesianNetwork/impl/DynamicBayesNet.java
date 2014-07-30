package eu.amidst.core.DynamicBayesianNetwork.impl;

import eu.amidst.core.DynamicBayesianNetwork.DynamicBayesianNetwork;
import eu.amidst.core.StaticBayesianNetwork.ParentSet;
import eu.amidst.core.DynamicDataBase.DynamicModelHeader;
import eu.amidst.core.DynamicDataBase.DynamicDataHeader;

import eu.amidst.core.Estimators.Estimator;

/**
 * Created by afa on 03/07/14.
 */
public class DynamicBayesNet implements DynamicBayesianNetwork {
    private ParentSet[][] parentSetTime0;
    private ParentSet[] parentSetTimeT;
    private Estimator[][] estimatorTime0;
    private Estimator[] estimatorTimeT;
    private DynamicModelHeader modelHeader;
    private DynamicDataHeader dataHeader;

    @Override
    public ParentSet getParentSet(int varID) {
        return null;
    }

    @Override
    public ParentSet getParentSetTimet(int varID) {
        return null;
    }

    @Override
    public int getMarkovOrder() {
        return 0;
    }

    @Override
    public void learnParameters() {

