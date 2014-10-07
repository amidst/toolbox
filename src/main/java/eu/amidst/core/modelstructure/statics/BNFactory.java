package eu.amidst.core.modelstructure.statics;


import eu.amidst.core.modelstructure.statics.impl.BayesianNetworkImpl;
import eu.amidst.core.header.statics.StaticModelHeader;

/**
 * Created by andres on 11/07/14.
 */
public class BNFactory {


    public static BayesianNetwork createBN(StaticModelHeader modelHeader){
        return new BayesianNetworkImpl(modelHeader);
    }
}