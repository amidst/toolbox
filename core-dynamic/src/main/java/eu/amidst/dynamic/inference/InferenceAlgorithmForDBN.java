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

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.core.variables.Variable;

/**
 * This interface handles and defines the algorithm used to run inference in {@link DynamicBayesianNetwork} models.
 * For examples see eu.amidst.dynamic.examples.inference.
 */
public interface InferenceAlgorithmForDBN {

    /**
     * Runs inference.
     */
    void runInference();

    /**
     * Sets the model for this InferenceAlgorithmForDBN.
     * @param model a {@link DynamicBayesianNetwork} model to which thisInferenceAlgorithmForDBN will be set.
     */
    void setModel(DynamicBayesianNetwork model);

    /**
     * Returns the original model of this InferenceAlgorithmForDBN.
     * @return a {@link DynamicBayesianNetwork} object that represents the original model of this InferenceAlgorithmForDBN.
     */
    DynamicBayesianNetwork getOriginalModel();

    /**
     * Sets the evidence for this InferenceAlgorithmForDBN.
     * @param assignment an {@link DynamicAssignment} object to which the evidence will be set.
     */
    void addDynamicEvidence(DynamicAssignment assignment);

    /**
     * Resets the parameters of this InferenceAlgorithmForDBN.
     */
    void reset();

    /**
     * Returns the filtered posterior distribution of a given {@link Variable} object.
     * @param var a {@link Variable} object.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @return an {@link UnivariateDistribution} object.
     */
    <E extends UnivariateDistribution> E getFilteredPosterior(Variable var);

    /**
     * Returns the predictive posterior distribution of a given {@link Variable} object for nTimesAhead.
     * @param var a {@link Variable} object.
     * @param nTimesAhead an {@code int} that represents the number of time steps ahead.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @return an {@link UnivariateDistribution} object.
     */
    <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead);

    /**
     * Returns the time of the last evidence of this InferenceAlgorithmForDBN.
     * @return a {@code long} that represents the time ID.
     */
    long getTimeIDOfLastEvidence();

    /**
     * Returns the time ID of the posterior of this InferenceAlgorithmForDBN.
     * @return a {@code long} that represents the time ID.
     */
    long getTimeIDOfPosterior();

}
