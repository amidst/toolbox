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

package moa.clusterers;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.filereaders.DataInstanceFromDataRow;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.moalink.converterFromMoaToAmidst.Converter;
import eu.amidst.moalink.converterFromMoaToAmidst.DataRowWeka;
import moa.cluster.Clustering;
import moa.core.Measurement;
import moa.options.FlagOption;
import moa.options.IntOption;
import weka.core.*;

import java.util.stream.IntStream;

/**
 * This class extends the {@link moa.clusterers.AbstractClusterer} and defines the AMIDST Clustering algorithm that could be run using the MOA’s graphical user interface.
 * MOA (Massive Online Analysis) is an open source software available at http://moa.cms.waikato.ac.nz
 */
public class AmidstClusteringAlgorithm extends AbstractClusterer {

    /** Represents the cluster variable in this AmidstClusteringAlgorithm. */
    Variable clusterVar_;

    /** Represents the set of {@link Attributes}. */
    Attributes attributes_;

    /** Represents the number of clusters desired in this AmidstClusteringAlgorithm. **/
    protected int numClusters = 2;

    /** Represents the parallel mode. */
    protected boolean parallelMode_ = false;

    /** Represents the data stream batch. */
    private DataOnMemoryListContainer<DataInstance> batch_;

    /** Represents the window counter. */
    private int windowCounter;

    /** Represents a {@link DAG} object. */
    private DAG dag = null;

    /** Represents a {@code BayesianNetwork} object. */
    private BayesianNetwork bnModel_;

    /** Represents the used {@link ParameterLearningAlgorithm}. */
    private ParameterLearningAlgorithm parameterLearningAlgorithm_;

    /** Represents the used {@link InferenceAlgorithm}. */
    InferenceAlgorithm predictions_;

    /**
     * Creates a new object of the class {@link moa.options.IntOption}.
     * Specifies the window size for this AmidstClusteringAlgorithm.
     */
    public IntOption timeWindowOption = new IntOption("timeWindow",
            't', "Range of the window.", 1000);

    /**
     * Creates a new object of the class {@link moa.options.IntOption}.
     * Specifies the number of clusters for this AmidstClusteringAlgorithm.
     */
    public IntOption numberClustersOption = new IntOption("numberClusters",
            'c', "Number of Clusters.", 5);

    /**
     * Returns the number of clusters in this AmidstClusteringAlgorithm.
     * @return an {@code int} that represents the number of clusters in this AmidstClusteringAlgorithm.
     */
    public int getNumClusters() {
        return numClusters;
    }

    /**
     * Sets the number of clusters for this AmidstClusteringAlgorithm.
     * @param numClusters an {@code int} value that represents the number of clusters to be set.
     */
    public void setNumClusters(int numClusters) {
        this.numClusters = numClusters;
    }

    /**
     * Creates a new object of the class {@link moa.options.FlagOption}.
     * Specifies whether the parallel mode is used for this AmidstClusteringAlgorithm learning process.
     */
    public FlagOption parallelModeOption = new FlagOption("parallelMode", 'p',
            "Learn parameters in parallel mode when possible (e.g. ML)");

    /**
     * Tests whether the learning of this AmidstClusteringAlgorithm is performed in parallel.
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
     * Returns the data set.
     * @param numatt an {@code int} that represents the number of attributes.
     * @param numclus an {@code int} that represents the number of clusters.
     * @return {@link Instances} object that represents the data set.
     */
    private Instances getDataset(int numatt, int numclus) {
        FastVector attributes = new FastVector();
        for (int i = 0; i < numatt; i++) {
            attributes.addElement(new Attribute("att" + (i + 1)));
        }

        if(numclus > 0){
            FastVector classLabels = new FastVector();
            for (int i = 0; i < numclus; i++) {
                classLabels.addElement("class" + (i + 1));
            }
            attributes.addElement(new Attribute("class", classLabels));
        }

        Instances myDataset = new Instances("horizon", attributes, 0);
        if(numclus > 0){
            myDataset.setClassIndex(myDataset.numAttributes() - 1);
        }
        return myDataset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetLearningImpl() {
        batch_ = null;
        windowCounter = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trainOnInstanceImpl(Instance instance) {
        if(batch_ == null){
            setParallelMode_(parallelModeOption.isSet());
            setNumClusters(numberClustersOption.getValue());

            attributes_ = Converter.convertAttributes(getDataset(instance.numAttributes(), 0).enumerateAttributes());
            Variables modelHeader = new Variables(attributes_);
            clusterVar_ = modelHeader.newMultinomialVariable("clusterVar", getNumClusters());

            batch_ = new DataOnMemoryListContainer(attributes_);
            predictions_ = new VMP();
            predictions_.setSeed(this.randomSeed);


            dag = new DAG(modelHeader);

            /* Set DAG structure. */
            /* Add the hidden cluster variable as a parent of all the predictive attributes. */
            if (isParallelMode_()) {
                dag.getParentSets().parallelStream()
                        .filter(w -> w.getMainVar().getVarID() != clusterVar_.getVarID())
                        .forEach(w -> w.addParent(clusterVar_));
            }
            else {
                dag.getParentSets().stream()
                        .filter(w -> w.getMainVar().getVarID() != clusterVar_.getVarID())
                        .filter(w -> w.getMainVar().isObservable())
                        .forEach(w -> w.addParent(clusterVar_));
            }

            System.out.println(dag.toString());

            parameterLearningAlgorithm_ = new SVB();
        }

        if(windowCounter >= timeWindowOption.getValue()){
            batch_ = new DataOnMemoryListContainer(attributes_);
            windowCounter = 0;
        }
        DataInstance dataInstance = new DataInstanceFromDataRow(new DataRowWeka(instance, attributes_));
        windowCounter++;
        batch_.add(dataInstance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clustering getClusteringResult() {
        //sourceClustering = new Clustering();

        Instances dataset = getDataset(attributes_.getNumberOfAttributes(), getNumClusters());
        Instances newInstances = new Instances(dataset);

        if(bnModel_==null) {
            //parameterLearningAlgorithm_.setParallelMode(isParallelMode_());
            parameterLearningAlgorithm_.setDAG(dag);
            ((SVB)parameterLearningAlgorithm_).setWindowsSize(timeWindowOption.getValue());
            parameterLearningAlgorithm_.initLearning();
            parameterLearningAlgorithm_.updateModel(batch_);
        }else {
            parameterLearningAlgorithm_.updateModel(batch_);
        }

        bnModel_ = parameterLearningAlgorithm_.getLearntBayesianNetwork();
        predictions_.setModel(bnModel_);

        for (DataInstance dataInstance: batch_) {
            this.predictions_.setEvidence(dataInstance);
            this.predictions_.runInference();
            Multinomial multinomial = this.predictions_.getPosterior(clusterVar_);

            double[] results =  multinomial.getProbabilities();

            int cnum = IntStream.rangeClosed(0, getNumClusters() - 1).reduce((a, b) -> (results[a] > results[b])? a: b).getAsInt();

            double[] attValues = dataInstance.toArray();
            Instance newInst = new DenseInstance(1.0, attValues);
            newInst.insertAttributeAt(attributes_.getNumberOfAttributes());
            newInst.setDataset(dataset);
            newInst.setClassValue(cnum);
            newInstances.add(newInst);
        }
        clustering = new Clustering(newInstances);

        return clustering;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean  keepClassLabel(){
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
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
        return null;
    }

}
