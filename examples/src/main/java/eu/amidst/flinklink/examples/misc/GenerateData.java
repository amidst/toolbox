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

package eu.amidst.flinklink.examples.misc;

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

    // load the true Bayesian network
    BayesianNetwork originalBnet = BayesianNetworkLoader.loadFromFile(args[0]);

    System.out.println("\n Network \n " + args[0]);
    System.out.println("\n Number of variables \n " + originalBnet.getDAG().getVariables().getNumberOfVars());

    //Sampling from the input BN
    BayesianNetworkSampler sampler = new BayesianNetworkSampler(originalBnet);
    sampler.setSeed(0);

    // Defines the size of the data to be generated from the input BN
    int sizeData = Integer.parseInt(args[1]);

    System.out.println("\n Sampling and saving the data... \n ");
    
    DataStream<DataInstance> data = sampler.sampleToDataStream(sizeData);

    DataStreamWriter.writeDataToFile(data, "./data.arff");
}

}
