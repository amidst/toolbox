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

package eu.amidst.core.inference;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * This interface handles and defines the algorithm used to run inference in {@link BayesianNetwork} models.
 *
 * @see "eu.amidst.core.examples.inference"
 */
public interface InferenceAlgorithm {

    /**
     * Runs inference.
     */
    void runInference();

    /**
     * Sets the model for this InferenceAlgorithm.
     * @param model a {@link BayesianNetwork} model to which this InferenceAlgorithm will be set.
     */
    void setModel(BayesianNetwork model);

    /**
     * Returns the original model of this InferenceAlgorithm.
     * @return a {@link BayesianNetwork} object that represents the original model of this InferenceAlgorithm.
     */
    BayesianNetwork getOriginalModel();

    /**
     * Sets the evidence for this InferenceAlgorithm.
     * @param assignment an {@link Assignment} object to which the evidence will be set.
     */
    void setEvidence(Assignment assignment);

    /**
     * Sets the parallel mode for this InferenceAlgorithm.
     * Note that this method is only implemented for the inference algorithms that can be run in parallel.
     * @param parallelMode_ a {@code boolean} that indicates whether this InferenceAlgorithm can be run in parallel (i.e., true) or not (i.e., false).
     */
    default void setParallelMode(boolean parallelMode_){

    }

    /**
     * Returns the expected value.
     * @param var a {@link Variable} object.
     * @param function a {@code Function} object.
     * @return a {@code double} that represents the expected value.
     */
    default double getExpectedValue(Variable var, Function<Double,Double> function){
        UnivariateDistribution univariateDistribution = this.getPosterior(var);

        Random random = new Random(0);
        int nSamples = 1000;
        double val = IntStream.range(0, nSamples).mapToDouble(i -> univariateDistribution.sample(random))
                                .map(sample -> function.apply(sample))
                                .sum();
        return val/nSamples;
    }

    /**
     * Returns the posterior of a given {@link Variable}.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @param var a {@link Variable} object.
     * @return an {@link UnivariateDistribution} object.
     */
    <E extends UnivariateDistribution> E getPosterior(Variable var);

    /**
     * Returns the posterior of a given {@link Variable}.
     * @param <E> a class extending {@link UnivariateDistribution}.
     * @param varID an {@code int} that represents the ID of a variable.
     * @return an {@link UnivariateDistribution} object.
     */
    default <E extends UnivariateDistribution> E getPosterior(int varID){
        return this.getPosterior(this.getOriginalModel().getVariables().getVariableById(varID));
    }

    /**
     * Returns the log probability of the evidence.
     * @return the log probability of the evidence.
     */
    double getLogProbabilityOfEvidence();

    /**
     * Sets the seed.
     * @param seed an {@code int} that represents the seed value to be set.
     */
    void setSeed(int seed);
}