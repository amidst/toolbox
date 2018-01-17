/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.flinklink.core.utils;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * It defines a sampler of data from a {@link BayesianNetwork}.
 */
public class BayesianNetworkSampler {

    static String SEED = "SEED";
    static String SAMPLER = "SAMPLER";

    BayesianNetwork network;
    eu.amidst.core.utils.BayesianNetworkSampler localSampler;
    private int seed;
    int batchSize = 1000;

    /**
     * Creates a new BayesianNetworkSampler given an input {@link BayesianNetwork} object.
     * @param network1 an input {@link BayesianNetwork} object.
     */
    public BayesianNetworkSampler(BayesianNetwork network1){
        this.network = network1;
        localSampler = new eu.amidst.core.utils.BayesianNetworkSampler(network1);
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }


    /**
     * Sets a given {@link Variable} object as latent. A latent variable doesn't contain an attribute and therefore
     * doesn't generate a sampling value.
     * @param var a given {@link Variable} object.
     */
    public void setLatentVar(Variable var){
        this.localSampler.setLatentVar(var);
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

    public DataFlink<DataInstance>  sampleToDataFlink(ExecutionEnvironment env, int nSamples) {

        try{
            int nBatches = nSamples/this.batchSize;

            DataSet<DataInstance> data = env.generateSequence(0,nBatches-1)
                                        .flatMap(new SampleMap(this.localSampler,batchSize,seed));

            Attributes attributes = this.localSampler.sampleToDataStream(1).getAttributes();

            return new DataFlinkWrapper<>(network.getName(), attributes, data);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }

    static class SampleMap extends RichFlatMapFunction<Long, DataInstance>{

        eu.amidst.core.utils.BayesianNetworkSampler localSampler;
        final int batchSize;
        final int seed;

        public SampleMap(eu.amidst.core.utils.BayesianNetworkSampler localSampler, int batchSize, int seed) {
            this.localSampler = localSampler;
            this.batchSize = batchSize;
            this.seed = seed;
        }

/*
        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            int seed = parameters.getInteger(SEED, 0);
            localSampler = Serialization.deserializeObject(parameters.getBytes(SAMPLER, null));
            //int seedAndIndex = seed + getRuntimeContext().getIndexOfThisSubtask();
            //localSampler.setSeed(seedAndIndex);
        }*/

        @Override
        public void flatMap(Long value, Collector<DataInstance> out) throws Exception {
            this.localSampler.setSeed(seed + value.intValue());
            this.localSampler.sampleToDataStream(batchSize).stream().forEach(d -> out.collect(d));
        }
    }

    static class DataFlinkWrapper<T extends DataInstance> implements DataFlink<T>, Serializable{

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = 4107783324901370839L;
        private final String name;

        Attributes attributes;
        DataSet<T> data;

        public DataFlinkWrapper(String name, Attributes attributes, DataSet<T> data) {
            this.name = name;
            this.attributes = attributes;
            this.data = data;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public DataSet<T> getDataSet() {
            return data;
        }
    }
}