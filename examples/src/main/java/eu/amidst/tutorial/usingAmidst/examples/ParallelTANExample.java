package eu.amidst.tutorial.usingAmidst.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.huginlink.learning.ParallelTAN;

/**
 *
 * Learn a TAN structure in parallel with Hugin using a subsample of the available data (on main memory) and
 * then learn the parameters in AMIDST using the whole data.
 *
 * Created by ana@cs.aau.dk on 01/06/16.
 */
public class ParallelTANExample {
    public static void main(String[] args) throws Exception {

        int sampleSize = 10000;
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("networks/simulated/asia.bn");
        System.out.println(bn);
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);

        ParallelTAN tan = new ParallelTAN();
        tan.setNumCores(4);
        tan.setNumSamplesOnMemory(1000);
        tan.setNameRoot("X");
        tan.setNameTarget("E");

        BayesianNetwork model = tan.learn(data);

        System.out.println(model.toString());
    }
}
