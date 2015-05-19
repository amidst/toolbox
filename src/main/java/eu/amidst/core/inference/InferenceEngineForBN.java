/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.inference;

import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.messagepassage.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 30/01/15.
 */
public final class InferenceEngineForBN {

    private enum InfAlg {VMP, EP};

    InfAlg infAlg;

    private static InferenceAlgorithmForBN inferenceAlgorithmForBN = new VMP();

    public static void setInfAlg(InfAlg infAlg) {
        infAlg = infAlg;
    }

    public static void setInferenceAlgorithmForBN(InferenceAlgorithmForBN inferenceAlgorithmForBN) {
        InferenceEngineForBN.inferenceAlgorithmForBN = inferenceAlgorithmForBN;
    }

    public static void runInference(){
        inferenceAlgorithmForBN.runInference();
    }

    public static void setModel(BayesianNetwork model){
        inferenceAlgorithmForBN.setModel(model);
    }

    public static void setEvidence(Assignment assignment){
        inferenceAlgorithmForBN.setEvidence(assignment);
    }

    public double getLogProbabilityOfEvidence(){
        return inferenceAlgorithmForBN.getLogProbabilityOfEvidence();
    }

    public void setSeed(int seed){
        inferenceAlgorithmForBN.setSeed(seed);
    }
    public static <E extends UnivariateDistribution> E getPosterior(Variable var){
        return inferenceAlgorithmForBN.getPosterior(var);
    }

    public static <E extends UnivariateDistribution> E getPosterior(String name){
        return inferenceAlgorithmForBN.getPosterior(inferenceAlgorithmForBN.getOriginalModel().getStaticVariables().getVariableByName(name));
    }


    public static void main(String[] arguments){

        BayesianNetworkGenerator.setNumberOfContinuousVars(2);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(1);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        HashMapAssignment assignment = new HashMapAssignment(1);

        Variable varB = bn.getStaticVariables().getVariableById(0);

        assignment.setValue(varB, 0.7);

        Variable varA = bn.getStaticVariables().getVariableById(1);


        InferenceEngineForBN.setInfAlg(InfAlg.VMP);
        InferenceEngineForBN.setModel(bn);
        InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.runInference();

        Normal posteriorOfA = InferenceEngineForBN.getPosterior(varA);

        System.out.println("P(A|B=0.7) = " + posteriorOfA.toString());

        InferenceAlgorithmForBN inf = new VMP();
        inf.setModel(bn);
        inf.setEvidence(assignment);
        inf.runInference();
        inf.getPosterior(varA);
    }
}
