package eu.amidst.core.StaticBayesianNetwork;

import eu.amidst.core.StaticDataBase.DataInstance;

/**
 * Created by afa on 02/07/14.
 */
public interface Classifier extends BayesianNetwork {
    void predict(DataInstance instance);
}
