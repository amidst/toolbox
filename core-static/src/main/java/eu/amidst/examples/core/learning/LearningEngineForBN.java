/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.examples.core.learning;

import com.google.common.base.Stopwatch;
import eu.amidst.examples.core.datastream.DataInstance;
import eu.amidst.examples.core.datastream.DataStream;
import eu.amidst.examples.core.models.BayesianNetwork;
import eu.amidst.examples.core.models.DAG;
import eu.amidst.examples.core.variables.StaticVariables;
import eu.amidst.examples.core.variables.Variable;

/**
 *
 * TODO Add a method for updating a model with one data instance:
 *
 * public BayesianNetwork updateModel(BayesianNetwork model, DataInstance instance);
 *
 * Created by andresmasegosa on 06/01/15.
 */
public final class LearningEngineForBN {


    private static StaticParameterLearningAlgorithm staticParameterLearningAlgorithm = MaximumLikelihoodForBN::learnParametersStaticModel;

    private static StaticStructuralLearningAlgorithm staticStructuralLearningAlgorithm = LearningEngineForBN::staticNaiveBayesStructure;


    private static DAG staticNaiveBayesStructure(DataStream<DataInstance> dataStream){
        StaticVariables modelHeader = new StaticVariables(dataStream.getAttributes());
        DAG dag = new DAG(modelHeader);
        Variable classVar = modelHeader.getVariableById(modelHeader.getNumberOfVars()-1);
        dag.getParentSets().stream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));

        return dag;
    }

    public static void setStaticParameterLearningAlgorithm(StaticParameterLearningAlgorithm staticParameterLearningAlgorithm) {
        LearningEngineForBN.staticParameterLearningAlgorithm = staticParameterLearningAlgorithm;
    }


    public static void setStaticStructuralLearningAlgorithm(StaticStructuralLearningAlgorithm staticStructuralLearningAlgorithm) {
        LearningEngineForBN.staticStructuralLearningAlgorithm = staticStructuralLearningAlgorithm;
    }

    public static BayesianNetwork learnParameters(DAG dag, DataStream<DataInstance> dataStream){
        return staticParameterLearningAlgorithm.learn(dag,dataStream);
    }


    public static DAG learnDAG(DataStream<DataInstance> dataStream){
        return staticStructuralLearningAlgorithm.learn(dataStream);
    }


    public static BayesianNetwork learnStaticModel(DataStream<DataInstance> database){

        Stopwatch watch = Stopwatch.createStarted();
        DAG dag = staticStructuralLearningAlgorithm.learn(database);
        System.out.println("Structural Learning : " + watch.stop());

        watch = Stopwatch.createStarted();
        BayesianNetwork network = staticParameterLearningAlgorithm.learn(dag,database);
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
