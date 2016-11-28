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
import eu.amidst.core.learning.parametric.bayesian.utils.DataPosteriorAssignment;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;

import java.io.IOException;
import java.io.Serializable;

/**
 * This class defines a Dynamic Naive Bayes Classifier model.
 */
public class DynamicNaiveBayesClassifier extends DynamicClassifier implements Serializable {

    private int nGlobalIterations = 3;

    private static final long serialVersionUID = 329639736967237932L;

    protected DynamicParallelVB learningAlgorithmFlink = null;

    transient private DataSet<DataPosteriorAssignment> previousPredictions = null;

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

    public void setGlobalIterations(int nGlobalIterations) {
        this.nGlobalIterations = nGlobalIterations;
    }

    private void initLearningFlink() {
        if(learningAlgorithmFlink==null) {
            learningAlgorithmFlink = new DynamicParallelVB();
            learningAlgorithmFlink.setDAG(this.getDynamicDAG());
            learningAlgorithmFlink.setOutput(false);
            learningAlgorithmFlink.setTestELBO(false);
            learningAlgorithmFlink.setBatchSize(500);
            learningAlgorithmFlink.setMaximumGlobalIterations(this.nGlobalIterations);
            learningAlgorithmFlink.setMaximumLocalIterations(10*this.nGlobalIterations);
            learningAlgorithmFlink.setGlobalThreshold(0.05);
            learningAlgorithmFlink.setLocalThreshold(0.01);
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

    public DataSet<Tuple3<Long, Double, Integer>> predict(int timeSlice, DataFlink<DynamicDataInstance> data) throws Exception {

//        System.out.println(data.getDataSet().count());

        DataFlink<DynamicDataInstance> dataWithoutClass = data.map(new DeleteClassValue(classVar));


        this.previousPredictions = learningAlgorithmFlink.predict(timeSlice, this.previousPredictions, dataWithoutClass);


//        this.previousPredictions.print();

        DataSet<Tuple2<Long, Double>> dataPredictions = this.previousPredictions
                                                .map(new BuildPredictions(classVar));

        DataSet<Tuple2<Long, Integer>> dataActualClassValues = data.getDataSet().map(new BuildActualClassValues(classVar));



        DataSet<Tuple3<Long, Double, Integer>> results = dataPredictions.join(dataActualClassValues)
                .where(0)
                .equalTo(0)
                .projectFirst(0,1)
                .projectSecond(1);
                //.types(Long.class, Double.class, Integer.class);


        return results;

//        DataSet<Tuple2<Long, Double>> dataPredictionsWithClass =
//                dataPredictions..join(data.getDataSet())
//                .where(0).equalTo("SEQUENCE_ID").proj

//        dataPredictions
//
//        DataSet<DynamicDataInstance> predictedClasses = dataPredictions.map(dynamicDataInstance -> {
//            DynamicDataInstance result = Serialization.deepCopy(dynamicDataInstance);
//            result.getVariables().forEach(variable -> {
//                if (!variable.equals(classVar)) {
//                    result.setValue(variable,Utils.missingValue());
//                }
//            });
//            return result;
//        });


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


    static class DeleteClassValue implements MapFunction<DynamicDataInstance, DynamicDataInstance> {

        final Variable classVar;

        public DeleteClassValue(Variable classVar) {
            this.classVar = classVar;
        }

        @Override
        public DynamicDataInstance map(DynamicDataInstance dynamicDataInstance) throws Exception {
            dynamicDataInstance.setValue(classVar, Utils.missingValue());
            return dynamicDataInstance;
        }
    }

    static class BuildPredictions implements MapFunction<DataPosteriorAssignment, Tuple2<Long,Double>> {

        final Variable classVar;

        public BuildPredictions(Variable classVar) {
            this.classVar = classVar;
        }

        @Override
        public Tuple2<Long,Double> map(DataPosteriorAssignment dataPosteriorAssignment) throws Exception {

            long sequence_id = dataPosteriorAssignment.getPosterior().getId();
            double predictedProbability = dataPosteriorAssignment.getPosterior().getPosterior(classVar).getParameters()[1];

            Tuple2<Long,Double> result = new Tuple2<>();
            result.setFields(sequence_id, predictedProbability);

            return result;
        }
    }

    static class BuildActualClassValues implements MapFunction<DynamicDataInstance, Tuple2<Long,Integer>> {

        final Variable classVar;

        public BuildActualClassValues(Variable classVar) {
            this.classVar = classVar;
        }

        @Override
        public Tuple2<Long,Integer> map(DynamicDataInstance dynamicDataInstance) throws Exception {

            long sequence_id = dynamicDataInstance.getSequenceID();
            int classValue = (int)dynamicDataInstance.getValue(classVar);

            Tuple2<Long,Integer> result = new Tuple2<>();
            result.setFields(sequence_id, classValue);

            return result;
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
