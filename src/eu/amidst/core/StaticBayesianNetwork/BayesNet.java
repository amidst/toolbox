package eu.amidst.core.StaticBayesianNetwork;

import eu.amidst.core.Estimators.Estimator;

/**
 * Created by afa on 02/07/14.
 */
public class BayesNet implements BayesianNetwork {
    private ParentSet[] parents;
    private Estimator[] estimators;
    private eu.amidst.core.StaticDataBase.StaticModelHeader modelHeader;
    private eu.amidst.core.StaticDataBase.StaticDataHeader dataHeader;

    public void learnParameters() {
    }
}
