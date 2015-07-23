/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.core.learning.structural;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.DAG;

// TODO Add a method for updating a model with one data instance:
// TODO public BayesianNetwork updateModel(BayesianNetwork model, DataInstance instance);

 /**
 * This class defines the {@link eu.amidst.core.models.BayesianNetwork} structural learning engine.
 */
public final class StructuralLearningEngine {

    /** Represents the used structural learning Algorithm. */
    private static StructuralLearningAlgorithm structuralLearningAlgorithm;

    /** Indicates the parallel processing mode, initialized as {@code false}. */
    private static boolean parallelMode = false;

     /**
      * Sets the parallel processing mode.
      * @param parallelMode {@code true} if the learning is performed in parallel, {@code false} otherwise.
      */
    public static void setParallelMode(boolean parallelMode) {
        StructuralLearningEngine.parallelMode = parallelMode;
    }

     /**
      * Sets the structural learning Algorithm.
      * @param structuralLearningAlgorithm the structural learning Algorithm.
      */
    public static void setStructuralLearningAlgorithm(StructuralLearningAlgorithm structuralLearningAlgorithm) {
        StructuralLearningEngine.structuralLearningAlgorithm = structuralLearningAlgorithm;
    }

     /**
      * Learns the {@link eu.amidst.core.models.BayesianNetwork} graphical structure, i.e. {@link DAG}, from a given {@link DataStream}.
      * @param dataStream a {@link DataStream}.
      * @return a directed acyclic graph {@link DAG}.
      */
    public static DAG learnDAG(DataStream<DataInstance> dataStream){
        structuralLearningAlgorithm.setDataStream(dataStream);
        structuralLearningAlgorithm.setParallelMode(parallelMode);
        structuralLearningAlgorithm.runLearning();
        return structuralLearningAlgorithm.getDAG();
    }

}
