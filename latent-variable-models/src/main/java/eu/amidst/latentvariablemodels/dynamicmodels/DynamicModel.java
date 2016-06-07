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

package eu.amidst.latentvariablemodels.dynamicmodels;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.dynamic.learning.parametric.bayesian.SVB;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

/**
 *
 * The DynamicModel abstract class is defined as a superclass to all dynamic standard models (not used for
 * classification, if so, extends DynamicClassifier)
 *
 * Created by andresmasegosa, ana@cs.aau.dk on 04/03/16.
 */
public abstract class DynamicModel {

    ParameterLearningAlgorithm learningAlgorithm = null;

    protected DynamicDAG dynamicDAG;

    protected DynamicVariables variables;

    protected int windowSize = 100;

    protected String errorMessage = "";

    protected boolean initialized = false;

    public DynamicModel(Attributes attributes) {
        this.variables = new DynamicVariables(attributes);
        if (!this.isValidConfiguration())
            throw new WrongConfigurationException(getErrorMessage());
    }

    public DynamicDAG getDynamicDAG() {
        if (dynamicDAG==null){
            buildDAG();
        }
        return dynamicDAG;
    }

    public void setLearningAlgorithm(ParameterLearningAlgorithm learningAlgorithm) {
        this.learningAlgorithm = learningAlgorithm;
    }

    public void setWindowSize(int windowSize){
        this.windowSize = windowSize;
    }


    private void initLearning() {
        learningAlgorithm = new SVB();
        learningAlgorithm.setDynamicDAG(this.getDynamicDAG());
        learningAlgorithm.setWindowsSize(windowSize);
        learningAlgorithm.initLearning();
        initialized = true;
    }


    public double updateModel(DataStream<DynamicDataInstance> dataStream){
        if (!initialized)
            initLearning();

        return learningAlgorithm.updateModel(dataStream);
    }

    public double updateModel(DataOnMemory<DynamicDataInstance> dataBatch){
        if (!initialized)
            initLearning();

        return learningAlgorithm.updateModel(dataBatch);
    }

    public DynamicBayesianNetwork getModel(){
        if (learningAlgorithm !=null){
            return this.learningAlgorithm.getLearntDBN();
        }
        else if (learningAlgorithm !=null){
            return this.learningAlgorithm.getLearntDBN();
        }
        return null;
    }


    public void resetModel(){
        initialized=false;
    }

    protected abstract void buildDAG();

    public boolean isValidConfiguration(){
        return true;
    }

    protected String getErrorMessage() {
        return errorMessage;
    }

    protected void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return this.getModel().toString();
    }
}
