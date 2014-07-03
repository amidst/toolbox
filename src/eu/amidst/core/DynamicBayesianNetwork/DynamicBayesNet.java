package eu.amidst.core.DynamicBayesianNetwork;

import eu.amidst.core.StaticBayesianNetwork.ParentSet;
import eu.amidst.core.Estimators.Estimator;

/**
 * Created by afa on 03/07/14.
 */
public class DynamicBayesNet implements DynamicBayesianNetwork {
    private ParentSet[][] parentSetTimeBeforeT;
    private eu.amidst.core.StaticBayesianNetwork.ParentSet[] parentSetTimet;
    private Estimator[][] estimatorTimeBeforet;
    private Estimator[] estimatorTimet;
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

    }
}
