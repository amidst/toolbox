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
import eu.amidst.core.learning.parametric.MaximumLikelihood;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 06/01/15.
 */
public class NaiveBayesClassifier implements Classifier{

    int classVarID;
    BayesianNetwork bnModel;
    boolean parallelMode = true;
    InferenceAlgorithm predictions;

    public NaiveBayesClassifier(){
        predictions=new VMP();
        predictions.setSeed(0);
    }

    public boolean isParallelMode() {
        return parallelMode;
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    @Override
    public double[] predict(DataInstance instance) {
        this.predictions.setEvidence(instance);
        Multinomial multinomial = this.predictions.getPosterior(this.classVarID);
        return multinomial.getParameters();
    }

    @Override
    public int getClassVarID() {
        return classVarID;
    }

    @Override
    public void setClassVarID(int classVarID) {
        this.classVarID = classVarID;
    }

    public BayesianNetwork getBNModel() {
        return bnModel;
    }

    private DAG staticNaiveBayesStructure(DataStream<DataInstance> dataStream){
        Variables modelHeader = new Variables(dataStream.getAttributes());
        Variable classVar = modelHeader.getVariableById(this.getClassVarID());
        DAG dag = new DAG(modelHeader);
        if (parallelMode)
            dag.getParentSets().parallelStream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));
        else
            dag.getParentSets().stream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));

        return dag;
    }

    @Override
    public void learn(DataStream<DataInstance> dataStream){
        ParameterLearningAlgorithm parameterLearningAlgorithm = new MaximumLikelihood();
        parameterLearningAlgorithm.setParallelMode(this.parallelMode);
        parameterLearningAlgorithm.setDAG(this.staticNaiveBayesStructure(dataStream));
        parameterLearningAlgorithm.setDataStream(dataStream);
        parameterLearningAlgorithm.initLearning();
        parameterLearningAlgorithm.runLearning();
        bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();
        predictions.setModel(bnModel);
    }


    public static void main(String[] args){

        BayesianNetworkGenerator.setNumberOfGaussianVars(0);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(50000, 10);
        BayesianNetworkGenerator.setSeed(0);

        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        int sampleSize = 100;
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        DataStream<DataInstance> data =  sampler.sampleToDataStream(sampleSize);

        for (int i = 1; i <= 10; i++) {
            NaiveBayesClassifier model = new NaiveBayesClassifier();
            model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 1);
            model.learn(data);
            BayesianNetwork nbClassifier = model.getBNModel();
        }

    }
}
