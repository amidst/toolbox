/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.learning.dynamic;

import com.google.common.base.Stopwatch;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.corestatic.variables.Variable;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public final class LearningEngineForDBN {
    private static DynamicParameterLearningAlgorithm dynamicParameterLearningAlgorithm = MaximumLikelihoodForDBN::learnDynamic;

    private static DynamicStructuralLearningAlgorithm dynamicStructuralLearningAlgorithm = LearningEngineForDBN::dynamicNaiveBayesStructure;


    private static DynamicDAG dynamicNaiveBayesStructure(DataStream<DynamicDataInstance> dataStream){
        DynamicVariables modelHeader = new DynamicVariables(dataStream.getAttributes());
        DynamicDAG dag = new DynamicDAG(modelHeader);
        Variable classVar = modelHeader.getVariableById(modelHeader.getNumberOfVars()-1);
        dag.getParentSetsTimeT()
                .stream()
                .filter(w-> w.getMainVar()
                        .getVarID()!=classVar.getVarID())
                .forEach(w -> {
                    w.addParent(classVar);
                    w.addParent(modelHeader.getInterfaceVariable(w.getMainVar()));
                });

        return dag;
    }


    public static void setDynamicParameterLearningAlgorithm(DynamicParameterLearningAlgorithm dynamicParameterLearningAlgorithm) {
        LearningEngineForDBN.dynamicParameterLearningAlgorithm = dynamicParameterLearningAlgorithm;
    }


    public static void setDynamicStructuralLearningAlgorithm(DynamicStructuralLearningAlgorithm dynamicStructuralLearningAlgorithm) {
        LearningEngineForDBN.dynamicStructuralLearningAlgorithm = dynamicStructuralLearningAlgorithm;
    }


    public static DynamicBayesianNetwork learnParameters(DynamicDAG dag, DataStream<DynamicDataInstance> dataStream){
        return dynamicParameterLearningAlgorithm.learn(dag, dataStream);
    }


    public static DynamicDAG learnDynamicDAG(DataStream<DynamicDataInstance> dataStream){
        return dynamicStructuralLearningAlgorithm.learn(dataStream);
    }

    public static DynamicBayesianNetwork learnDynamicModel(DataStream<DynamicDataInstance> database){
        Stopwatch watch = Stopwatch.createStarted();
        DynamicDAG dag = dynamicStructuralLearningAlgorithm.learn(database);
        System.out.println("Structural Learning : " + watch.stop());

        watch = Stopwatch.createStarted();
        DynamicBayesianNetwork network = dynamicParameterLearningAlgorithm.learn(dag,database);
        System.out.println("Parameter Learning: " + watch.stop());

        return network;
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
