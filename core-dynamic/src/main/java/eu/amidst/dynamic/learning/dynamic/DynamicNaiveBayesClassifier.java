/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.learning.dynamic;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.DynamicModelFactory;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public class DynamicNaiveBayesClassifier {

    int classVarID;
    DynamicBayesianNetwork bnModel;
    boolean parallelMode = true;
    boolean connectChildrenTemporally = false;

    public boolean isParallelMode() {
        return parallelMode;
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    public boolean isConnectChildrenTemporally() {
        return connectChildrenTemporally;
    }

    public void setConnectChildrenTemporally(boolean connectChildrenTemporally) {
        this.connectChildrenTemporally = connectChildrenTemporally;
    }

    public int getClassVarID() {
        return classVarID;
    }

    //TODO: Consider the case where the dynamic data base have TIME_ID and SEQ_ID
    public void setClassVarID(int classVarID) {
        this.classVarID = classVarID;
    }

    public DynamicBayesianNetwork getDynamicBNModel() {
        return bnModel;
    }

    private DynamicDAG dynamicNaiveBayesStructure(DataStream<DynamicDataInstance> dataStream){

        DynamicVariables dynamicVariables = DynamicModelFactory.newDynamicVariables(dataStream.getAttributes());
        Variable classVar = dynamicVariables.getVariableById(this.getClassVarID());
        DynamicDAG dag = DynamicModelFactory.newDynamicDAG(dynamicVariables);

        // TODO Remove this commented part. Done for efficiency in the inference demos.

        dag.getParentSetsTimeT().stream()
                .filter(w -> w.getMainVar().getVarID() != classVar.getVarID())
                .forEach(w -> {
                    w.addParent(classVar);
                    if(isConnectChildrenTemporally())
                        w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                });


        dag.getParentSetTimeT(classVar).addParent(dynamicVariables.getInterfaceVariable(classVar));

        return dag;
    }

    public void learn(DataStream<DynamicDataInstance> dataStream){
        LearningEngineForDBN.setDynamicStructuralLearningAlgorithm(this::dynamicNaiveBayesStructure);
        MaximumLikelihoodForDBN.setParallelMode(this.isParallelMode());
        LearningEngineForDBN.setDynamicParameterLearningAlgorithm(MaximumLikelihoodForDBN::learnDynamic);
        bnModel = LearningEngineForDBN.learnDynamicModel(dataStream);
    }

    public static void main(String[] args) throws IOException {

        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(5, 2);
        BayesianNetworkGenerator.setSeed(0);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 1000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        String file = "./datasets/randomdata.arff";
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(sampleSize);
        DataStreamWriter.writeDataToFile(dataStream, file);

        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);

        for (int i = 1; i <= 1; i++) {
            DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
            model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
            model.setParallelMode(true);
            model.learn(data);
            DynamicBayesianNetwork nbClassifier = model.getDynamicBNModel();
            System.out.println(nbClassifier.toString());
        }

    }
}
