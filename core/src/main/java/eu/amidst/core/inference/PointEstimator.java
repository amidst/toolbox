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

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;

/**
 * This interface handles a Bayesian posterior point-estimator in a {@link BayesianNetwork}.
 *
 * @see "eu.amidst.core.examples.inference"
 */
public interface PointEstimator {

    /**
     * Runs inference.
     */
    void runInference();

    /**
     * Sets the model for this PointEstimator.
     * @param model a {@link BayesianNetwork} model to which this PointEstimator will be applied.
     */
    void setModel(BayesianNetwork model);

    /**
     * Returns the original model of this PointEstimator.
     * @return a {@link BayesianNetwork} object that represents the original model of this InferenceAlgorithm.
     */
    BayesianNetwork getOriginalModel();

    /**
     * Sets the evidence for this PointEstimator.
     * @param assignment an {@link Assignment} object to which the evidence will be set.
     */
    void setEvidence(Assignment assignment);

    /**
     * Sets the parallel mode for this PointEstimator.
     * Note that this method is only implemented for the point-estimaros that can be run in parallel.
     * @param parallelMode_ a {@code boolean} that indicates whether this PointEstimator can be run in parallel (i.e., true) or not (i.e., false).
     */
    default void setParallelMode(boolean parallelMode_){

    }

    /**
     * Sets the seed.
     * @param seed an {@code int} that represents the seed value to be set.
     */
    void setSeed(int seed);

    /**
     * Returns the log probability of the computed estimate.
     * @return an {@link Assignment} with the computed estimate.
     */
    Assignment getEstimate();

    /**
     * Returns the log probability of the computed estimate.
     * @return the log probability of the computed estimate.
     */
    double getLogProbabilityOfEstimate();


}
