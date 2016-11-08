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

package eu.amidst.sparklink.core.learning;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.sparklink.core.data.DataSpark;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class implements the {@link ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class ParallelMaximumLikelihood implements ParameterLearningAlgorithm, Serializable {

    /**
     * Represents the {@link DataSpark} used for learning the parameters.
     */
    protected transient DataSpark dataSpark;

    /**
     * Represents the directed acyclic graph {@link DAG}.
     */
    protected transient DAG dag;

    /**
     * Represents a {@link EF_BayesianNetwork} object
     */
    protected EF_BayesianNetwork efBayesianNetwork;

    /**
     * Represents the sufficient statistics used for parameter learning.
     */
    protected transient SufficientStatistics sumSS;

    double numInstances;

    public void initLearning() {
        efBayesianNetwork = new EF_BayesianNetwork(dag);
        sumSS = efBayesianNetwork.createInitSufficientStatistics();

    }

    @Override
    public void setBatchSize(int batchSize) {
    }

    @Override
    public int getBatchSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void setDataSpark(DataSpark data) {
        this.dataSpark = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        //TODO: temporal solution, the logMarginalProbability should be actually calculated.
        return Double.NaN;
        //throw new UnsupportedOperationException("Method not implemented yet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {
        this.initLearning();
        this.updateModel(this.dataSpark);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataSpark dataUpdate) {

            //this.sumSS = computeSufficientStatistics(dataUpdate, efBayesianNetwork);

            this.sumSS = dataUpdate.getDataSet()
                .mapPartitions( iter -> sufficientStatisticsMap(iter, this.efBayesianNetwork))
                .reduce(ParallelMaximumLikelihood::sufficientStatisticsReduce);

        //Add the prior
            sumSS.sum(efBayesianNetwork.createInitSufficientStatistics());

            // FIXME: Maybe a generic method from the class, what about caching?
            numInstances = dataSpark.getDataSet().count();
            numInstances++;//Initial counts


        return this.getLogMarginalProbability();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDAG(DAG dag_) {
        this.dag = dag_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSeed(int seed) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        //Normalize the sufficient statistics
        SufficientStatistics normalizedSS = efBayesianNetwork.createZeroSufficientStatistics();
        normalizedSS.copy(sumSS);
        normalizedSS.divideBy(numInstances);

        efBayesianNetwork.setMomentParameters(normalizedSS);
        return efBayesianNetwork.toBayesianNetwork(dag);
    }

    @Override
    public void setOutput(boolean activateOutput) {

    }

//    private static SufficientStatistics computeSufficientStatistics(DataSpark data, EF_BayesianNetwork ef_bayesianNetwork) {
//
//        return data.getDataSet()
//                .mapPartitions(new M)
//                .reduce(ParallelMaximumLikelihood::sufficientStatisticsReduce);
////        return data.getDataSet()
////                .mapPartitions( iter -> sufficientStatisticsMap(iter, ef_bayesianNetwork))
////                .reduce(ParallelMaximumLikelihood::sufficientStatisticsReduce);
//    }

    private static Iterable<SufficientStatistics> sufficientStatisticsMap(Iterator<DataInstance> iter, EF_BayesianNetwork ef_bayesianNetwork) {

        SufficientStatistics accumulator = null;
        if (iter.hasNext())
            accumulator = ef_bayesianNetwork.getSufficientStatistics(iter.next());;

        while (iter.hasNext()) {
                accumulator.sum(ef_bayesianNetwork.getSufficientStatistics(iter.next()));
        }

        // FIXME: Is this really necessary?
        ArrayList<SufficientStatistics> result = new ArrayList<SufficientStatistics>();
        result.add(accumulator);

        return result;
    }

    private static SufficientStatistics sufficientStatisticsReduce(SufficientStatistics sta1, SufficientStatistics sta2) {

        sta1.sum(sta2);

        return sta1;
    }

}