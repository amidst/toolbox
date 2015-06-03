/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.examples;

import eu.amidst.examples.core.datastream.DataInstance;
import eu.amidst.examples.core.datastream.DataStream;
import eu.amidst.examples.core.learning.NaiveBayesClassifier;
import eu.amidst.examples.core.models.BayesianNetwork;
import eu.amidst.examples.core.utils.BayesianNetworkGenerator;
import eu.amidst.examples.core.utils.BayesianNetworkSampler;

/**
 * Created by andresmasegosa on 15/01/15.
 */
public class NaiveBayesClassifierDemo {
    public static void main(String[] args) {

        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setSeed(0);
        BayesianNetworkGenerator.setNumberOfContinuousVars(0);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(10);
        BayesianNetworkGenerator.setNumberOfStates(10);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(bn.toString());

        int sampleSize = 10000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        DataStream<DataInstance> data =  sampler.sampleToDataStream(sampleSize);

        NaiveBayesClassifier model = new NaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
        model.setParallelMode(true);
        model.learn(data);
        BayesianNetwork nbClassifier = model.getBNModel();
        System.out.println(nbClassifier.toString());

    }
}
