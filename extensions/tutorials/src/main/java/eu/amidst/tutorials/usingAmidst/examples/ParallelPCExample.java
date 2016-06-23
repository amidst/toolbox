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

package eu.amidst.tutorials.usingAmidst.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.huginlink.learning.ParallelPC;

/**
 *
 * Learn a TAN structure in parallel with Hugin using a subsample of the available data (on main memory) and
 * then learn the parameters in AMIDST using the whole data.
 *
 * Created by ana@cs.aau.dk on 01/06/16.
 */
public class ParallelPCExample {
    public static void main(String[] args) throws Exception {

        int sampleSize = 10000;
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("networks/dataWeka/asia.bn");
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        DataStream<DataInstance> data = sampler.sampleToDataStream(sampleSize);

        ParallelPC pc = new ParallelPC();
        pc.setNumCores(4);
        pc.setNumSamplesOnMemory(5000);

        BayesianNetwork model = pc.learn(data);

        System.out.println(model.toString());
    }
}
