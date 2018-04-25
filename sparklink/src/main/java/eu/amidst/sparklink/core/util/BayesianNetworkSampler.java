package eu.amidst.sparklink.core.util;

import com.google.common.collect.Lists;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Variable;
import eu.amidst.sparklink.core.data.DataSpark;

import eu.amidst.sparklink.core.data.DataSparkFromRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;


/**
 * Created by jarias on 22/06/16.
 */
public class BayesianNetworkSampler implements Serializable {

        static String SEED = "SEED";
        static String SAMPLER = "SAMPLER";

        BayesianNetwork network;
        eu.amidst.core.utils.BayesianNetworkSampler localSampler;

        private int seed;
        int batchSize = 1000;

        /**
         * Creates a new BayesianNetworkSampler given an input {@link BayesianNetwork} object.
         * @param bn an input {@link BayesianNetwork} object.
         */
        public BayesianNetworkSampler(BayesianNetwork bn){
            this.network = bn;
            localSampler = new eu.amidst.core.utils.BayesianNetworkSampler(bn);
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public void setSeed(int seed) {
            this.seed = seed;
        }


        /**
         * Sets a given {@link Variable} object as hidden.
         * @param var a given {@link Variable} object.
         */
        public void setHiddenVar(Variable var) {
            this.localSampler.setHiddenVar(var);
        }

        /**
         * Sets a given {@link Variable} object as noisy.
         * @param var a given {@link Variable} object.
         * @param noiseProb a double that represents the noise probability.
         */
        public void setMARVar(Variable var, double noiseProb){ this.localSampler.setMARVar(var, noiseProb);}


        public eu.amidst.core.utils.BayesianNetworkSampler getLocalSampler() {
            return localSampler;
        }


        public DataSpark sampleToDataSpark(JavaSparkContext sc, int nSamples, int parallelism) {

            int localNSamples = nSamples/parallelism;

            JavaRDD<Integer> partitions = sc.parallelize(Arrays.asList(new Integer[parallelism]), parallelism);

            Function2 getPartitionSample = new Function2<Integer, Iterator<Integer>, Iterator<DataInstance>>(){
                @Override
                public Iterator<DataInstance> call(Integer ind, Iterator<Integer> iterator) throws Exception {
                    localSampler.setSeed(seed+ind);
                    return localSampler.sampleToDataStream(localNSamples).iterator();
                }
            };

            JavaRDD<DataInstance> sampleRDD = partitions.mapPartitionsWithIndex(getPartitionSample, false);

            // Get the attributes from a local instance
            Attributes attributes = this.localSampler.sampleToDataStream(1).getAttributes();

            return new DataSparkFromRDD(sampleRDD, attributes);

        }

    }