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
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class implements a (Multivariate) Gaussian Mixture Model
 * See Murphy, K. P. (2012). Machine learning: a probabilistic perspective. MIT press, page 339.
 * Created by rcabanas on 07/03/16.
 */
public class GaussianMixture extends Model<GaussianMixture> {

    /* diagonal flag*/
    private boolean diagonal;

    /*number of states of the hidden variable*/
    private int numStatesHiddenVar;

    /** hidden variable */
    private Variable hiddenVar;


    /**
     * Constructor of classifier from a list of attributes (e.g. from a datastream).
     * The following parameters are set to their default values: numStatesHiddenVar = 2
     * and diagonal = true.
     * @param attributes object of the class Attributes
     */
    public GaussianMixture(Attributes attributes) throws WrongConfigurationException {
        super(attributes);
        this.numStatesHiddenVar = 2;
        this.diagonal = true;
    }




    /**
     * Builds the DAG over the set of variables given with the structure of the model
     */
    @Override
    protected void buildDAG() {

        hiddenVar = vars.newMultinomialVariable("HiddenVar",numStatesHiddenVar);

        //We create a standard naive Bayes
        dag = new DAG(vars);

        dag.getParentSets().stream().filter(w -> !w.getMainVar().equals(hiddenVar)).forEach(w -> w.addParent(hiddenVar));

        // if it is not diagonal add the links between the attributes (features)
        if(!isDiagonal()) {
            List<Variable> attrVars = vars.getListOfVariables().stream().filter(v -> !v.equals(hiddenVar)).collect(Collectors.toList());

            for (int i=0; i<attrVars.size()-1; i++){
                for(int j=i+1; j<attrVars.size(); j++) {
                    // Add the links
                    dag.getParentSet(attrVars.get(i)).addParent(attrVars.get(j));
                }
            }
        }
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
     * Method to set the value of the diagonal flag.
     * @param diagonal boolean value, when true means that the attributes are independent.
     */
    public GaussianMixture setDiagonal(boolean diagonal) {
        this.diagonal = diagonal;
        resetModel();
        return this;
    }

    /**
     * Method to obtain the number of states of the hidden (latent) variable
     * @return integer value
     */

    public int getNumStatesHiddenVar() {
        return numStatesHiddenVar;
    }


    /**
     * Method to obtain the number of states of the hidden (latent) variable
     * @param numStatesHiddenVar integer value
     */
    public GaussianMixture setNumStatesHiddenVar(int numStatesHiddenVar) {
        this.numStatesHiddenVar = numStatesHiddenVar;
        resetModel();
        return this;
    }

    /**
     * Method to obtain the hidden (latent) variable
     * @return object of the class Variable
     */
    public Variable getHiddenVar() {
        return hiddenVar;
    }

    /**
     * tests if the attributes passed as an argument in the constructor are suitable for this classifier
     * @return boolean value with the result of the test.
     */
    @Override
    public boolean isValidConfiguration(){

        boolean isValid = true;
        if(!vars.getListOfVariables().stream()
                .map( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL))
                .reduce((n1,n2) -> n1 && n2).get().booleanValue()){
            isValid = false;
            System.err.println("Invalid configuration: all the variables must be real");
        }
        return  isValid;

    }

    //////////// example of use

    public static void main(String[] args) throws WrongConfigurationException {

        DataStream<DataInstance> data = DataSetGenerator.generate(1234,500, 0, 1);


        GaussianMixture GMM = new GaussianMixture(data.getAttributes());
        GMM.setDiagonal(false);
        GMM.setNumStatesHiddenVar(2);

        GMM.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            GMM.updateModel(batch);
        }
        System.out.println(GMM.getModel());
        System.out.println(GMM.getDAG());

		System.out.println("HiddenVar");
		System.out.println(GMM.getPosteriorDistribution("HiddenVar").toString());


    /*    try {
            DataStreamWriter.writeDataToFile(data, "tmp/gmm2vars.arff");
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }
}

