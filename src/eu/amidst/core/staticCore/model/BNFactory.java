package eu.amidst.core.StaticBayesianNetwork;


import eu.amidst.core.StaticBayesianNetwork.impl.BayesianNetworkImpl;
import eu.amidst.core.headers.StaticModelHeader;

/**
 * Created by andres on 11/07/14.
 */
public class BNFactory {


    public static BayesianNetwork createBN(StaticModelHeader modelHeader){
        return new BayesianNetworkImpl(modelHeader);
    }
}