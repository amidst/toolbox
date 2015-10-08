/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.classifiers;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.DAGGenerator;

/**
 * The NaiveBayesClassifier class implements the interface {@link Classifier} and defines a Naive Bayes Classifier.
 */
public class NaiveBayesClassifier implements Classifier{

    /** Represents the ID of the class variable. */
    int classVarID;

    /** Represents the Naive Bayes Classifier, which is considered as a {@link BayesianNetwork}. */
    BayesianNetwork bnModel;

    /** Represents the parallel mode, which is initialized as true. */
    boolean parallelMode = true;

    /** Represents the inference algorithm. */
    InferenceAlgorithm predictions;

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
        this.predictions.setEvidence(instance);
        Multinomial multinomial = this.predictions.getPosterior(this.classVarID);
        return multinomial.getParameters();
    }

    /**
     * Returns the ID of the class variable.
     * @return the ID of the class variable.
     */
    @Override
    public int getClassVarID() {
        return classVarID;
    }

    /**
     * Sets the ID of the class variable.
     * @param classVarID the ID of the class variable.
     */
    @Override
    public void setClassVarID(int classVarID) {
        this.classVarID = classVarID;
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
        ParameterLearningAlgorithm parameterLearningAlgorithm = new ParallelMaximumLikelihood();
        parameterLearningAlgorithm.setParallelMode(this.parallelMode);
        parameterLearningAlgorithm.setDAG(DAGGenerator.getNaiveBayesStructure(dataStream.getAttributes(),this.classVarID));
        parameterLearningAlgorithm.setDataStream(dataStream);
        parameterLearningAlgorithm.initLearning();
        parameterLearningAlgorithm.runLearning();
        bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();
        predictions.setModel(bnModel);
    }
}
