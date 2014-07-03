package eu.amidst.core.DynamicBayesianNetwork;

/**
 * Created by afa on 03/07/14.
 */
public interface DynamicBayesianNetwork {
    eu.amidst.core.StaticBayesianNetwork.ParentSet getParentSet(int varID);

    eu.amidst.core.StaticBayesianNetwork.ParentSet getParentSetTimet(int varID);

    int getMarkovOrder();

    void learnParameters();
}
