/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.learning.parametric;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;

/**
 *
 * TODO Add a method for updating a model with one data instance:
 *
 * public BayesianNetwork updateModel(BayesianNetwork model, DataInstance instance);
 *
 * Created by andresmasegosa on 06/01/15.
 */
public final class LearningEngine {

    private static boolean parallelMode = false;


    private static ParameterLearningAlgorithm parameterLearningAlgorithm = new MaximumLikelihood();


    public static void setParameterLearningAlgorithm(ParameterLearningAlgorithm parameterLearningAlgorithm) {
        LearningEngine.parameterLearningAlgorithm = parameterLearningAlgorithm;
    }

    public static void setParallelMode(boolean parallelMode) {
        LearningEngine.parallelMode = parallelMode;
    }

    public static BayesianNetwork learnParameters(DAG dag, DataStream<DataInstance> dataStream){
        parameterLearningAlgorithm.setParallelMode(parallelMode);
        parameterLearningAlgorithm.setDAG(dag);
        parameterLearningAlgorithm.setDataStream(dataStream);
        parameterLearningAlgorithm.runLearning();
        return parameterLearningAlgorithm.getLearntBayesianNetwork();
    }

    public static void main(String[] args) throws Exception{

//        String dataFile = new String("./datasets/Pigs.arff");
//        DataStream<StaticDataInstance> data = new StaticDataOnDiskFromFile(new ARFFDataReader(dataFile));
//
//        ParallelTAN tan= new ParallelTAN();
//        tan.setNumCores(4);
//        tan.setNumSamplesOnMemory(1000);
//        tan.setNameRoot("p630400490");
//        tan.setNameTarget("p48124091");
//        LearningEngine.setStaticStructuralLearningAlgorithm(tan::learnDAG);
//
//        MaximumLikelihood.setBatchSize(1000);
//        LearningEngine.setStaticParameterLearningAlgorithm(MaximumLikelihood::learnParametersStaticModel);
//
//        BayesianNetwork tanModel = LearningEngine.learnStaticModel(data);

    }

}
