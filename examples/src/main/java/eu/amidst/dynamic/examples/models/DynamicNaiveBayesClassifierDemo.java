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

package eu.amidst.dynamic.examples.models;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.learning.parametric.DynamicNaiveBayesClassifier;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;

import java.io.IOException;

/**
 * Created by andresmasegosa on 15/01/15.
 */
public class DynamicNaiveBayesClassifierDemo {
    public static void main(String[] args) throws IOException {

        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(5, 3);
        BayesianNetworkGenerator.setSeed(0);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 1000000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        String file = "./datasets/simulated/randomdata.arff";
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(sampleSize);
        DataStreamWriter.writeDataToFile(dataStream, file);

        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork nbClassifier = model.getDynamicBNModel();
        System.out.println(nbClassifier.toString());


    }
}
