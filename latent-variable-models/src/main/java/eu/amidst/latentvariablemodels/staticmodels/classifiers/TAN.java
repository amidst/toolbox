/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.latentvariablemodels.staticmodels.classifiers;

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
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

/**
 * This class implements a Tree Augmente Naive Bayes classifier, TAN. For more details:
 *
 * N. Friedman, D. Geiger, and M. Goldszmidt. Bayesian network classifiers. Machine Learning, 29(2-3):131–163, 1997.
 *
 * Created by andresmasegosa on 4/3/16.
 */

public class TAN extends Classifier<TAN> {


    /** This class provides a link to the <a href="https://www.hugin.com">Hugin</a>'s functionality to learn in parallel a TAN model.*/
    private ParallelTAN parallelTAN;

    /** String with the name of the node used as a root */
    private String rootVarName;

    /**
     * Constructor of the TAN classifier from a list of attributes.
     * @param attributes list of attributes of the classifier (i.e. its variables)
     * @throws WrongConfigurationException
     */
    public TAN(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
    }


    /**
     * In this class this method does nothing: the DAG is built in the hugin classes
     */
    @Override
    protected void buildDAG() {

    }


    /**
     * This method learns the model from a data stream
     * @param dataStream Object with the data stream
     */
    @Override
    public double updateModel(DataStream<DataInstance> dataStream){

        if(classVar==null || rootVarName==null) {
            classVar = this.vars.getListOfVariables().stream()
                    .filter(Variable::isMultinomial).findAny().get();

            rootVarName = this.vars.getListOfVariables().stream()
                    .filter(Variable::isMultinomial)
                    .filter(variable -> !variable.equals(classVar)).findAny().get().getName();
        }

        parallelTAN = new ParallelTAN();
        this.dag = new DAG(this.vars);

        parallelTAN.setNameTarget(classVar.getName());
        parallelTAN.setNameRoot(rootVarName);
        parallelTAN.setNumCores(1);
        parallelTAN.setNumSamplesOnMemory(5000);

        try {
            this.dag = parallelTAN.learnDAG(dataStream);
        }
        catch (ExceptionHugin e) {
            System.out.println(e.getMessage());
        }

        learningAlgorithm = new SVB();
        learningAlgorithm.setDAG(this.dag);
        learningAlgorithm.setDataStream(dataStream);
        learningAlgorithm.initLearning();
        learningAlgorithm.runLearning();

        return learningAlgorithm.getLogMarginalProbability();
    }

    /**
     * Sets the root variable in the TAN classifier
     * @param rootVarName String indicating the name of the root variable
     */
    public TAN setRootVarName(String rootVarName) {
        this.rootVarName = rootVarName;
        return this;
    }





    /*
    * tests if the attributes passed as an argument in the constructor are suitable for this classifier
    * @return boolean value with the result of the test.
    */
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


        model.setClassName(classVarName);
        model.setRootVarName(rootVarName);

        model.updateModel(data);

        System.out.println(model.getDAG());
        System.out.println();
        System.out.println(model.getModel());

    }

}
