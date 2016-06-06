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

package eu.amidst.latentvariablemodels.dynamicmodels.classifiers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;

import java.io.IOException;

/**
 * This class defines a Dynamic Naive Bayes Classifier model.
 */
public class DynamicNaiveBayesClassifier extends DynamicClassifier {

    /** Represents the Dynamic Naive Bayes Classifier model, which is considered as a {@link DynamicBayesianNetwork} object. */
    DynamicBayesianNetwork bnModel;



    /** Represents whether the children will be connected temporally or not, which is initialized as false. */
    boolean connectChildrenTemporally = false;

    /**
     * Returns  whether the children are connected temporally or not.
     * @return a {@code boolean} that is equal to true if the children are connected temporally.
     */
    public boolean areChildrenTemporallyConnected() {
        return connectChildrenTemporally;
    }

    /**
     * Sets the temporal connection between children for this DynamicNaiveBayesClassifier.
     * @param connectChildrenTemporally a {@code boolean} that is equal to true if the children are connected temporally, and false otherwise.
     */
    public void setConnectChildrenTemporally(boolean connectChildrenTemporally) {
        this.connectChildrenTemporally = connectChildrenTemporally;
    }

    public DynamicNaiveBayesClassifier(Attributes attributes) {
        super(attributes);


//        int classVarIndexInAttributes = (attributes.getNumberOfAttributes() - 1);
//        String classVarName = attributes.getFullListOfAttributes().get(classVarIndexInAttributes).getName();
//        this.setClassName(classVarName);
    }

    //TODO: Consider the case where the dynamic data base have TIME_ID and SEQ_ID
    protected void buildDAG() {

        dynamicDAG = new DynamicDAG(variables);
        dynamicDAG.getParentSetsTimeT().stream()
                // For all variables that are not the class variable
                .filter(w -> !w.getMainVar().equals(classVar))
                .forEach(w -> {
                    // Add the class variable as a parent
                    w.addParent(classVar);
                    // If true, add a connection to its interface replication
                    if(areChildrenTemporallyConnected())
                        w.addParent(variables.getInterfaceVariable(w.getMainVar()));
                });

        // Connect the class variale to its interface replication
        dynamicDAG.getParentSetTimeT(classVar).addParent(variables.getInterfaceVariable(classVar));


    }


//    /**
//     * Learns this DynamicNaiveBayesClassifier from a given data stream.
//     * @param dataStream a {@link DataStream} of {@link DynamicDataInstance}s.
//     */
//    public void learn(DataStream<DynamicDataInstance> dataStream){
//        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
//        parallelMaximumLikelihood.setDynamicDAG(dynamicDAG);
//        parallelMaximumLikelihood.initLearning();
//        parallelMaximumLikelihood.updateModel(dataStream);
//        bnModel = parallelMaximumLikelihood.getLearntDBN();
//    }

    public static void main(String[] args) throws IOException {

//        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
//        BayesianNetworkGenerator.setNumberOfMultinomialVars(5, 2);
//        BayesianNetworkGenerator.setSeed(0);
//        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);
//
//        int sampleSize = 1000;
//        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
//        String file = "./datasets/simulated/exampleDS_d0_c5.arff";
//        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(sampleSize);
//        DataStreamWriter.writeDataToFile(dataStream, file);

        String file = "./datasets/simulated/exampleDS_d2_c3.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);
        data.getAttributes().getFullListOfAttributes().forEach(attribute -> System.out.println(attribute.getName()));

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier(data.getAttributes());

        int classVarIndexInAttributes = 2;
        String classVarName = data.getAttributes().getFullListOfAttributes().get(classVarIndexInAttributes).getName();
        model.setClassName(classVarName);

        model.setConnectChildrenTemporally(true);
        model.updateModel(data);
        DynamicBayesianNetwork nbClassifier = model.getModel();

        System.out.println(nbClassifier.toString());


    }
}
