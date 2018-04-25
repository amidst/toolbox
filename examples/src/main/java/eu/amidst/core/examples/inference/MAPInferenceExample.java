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

package eu.amidst.core.examples.inference;


import eu.amidst.core.inference.MAPInference;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * This example we show how to perform MAP inference on a general Bayesian network.
 *
 */
public class MAPInferenceExample {

    public static void main(String[] args) throws Exception {

        //We first load the WasteIncinerator bayesian network which has multinomial and Gaussian variables.
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/simulated/WasteIncinerator.bn");
        List<Variable> topologicalOrder = Utils.getTopologicalOrder(bn.getDAG());

        //We recover the relevant variables for this example: Mout which is normally distributed, and W which is multinomial.
        Variable varMin = bn.getVariables().getVariableByName("Min");
        Variable varW = bn.getVariables().getVariableByName("W");

        //First we create an instance of a point estimator. In this case, we use the MPEInference class.
        MAPInference mapInference = new MAPInference();

        //Then, we set the BN model
        mapInference.setModel(bn);

        System.out.println(bn.toString());

        //If exists, we also set the evidence.
        Assignment assignment = new HashMapAssignment(2);
        assignment.setValue(varW, 0);
        assignment.setValue(varMin, 0.15);
        mapInference.setEvidence(assignment);


        System.out.println("Evidence: " + assignment.outputString(topologicalOrder) + "\n");

        // Set also the list of variables of interest (or MAP variables).
        List<Variable> varsInterest = new ArrayList<>();

        Variable var1 = bn.getVariables().getVariableByName("B");
        Variable var2 = bn.getVariables().getVariableByName("C");

        varsInterest.add(var1);
        varsInterest.add(var2);
        mapInference.setMAPVariables(varsInterest);
        System.out.println("Variables of Interest: " + var1.getName() + ", " + var2.getName() + "\n");

        //We can also set to be run in parallel on multicore CPUs
        mapInference.setParallelMode(true);

        //Then we run inference
        mapInference.runInference();

        //We show the found MPE estimate
        System.out.println("MAP = " + mapInference.getEstimate().outputString(topologicalOrder));


        //And its probability
        System.out.println("P(MAP) = " + Math.exp(mapInference.getLogProbabilityOfEstimate()));

    }
}