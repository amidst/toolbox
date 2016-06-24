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

package eu.amidst.latentvariablemodels.staticmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.parametric.dVMP;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

/**
 * The Model abstract class is defined as a superclass to all static standard models (not used for classification, if so,
 * extends Classifier)
 *
 * Created by andresmasegosa on 4/3/16.
 */
public abstract class Model {

    protected ParameterLearningAlgorithm learningAlgorithm = null;

    protected eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm learningAlgorithmFlink = null;

    protected DAG dag;

    protected Variables vars = null;

    protected String errorMessage = "";

    protected int windowSize = 1000;

    protected boolean initialized = false;

    public Model(Attributes attributes) {
        vars = new Variables(attributes);

        if(!this.isValidConfiguration()) {
            throw new WrongConfigurationException(getErrorMessage());
        }
    }

    public DAG getDAG() {
        if (dag==null){
            buildDAG();
        }
        return dag;
    }

    public void setLearningAlgorithm(ParameterLearningAlgorithm learningAlgorithm) {
        this.learningAlgorithm = learningAlgorithm;
    }

    public void setFlinkLearningAlgorithm(eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm learningAlgorithm) {
        this.learningAlgorithmFlink = learningAlgorithm;
    }

    public void setWindowSize(int windowSize){
        this.windowSize = windowSize;
    }


    private void initLearningFlink() {
        if(learningAlgorithmFlink==null) {
            dVMP dvmp = new dVMP();
            dvmp.setBatchSize(100);
            dvmp.setMaximumGlobalIterations(10);
            dvmp.setMaximumLocalIterations(100);
            dvmp.setLocalThreshold(0.0001);
            dvmp.setGlobalThreshold(0.01);
            learningAlgorithmFlink = dvmp;
        }

        learningAlgorithmFlink.setBatchSize(windowSize);
        learningAlgorithmFlink.setDAG(this.getDAG());
        learningAlgorithmFlink.initLearning();
        initialized=true;
    }


    private  void initLearning() {
        if(learningAlgorithm==null) {
            SVB svb = new SVB();
            svb.setWindowsSize(100);
            svb.getPlateuStructure().getVMP().setTestELBO(false);
            svb.getPlateuStructure().getVMP().setMaxIter(100);
            svb.getPlateuStructure().getVMP().setThreshold(0.0001);

            learningAlgorithm = svb;
        }
        learningAlgorithm.setWindowsSize(windowSize);
        learningAlgorithm.setDAG(this.getDAG());
        learningAlgorithm.setOutput(true);
        learningAlgorithm.initLearning();
        initialized=true;
    }



    public double updateModel(DataFlink<DataInstance> dataFlink){
        if (!initialized)
            initLearningFlink();

        return this.learningAlgorithmFlink.updateModel(dataFlink);
    }

    public double updateModel(DataStream<DataInstance> dataStream){
        if (!initialized)
            initLearning();


        return this.learningAlgorithm.updateModel(dataStream);
    }

    public double updateModel(DataOnMemory<DataInstance> datBatch){
        if (!initialized)
            initLearning();

        return learningAlgorithm.updateModel(datBatch);
    }

    public void resetModel(){
        initialized=false;
    }

    public BayesianNetwork getModel(){
        if (learningAlgorithm !=null){
            return this.learningAlgorithm.getLearntBayesianNetwork();
        }

        if (learningAlgorithmFlink!=null)
            return this.learningAlgorithmFlink.getLearntBayesianNetwork();

        return null;
    }

    protected String getErrorMessage() {
        return errorMessage;
    }

    protected void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    protected abstract void buildDAG();

    public boolean isValidConfiguration(){
        return true;
    }

    @Override
    public String toString() {
        return this.getModel().toString();
    }

}
