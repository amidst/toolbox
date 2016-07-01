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

package eu.amidst.core.utils;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;

/**
 * This class aims at generate randomly a data set with some given features as number of samples, number of continuous
 * and number of discrete variables.
 *
 * Created by andresmasegosa on 9/3/16.
 */
public final class DataSetGenerator {

    /**
     * Generate a DataStream with the given number of samples and attributes (discrete and continuous).
     * @param seed, the seed of the random number generator.
     * @param nSamples, the number of samples of the data stream.
     * @param nDiscreteAtts, the number of discrete attributes.
     * @param nContinuousAttributes, the number of continuous attributes.
     * @return A valid {@code DataStream} object.
     */
    public static DataStream<DataInstance> generate(int seed, int nSamples, int nDiscreteAtts, int nContinuousAttributes){
        BayesianNetworkGenerator.setSeed(seed);
        BayesianNetworkGenerator.setNumberOfGaussianVars(nContinuousAttributes);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(nDiscreteAtts,2);
        int nTotal = nDiscreteAtts+nContinuousAttributes;
        int nLinksMin = nTotal-1;
        int nLinksMax = nTotal*(nTotal-1)/2;
        BayesianNetworkGenerator.setNumberOfLinks((int)(0.8*nLinksMin + 0.2*nLinksMax));

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(BayesianNetworkGenerator.generateBayesianNetwork());
        sampler.setSeed(seed);
        return sampler.sampleToDataStream(nSamples);
    }



}
