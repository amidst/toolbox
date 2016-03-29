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

package eu.amidst.core.learning.structural;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.DAG;

/**
 * This interface defines the Algorithm for learning the {@link eu.amidst.core.models.BayesianNetwork} graphical structure.
 */
public interface StructuralLearningAlgorithm {

    /**
     * Initializes the structural learning process.
     */
    void initLearning();

    /**
     * Updates the model using a given {@link DataOnMemory} object.
     * @param batch a {@link DataOnMemory} object.
     * @return a double value.
     */
    double updateModel(DataOnMemory<DataInstance> batch);

    /**
     * Sets the {@link DataStream} to be used by this StructuralLearningAlgorithm.
     * @param data a {@link DataStream} object.
     */
    void setDataStream(DataStream<DataInstance> data);

    /**
     * Runs the structural learning process.
     */
    void runLearning();

    /**
     * Returns the {@link DAG} structure.
     * @return the {@link DAG} structure.
     */
    DAG getDAG();

    /**
     * Sets the parallel processing mode.
     * @param parallelMode {@code true} if the learning is performed in parallel, {@code false} otherwise.
     */
    void setParallelMode(boolean parallelMode);

}
