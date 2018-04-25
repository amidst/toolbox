package eu.amidst.sparklink.core.util;

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.sparklink.core.data.DataSpark;
import org.apache.spark.api.java.JavaSparkContext;


/**
 * Created by rcabanas on 30/09/16.
 */
public final class DataSetGenerator {

        /**
         * Generate a DataSpark with the given number of samples and attributes (discrete and continuous).
         * @param jsc, JavaSparkContext
         * @param seed, the seed of the random number generator.
         * @param nSamples, the number of samples of the data stream.
         * @param nDiscreteAtts, the number of discrete attributes.
         * @param nContinuousAttributes, the number of continuous attributes.
         * @return A valid {@code DataSpark} object.
         */

        public static DataSpark generate(JavaSparkContext jsc, int seed, int nSamples, int nDiscreteAtts, int nContinuousAttributes){
            BayesianNetworkGenerator.setSeed(seed);
            BayesianNetworkGenerator.setNumberOfGaussianVars(nContinuousAttributes);
            BayesianNetworkGenerator.setNumberOfMultinomialVars(nDiscreteAtts,2);
            int nTotal = nDiscreteAtts+nContinuousAttributes;
            int nLinksMin = nTotal-1;
            int nLinksMax = nTotal*(nTotal-1)/2;
            BayesianNetworkGenerator.setNumberOfLinks((int)(0.8*nLinksMin + 0.2*nLinksMax));

            BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();


            // Sample from the BN
            int parallelism = 4;
            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            return sampler.sampleToDataSpark(jsc, nSamples, parallelism);

        }



}
