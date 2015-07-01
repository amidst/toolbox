/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */
package eu.amidst.core.examples.utils;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.IOException;

/**
 *
 * This example shows how to use the BayesianNetworkSampler class to randomly generate a data sample
 * for a given Bayesian network.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class BayesianNetworkSamplerExample {

    public static void main(String[] agrs) throws IOException, ClassNotFoundException {

        //We first load the WasteIncinerator bayesian network which has multinomial and Gaussian variables.
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");

        //We simply create an BayesianNetworkSampler object, passing to the constructor the BN model.
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(0);

        //The method sampleToDataStream returns a DataStream with ten DataInstance objects.
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(10);

        //We finally save the sampled data set to a arff file.
        DataStreamWriter.writeDataToFile(dataStream, "datasets/sample-WasteIncinerator.arff");
    }
}