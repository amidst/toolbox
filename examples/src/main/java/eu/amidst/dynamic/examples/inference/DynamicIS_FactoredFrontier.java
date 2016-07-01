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

package eu.amidst.dynamic.examples.inference;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceEngineForDBN;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;

import java.io.IOException;
import java.util.Random;

/**
 * This example shows how to use the Factored Frontier algorithm with Importance Sampling described in
 * Deliverable 3.4 (Section 6).
 * Created by ana@cs.aau.dk on 16/11/15.
 */
public class DynamicIS_FactoredFrontier {
    public static void main(String[] args) throws IOException {

        Random random = new Random(1);

        //We first generate a dynamic Bayesian network (NB structure with class and attributes temporally linked)
        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(3);
        DynamicBayesianNetwork extendedDBN = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random, 2, true);

        System.out.println(extendedDBN.toString());

        //We select the target variable for inference, in this case the class variable
        Variable classVar = extendedDBN.getDynamicVariables().getVariableByName("ClassVar");

        //We create a dynamic dataset with 3 sequences for prediction. The class var is made hidden.
        DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(extendedDBN);
        dynamicSampler.setHiddenVar(classVar);
        DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(3, 1000);

        //We select IS with the factored frontier algorithm as the Inference Algorithm
        ImportanceSampling importanceSampling = new ImportanceSampling();
        importanceSampling.setKeepDataOnMemory(true);
        FactoredFrontierForDBN FFalgorithm = new FactoredFrontierForDBN(importanceSampling);
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(FFalgorithm);

        //Then, we set the DBN model
        InferenceEngineForDBN.setModel(extendedDBN);

        int time = 0;
        UnivariateDistribution posterior = null;
        for (DynamicDataInstance instance : dataPredict) {
            //The InferenceEngineForDBN must be reset at the begining of each Sequence.
            if (instance.getTimeID() == 0 && posterior != null) {
                InferenceEngineForDBN.reset();
                time = 0;
            }

            //We also set the evidence.
            InferenceEngineForDBN.addDynamicEvidence(instance);

            //Then we run inference
            InferenceEngineForDBN.runInference();

            //Then we query the posterior of the target variable
            posterior = InferenceEngineForDBN.getFilteredPosterior(classVar);


            //We show the output
            System.out.println("P(ClassVar|e[0:" + (time++) + "]) = " + posterior);
        }
    }
}
