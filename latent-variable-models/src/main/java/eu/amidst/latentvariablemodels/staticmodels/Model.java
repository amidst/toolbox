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
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.BayesianParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
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
public abstract class Model<T extends Model> {

    protected final Attributes atts;

    protected ParameterLearningAlgorithm learningAlgorithm = null;

    protected eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm learningAlgorithmFlink = null;

    protected DAG dag;

    protected Variables vars = null;

    protected String errorMessage = "";

    protected int windowSize = 100;

    protected boolean initialized = false;

    protected PlateuStructure plateuStructure;

    public Model(Attributes attributes) {
        this.atts = attributes;
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

    public ParameterLearningAlgorithm getLearningAlgorithm() {
        return learningAlgorithm;
    }

    public eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm getLearningAlgorithmFlink() {
        return learningAlgorithmFlink;
    }

    public PlateuStructure getPlateuStructure() {
        if (plateuStructure==null){
            buildPlateuStructure();
        }
        return plateuStructure;
    }

    public void setLearningAlgorithm(ParameterLearningAlgorithm learningAlgorithm) {
        this.learningAlgorithm = learningAlgorithm;
    }

    public void setFlinkLearningAlgorithm(eu.amidst.flinklink.core.learning.parametric.ParameterLearningAlgorithm learningAlgorithm) {
        this.learningAlgorithmFlink = learningAlgorithm;
    }

    public  T setWindowSize(int windowSize){
        this.windowSize = windowSize;
        return (T) this;
    }

    public int getWindowSize() {
        return windowSize;
    }

    protected void initLearningFlink() {
        if(learningAlgorithmFlink==null) {
            dVMP dvmp = new dVMP();
            dvmp.setBatchSize(100);
            dvmp.setMaximumGlobalIterations(10);
            dvmp.setMaximumLocalIterations(100);
            dvmp.setLocalThreshold(0.00001);
            dvmp.setGlobalThreshold(0.01);
            learningAlgorithmFlink = dvmp;
        }

        learningAlgorithmFlink.setBatchSize(windowSize);

        if (this.getDAG()!=null)
            learningAlgorithmFlink.setDAG(this.getDAG());
        else if (this.getPlateuStructure()!=null)
            ((BayesianParameterLearningAlgorithm)learningAlgorithmFlink).setPlateuStructure(this.getPlateuStructure());
        else
            throw new IllegalArgumentException("Non provided dag or PlateauStructure");


        learningAlgorithmFlink.initLearning();
        initialized=true;
    }


    protected  void initLearning() {
        if(learningAlgorithm==null) {
            SVB svb = new SVB();
            svb.setWindowsSize(100);
            svb.getPlateuStructure().getVMP().setTestELBO(false);
            svb.getPlateuStructure().getVMP().setMaxIter(100);
            svb.getPlateuStructure().getVMP().setThreshold(0.00001);

            learningAlgorithm = svb;
        }
        learningAlgorithm.setWindowsSize(windowSize);
        if (this.getDAG()!=null)
            learningAlgorithm.setDAG(this.getDAG());
        else if (this.getPlateuStructure()!=null)
            ((BayesianParameterLearningAlgorithm)learningAlgorithm).setPlateuStructure(this.getPlateuStructure());
        else
            throw new IllegalArgumentException("Non provided dag or PlateauStructure");

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
        learningAlgorithm=null;
        learningAlgorithmFlink = null;
        this.dag=null;
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

    protected void buildDAG(){
        this.dag=null;
    }

    public void buildPlateuStructure(){
        this.plateuStructure=null;
    }


    public boolean isValidConfiguration(){
        return true;
    }

    @Override
    public String toString() {
        return this.getModel().toString();
    }


    public <E extends UnivariateDistribution> E getPosteriorDistribution(String varName) {
		if (learningAlgorithm !=null){
			 return (E)this.learningAlgorithm.getLearntBayesianNetwork()
					 .getConditionalDistribution(dag.getVariables().getVariableByName(varName));

		} else if (learningAlgorithmFlink != null ){
            return (E)this.learningAlgorithmFlink.getLearntBayesianNetwork()
                    .getConditionalDistribution(dag.getVariables().getVariableByName(varName));
        }

		return null;

    }

}
