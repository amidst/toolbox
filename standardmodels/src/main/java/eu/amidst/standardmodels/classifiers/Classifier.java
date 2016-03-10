/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.standardmodels.classifiers;


import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.standardmodels.Model;
import eu.amidst.standardmodels.eu.amidst.standardmodels.exceptions.WrongConfigurationException;

/**
 * The Classifier abstract class is defined for Bayesian classification models.
 */
public abstract class Classifier extends Model {

    /** Represents the inference algorithm. */
    private InferenceAlgorithm inferenceAlgoPredict;

    /** class variable */
    protected Variable classVar;


    public Classifier(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        classVar = vars.getListOfVariables().get(vars.getNumberOfVars()-1);
        inferenceAlgoPredict = new ImportanceSampling();

    }


    /**
     * Predicts the class membership probabilities for a given instance.
     * @param instance the data instance to be classified.
     * @return an array of doubles containing the estimated membership probabilities of the data instance for each class label.
     */
    public Multinomial predict(DataInstance instance) {
        if (!Utils.isMissingValue(instance.getValue(classVar)))
            System.out.println("Class Variable can not be set.");

        inferenceAlgoPredict.setModel(this.getModel());
        this.inferenceAlgoPredict.setEvidence(instance);

        System.out.println(instance);

        this.inferenceAlgoPredict.runInference();


        return this.inferenceAlgoPredict.getPosterior(classVar);



    }



    /////// getters and setters ///////


    /**
     * Method to obtain the class variable
     * @return object of the type {@link Variable} indicating which is the class variable
     */
    public Variable getClassVar() {
        return classVar;
    }

    public void setClassVar(Variable classVar) {
        this.classVar = classVar;
        dag = null;
    }

    public void setClassName(String className) {
        setClassVar(vars.getVariableByName(className));
    }


    public InferenceAlgorithm getInferenceAlgoPredict() {
        return inferenceAlgoPredict;
    }

    public void setInferenceAlgoPredict(InferenceAlgorithm inferenceAlgoPredict) {
        this.inferenceAlgoPredict = inferenceAlgoPredict;
    }
}
