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
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.core.utils.Serialization;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by andresmasegosa on 1/9/15.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("./networks/dataWeka/asia.bn");

        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().outputString());
        //System.out.println(asianet.outputString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        DataStreamWriter.writeDataToFile(data, "./datasets/simulated/tmp.arff");


        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "./datasets/simulated/tmp.arff", false);

        SVB svb = new SVB();
        svb.setDAG(asianet.getDAG());
        svb.setWindowsSize(100);
        svb.initLearning();

        Serialization.serializeObject(svb);

    }
}