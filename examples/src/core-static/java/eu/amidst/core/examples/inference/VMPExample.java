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

package eu.amidst.core.examples.inference;

import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

/**
 *
 * This example we show how to perform inference on a general Bayesian network using the Variational Message Passing (VMP)
 * algorithm detailed in
 *
 * Winn, J. M., & Bishop, C. M. (2005). Variational message passing. In Journal of Machine Learning Research (pp. 661-694).
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class VMPExample {

    public static void main(String[] args) throws Exception {

        //We first load the WasteIncinerator bayesian network which has multinomial and Gaussian variables.
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");

        //We recover the relevant variables for this example: Mout which is normally distributed, and W which is multinomial.
        Variable varMout = bn.getStaticVariables().getVariableByName("Mout");
        Variable varW = bn.getStaticVariables().getVariableByName("W");

        //First we create an instance of a inference algorithm. In this case, we use the VMP class.
        InferenceAlgorithm inferenceAlgorithm = new VMP();
        //Then, we set the BN model
        inferenceAlgorithm.setModel(bn);

        //If exists, we also set the evidence.
        Assignment assignment = new HashMapAssignment(1);
        assignment.setValue(varW,0);
        inferenceAlgorithm.setEvidence(assignment);

        //Then we run inference
        inferenceAlgorithm.runInference();

        //Then we query the posterior of
        System.out.println("P(Mout|W=0) = " + inferenceAlgorithm.getPosterior(varMout));

        //Or some more refined queries
        System.out.println("P(0.7<Mout<6.59 | W=0) = " + inferenceAlgorithm.getExpectedValue(varMout, v -> (0.7 < v && v < 6.59) ? 1.0 : 0.0 ));

        //We can also compute the probability of the evidence
        System.out.println("P(W=0) = "+Math.exp(inferenceAlgorithm.getLogProbabilityOfEvidence()));


    }
}