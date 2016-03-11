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

package eu.amidst.standardmodels.classifiers;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.huginlink.learning.ParallelTAN;
import eu.amidst.standardmodels.Model;
import eu.amidst.standardmodels.exceptions.WrongConfigurationException;

/**
 * Created by andresmasegosa on 4/3/16.
 */

// TODO: change to extend Classifier class
public class TAN extends Model {

    private ParallelTAN parallelTAN;
    private String classVarName;
    private String rootVarName;


    public TAN(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
    }

    @Override
    protected void buildDAG() {

    }

    @Override
    public void learnModel(DataStream<DataInstance> dataStream){

        if(classVarName==null || rootVarName==null) {
            Variable classVar = this.vars.getListOfVariables().stream()
                    .filter(Variable::isMultinomial).findAny().get();
            classVarName = classVar.getName();

            rootVarName = this.vars.getListOfVariables().stream()
                    .filter(Variable::isMultinomial)
                    .filter(variable -> !variable.equals(classVar)).findAny().get().getName();
        }

        parallelTAN = new ParallelTAN();
        this.dag = new DAG(this.vars);

        parallelTAN.setNameTarget(classVarName);
        parallelTAN.setNameRoot(rootVarName);
        parallelTAN.setNumCores(1);
        parallelTAN.setNumSamplesOnMemory(5000);

        try {
            this.dag = parallelTAN.learnDAG(dataStream);
        }
        catch (ExceptionHugin e) {
            System.out.println(e.getMessage());
        }

        dvmp=null;
        learningAlgorithm = new SVB();
        learningAlgorithm.setDAG(this.dag);
        learningAlgorithm.setDataStream(dataStream);
        learningAlgorithm.initLearning();
        learningAlgorithm.runLearning();
    }

    public void setClassVarName(String classVarName) {
        this.classVarName = classVarName;
    }

    public void setRootVarName(String rootVarName) {
        this.rootVarName = rootVarName;
    }

    @Override
    public boolean isValidConfiguration() {

        boolean isValid = true;

        long numFinite = vars.getListOfVariables().stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.FINITE_SET))
                .count();

        if(numFinite <2) {
            isValid = false;
            String errorMsg = "Invalid configuration: There should be at least 2 discrete variables (root and class)";
            this.setErrorMessage(errorMsg);
        }

        return  isValid;
    }

    public static void main(String[] args) throws WrongConfigurationException {

        int seed=6236;
        int nSamples=5000;
        int nDiscreteVars=5;
        int nContinuousVars=10;

        DataStream<DataInstance> data = DataSetGenerator.generate(seed,nSamples,nDiscreteVars,nContinuousVars);

        String classVarName="DiscreteVar0";
        String rootVarName="DiscreteVar1";


        TAN model = new TAN(data.getAttributes());

        model.setClassVarName(classVarName);
        model.setRootVarName(rootVarName);

        model.learnModel(data);

        System.out.println(model.getDAG());
        System.out.println();
        System.out.println(model.getModel());

    }

}
