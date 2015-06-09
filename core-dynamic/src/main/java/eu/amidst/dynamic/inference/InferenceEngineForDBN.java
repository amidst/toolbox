/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.inference;

import eu.amidst.dynamic.datastream.DataSequence;
import eu.amidst.corestatic.distribution.UnivariateDistribution;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.corestatic.variables.Variable;

import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 12/02/15.
 */
public final class InferenceEngineForDBN {


    private static InferenceAlgorithmForDBN inferenceAlgorithmForDBN;// = new VMP();

    public static InferenceAlgorithmForDBN getInferenceAlgorithmForDBN() {
        return inferenceAlgorithmForDBN;
    }

    public static void setInferenceAlgorithmForDBN(InferenceAlgorithmForDBN inferenceAlgorithmForDBN) {
        InferenceEngineForDBN.inferenceAlgorithmForDBN = inferenceAlgorithmForDBN;
    }

    public static void reset() {inferenceAlgorithmForDBN.reset();}

    public static void runInference(){
        inferenceAlgorithmForDBN.runInference();
    }

    public static void setModel(DynamicBayesianNetwork model){
        inferenceAlgorithmForDBN.setModel(model);
    }

    public static DynamicBayesianNetwork getModel(){
        return inferenceAlgorithmForDBN.getOriginalModel();
    }

    public static void addDynamicEvidence(DynamicAssignment assignment){
        inferenceAlgorithmForDBN.addDynamicEvidence(assignment);
    }

    public static <E extends UnivariateDistribution> E getFilteredPosterior(Variable var){
        return inferenceAlgorithmForDBN.getFilteredPosterior(var);
    }

    public static <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead){
        return inferenceAlgorithmForDBN.getPredictivePosterior(var,nTimesAhead);
    }

    public static int getTimeIDOfPosterior(){
        return inferenceAlgorithmForDBN.getTimeIDOfPosterior();
    }

    public static int getTimeIDOfLastEvidence(){
        return inferenceAlgorithmForDBN.getTimeIDOfLastEvidence();
    }


    public static <E extends UnivariateDistribution> Stream<E> getStreamOfFilteredPosteriors(DataSequence dataSequence, Variable var){
        return dataSequence.stream().map( data -> {
            inferenceAlgorithmForDBN.addDynamicEvidence(data);
            inferenceAlgorithmForDBN.runInference();
            return inferenceAlgorithmForDBN.getFilteredPosterior(var);
        });
    }

    public static <E extends UnivariateDistribution> Stream<E> getStreamOfPredictivePosteriors(DataSequence dataSequence, Variable var, int nTimesAhead){
        return dataSequence.stream().map( data -> {
            inferenceAlgorithmForDBN.addDynamicEvidence(data);
            inferenceAlgorithmForDBN.runInference();
            return inferenceAlgorithmForDBN.getPredictivePosterior(var, nTimesAhead);
        });
    }

    public static <E extends UnivariateDistribution> E getLastFilteredPosteriorInTheSequence(DataSequence dataSequence, Variable var){
        final UnivariateDistribution[] dist = new UnivariateDistribution[1];
        getStreamOfFilteredPosteriors(dataSequence, var).forEach(e -> dist[0]=e);
        return (E)dist[0];
    }

    public static <E extends UnivariateDistribution> E getLastPredictivePosteriorInTheSequence(DataSequence dataSequence, Variable var, int nTimesAhead){
        final UnivariateDistribution[] dist = new UnivariateDistribution[1];
        getStreamOfPredictivePosteriors(dataSequence, var, nTimesAhead).forEach(e -> dist[0]=e);
        return (E)dist[0];
    }

}
