package eu.amidst.core.StaticBayesianNetwork;

import eu.amidst.core.Estimators.Estimator;
import eu.amidst.core.StaticDataBase.StaticModelHeader;
import eu.amidst.core.StaticDataBase.StaticDataHeader;


/**
 * Created by afa on 02/07/14.
 */
public class BayesNet implements BayesianNetwork {
    private ParentSet[] parents;
    private Estimator[] estimators;
    private StaticModelHeader modelHeader;
    private StaticDataHeader dataHeader;

    public void learnParameters() {
    }
}
