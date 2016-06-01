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

package weka.clusterers;

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
import weka.core.Instance;
import weka.core.Instances;

/**
 * Created by ana@cs.aau.dk on 21/03/16.
 */
public class AmidstClustering extends RandomizableClusterer implements NumberOfClustersRequestable {

    /** Represents the cluster variable in this AmidstClusteringAlgorithm. */
    Variable clusterVar_;

    /** Represents the set of {@link Attributes}. */
    Attributes attributes_;

    /** Represents the number of clusters desired in this AmidstClusteringAlgorithm. **/
    protected int m_NumClusters = 2;

    /** Represents a {@link DAG} object. */
    private DAG dag = null;

    /** Represents a {@code BayesianNetwork} object. */
    private BayesianNetwork bnModel_;

    /** Represents the used {@link ParameterLearningAlgorithm}. */
    private ParameterLearningAlgorithm parameterLearningAlgorithm_;

    /** Represents the used {@link InferenceAlgorithm}. */
    InferenceAlgorithm inferenceAlgorithm_;


    @Override
    public double[] distributionForInstance(Instance instance) throws Exception {
        if(bnModel_ == null) {
            throw new UnsupportedOperationException("The model was not learnt");
            //return new double[0];
        }

        DataInstance dataInstance = new DataInstanceFromDataRow(new DataRowWeka(instance, this.attributes_));
        double realValue = dataInstance.getValue(clusterVar_);
        dataInstance.setValue(clusterVar_, eu.amidst.core.utils.Utils.missingValue());
        this.inferenceAlgorithm_.setEvidence(dataInstance);
        this.inferenceAlgorithm_.runInference();
        Multinomial multinomial = this.inferenceAlgorithm_.getPosterior(clusterVar_);
        dataInstance.setValue(clusterVar_, realValue);

        return multinomial.getProbabilities();
    }

    @Override
    public void buildClusterer(Instances data) throws Exception {

        attributes_ = Converter.convertAttributes(data.enumerateAttributes());
        Variables modelHeader = new Variables(attributes_);
        clusterVar_ = modelHeader.newMultinomialVariable("clusterVar", this.numberOfClusters());

        inferenceAlgorithm_ = new ImportanceSampling();
        inferenceAlgorithm_.setSeed(this.getSeed());

        dag = new DAG(modelHeader);

        /* Set DAG structure. */
        /* Add the hidden cluster variable as a parent of all the predictive attributes. */
        dag.getParentSets().stream()
        .filter(w -> w.getMainVar().getVarID() != clusterVar_.getVarID())
        .filter(w -> w.getMainVar().isObservable())
        .forEach(w -> w.addParent(clusterVar_));


        System.out.println(dag.toString());

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

    @Override
    public int numberOfClusters() throws Exception {
        return m_NumClusters;
    }

    @Override
    public void setNumClusters(int numClusters) throws Exception {
        if (numClusters <= 0) {
            throw new Exception("Number of clusters must be > 0");
        }
        m_NumClusters = numClusters;
    }

    /**
     * Main method for testing this class.
     *
     * @param argv should contain the following arguments:
     *          <p>
     *          -t training file [-T test file] [-N number of clusters] [-S random
     *          seed]
     */
    public static void main(String[] argv) {
        runClusterer(new AmidstClustering(), argv);
    }
}
