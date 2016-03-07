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
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa and rcabanas on 4/3/16.
 *
 * This class implements the model of Gaussian Discriminant Analysis
 *
 */
public class GaussianDiscriminatAnalysis extends Model {

    /* diagonal flag:
    * If in the  model one assumes that the covariance matrices are diagonal,
    * then this means that we assume the features are conditionally independent,
    * and the resulting classifier is equivalent to the Gaussian Naive Bayes classifier
    */
    private boolean diagonal;

    /** class variable */
    private Variable classVar = null;

    /**
     * Constructor of classifier from a list of attributes (e.g. from a datastream).
     * The default parameters are used: the class variable is the last one and the
     * diagonal flag is set to false.
     * @param attributes
     */
    public GaussianDiscriminatAnalysis(Attributes attributes) {
        super(attributes);
        Variables vars = new Variables(attributes);
        // default parameters
        classVar = vars.getListOfVariables().get(vars.getNumberOfVars()-1);
        diagonal = false;

    }





    @Override
    protected void buildDAG(Attributes attributes) {

        //We create a standard naive Bayes
        Variables vars = new Variables(attributes);


        dag = new DAG(vars);

        dag.getParentSets().stream().filter(w -> w.getMainVar() != classVar).forEach(w -> w.addParent(classVar));

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
        Variables vars = new Variables(attributes);
        boolean isValid = true;

        if(!vars.getListOfVariables().stream()
                .filter(v -> !v.equals(classVar))
                .map( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL))
                .reduce((n1,n2) -> n1 && n2).get().booleanValue()){

            isValid = false;

            System.err.println("Invalid configuration: all the features of the classifier should be real variables");

        }


        if(!classVar.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.FINITE_SET)) {

            isValid = false;
            System.err.println("Invalid configuration: the class should be a discrete variable");

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


    /**
     * Sets the value of the diagonal flag.
     * @param diagonal
     */
    public void setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
    }

    /**
     * Method to obtain the class variable
     * @return object of the type {@link Variable} indicating which is the class variable
     */
    public Variable getClassVar() {
        return classVar;
    }

    /**
     * Sets the class variable
     * @param classVar object of the class {@link Variable} indicating which is the class variable
     */
    public void setClassVar(Variable classVar) {
        this.classVar = classVar;
    }


    /**
     * Sets the class variable
     * @param indexVar integer indicating the position of the class variable in the attributes list. The first
     *                 variable has the index 0
     */
    public void setClassVar(int indexVar) {
        Variables vars = new Variables(attributes);
        classVar = vars.getListOfVariables().get(indexVar);
    }


    //////////// example of use

    public static void main(String[] args) {

        String file = "datasets/tmp2.arff";
        //file = "datasets/WasteIncineratorSample.arff";
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(file);

        GaussianDiscriminatAnalysis gda = new GaussianDiscriminatAnalysis(data.getAttributes());
        gda.setDiagonal(true);
        gda.setClassVar(1);



      //  System.out.println(gda.getDAG());

        if(gda.isValidConfiguration()) {
            gda.learnModel(data);
            for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
                System.out.println("update model");
                gda.updateModel(batch);
            }
            System.out.println(gda.getModel());
        }

    }



}
