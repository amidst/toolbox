/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.learning.dynamic;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;


/**
 * This class defines the dynamic Bayesian learning engine for {@link DynamicBayesianNetwork} models.
 */
public final class DynamicBayesianLearningEngine {

    /** Represents the used dynamic Bayesian learning Algorithm, initialized to the {@link DynamicSVB} algorithm. */
    private static DynamicBayesianLearningAlgorithm dynamicBayesianLearningAlgorithm = new DynamicSVB();

    /**
     * Sets the dynamic Bayesian learning Algorithm for this DynamicBayesianLearningEngine.
     * @param dynamicBayesianLearningAlgorithm a {@link DynamicBayesianLearningAlgorithm} object.
     */
    public static void setDynamicBayesianLearningAlgorithm(DynamicBayesianLearningAlgorithm dynamicBayesianLearningAlgorithm) {
        DynamicBayesianLearningEngine.dynamicBayesianLearningAlgorithm = dynamicBayesianLearningAlgorithm;
    }

    /**
     * Runs the learning process.
     */
    public static void runLearning() {
        dynamicBayesianLearningAlgorithm.runLearning();
    }

    /**
     * Sets the {@link DataStream} to be used for this DynamicBayesianLearningEngine.
     * @param data a {@link DataStream} of {@link DynamicDataInstance} objects.
     */
    public static void setDataStream(DataStream<DynamicDataInstance> data){
        dynamicBayesianLearningAlgorithm.setDataStream(data);
    }

    /**
     * Sets the parallel processing mode.
     * @param parallelMode {@code true} if the learning is performed in parallel, {@code false} otherwise.
     */
    public void setParallelMode(boolean parallelMode) {
        dynamicBayesianLearningAlgorithm.setParallelMode(parallelMode);
    }

    /**
     * Sets the {@link DynamicDAG} for this DynamicBayesianLearningEngine.
     * @param dag a valid {@link DynamicDAG} object.
     */
    public static void setDynamicDAG(DynamicDAG dag){
        dynamicBayesianLearningAlgorithm.setDynamicDAG(dag);
    }

    /**
     * Returns the learnt {@link DynamicBayesianNetwork}.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public static DynamicBayesianNetwork getLearntDBN(){
        return dynamicBayesianLearningAlgorithm.getLearntDBN();
    }

}
