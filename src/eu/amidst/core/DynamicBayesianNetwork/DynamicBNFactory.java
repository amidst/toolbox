package eu.amidst.core.DynamicBayesianNetwork;


import eu.amidst.core.DynamicBayesianNetwork.impl.DynamicBayesNet;

/**
 * Created by andres on 11/07/14.
 */
public class DynamicBNFactory {


    public static DynamicBayesianNetwork createBN(){
        return new DynamicBayesNet();
    }
}