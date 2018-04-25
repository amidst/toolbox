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
package eu.amidst.core.examples.conceptdrift;


import eu.amidst.core.conceptdrift.MaximumLikelihoodFading;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.examples.learning.MaximimumLikelihoodByBatchExample;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;

/**
 *
 * This example shows how to adaptively learn the parameters of a Bayesian network from a stream of data using exponential
 * forgetting with a given fading factor, directly inspired by the approach presented in
 *
 * <i>     Olesen, K. G., Lauritzen, S. L., and Jensen, F. V. (1992, July). aHUGIN: A system creating adaptive causal
 *      probabilistic networks. In Proceedings of the Eighth international conference on Uncertainty
 *      in Artificial Intelligence (pp. 223-229). Morgan Kaufmann Publishers Inc.
 * </i>
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class MaximimumLikelihoodFadingExample {

    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.open("datasets/simulated/WasteIncineratorSample.arff");

        //We create a ParameterLearningAlgorithm object with the MaximumLikelihoodFading builder
        MaximumLikelihoodFading parameterLearningAlgorithm = new MaximumLikelihoodFading();

        //We fix the DAG structure
        parameterLearningAlgorithm.setDAG(MaximimumLikelihoodByBatchExample.getNaiveBayesStructure(data, 0));

        //We fix the fading or forgeting factor
        parameterLearningAlgorithm.setFadingFactor(0.9);

        //We set the batch size which will be employed to learn the model
        parameterLearningAlgorithm.setWindowsSize(100);

        //We set the data which is going to be used for leaning the parameters
        parameterLearningAlgorithm.setDataStream(data);

        //We perform the learning
        parameterLearningAlgorithm.runLearning();

        //And we get the model
        BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

        //We print the model
        System.out.println(bnModel.toString());
    }

}