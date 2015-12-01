package eu.amidst.flinklink.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;

/**
 * Created by Hanen on 17/11/15.
 */
public class GenerateData {

public static void main(String[] args) throws Exception {

    // load the true Asia Bayesian network
    BayesianNetwork originalBnet = BayesianNetworkLoader.loadFromFile(args[0]);

    System.out.println("\n Network \n " + args[0]);
    //System.out.println(originalBnet.getDAG().outputString());
    //System.out.println(originalBnet.outputString());

    //Sampling from Asia BN
    BayesianNetworkSampler sampler = new BayesianNetworkSampler(originalBnet);
    sampler.setSeed(0);
    int sizeData = Integer.parseInt(args[1]);
    DataStream<DataInstance> data = sampler.sampleToDataStream(sizeData);

    DataStreamWriter.writeDataToFile(data, "./tmp.arff");

}

}
