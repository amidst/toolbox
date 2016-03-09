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

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.standardmodels.eu.amidst.standardmodels.exceptions.WrongConfigurationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa and rcabanas on 4/3/16.
 *
 * This class implements the model of Gaussian Discriminant Analysis
 *
 */
public class GaussianDiscriminantAnalysis extends Classifier {

    /* diagonal flag:
    * If in the  model one assumes that the covariance matrices are diagonal,
    * then this means that we assume the features are conditionally independent,
    * and the resulting classifier is equivalent to the Gaussian Naive Bayes classifier
    */
    private boolean diagonal;



    /**
     * Constructor of classifier from a list of attributes (e.g. from a datastream).
     * The default parameters are used: the class variable is the last one and the
     * diagonal flag is set to false.
     * @param attributes
     * @throws WrongConfigurationException
     */
    public GaussianDiscriminantAnalysis(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
        // default parameters
        diagonal = false;

    }



    @Override
    protected void buildDAG() {

        //We create a standard naive Bayes
        dag = new DAG(vars);

        dag.getParentSets().stream().filter(w -> !w.getMainVar().equals(classVar)).forEach(w -> w.addParent(classVar));

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

    @Override
    public boolean isValidConfiguration(){
        boolean isValid = true;


        long numReal = vars.getListOfVariables().stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL))
                .count();

        long numFinite = vars.getListOfVariables().stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.FINITE_SET))
                .count();


        if(numFinite != 1 || numReal != vars.getNumberOfVars()-1) {
            isValid = false;
            String errorMsg = "Invalid configuration: wrong number types of variables domains. It should contain 1 discrete variable and the rest shoud be real";
            this.setErrorMessage(errorMsg);

        }

        return  isValid;

    }


    /////// Getters and setters

    /**
     * Method to obtain the value of the diagonal flag.
     * @return boolean value
     */
    public boolean isDiagonal() {
        return diagonal;
    }


    public void setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
        dag = null;
    }



    //////////// example of use

    public static void main(String[] args) throws WrongConfigurationException {

        String file = "datasets/tmp2.arff";
        //file = "datasets/syntheticDataDaimler.arff";
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(file);

        GaussianDiscriminantAnalysis gda = new GaussianDiscriminantAnalysis(data.getAttributes());
        gda.setDiagonal(false);
        gda.setClassName("default");

        if(gda.isValidConfiguration()) {
            gda.learnModel(data);
            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
                gda.updateModel(batch);
            }
            System.out.println(gda.getModel());
            System.out.println(gda.getDAG());
        }



        // predict the class of one instances

        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,10);

        for(DataInstance d : dataTest) {
            d.setValue(gda.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = gda.predict(d);
            System.out.println(posteriorProb.toString());

        }
    }
}
