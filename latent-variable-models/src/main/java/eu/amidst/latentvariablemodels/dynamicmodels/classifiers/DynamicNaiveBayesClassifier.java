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
import eu.amidst.core.learning.parametric.bayesian.DataPosteriorAssignment;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.utils.Utils;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;

import java.io.IOException;
import java.io.Serializable;

/**
 * This class defines a Dynamic Naive Bayes Classifier model.
 */
public class DynamicNaiveBayesClassifier extends DynamicClassifier implements Serializable {

    private static final long serialVersionUID = 329639736967237932L;

    protected DynamicParallelVB learningAlgorithmFlink = null;
    private DataSet<DataPosteriorAssignment> previousPredictions = null;

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
        previousPredictions = null;
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
            learningAlgorithmFlink.setOutput(false);
            learningAlgorithmFlink.setTestELBO(false);
            learningAlgorithmFlink.setMaximumGlobalIterations(10);
            learningAlgorithmFlink.setMaximumLocalIterations(100);
            learningAlgorithmFlink.setGlobalThreshold(0.1);
            learningAlgorithmFlink.setLocalThreshold(0.1);
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

    public DataSet<DynamicDataInstance> predict(int timeSlice, DataFlink<DynamicDataInstance> data) {

        DataFlink<DynamicDataInstance> dataWithoutClass = data.map(new MapFunction<DynamicDataInstance, DynamicDataInstance>() {
            @Override
            public DynamicDataInstance map(DynamicDataInstance dynamicDataInstance) throws Exception {
                DynamicDataInstance result = Serialization.deepCopy(dynamicDataInstance);
                result.setValue(classVar, Utils.missingValue());
                return result;
            }
        });

        this.previousPredictions = learningAlgorithmFlink.predict(timeSlice, this.previousPredictions, dataWithoutClass);

        DataSet<DynamicDataInstance> dataPredictions = this.previousPredictions.map(dataPosteriorAssignment -> {
            long sequence_id = dataPosteriorAssignment.getPosterior().getId();
            DynamicDataInstance dataPosterior = ((DynamicDataInstance)dataPosteriorAssignment.getAssignment());
            dataPosterior.setValue(dataPosterior.getAttributes().getSeq_id(),sequence_id);
            return dataPosterior;
        });

        DataSet<DynamicDataInstance> predictedClasses = dataPredictions.map(dynamicDataInstance -> {
            DynamicDataInstance result = Serialization.deepCopy(dynamicDataInstance);
            result.getVariables().forEach(variable -> {
                if (!variable.equals(classVar)) {
                    result.setValue(variable,Utils.missingValue());
                }
            });
            return result;
        });

        return predictedClasses;

//        DataSet<DynamicDataInstance> dataPlusPredictions = data.getDataSet()
//                .join(dataPredictions)
//                .where("SEQUENCE_ID")       // key of the first input (users)
//                .equalTo("SEQUENCE_ID")    // key of the second input (stores)
//                .map(new MapFunction<Tuple2<DynamicDataInstance, DynamicDataInstance>, DynamicDataInstance>() {
//                    @Override
//                    public DynamicDataInstance map(Tuple2<DynamicDataInstance, DynamicDataInstance> dynamicDataInstanceDynamicDataInstanceTuple2) throws Exception {
//                        DynamicDataInstance dataNoClass = dynamicDataInstanceDynamicDataInstanceTuple2.getField(0);
//                        DynamicDataInstance dataPredicted = dynamicDataInstanceDynamicDataInstanceTuple2.getField(1);
//                        return null;
//                    }
//                });
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
