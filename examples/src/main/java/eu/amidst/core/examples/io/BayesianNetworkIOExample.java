package eu.amidst.core.examples.io;


import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;

import java.util.Random;

/**
 *
 * In this example we show how to load and save Bayesian networks models for a binary file with ".bn" extension. In
 * this toolbox Bayesian networks models are saved as serialized objects.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class BayesianNetworkIOExample {

    public static void main(String[] args) throws Exception {

        //We can load a Bayesian network using the static class BayesianNetworkLoader
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/simulated/WasteIncinerator.bn");

        //Now we print the loaded model
        System.out.println(bn.toString());

        //Now we change the parameters of the model
        bn.randomInitialization(new Random(0));

        //We can save this Bayesian network to using the static class BayesianNetworkWriter
        BayesianNetworkWriter.save(bn, "networks/simulated/tmp.bn");

    }
}
