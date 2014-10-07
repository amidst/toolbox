package eu.amidst.core.modelstructure.dynamics;


import eu.amidst.core.modelstructure.dynamics.impl.DynamicBayesNetworkImpl;
import eu.amidst.core.header.dynamics.DynamicModelHeader;

/**
 * Created by andres on 11/07/14.
 */
public class DynamicBNFactory {


    public static DynamicBayesianNetwork createDynamicBN(DynamicModelHeader modelHeader){
        return new DynamicBayesNetworkImpl(modelHeader);
    }
}