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

package moa.classifiers.bayes;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.filereaders.DataInstanceFromDataRow;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.moalink.converterFromMoaToAmidst.Converter;
import eu.amidst.moalink.converterFromMoaToAmidst.DataRowWeka;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Regressor;
import moa.core.InstancesHeader;
import moa.core.Measurement;
import moa.options.FlagOption;
import moa.options.IntOption;
import weka.core.Instance;

import java.util.stream.IntStream;

/**
 * This class extends the {@link moa.classifiers.AbstractClassifier} and defines the AMIDST Regressor that could be run using the MOA’s graphical user interface.
 * MOA (Massive Online Analysis) is an open source software available at http://moa.cms.waikato.ac.nz
 */
public class AmidstRegressor extends AbstractClassifier implements Regressor {

    /** Represents the batch size. */
    protected  int batchSize_ = 100;

    /** Represents the number of Gaussian hidden variables in this AmidstRegressor. */
    protected  int nOfGaussianHiddenVars_ = 0;

    /** Represents the number of Multinomial hidden variables in this AmidstRegressor. */
    protected  int nOfStatesMultHiddenVar_ = 0;

    /** Represents the parallel mode. */
    protected boolean parallelMode_ = false;

    /** Represents a {@link DAG} object. */
    private DAG dag = null;

    /** Represents the target variable in this AmidstRegressor. */
    private Variable targetVar_;

    /** Represents the data stream batch. */
    private DataOnMemoryListContainer<DataInstance> batch_;

    /** Represents the used {@link ParameterLearningAlgorithm}. */
    private ParameterLearningAlgorithm parameterLearningAlgorithm_;

    /** Represents a {@code BayesianNetwork} object. */
    private BayesianNetwork bnModel_;

    /** Represents the used {@link InferenceAlgorithm}. */
    InferenceAlgorithm predictions_;

    /** Represents the set of {@link Attributes}. */
    Attributes attributes_;

    /**
     * Creates a new object of the class {@link moa.options.IntOption}.
     * Specifies the size of the batch used for learning this AmidstRegressor.
     */
    public IntOption batchSizeOption = new IntOption("windowsSize",
            'w', "Size of the batch in which to perform learning (significant if hidden variables are used)",
            100);

    /**
     * Returns the batch size.
     * @return an {@code int} that represents the batch size.
     */
    public int getBatchSize_() {
        return batchSize_;
    }

    /**
     * Sets the batch size.
     * @param batchSize_ an {@code int} value that represents the batch size to be set.
     */
    public void setBatchSize_(int batchSize_) {
        this.batchSize_ = batchSize_;
    }

    /**
     * Creates a new object of the class {@link moa.options.IntOption}.
     * Specifies the number of Gaussian hidden variables in this AmidstRegressor.
     */
    public IntOption nOfGaussianHiddenVarsOption = new IntOption("nOfGaussHiddenVars",
            'g', "Number of Gaussian hidden super-parent variables",
            0);

    /**
     * Returns the number of Gaussian hidden variables in this AmidstRegressor.
     * @return an {@code int} that represents the number of Gaussian hidden variables in this AmidstRegressorr.
     */
    public int getnOfGaussianHiddenVars_() {
        return nOfGaussianHiddenVars_;
    }

    /**
     * Sets the number of Gaussian hidden variables in this AmidstRegressor.
     * @param nOfGaussianHiddenVars_ an {@code int} value that represents the number of Gaussian hidden variables to be set.
     */
    public void setnOfGaussianHiddenVars_(int nOfGaussianHiddenVars_) {
        this.nOfGaussianHiddenVars_ = nOfGaussianHiddenVars_;
    }

    /**
     * Creates a new object of the class {@link moa.options.IntOption}.
     * Specifies the number of states of the Multinomial hidden variable in this AmidstRegressor.
     */
    public IntOption nOfStatesMultHiddenVarOption = new IntOption("nOfStatesMultHiddenVar",
            'm', "Number of states of the multinomial hidden super-parent variable",
            0);

    /**
     * Returns the number of states of the Multinomial hidden variables in this AmidstRegressor.
     * @return an {@code int} that represents the number of states of the Multinomial hidden variables in this AmidstRegressor.
     */
    public int getnOfStatesMultHiddenVar_() {
        return nOfStatesMultHiddenVar_;
    }

    /**
     * Sets the number of states of the Multinomial hidden variables in this AmidstRegressor.
     * @param nOfStatesMultHiddenVar_ an {@code int} value that represents the number of states of the Multinomial hidden variables to be set.
     */
    public void setnOfStatesMultHiddenVar_(int nOfStatesMultHiddenVar_) {
        this.nOfStatesMultHiddenVar_ = nOfStatesMultHiddenVar_;
    }

    /**
     * Creates a new object of the class {@link moa.options.FlagOption}.
     * Specifies whether the parallel mode is used for this AmidstRegressor learning process.
     */
    public FlagOption parallelModeOption = new FlagOption("parallelMode", 'p',
            "Learn parameters in parallel mode when possible (e.g. ML)");

    /**
     * Tests whether the learning of this AmidstRegressor is performed in parallel.
     * @return {@code true} if the learning is performed in parallel, {@code false} otherwise.
     */
    public boolean isParallelMode_() {
        return parallelMode_;
    }

    /**
     * Sets the parallel mode.
     * @param parallelMode_ a {@code boolean} that represents the parallel mode value to be set.
     */
    public void setParallelMode_(boolean parallelMode_) {
        this.parallelMode_ = parallelMode_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetLearningImpl() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModelContext(InstancesHeader ih) {
        super.setModelContext(ih);
        setBatchSize_(batchSizeOption.getValue());
        setnOfGaussianHiddenVars_(nOfGaussianHiddenVarsOption.getValue());
        setnOfStatesMultHiddenVar_(nOfStatesMultHiddenVarOption.getValue());
        setParallelMode_(parallelModeOption.isSet());

        attributes_ = Converter.convertAttributes(this.modelContext);
        Variables modelHeader = new Variables(attributes_);
        targetVar_ = modelHeader.getVariableByName(modelContext.classAttribute().name());

        batch_ = new DataOnMemoryListContainer(attributes_);
        predictions_ = new VMP();
        predictions_.setSeed(this.randomSeed);

        /* Create both Gaussian and Multinomial hidden variables. */

        if(getnOfGaussianHiddenVars_() > 0)
            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1).forEach(i -> modelHeader.newGaussianVariable("HiddenG_" + i));
        if(getnOfStatesMultHiddenVar_() > 0)
            modelHeader.newMultinomialVariable("HiddenM", getnOfStatesMultHiddenVar_());

        dag = new DAG(modelHeader);

        /* Set DAG structure. */
        /* Add classVar and all hidden variables as parents of all predictive attributes. */
        /* Note however that only Gaussian predictive attributes are allowed. */
        if (isParallelMode_()) {
            dag.getParentSets().parallelStream()
                    .filter(w -> w.getMainVar().getVarID() != targetVar_.getVarID())
                    .filter(w -> w.getMainVar().isObservable())
                    .forEach(w -> {
                        if(!w.getMainVar().isNormal())
                            throw new UnsupportedOperationException("Only Numeric predictive attributes are currently supported");
                        w.addParent(targetVar_);
                        if (getnOfStatesMultHiddenVar_() != 0)
                            w.addParent(modelHeader.getVariableByName("HiddenM"));
                        if (getnOfGaussianHiddenVars_() != 0)
                            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1).parallel()
                                    .forEach(hv -> w.addParent(modelHeader.getVariableByName("HiddenG_" + hv)));
                    });

        }
        else {
            dag.getParentSets().stream()
                    .filter(w -> w.getMainVar().getVarID() != targetVar_.getVarID())
                    .filter(w -> w.getMainVar().isObservable())
                    .forEach(w -> {
                        if(!w.getMainVar().isNormal())
                            throw new UnsupportedOperationException("Only Numeric predictive attributes are currently supported");
                        w.addParent(targetVar_);
                        if (getnOfStatesMultHiddenVar_() != 0)
                            w.addParent(modelHeader.getVariableByName("HiddenM"));
                        if (getnOfGaussianHiddenVars_() != 0)
                            IntStream.rangeClosed(0, getnOfGaussianHiddenVars_()-1)
                                    .forEach(hv -> w.addParent(modelHeader.getVariableByName("HiddenG_" + hv)));});
        }

        System.out.println(dag.toString());

        /*
        if(getnOfStatesMultHiddenVar_() == 0 && getnOfGaussianHiddenVars_() == 0){   //ML can be used when Lapalace is introduced
            parameterLearningAlgorithm_ = new ParallelMaximumLikelihood();
        }else
            parameterLearningAlgorithm_ = new SVB();
            */
        parameterLearningAlgorithm_ = new SVB();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trainOnInstanceImpl(Instance instance) {
        DataInstance dataInstance = new DataInstanceFromDataRow(new DataRowWeka(instance, this.attributes_));
        if(batch_.getNumberOfDataInstances() < getBatchSize_()-1) {  //store
            batch_.add(dataInstance);
        }else{                                                  //store & learn
            batch_.add(dataInstance);
            if(bnModel_==null) {
                //parameterLearningAlgorithm_.setParallelMode(isParallelMode_());
                parameterLearningAlgorithm_.setDAG(dag);
                parameterLearningAlgorithm_.initLearning();
                parameterLearningAlgorithm_.updateModel(batch_);
            }else{
                parameterLearningAlgorithm_.updateModel(batch_);
            }
            bnModel_ = parameterLearningAlgorithm_.getLearntBayesianNetwork();
            predictions_.setModel(bnModel_);
            batch_ = new DataOnMemoryListContainer(attributes_);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getModelDescription(StringBuilder stringBuilder, int i) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRandomizable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getVotesForInstance(Instance instance) {
        if(bnModel_ == null) {
            return new double[0];
        }

        DataInstance dataInstance = new DataInstanceFromDataRow(new DataRowWeka(instance, this.attributes_));
        double realValue = dataInstance.getValue(targetVar_);
        dataInstance.setValue(targetVar_, Utils.missingValue());
        this.predictions_.setEvidence(dataInstance);
        this.predictions_.runInference();
        Normal normal = this.predictions_.getPosterior(targetVar_);
        dataInstance.setValue(targetVar_, realValue);
        return new double[] {normal.getMean()};
    }
}
