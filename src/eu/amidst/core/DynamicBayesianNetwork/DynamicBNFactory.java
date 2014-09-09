package eu.amidst.core.DynamicBayesianNetwork;


import eu.amidst.core.DynamicBayesianNetwork.impl.DynamicBayesNetworkImpl;
import eu.amidst.core.headers.DynamicModelHeader;

/**
 * Created by andres on 11/07/14.
 */
public class DynamicBNFactory {


    public static DynamicBayesianNetwork createDynamicBN(DynamicModelHeader modelHeader){
        return new DynamicBayesNetworkImpl(modelHeader);
    }
}