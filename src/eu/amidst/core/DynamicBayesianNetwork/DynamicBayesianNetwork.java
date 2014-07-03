package eu.amidst.core.DynamicBayesianNetwork;

import eu.amidst.core.StaticBayesianNetwork.ParentSet;

/**
 * Created by afa on 03/07/14.
 */
public interface DynamicBayesianNetwork {
    ParentSet getParentSet(int varID);

    ParentSet getParentSetTimet(int varID);

    int getMarkovOrder();

    void learnParameters();
}
