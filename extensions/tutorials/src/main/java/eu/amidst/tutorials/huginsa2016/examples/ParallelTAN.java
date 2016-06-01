package eu.amidst.tutorials.huginsa2016.examples;

import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.IOException;

/**
 *
 * Learn a TAN structure in parallel with Hugin using a subsample of the available data (on main memory) and
 * then learn the parameters in AMIDST using the whole data.
 *
 * Created by ana@cs.aau.dk on 01/06/16.
 */
public class ParallelTAN {
    public static void main(String[] args) throws IOException{

        int streamSampleSize = 5000;

        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(100, 10);
        BayesianNetworkGenerator.setSeed(0);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        DataStream<DataInstance> data =  sampler.sampleToDataStream(streamSampleSize);

        for (int i = 1; i <= 4; i++) {
            int samplesOnMemory = 1000;
            int numCores = i;
            System.out.println("Learning TAN: " + samplesOnMemory + " samples on memory, " + numCores + "core/s ...");
            eu.amidst.huginlink.learning.ParallelTAN tan = new eu.amidst.huginlink.learning.ParallelTAN();
            tan.setNumCores(numCores);
            tan.setNumSamplesOnMemory(samplesOnMemory);
            tan.setNameRoot(bn.getVariables().getListOfVariables().get(0).getName());
            tan.setNameTarget(bn.getVariables().getListOfVariables().get(1).getName());
            Stopwatch watch = Stopwatch.createStarted();
            BayesianNetwork model = tan.learn(data);
            System.out.println(watch.stop());
        }
    }
}
