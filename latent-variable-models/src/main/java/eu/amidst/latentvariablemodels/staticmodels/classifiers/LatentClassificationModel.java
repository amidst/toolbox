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

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class implements the latent classification models. For more details, see
 * Langseth, H., and Nielsen, T. D. (2005). Latent classification models. Machine Learning, 59(3), 237-265.
 * Created by rcabanas on 10/03/16.
 *
 */
public class LatentClassificationModel extends Classifier<LatentClassificationModel> {


    /** number of continuous hidden variables */
    private int numContinuousHidden;

    /** states of the multinomial hidden variable */
    private int numStatesHidden;

    /** multinomial hidden variable */
    private Variable hiddenMultinomial;

    /** set of continuous hidden variables*/
    private List<Variable> contHiddenList;


    /**
     * Constructor of classifier from a list of attributes.
     * The default parameters are used: the class variable is the last one and the
     * diagonal flag is set to false (predictive variables are NOT independent).
     * @param attributes list of attributes of the classifier (i.e. its variables)
     * @throws WrongConfigurationException
     */
    public LatentClassificationModel(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        //default values
        this.numContinuousHidden = 2;
        this.numStatesHidden = 2;


    }


    /**
     * Builds the DAG over the set of variables given with the naive Bayes structure
     */
    @Override
    protected void buildDAG() {



        //Obtain the predictive attributes
        List<Variable> attrVars = vars.getListOfVariables().stream()
                .filter(v -> !v.equals(classVar)).collect(Collectors.toList());


        //Create the hidden variabels
        hiddenMultinomial = vars.newMultinomialVariable("M", numStatesHidden);

        contHiddenList = new ArrayList<Variable>();
        for(int i=0; i<numContinuousHidden; i++) {
            contHiddenList.add(vars.newGaussianVariable("Z"+Integer.toString(i)));
        }


        dag = new DAG(vars);

        //arcs from the class to the hidden variables
        dag.getParentSet(hiddenMultinomial).addParent(classVar);
        contHiddenList.stream().forEach(z -> dag.getParentSet(z).addParent(classVar));


        //arcs from the hidden vars to each attribute
        attrVars.stream().forEach(x->dag.getParentSet(x).addParent(hiddenMultinomial));

        for (Variable z : contHiddenList) {
            attrVars.stream().forEach(x->dag.getParentSet(x).addParent(z));
        }


    }
    /*
    * tests if the attributes passed as an argument in the constructor are suitable for this classifier
    * @return boolean value with the result of the test.
            */
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



    //////// Getters and setters /////////

    /**
     * Method to obtain the number of continuous hidden variables
     * @return integer value
     */
    public int getNumContinuousHidden() {
        return numContinuousHidden;
    }

    /**
     * method for getting number of states of the hidden multinomial variable
     * @return integer value
     */
    public int getNumStatesHidden() {
        return numStatesHidden;
    }

    /**
     * sets the number of continuous hidden variables
     * @param numContinuousHidden integer value
     */
    public LatentClassificationModel setNumContinuousHidden(int numContinuousHidden) {
        this.numContinuousHidden = numContinuousHidden;
        dag = null;
        return this;
    }

    /**
     * sets the number of states of the hidden multinomial variable
     * @param numStatesHidden integer value
     */
    public LatentClassificationModel setNumStatesHidden(int numStatesHidden) {
        this.numStatesHidden = numStatesHidden;
        dag = null;
        return this;
    }

    /**
     * method for getting the hidden multinomial variable
     * @return object of type Variable
     */
    public Variable getHiddenMultinomial() {
        return hiddenMultinomial;
    }

    /**
     * method for getting the list of continuous hidden variables
     * @return list of variables
     */
    public List<Variable> getContHiddenList() {
        return contHiddenList;
    }

    ///////// main class (example of use) //////

    public static void main(String[] args) throws WrongConfigurationException {

        DataStream<DataInstance> data = DataSetGenerator.generate(1234,1000, 1, 10);
        System.out.println(data.getAttributes().toString());

        //Parameters of the classifier
        String classVarName = "DiscreteVar0";
        int numContinuousHidden = 4;
        int numStatesHidden = 3;

        //Initializes the classifier
        LatentClassificationModel lcm =
                new LatentClassificationModel(data.getAttributes())
                        .setClassName(classVarName)
                        .setNumContinuousHidden(numContinuousHidden)
                        .setNumStatesHidden(numStatesHidden);


        //Learning

        lcm.updateModel(data);
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(100)) {
            lcm.updateModel(batch);
        }

        //Shows the resulting model
        System.out.println(lcm.getModel());
        System.out.println(lcm.getDAG());


        // Uncomment the following 2 lines to get the bug
        InferenceAlgorithm algo = new VMP();
        lcm.setInferenceAlgoPredict(algo);


        // predict the class of one instances
        System.out.println("Predicts some instances, i.e. computes the posterior probability of the class");
        List<DataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,100);


        int i = 1;
        for(DataInstance d : dataTest) {

                d.setValue(lcm.getClassVar(), Utils.missingValue());
                Multinomial posteriorProb = lcm.predict(d);
                System.out.println(posteriorProb.toString());

        }



    }



}
