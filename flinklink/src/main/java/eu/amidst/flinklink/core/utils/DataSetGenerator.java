package eu.amidst.flinklink.core.utils;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.flinklink.core.data.DataFlink;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by rcabanas on 10/06/16.
 */
public final class DataSetGenerator {

        /**
         * Generate a DataFlink with the given number of samples and attributes (discrete and continuous).
         * @param seed, the seed of the random number generator.
         * @param nSamples, the number of samples of the data stream.
         * @param nDiscreteAtts, the number of discrete attributes.
         * @param nContinuousAttributes, the number of continuous attributes.
         * @return A valid {@code DataStream} object.
         */

        public static DataFlink<DataInstance> generate(ExecutionEnvironment env, int seed, int nSamples, int nDiscreteAtts, int nContinuousAttributes){
            BayesianNetworkGenerator.setSeed(seed);
            BayesianNetworkGenerator.setNumberOfGaussianVars(nContinuousAttributes);
            BayesianNetworkGenerator.setNumberOfMultinomialVars(nDiscreteAtts,2);
            int nTotal = nDiscreteAtts+nContinuousAttributes;
            int nLinksMin = nTotal-1;
            int nLinksMax = nTotal*(nTotal-1)/2;
            BayesianNetworkGenerator.setNumberOfLinks((int)(0.8*nLinksMin + 0.2*nLinksMax));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(BayesianNetworkGenerator.generateBayesianNetwork());
            sampler.setSeed(seed);
            return sampler.sampleToDataFlink(env,nSamples);
        }



}
