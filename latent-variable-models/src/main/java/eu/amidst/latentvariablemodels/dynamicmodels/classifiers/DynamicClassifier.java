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
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.inference.FactoredFrontierForDBN;
import eu.amidst.dynamic.inference.InferenceAlgorithmForDBN;
import eu.amidst.dynamic.inference.InferenceEngineForDBN;
import eu.amidst.latentvariablemodels.dynamicmodels.DynamicModel;
import eu.amidst.latentvariablemodels.staticmodels.exceptions.WrongConfigurationException;

/**
 * The DynamicClassifier abstract class is defined for dynamic Bayesian classification models.
 *
 * Created by ana@cs.aau.dk on 11/03/16.
 */
public abstract class DynamicClassifier<T extends DynamicClassifier> extends DynamicModel<DynamicClassifier>{

    /** Represents the static inference algorithm. */
    private InferenceAlgorithm inferenceAlgoPredict = new ImportanceSampling();

    /** Factored Frontier algorithm for DBN*/
    private InferenceAlgorithmForDBN dynamicInferenceAlgoPredict;

    /** class variable */
    protected Variable classVar;

    public InferenceAlgorithm getInferenceAlgoPredict() {
        return inferenceAlgoPredict;
    }

    public T setInferenceAlgoPredict(InferenceAlgorithm inferenceAlgoPredict) {
        this.inferenceAlgoPredict = inferenceAlgoPredict;
        return (T) this;
    }

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
     */
    public T setClassVar(Variable classVar){

        if(!classVar.isMultinomial()) {
            throw new UnsupportedOperationException("class variable is not a multinomial");
        }

        this.classVar = classVar;
        resetModel();
        return (T)this;

    }

    /**
     * Method to set the class variable. Note that it should be multinomial
     * @param className String with the name of the class variable
     * @throws WrongConfigurationException is thrown when the variable is not a multinomial.
     */
    public T setClassName(String className) throws WrongConfigurationException {
        setClassVar(variables.getVariableByName(className));
        return (T)this;

    }

    public DynamicClassifier(Attributes attributes) {
        super(attributes);
        classVar = variables.getListOfDynamicVariables().get(variables.getNumberOfVars()-1);
        dynamicInferenceAlgoPredict = new FactoredFrontierForDBN(getInferenceAlgoPredict());
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(dynamicInferenceAlgoPredict);
    }


    /**
     * Predicts the class membership probabilities for a given instance.
     * @param instance the data instance to be classified. The value associated to the class variable must be
     *                 a missing value (i.e. a NaN)
     * @return the posterior probability of the class variable
     */
    public Multinomial predict(DynamicDataInstance instance) {
        if (!Utils.isMissingValue(instance.getValue(classVar)))
            System.out.println("Class Variable can not be set.");

        InferenceEngineForDBN.setModel(this.getModel());
        if (instance.getTimeID()==0) {
            InferenceEngineForDBN.reset();
        }
        InferenceEngineForDBN.addDynamicEvidence(instance);

        System.out.println(instance);

        InferenceEngineForDBN.runInference();

        return InferenceEngineForDBN.getFilteredPosterior(classVar);



    }
}
