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

package weka.classifiers.bayes;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.filereaders.DataInstanceFromDataRow;
import eu.amidst.core.distribution.Normal;
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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import java.util.stream.IntStream;

/**
 * This class extends the {@link AbstractClassifier} and defines the AMIDST Classifier that could be run using the MOA’s graphical user interface.
 * MOA (Massive Online Analysis) is an open source software available at http://moa.cms.waikato.ac.nz
 */
public class AmidstRegressor extends AbstractClassifier implements OptionHandler, Randomizable{
    /** Represents the number of Gaussian hidden variables in this AmidstClassifier. */
    protected  int nOfGaussianHiddenVars_ = 0;

    /** Represents a {@link DAG} object. */
    private DAG dag = null;

    /** Represents the class variable in this AmidstClassifier. */
    private Variable targetVar_;

    /** Represents the used {@link ParameterLearningAlgorithm}. */
    private ParameterLearningAlgorithm parameterLearningAlgorithm_;

    /** Represents a {@code BayesianNetwork} object. */
    private BayesianNetwork bnModel_;

    /** Represents the used {@link InferenceAlgorithm}. */
    InferenceAlgorithm inferenceAlgorithm_;

    /** Represents the set of {@link Attributes}. */
    Attributes attributes_;

    public int getnOfGaussianHiddenVars_() {
        return nOfGaussianHiddenVars_;
    }

    public void setnOfGaussianHiddenVars_(int nOfGaussianHiddenVars_) {
        this.nOfGaussianHiddenVars_ = nOfGaussianHiddenVars_;
    }

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
        result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capabilities.Capability.MISSING_VALUES);

        // class
        result.enable(Capabilities.Capability.NUMERIC_CLASS);
        result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

        // instances
        result.setMinimumNumberInstances(0);

        return result;
    }


    @Override
    public void buildClassifier(Instances data) throws Exception {

        attributes_ = Converter.convertAttributes(data.enumerateAttributes(),data.classAttribute());
        Variables modelHeader = new Variables(attributes_);
        targetVar_ = modelHeader.getVariableByName(data.classAttribute().name());
        
        inferenceAlgorithm_ = new ImportanceSampling();
        inferenceAlgorithm_.setSeed(this.getSeed());

        /* Create both Gaussian and Multinomial hidden variables. */

        if(getnOfGaussianHiddenVars_() > 0)
            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1)
                    .forEach(i -> modelHeader.newGaussianVariable("HiddenG_" + i));

        dag = new DAG(modelHeader);

        /* Set DAG structure. */
        /* 1. Add classVar all Gaussian hidden variables as parents of the target_var. */
        if(getnOfGaussianHiddenVars_() > 0)
            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1).parallel()
                    .forEach(hv -> dag.getParentSet(targetVar_).addParent(modelHeader.getVariableByName("HiddenG_" + hv)));

        /* 2. Add all predictive attributes as parents of the target_var.  */
        /* Note however that Gaussian hidden variables can be only parents of Gaussian predictive attributes. */
        modelHeader.getListOfVariables().stream()
                .filter(w -> w.getVarID() != targetVar_.getVarID())
                .filter(w -> w.isObservable())
                .forEach(w -> {
                    dag.getParentSet(targetVar_).addParent(w);
                    if (getnOfGaussianHiddenVars_() != 0)
                        IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1)
                                .forEach(hv -> dag.getParentSet(w).addParent(modelHeader.getVariableByName("HiddenG_" + hv)));});

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


    @Override
    public double classifyInstance(Instance instance) throws Exception {
        if(bnModel_ == null) {
            throw new UnsupportedOperationException("The model was not learnt");
        }

        DataInstance dataInstance = new DataInstanceFromDataRow(new DataRowWeka(instance, this.attributes_));
        double realValue = dataInstance.getValue(targetVar_);
        dataInstance.setValue(targetVar_, eu.amidst.core.utils.Utils.missingValue());
        this.inferenceAlgorithm_.setEvidence(dataInstance);
        this.inferenceAlgorithm_.runInference();
        Normal normal = this.inferenceAlgorithm_.getPosterior(targetVar_);
        dataInstance.setValue(targetVar_, realValue);
        return normal.getMean();
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @Override
    public Enumeration<Option> listOptions() {

        Vector<Option> newVector = new Vector<Option>(1);

        newVector.addElement(new Option(
                "\tNumber of Gaussian hidden variables.\n"+
                        "\t(Default = 0)",
                "G", 1,"-G <number of Gaussian hidden>"));

        newVector.addAll(Collections.list(super.listOptions()));

        return newVector.elements();
    }

    /**
     * Parses a given list of options.
     * <p/>
     *
     * <!-- options-start --> Valid options are:
     * <p/>
     *
     * <pre>
     * -G
     *  Number of hidden Gaussian variables
     * </pre>
     *
     * <!-- options-end -->
     *
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    @Override
    public void setOptions(String[] options) throws Exception {
        String numGaussString = Utils.getOption('G', options);
        if (numGaussString.length() != 0) {
            setnOfGaussianHiddenVars_(Integer.parseInt(numGaussString));
        } else {
            setnOfGaussianHiddenVars_(0);
        }
    }

    /**
     * Gets the current settings of the classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    @Override
    public String[] getOptions() {

        Vector<String> options = new Vector<String>();
        options.add("-G"); options.add("" + getnOfGaussianHiddenVars_());

        Collections.addAll(options, super.getOptions());

        return options.toArray(new String[0]);
    }

    @Override
    public void setSeed(int seed) {

    }

    @Override
    public int getSeed() {
        return 0;
    }

    /**
     * Main method for testing this class.
     *
     * @param argv the options
     */
    public static void main(String[] argv) {
        runClassifier(new AmidstRegressor(), argv);
    }
}
