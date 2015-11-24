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

package eu.amidst.core.classifiers;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import junit.framework.TestCase;

/**
 * Created by andresmasegosa on 23/11/15.
 */
public class NaiveBayesClassifierTest extends TestCase {

    public static void test(){
        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setSeed(0);
        BayesianNetworkGenerator.setNumberOfGaussianVars(3);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(3, 2);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);
        System.out.println(bn.toString());

        int sampleSize = 10000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        DataStream<DataInstance> data =  sampler.sampleToDataStream(sampleSize);

        long time = System.nanoTime();
        NaiveBayesClassifier model = new NaiveBayesClassifier();
        model.setClassName(data.getAttributes().getFullListOfAttributes().get(data.getAttributes().getFullListOfAttributes().size() - 1).getName());
        model.setParallelMode(true);
        model.learn(data, 100);
        BayesianNetwork nbClassifier = model.getBNModel();
        System.out.println(nbClassifier.toString());

        System.out.println("Time: " + (System.nanoTime() - time) / 1000000000.0);


        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();

        parallelMaximumLikelihood.setBatchSize(100);
        parallelMaximumLikelihood.setDAG(bn.getDAG());
        parallelMaximumLikelihood.setLaplace(true);
        parallelMaximumLikelihood.setDataStream(sampler.sampleToDataStream(sampleSize));

        parallelMaximumLikelihood.runLearning();
        BayesianNetwork nb = parallelMaximumLikelihood.getLearntBayesianNetwork();
        System.out.println(nb.toString());
        assertTrue(nb.equalBNs(nbClassifier, 0.1));
        assertTrue(bn.equalBNs(nbClassifier, 0.3));
        assertTrue(bn.equalBNs(nb, 0.3));

    }
}