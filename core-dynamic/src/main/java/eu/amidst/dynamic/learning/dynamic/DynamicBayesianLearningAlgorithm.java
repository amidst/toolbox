/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.learning.dynamic;

import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;

/**
 * This interface defines the Dynamic Bayesian learning algorithm for {@link DynamicBayesianNetwork} models.
 */
public interface DynamicBayesianLearningAlgorithm {

    /**
     * Updates the model using a given {@link DataOnMemory} of {@link DynamicDataInstance}s.
     * @param batch a {@link DataOnMemory} of {@link DynamicDataInstance}s.
     * @return a double value.
     */
    double updateModel(DataOnMemory<DynamicDataInstance> batch);

    /**
     * Returns the log of the marginal probability.
     * @return the log of the marginal probability.
     */
    double getLogMarginalProbability();

    /**
     * Initializes the parameter learning process.
     */
    void initLearning();

    /**
     * Runs the learning process.
     */
    void runLearning();

    /**
     * Sets the {@link DynamicDAG} for this DynamicBayesianLearningAlgorithm.
     * @param dag a valid {@link DynamicDAG} object.
     */
    void setDynamicDAG(DynamicDAG dag);

    /**
     * Sets the {@link DataStream} to be used by this DynamicBayesianLearningAlgorithm.
     * @param data a {@link DataStream} of {@link DynamicDataInstance} objects.
     */
    void setDataStream(DataStream<DynamicDataInstance> data);

    /**
     * Returns the learnt {@link DynamicBayesianNetwork}.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    DynamicBayesianNetwork getLearntDBN();

    /**
     * Sets the parallel processing mode.
     * @param parallelMode {@code true} if the learning is performed in parallel, {@code false} otherwise.
     */
    void setParallelMode(boolean parallelMode);

}
