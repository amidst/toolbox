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

package weka.classifiers.bayes;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.filereaders.DataInstanceFromDataRow;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.wekalink.converterFromWekaToAmidst.Converter;
import eu.amidst.wekalink.converterFromWekaToAmidst.DataRowWeka;
import weka.classifiers.AbstractClassifier;
import weka.core.*;

/**
 * This class extends the {@link AbstractClassifier} and defines the AMIDST Classifier that could be run using the MOA’s graphical user interface.
 * MOA (Massive Online Analysis) is an open source software available at http://moa.cms.waikato.ac.nz
 */
public abstract class AmidstGeneralClassifier extends AbstractClassifier implements OptionHandler, Randomizable{

    /** Represents a {@link DAG} object. */
    protected DAG dag = null;

    /** Represents the class variable in this AmidstClassifier. */
    protected Variable classVar_;

    /** Represents the used {@link ParameterLearningAlgorithm}. */
    protected ParameterLearningAlgorithm parameterLearningAlgorithm_;

    /** Represents a {@code BayesianNetwork} object. */
    protected BayesianNetwork bnModel_;

    /** Represents the used {@link InferenceAlgorithm}. */
    protected InferenceAlgorithm inferenceAlgorithm_;

    /** Represents the set of {@link Attributes}. */
    protected Attributes attributes_;


    /**
     * Returns default capabilities of the classifier.
     *
     * @return the capabilities of this classifier
     */
    @Override
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capabilities.Capability.MISSING_VALUES);

        // class
        result.enable(Capabilities.Capability.NOMINAL_CLASS);
        result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

        // instances
        result.setMinimumNumberInstances(0);

        return result;
    }


    /**
     * Return the DAG defining the classification model
     * @return A valid {@link DAG} obejct.
     */
    public abstract DAG buildDAG();

    @Override
    public void buildClassifier(Instances data) throws Exception {

        attributes_ = Converter.convertAttributes(data.enumerateAttributes(),data.classAttribute());
        Variables modelHeader = new Variables(attributes_);
        classVar_ = modelHeader.getVariableByName(data.classAttribute().name());

        inferenceAlgorithm_ = new ImportanceSampling();
        inferenceAlgorithm_.setSeed(this.getSeed());

        dag = buildDAG();

        System.out.println(dag.toString());

        /*
        if(getnOfStatesMultHiddenVar_() == 0 && getnOfGaussianHiddenVars_() == 0){   //ML can be used when Lapalace is introduced
            parameterLearningAlgorithm_ = new ParallelMaximumLikelihood();
        }else
            parameterLearningAlgorithm_ = new SVB();
            */
        parameterLearningAlgorithm_ = new SVB();
        parameterLearningAlgorithm_.setDAG(dag);


        DataOnMemoryListContainer<DataInstance> batch_ = new DataOnMemoryListContainer(attributes_);

        data.stream().forEach(instance ->
            batch_.add(new DataInstanceFromDataRow(new DataRowWeka(instance, this.attributes_)))
        );

        parameterLearningAlgorithm_.setDataStream(batch_);
        parameterLearningAlgorithm_.initLearning();
        parameterLearningAlgorithm_.runLearning();
        //parameterLearningAlgorithm_.updateModel(batch_);

        bnModel_ = parameterLearningAlgorithm_.getLearntBayesianNetwork();

        System.out.println(bnModel_);
        inferenceAlgorithm_.setModel(bnModel_);
    }

    /**
     * Updates the classifier with the given instance.
     *
     * @param instance the new training instance to include in the model
     * @exception Exception if the instance could not be incorporated in the
     *              model.
     */
    public void updateClassifier(Instance instance) throws Exception {
        DataOnMemoryListContainer<DataInstance> batch_ = new DataOnMemoryListContainer(attributes_);

        batch_.add(new DataInstanceFromDataRow(new DataRowWeka(instance, this.attributes_)));

        parameterLearningAlgorithm_.updateModel(batch_);

        bnModel_ = parameterLearningAlgorithm_.getLearntBayesianNetwork();
        inferenceAlgorithm_.setModel(bnModel_);
    }

    /**
     * Calculates the class membership probabilities for the given test instance.
     *
     * @param instance the instance to be classified
     * @return predicted class probability distribution
     * @exception Exception if there is a problem generating the prediction
     */
    @Override
    public double[] distributionForInstance(Instance instance) throws Exception {
        if(bnModel_ == null) {
            throw new UnsupportedOperationException("The model was not learnt");
            //return new double[0];
        }

        DataInstance dataInstance = new DataInstanceFromDataRow(new DataRowWeka(instance, this.attributes_));
        double realValue = dataInstance.getValue(classVar_);
        dataInstance.setValue(classVar_, eu.amidst.core.utils.Utils.missingValue());
        this.inferenceAlgorithm_.setEvidence(dataInstance);
        this.inferenceAlgorithm_.runInference();
        Multinomial multinomial = this.inferenceAlgorithm_.getPosterior(classVar_);
        dataInstance.setValue(classVar_, realValue);
        return multinomial.getProbabilities();
    }


    @Override
    public void setSeed(int seed) {

    }

    @Override
    public int getSeed() {
        return 0;
    }

}
