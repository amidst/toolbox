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

package eu.amidst.standardmodels;

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
import eu.amidst.standardmodels.exceptions.WrongConfigurationException;

/**
 * The Model abstract class is defined as a superclass to all static standard models (not used for classification, if so,
 * extends Classifier)
 *
 * Created by andresmasegosa on 4/3/16.
 */
public abstract class Model {

    protected ParameterLearningAlgorithm learningAlgorithm;

    protected dVMP dvmp = new dVMP();

    protected DAG dag;

    protected Variables vars = null;

    protected String errorMessage = "";

    public Model(Attributes attributes) throws WrongConfigurationException {
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

    public void learnModel(DataOnMemory<DataInstance> datBatch){
        dvmp=null;
        learningAlgorithm = new SVB();
        learningAlgorithm.setDAG(this.getDAG());
        learningAlgorithm.initLearning();
        learningAlgorithm.updateModel(datBatch);
    }

    public void learnModel(DataStream<DataInstance> dataStream){
        dvmp=null;
        learningAlgorithm = new SVB();
        learningAlgorithm.setDAG(this.getDAG());
        learningAlgorithm.setDataStream(dataStream);
        learningAlgorithm.initLearning();
        learningAlgorithm.runLearning();
    }

    public void learnModel(DataFlink<DataInstance> dataFlink){

    }


    public void updateModel(DataFlink<DataInstance> dataFlink){

    }

    public void updateModel(DataOnMemory<DataInstance> datBatch){
        if (learningAlgorithm ==null || dag == null ) {
            learningAlgorithm = new SVB();
            learningAlgorithm.setDAG(this.getDAG());
            learningAlgorithm.initLearning();
            dvmp=null;
        }

        learningAlgorithm.updateModel(datBatch);
    }


    public BayesianNetwork getModel(){
        if (learningAlgorithm !=null){
            return this.learningAlgorithm.getLearntBayesianNetwork();
        }

        if (this.dvmp!=null)
            return this.dvmp.getLearntBayesianNetwork();

        return null;
    }


    protected String getErrorMessage() {
        return errorMessage;
    }

    protected void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    protected abstract void buildDAG();

    public abstract boolean isValidConfiguration();


    @Override
    public String toString() {
        return this.getModel().toString();
    }


}
