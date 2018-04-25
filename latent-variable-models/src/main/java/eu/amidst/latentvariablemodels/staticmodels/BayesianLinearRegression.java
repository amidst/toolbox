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
import eu.amidst.core.inference.InferenceEngine;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class implements a Bayesian Linear Regression Model. See e.g.:
 *
 * Kevin P. Murphy. 2012. Machine Learning: A Probabilistic Perspective. The MIT Press. Page 231
 *
 * Created by rcabanas on 08/03/16.
 */
public class BayesianLinearRegression extends Model<BayesianLinearRegression> {


    /* diagonal flag */
    private boolean diagonal;

    /** class variable */
    private Variable classVar;

    /**
     * Constructor from a list of attributes.
     * The default parameters are used: the class variable is the last one and the
     * diagonal flag is set to false (predictive variables are NOT independent).
     * @param attributes list of attributes of the classifier (i.e. its variables)
     * @throws WrongConfigurationException
     */
    public BayesianLinearRegression(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        classVar = vars.getListOfVariables().get(vars.getNumberOfVars()-1);
        this.diagonal = false;

    }


    /**
     * Builds the DAG of the model.
     */
    @Override
    protected void buildDAG() {
        dag = new DAG(vars);


        //arcs from the features to the class
        vars.getListOfVariables().stream()
                .filter(v -> !v.equals(classVar))
                .forEach(v -> dag.getParentSet(classVar).addParent(v));

        // if it is not diagonal add the links between the attributes (features)
        if(!isDiagonal()) {

            List<Variable> attrVars = vars.getListOfVariables().stream().filter(v -> !v.equals(classVar)).collect(Collectors.toList());

            for (int i=0; i<attrVars.size()-1; i++){
                for(int j=i+1; j<attrVars.size(); j++) {
                    // Add the links
                    dag.getParentSet(attrVars.get(i)).addParent(attrVars.get(j));



                }

            }


        }



    }

    /**
     * tests if the attributes passed as an argument in the constructor are suitable
     * @return boolean value with the result of the test.
     */

    @Override
    public boolean isValidConfiguration() {


        boolean isValid = vars.getListOfVariables().stream()
                .allMatch(v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL));

        if(!isValid) {
            setErrorMessage("All the variables must be REAL");
        }


        return isValid;
    }


    //////Getters and setters ////////

    /**
     * Method to obtain the value of the diagonal flag.
     * @return boolean value
     */
    public boolean isDiagonal() {
        return diagonal;
    }



    /**
     * Method to obtain the class variable
     * @return object of the type {@link Variable} indicating which is the class variable
     */
    public Variable getClassVar() {
        return classVar;
    }


    /**
     * Method for setting the diagonal flag
     * @param diagonal boolean value
     */
    public BayesianLinearRegression setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
        dag = null;
        return this;
    }

    /**
     * Method to set the class variable.
     * @param classVar object of the type {@link Variable} indicating which is the class variable
     */
    public BayesianLinearRegression setClassVar(Variable classVar){

        this.classVar = classVar;
        dag = null;
        return this;

    }

    /**
     * Method to set the class variable.
     * @param className String with the name of the class variable
     */
    public BayesianLinearRegression setClassName(String className){
        setClassVar(vars.getVariableByName(className));
        return this;
    }



    //////// Example of use ///////

    public static void main(String[] args) throws WrongConfigurationException {

        DataStream<DataInstance> data = DataSetGenerator.generate(0,1000, 0, 10);
        System.out.println(data.getAttributes().toString());

        String className = "GaussianVar0";


        BayesianLinearRegression BLR =
                new BayesianLinearRegression(data.getAttributes())
                        .setClassName(className)
                        .setWindowSize(100)
                        .setDiagonal(false);

        BLR.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            BLR.updateModel(batch);
        }
        System.out.println(BLR.getModel());
        System.out.println(BLR.getDAG());



        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,5);




        for(DataInstance d : dataTest) {

            Assignment assignment = new HashMapAssignment(BLR.getModel().getNumberOfVars()-1);
            for (int i=0; i<BLR.getModel().getNumberOfVars(); i++) {
                Variable v = BLR.getModel().getVariables().getVariableById(i);
                if(!v.equals(BLR.getClassVar()))
                    assignment.setValue(v,d.getValue(v));
            }

            UnivariateDistribution posterior = InferenceEngine.getPosterior(BLR.getClassVar(), BLR.getModel(),assignment);

            System.out.println(posterior.toString());


        }

    }







}
