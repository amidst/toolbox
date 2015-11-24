/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.classifiers;

import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.ParallelMLMissingData;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The NaiveBayesClassifier class implements the interface {@link Classifier} and defines a Naive Bayes Classifier.
 */
public class NaiveBayesClassifier implements Classifier{

    /** Represents the name of the class variable. */
    String className;

    /** Represents the ID of the class variable. */
    String classVarID;

    /** Represents the Naive Bayes Classifier, which is considered as a {@link BayesianNetwork}. */
    BayesianNetwork bnModel;

    /** Represents the parallel mode, which is initialized as true. */
    boolean parallelMode = true;

    /** Represents the inference algorithm. */
    InferenceAlgorithm predictions;

    /** Represents the class variable */
    Variable classVar;

    /**
     * Creates a new NaiveBayesClassifier.
     */
    public NaiveBayesClassifier(){
        predictions=new VMP();
        predictions.setSeed(0);
    }

    /**
     * Returns whether the parallel mode is supported or not.
     * @return true if the parallel mode is supported.
     */
    public boolean isParallelMode() {
        return parallelMode;
    }

    /**
     * Sets the parallel mode for this NaiveBayesClassifier.
     * @param parallelMode boolean equals to true if the parallel mode is supported, and false otherwise.
     */
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    /**
     * Predicts the class membership probabilities for a given instance.
     * @param instance the data instance to be classified.
     * @return an array of doubles containing the estimated membership probabilities of the data instance for each class label.
     */
    @Override
    public double[] predict(DataInstance instance) {
        if (!Utils.isMissingValue(instance.getValue(classVar)))
            System.out.println("Class Variable can not be set.");
        this.predictions.setEvidence(instance);
        this.predictions.runInference();
        Multinomial multinomial = this.predictions.getPosterior(classVar);
        return multinomial.getParameters();
    }

    /**
     * Returns the name of the class variable.
     * @return the name of the class variable.
     */
    @Override
    public String getClassName() {
        return className;
    }

    /**
     * Sets the ID of the class variable.
     * @param className the ID of the class variable.
     */
    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns this NaiveBayesClassifier considered as a Bayesian network model.
     * @return a BayesianNetwork.
     */
    public BayesianNetwork getBNModel() {
        return bnModel;
    }

    /**
     * Trains this NaiveBayesClassifier using the given data streams.
     * @param dataStream a data stream {@link DataStream}.
     */
    @Override
    public void learn(DataStream<DataInstance> dataStream){
        ParallelMLMissingData parameterLearningAlgorithm = new ParallelMLMissingData();
        parameterLearningAlgorithm.setParallelMode(this.parallelMode);
        parameterLearningAlgorithm.setDAG(DAGGenerator.getNaiveBayesStructure(dataStream.getAttributes(),this.className));
        parameterLearningAlgorithm.setDataStream(dataStream);
        parameterLearningAlgorithm.initLearning();
        parameterLearningAlgorithm.runLearning();
        bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();
        predictions.setModel(bnModel);
    }


    /**
     * Trains this NaiveBayesClassifier using the given data streams.
     * @param dataStream a data stream {@link DataStream}.
     * @param batchSize the size of the batch for the parallel ML algorithm.
     */
    public void learn(DataStream<DataInstance> dataStream, int batchSize){
        ParallelMLMissingData parameterLearningAlgorithm = new ParallelMLMissingData();
        parameterLearningAlgorithm.setBatchSize(batchSize);
        parameterLearningAlgorithm.setParallelMode(this.parallelMode);
        parameterLearningAlgorithm.setDAG(DAGGenerator.getNaiveBayesStructure(dataStream.getAttributes(),this.className));
        parameterLearningAlgorithm.setDataStream(dataStream);
        parameterLearningAlgorithm.initLearning();
        parameterLearningAlgorithm.runLearning();
        bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();
        predictions.setModel(bnModel);
    }
}
