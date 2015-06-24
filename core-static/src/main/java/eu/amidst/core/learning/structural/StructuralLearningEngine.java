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

/**
 *
 * TODO Add a method for updating a model with one data instance:
 *
 * public BayesianNetwork updateModel(BayesianNetwork model, DataInstance instance);
 *
 * Created by andresmasegosa on 06/01/15.
 */
public final class StructuralLearningEngine {

    private static StructuralLearningAlgorithm structuralLearningAlgorithm;
    private static boolean parallelMode = false;

    public static void setParallelMode(boolean parallelMode) {
        StructuralLearningEngine.parallelMode = parallelMode;
    }

    public static void setStructuralLearningAlgorithm(StructuralLearningAlgorithm structuralLearningAlgorithm) {
        StructuralLearningEngine.structuralLearningAlgorithm = structuralLearningAlgorithm;
    }

    public static DAG learnDAG(DataStream<DataInstance> dataStream){
        structuralLearningAlgorithm.setDataStream(dataStream);
        structuralLearningAlgorithm.setParallelMode(parallelMode);
        structuralLearningAlgorithm.runLearning();
        return structuralLearningAlgorithm.getDAG();
    }



    public static void main(String[] args) throws Exception{

    }

}
