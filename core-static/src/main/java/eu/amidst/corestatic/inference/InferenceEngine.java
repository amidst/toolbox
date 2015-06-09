/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.corestatic.inference;

import eu.amidst.corestatic.distribution.Normal;
import eu.amidst.corestatic.distribution.UnivariateDistribution;
import eu.amidst.corestatic.inference.messagepassing.VMP;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.utils.BayesianNetworkGenerator;
import eu.amidst.corestatic.variables.Assignment;
import eu.amidst.corestatic.variables.HashMapAssignment;
import eu.amidst.corestatic.variables.Variable;

/**
 * Created by andresmasegosa on 30/01/15.
 */
public final class InferenceEngine {

    private static InferenceAlgorithm inferenceAlgorithm = new VMP();

    public static void setInferenceAlgorithm(InferenceAlgorithm inferenceAlgorithm) {
        InferenceEngine.inferenceAlgorithm = inferenceAlgorithm;
    }

    public static <E extends UnivariateDistribution> E getPosterior(Variable var, BayesianNetwork bayesianNetwork, Assignment assignment) {
        inferenceAlgorithm.setModel(bayesianNetwork);
        inferenceAlgorithm.setEvidence(assignment);
        inferenceAlgorithm.runInference();
        return inferenceAlgorithm.getPosterior(var);
    }

    public static <E extends UnivariateDistribution> E getPosterior(Variable var, BayesianNetwork bayesianNetwork) {
        inferenceAlgorithm.setModel(bayesianNetwork);
        inferenceAlgorithm.runInference();
        return inferenceAlgorithm.getPosterior(var);
    }


    public static void main(String[] arguments){

        BayesianNetworkGenerator.setNumberOfContinuousVars(2);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(1);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        HashMapAssignment assignment = new HashMapAssignment(1);

        Variable varB = bn.getStaticVariables().getVariableById(0);

        assignment.setValue(varB, 0.7);

        Variable varA = bn.getStaticVariables().getVariableById(1);

        Normal posteriorOfA = InferenceEngine.getPosterior(varA, bn, assignment);

        System.out.println("P(A|B=0.7) = " + posteriorOfA.toString());

        InferenceAlgorithm inf = new VMP();
        inf.setModel(bn);
        inf.setEvidence(assignment);
        inf.runInference();
        inf.getPosterior(varA);
    }
}
