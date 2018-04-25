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
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.latentvariablemodels.staticmodels.Model;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

/**
 * The Classifier abstract class is defined for Bayesian classification models.
 */
public abstract class Classifier<T extends Classifier> extends Model<T> {

    /** Represents the inference algorithm. */
    protected InferenceAlgorithm inferenceAlgoPredict;

    /** class variable */
    protected Variable classVar = null;

    /**
     * Constructor of a classifier which is initialized with the default arguments:
     * the last variable in attributes is the class variable and importance sampling
     * is the inference algorithm for making the predictions.
     * @param attributes list of attributes of the classifier (i.e. its variables)
     * @throws WrongConfigurationException is thrown when the attributes passed are not suitable
     * for such classifier
     */
    protected Classifier(Attributes attributes) throws WrongConfigurationException {
        super(attributes);

        classVar = vars.getListOfVariables().get(vars.getNumberOfVars()-1);
        inferenceAlgoPredict = new ImportanceSampling();

    }


    /**
     * Predicts the class membership probabilities for a given instance.
     * @param instance the data instance to be classified. The value associated to the class variable must be
     *                 a missing value (i.e. a NaN)
     * @return the posterior probability of the class variable
     */
    public Multinomial predict(DataInstance instance) {
        if (!Utils.isMissingValue(instance.getValue(classVar)))
            System.out.println("Class Variable can not be set.");

        inferenceAlgoPredict.setModel(this.getModel());
        this.inferenceAlgoPredict.setEvidence(instance);

        //System.out.println(instance);

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


    /**
     * Method to set the class variable. Note that it should be multinomial
     * @param classVar object of the type {@link Variable} indicating which is the class variable
     * @throws WrongConfigurationException is thrown when the variable is not a multinomial.
     */
    public T setClassVar(Variable classVar) throws WrongConfigurationException {

        if(!classVar.isMultinomial()) {
            setErrorMessage("class variable is not a multinomial");
            throw new WrongConfigurationException(errorMessage);
        }

        this.classVar = classVar;
        dag = null;
        return ((T) this);

    }

    /**
     * Method to set the class variable. Note that it should be multinomial
     * @param className String with the name of the class variable
     * @throws WrongConfigurationException is thrown when the variable is not a multinomial.
     */
    public T setClassName(String className) throws WrongConfigurationException {
        setClassVar(vars.getVariableByName(className));
        return ((T) this);
    }


    /**
     * Method to obtain the inference algorithm used for making the predictions. By default,
     * importance sampling is used.
     * @return an object of the class InferenceAlgorithm
     */
    public InferenceAlgorithm getInferenceAlgoPredict() {
        return inferenceAlgoPredict;
    }

    /**
     * Method to set the inference algorithm used for making the predictions. By default,
     * importance sampling is used.
     * @param inferenceAlgoPredict, object of the class InferenceAlgorithm
     */
    public T setInferenceAlgoPredict(InferenceAlgorithm inferenceAlgoPredict) {
        this.inferenceAlgoPredict = inferenceAlgoPredict;
        return ((T) this);
    }
}
