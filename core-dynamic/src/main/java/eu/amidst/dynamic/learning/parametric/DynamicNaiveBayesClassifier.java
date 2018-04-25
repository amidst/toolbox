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

package eu.amidst.dynamic.learning.parametric;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

import java.io.IOException;

/**
 * This class defines a Dynamic Naive Bayes Classifier model.
 */
public class DynamicNaiveBayesClassifier {

    /** Represents the ID of class variable */
    int classVarID;

    /** Represents the Dynmaic Naive Bayes Classifier model, which is considered as a {@link DynamicBayesianNetwork} object. */
    DynamicBayesianNetwork bnModel;

    /** Represents the parallel mode, which is initialized as true. */
    boolean parallelMode = true;

    /** Represents whether the children will be connected temporally or not, which is initialized as false. */
    boolean connectChildrenTemporally = false;

    /**
     * Returns whether the parallel mode is supported or not.
     * @return true if the parallel mode is supported.
     */
    public boolean isParallelMode() {
        return parallelMode;
    }

    /**
     * Sets the parallel mode for this DynamicNaiveBayesClassifier.
     * @param parallelMode a {@code boolean} that is equal to true if the parallel mode is supported, and false otherwise.
     */
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    /**
     * Returns  whether the children are connected temporally or not.
     * @return a {@code boolean} that is equal to true if the children are connected temporally.
     */
    public boolean isConnectChildrenTemporally() {
        return connectChildrenTemporally;
    }

    /**
     * Sets the temporal connection between children for this DynamicNaiveBayesClassifier.
     * @param connectChildrenTemporally a {@code boolean} that is equal to true if the children are connected temporally, and false otherwise.
     */
    public void setConnectChildrenTemporally(boolean connectChildrenTemporally) {
        this.connectChildrenTemporally = connectChildrenTemporally;
    }

    /**
     * Returns the ID of the class variable.
     * @return an {@code int} that represents the ID of the class variable.
     */
    public int getClassVarID() {
        return classVarID;
    }

    //TODO: Consider the case where the dynamic data base have TIME_ID and SEQ_ID

    /**
     * Sets the ID of the class variable.
     * @param classVarID an {@code int} that represents the ID of the class variable.
     */
    public void setClassVarID(int classVarID) {
        this.classVarID = classVarID;
    }

    /**
     * Returns this DynamicNaiveBayesClassifier considered as a dynamic Bayesian network model.
     * @return a {@link DynamicBayesianNetwork} object.
     */
    public DynamicBayesianNetwork getDynamicBNModel() {
        return bnModel;
    }

    /**
     * Learns the dynamic graphical structure for this DynamicNaiveBayesClassifier given an input data stream.
     * @param dataStream a {@link DataStream} of {@link DynamicDataInstance}s.
     * @return a {@link DynamicDAG} object.
     */
    private DynamicDAG dynamicNaiveBayesStructure(DataStream<DynamicDataInstance> dataStream){

        DynamicVariables dynamicVariables = new DynamicVariables(dataStream.getAttributes());
        Variable classVar = dynamicVariables.getVariableById(this.getClassVarID());
        DynamicDAG dag = new DynamicDAG(dynamicVariables);
        
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

    /**
     * Learns this DynamicNaiveBayesClassifier from a given data stream.
     * @param dataStream a {@link DataStream} of {@link DynamicDataInstance}s.
     */
    public void learn(DataStream<DynamicDataInstance> dataStream){
        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setDynamicDAG(this.dynamicNaiveBayesStructure(dataStream));
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(dataStream);
        bnModel = parallelMaximumLikelihood.getLearntDBN();
    }

    public static void main(String[] args) throws IOException {

        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(5, 2);
        BayesianNetworkGenerator.setSeed(0);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 1000;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        String file = "./datasets/simulated/randomdata.arff";
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
