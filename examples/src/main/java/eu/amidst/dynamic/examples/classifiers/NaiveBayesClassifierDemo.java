/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.examples.classifiers;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.classifiers.NaiveBayesClassifier;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;

/**
 * Created by andresmasegosa on 15/01/15.
 */
public class NaiveBayesClassifierDemo {
    public static void main(String[] args) {

        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setSeed(0);
        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(10, 10);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(bn.toString());

        int sampleSize = 10000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        DataStream<DataInstance> data =  sampler.sampleToDataStream(sampleSize);

        NaiveBayesClassifier model = new NaiveBayesClassifier();
        model.setClassName(data.getAttributes().getFullListOfAttributes().get(0).getName());
        model.setParallelMode(true);
        model.learn(data);
        BayesianNetwork nbClassifier = model.getBNModel();
        System.out.println(nbClassifier.toString());

    }
}
