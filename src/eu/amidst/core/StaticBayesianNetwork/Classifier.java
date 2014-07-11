package eu.amidst.core.StaticBayesianNetwork;

import eu.amidst.core.StaticDataBase.DataInstance;

/**
 * Created by afa on 02/07/14.
 */
public interface Classifier {
    void predict(DataInstance instance);
}
