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
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;

import java.io.IOException;

/**
 * This class defines a Dynamic Naive Bayes Classifier model.
 */
public class DynamicNaiveBayesClassifier extends DynamicClassifier {

    protected DynamicParallelVB learningAlgorithmFlink = null;

    /** Represents whether the children will be connected temporally or not, which is initialized as false. */
    boolean connectChildrenTemporally = false;

    /**
     * Returns  whether the children are connected temporally or not.
     * @return a {@code boolean} that is equal to true if the children are connected temporally.
     */
    public boolean connectChildrenTemporally() {
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
    }

    protected void buildDAG() {

        dynamicDAG = new DynamicDAG(variables);
        dynamicDAG.getParentSetsTimeT().stream()
                // For all variables that are not the class variable
                .filter(w -> !w.getMainVar().equals(classVar))
                .forEach(w -> {
                    // Add the class variable as a parent
                    w.addParent(classVar);
                    // If true, add a connection to its interface replication
                    if(connectChildrenTemporally())
                        w.addParent(variables.getInterfaceVariable(w.getMainVar()));
                });

        // Connect the class variale to its interface replication
        dynamicDAG.getParentSetTimeT(classVar).addParent(variables.getInterfaceVariable(classVar));
    }

    public void updateModel(int timeSlice, DataFlink<DynamicDataInstance> dataStream) {
        if (!initialized)
            initLearningFlink();

        learningAlgorithmFlink.updateModelWithNewTimeSlice(timeSlice, dataStream);
    }

    private void initLearningFlink() {
        if(learningAlgorithmFlink==null) {
            learningAlgorithmFlink = new DynamicParallelVB();
            learningAlgorithmFlink.setBatchSize(windowSize);
            learningAlgorithmFlink.setDAG(this.getDynamicDAG());
            learningAlgorithmFlink.setOutput(true);
            learningAlgorithmFlink.setMaximumGlobalIterations(2);
            learningAlgorithmFlink.setMaximumLocalIterations(2);
            learningAlgorithmFlink.initLearning();
        }
        initialized=true;
    }

    public DynamicBayesianNetwork getModel() {
        if (learningAlgorithmFlink !=null){
            return learningAlgorithmFlink.getLearntDynamicBayesianNetwork();
        }
        else {
            return super.getModel();
        }
    }

    public static void main(String[] args) throws IOException {

        String file = "./datasets/simulated/exampleDS_d2_c3.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);
        System.out.println("ATTRIBUTES:");
        data.getAttributes().getFullListOfAttributes().forEach(attribute -> System.out.println(attribute.getName()));
        System.out.println();

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
