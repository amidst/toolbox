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

package eu.amidst.latentvariablemodels.dynamicmodels.classifiers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DataSetGenerator;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class implements the dynamic latent classification models. For more details:
 *
 * See Shengtong Zhong, Helge Langseth, and Thomas Dyhre Nielsen. A classification-based approach to monitoring the
 * safety of dynamic systems. Reliability Engineering and System Safety, 121:61–71, 2014.
 *
 * Shengtong Zhong, Ana M. Martínez, Thomas Dyhre Nielsen, and Helge Langseth. Towards a more expressive model for
 * dynamic classification. In Proceedings of the Twentythird International Florida Artificial Intelligence Research
 * Symposium Conference, 2010.
 *
 * Created by ana@cs.aau.dk on 05/03/16.
 */
public class DynamicLatentClassificationModel extends DynamicClassifier<DynamicLatentClassificationModel>{

    /** number of continuous hidden variables */
    private int numContinuousHidden;

    /** states of the multinomial hidden variable */
    private int numStatesHidden;

    /** multinomial hidden variable */
    private Variable hiddenMultinomial;

    /** set of continuous hidden variables*/
    private List<Variable> contHiddenList;

    public int getNumContinuousHidden() {
        return numContinuousHidden;
    }

    public DynamicLatentClassificationModel setNumContinuousHidden(int numContinuousHidden) {
        this.numContinuousHidden = numContinuousHidden;
        resetModel();
        return this;
    }

    public int getNumStatesHidden() {
        return numStatesHidden;
    }

    public DynamicLatentClassificationModel setNumStatesHidden(int numStatesHidden) {
        this.numStatesHidden = numStatesHidden;
        resetModel();
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
     * @return object of the class List<Variable> containing the list
     */
    public List<Variable> getContHiddenList() {
        return contHiddenList;
    }

    public DynamicLatentClassificationModel(Attributes attributes) {
        super(attributes);
        this.numContinuousHidden = 2;
        this.numStatesHidden = 2;
    }


    @Override
    protected void buildDAG() {

        //Obtain the predictive attributes
        List<Variable> attrVars = variables.getListOfDynamicVariables().stream()
                .filter(v -> !v.equals(classVar)).collect(Collectors.toList());


        //Create the hidden variabels
        hiddenMultinomial = variables.newMultinomialDynamicVariable("M", numStatesHidden);

        contHiddenList = new ArrayList<Variable>();
        for(int i=0; i<numContinuousHidden; i++) {
            contHiddenList.add(variables.newGaussianDynamicVariable("Z"+Integer.toString(i)));
        }


        dynamicDAG = new DynamicDAG(variables);

        //arcs from the class to the hidden variables
        dynamicDAG.getParentSetTimeT(hiddenMultinomial).addParent(classVar);
        contHiddenList.stream().forEach(z -> dynamicDAG.getParentSetTimeT(z).addParent(classVar));


        //arcs from the hidden vars to each attribute
        attrVars.stream().forEach(x->dynamicDAG.getParentSetTimeT(x).addParent(hiddenMultinomial));

        for (Variable z : contHiddenList) {
            attrVars.stream().forEach(x->dynamicDAG.getParentSetTimeT(x).addParent(z));
        }

        //Add dynamic links on class and continuous layer
        dynamicDAG.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());

        for (Variable variable : contHiddenList) {
            dynamicDAG.getParentSetTimeT(variable).addParent(variable.getInterfaceVariable());
        }

    }

    @Override
    public boolean isValidConfiguration() {


        boolean isValid = true;

        long numReal = variables.getListOfDynamicVariables()
                .stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.REAL))
                .count();

        long numFinite = variables.getListOfDynamicVariables()
                .stream()
                .filter( v -> v.getStateSpaceTypeEnum().equals(StateSpaceTypeEnum.FINITE_SET))
                .count();


        if(numFinite != 1 || numReal != variables.getNumberOfVars()-1) {
            isValid = false;
            setErrorMessage("Invalid configuration: wrong number types of variables domains. " +
                    "It should contain 1 discrete variable and the rest shoud be real");

        }

        return isValid;



    }

    public static void main(String[] args) throws WrongConfigurationException {

        DataStream<DynamicDataInstance> data = DataSetGenerator.generate(1,1000, 1, 10);

        //Parameters of the classifier
        String classVarName = "DiscreteVar0";
        int numContinuousHidden = 4;
        int numStatesHidden = 3;

        //Initializes the classifier
        DynamicLatentClassificationModel dLCM = new DynamicLatentClassificationModel(data.getAttributes());
        dLCM.setClassName(classVarName);
        dLCM.setNumContinuousHidden(numContinuousHidden);
        dLCM.setNumStatesHidden(numStatesHidden);


        //Learning

        dLCM.updateModel(data);
        for (DataOnMemory<DynamicDataInstance> batch : data.iterableOverBatches(100)) {
            dLCM.updateModel(batch);
        }

        //Shows the resulting model
        System.out.println(dLCM.getDynamicDAG());
        System.out.println(dLCM.getModel());


        //Change the inference algorithm to use as factored frontier
        dLCM.setInferenceAlgoPredict(new VMP());


        // predict the class of one instances
        System.out.println("Predicts some instances, i.e. computes the posterior probability of the class");
        List<DynamicDataInstance> dataTest = data.stream().collect(Collectors.toList()).subList(0,100);


        int i = 1;
        for(DynamicDataInstance d : dataTest) {

            d.setValue(dLCM.getClassVar(), Utils.missingValue());
            Multinomial posteriorProb = dLCM.predict(d);
            System.out.println(posteriorProb.toString());

        }



    }

}
