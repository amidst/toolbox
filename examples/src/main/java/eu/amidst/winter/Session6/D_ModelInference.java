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

package eu.amidst.winter.Session6;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceAlgorithmForDBN;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;

import java.io.IOException;

/**
 *
 * Session 6. Make inference (i.e. predictions) with the learnt model.
 *
 * Created by andresmasegosa on 17/01/2018.
 */
public class D_ModelInference {
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        //Load the learnt model
        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("./models/DynamicFireDetectorModel.dbn");
        System.out.println(dbn);

        //Testing dataset
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.open("./datasets/TimeIndexedSensorReadingsEvidence.arff");

        //Select the inference algorithm: Factored Frontier Algorithm backed by Importance Sampling
        InferenceAlgorithmForDBN infer = new FactoredFrontierForDBN(new ImportanceSampling());

        //Then, we set the DBN model
        infer.setModel(dbn);

        //Set-up the variable of interest.
        Variable fire = dbn.getDynamicVariables().getVariableByName("Fire");

        //Classify each instance
        int t = 0;
        for (DynamicDataInstance instance : data) {

            //Set the values of the evidence for the given variables.
            infer.addDynamicEvidence(instance);

            //Perform the inference
            infer.runInference();

            //Retrieve and print the results of the inference.
            UnivariateDistribution posterior = infer.getFilteredPosterior(fire);
            System.out.println(instance);
            System.out.println("t="+t+", P(Fire | Evidence)  = " + posterior+"\n");

            t++;
        }

    }
}
