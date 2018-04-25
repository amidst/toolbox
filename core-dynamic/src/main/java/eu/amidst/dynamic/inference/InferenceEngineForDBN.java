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

package eu.amidst.dynamic.inference;

import eu.amidst.dynamic.datastream.DataSequence;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.core.variables.Variable;
import java.util.stream.Stream;

/**
 * This class defines the Inference Engine for Dynamic Bayesian Network models.
 */
public final class InferenceEngineForDBN {

    /** Represents the used {@link InferenceAlgorithmForDBN}. */
    private static InferenceAlgorithmForDBN inferenceAlgorithmForDBN;

    /**
     * Sets the dynamic inference algorithm for this InferenceEngineForDBN.
     * @param inferenceAlgorithmForDBN an {@link InferenceAlgorithmForDBN} object.
     */
    public static void setInferenceAlgorithmForDBN(InferenceAlgorithmForDBN inferenceAlgorithmForDBN) {
        InferenceEngineForDBN.inferenceAlgorithmForDBN = inferenceAlgorithmForDBN;
    }

    /**
     * Resets the parameters of the corresponding InferenceAlgorithmForDBN object.
     */
    public static void reset() {inferenceAlgorithmForDBN.reset();}

    /**
     * Runs inference.
     */
    public static void runInference(){
        inferenceAlgorithmForDBN.runInference();
    }

     /**
     * Sets the model for this InferenceEngineForDBN.
     * @param model a {@link DynamicBayesianNetwork} model to which this InferenceEngineForDBN will be set.
     */
    public static void setModel(DynamicBayesianNetwork model){
        inferenceAlgorithmForDBN.setModel(model);
    }

     /**
     * Returns the original model of this InferenceEngineForDBN.
     * @return a {@link DynamicBayesianNetwork} object that represents the original model of this InferenceEngineForDBN.
     */
    public static DynamicBayesianNetwork getModel(){
        return inferenceAlgorithmForDBN.getOriginalModel();
    }

    /**
     * Sets the evidence for this InferenceEngineForDBN.
     * @param assignment a {@link DynamicAssignment} object to which the evidence will be set.
     */
    public static void addDynamicEvidence(DynamicAssignment assignment){
        inferenceAlgorithmForDBN.addDynamicEvidence(assignment);
    }

    /**
     * Returns the filtered posterior distribution of a given {@link Variable} object.
     * @param var a {@link Variable} object.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @return an {@link UnivariateDistribution} object.
     */
    public static <E extends UnivariateDistribution> E getFilteredPosterior(Variable var){
        return inferenceAlgorithmForDBN.getFilteredPosterior(var);
    }

    /**
     * Returns the predictive posterior distribution of a given {@link Variable} object for nTimesAhead.
     * @param var a {@link Variable} object.
     * @param nTimesAhead an {@code int} that represents the number of time steps ahead.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @return an {@link UnivariateDistribution} object.
     */
    public static <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead){
        return inferenceAlgorithmForDBN.getPredictivePosterior(var,nTimesAhead);
    }

    /**
     * Returns the filtered posterior distributions of a given {@link Variable} object and an input {@link DataSequence}.
     * @param dataSequence an input {@link DataSequence} object.
     * @param var a {@link Variable} object.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @return an {@link UnivariateDistribution} object.
     */
    public static <E extends UnivariateDistribution> Stream<E> getStreamOfFilteredPosteriors(DataSequence dataSequence, Variable var){
        return dataSequence.stream().map( data -> {
            inferenceAlgorithmForDBN.addDynamicEvidence(data);
            inferenceAlgorithmForDBN.runInference();
            return inferenceAlgorithmForDBN.getFilteredPosterior(var);
        });
    }

    /**
     * Returns the predictive posterior distributions of a given {@link Variable} object and an input {@link DataSequence}.
     * @param dataSequence an input {@link DataSequence} object.
     * @param var a {@link Variable} object.
     * @param nTimesAhead an {@code int} that represents the number of time steps ahead.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @return an {@link UnivariateDistribution} object.
     */
    public static <E extends UnivariateDistribution> Stream<E> getStreamOfPredictivePosteriors(DataSequence dataSequence, Variable var, int nTimesAhead){
        return dataSequence.stream().map( data -> {
            inferenceAlgorithmForDBN.addDynamicEvidence(data);
            inferenceAlgorithmForDBN.runInference();
            return inferenceAlgorithmForDBN.getPredictivePosterior(var, nTimesAhead);
        });
    }

    /**
     * Returns the last filtered posterior distribution of a given {@link Variable} object in an input {@link DataSequence}.
     * @param dataSequence an input {@link DataSequence} object.
     * @param var a {@link Variable} object.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @return an {@link UnivariateDistribution} object.
     */
    public static <E extends UnivariateDistribution> E getLastFilteredPosteriorInTheSequence(DataSequence dataSequence, Variable var){
        final UnivariateDistribution[] dist = new UnivariateDistribution[1];
        getStreamOfFilteredPosteriors(dataSequence, var).forEach(e -> dist[0]=e);
        return (E)dist[0];
    }

    /**
     * Returns the last predictive posterior distribution of a given {@link Variable} object in an input {@link DataSequence}.
     * @param dataSequence an input {@link DataSequence} object.
     * @param var a {@link Variable} object.
     * @param nTimesAhead an {@code int} that represents the number of time steps ahead.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @return an {@link UnivariateDistribution} object.
     */
    public static <E extends UnivariateDistribution> E getLastPredictivePosteriorInTheSequence(DataSequence dataSequence, Variable var, int nTimesAhead){
        final UnivariateDistribution[] dist = new UnivariateDistribution[1];
        getStreamOfPredictivePosteriors(dataSequence, var, nTimesAhead).forEach(e -> dist[0]=e);
        return (E)dist[0];
    }

}
