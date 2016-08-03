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

package eu.amidst.flinklink.core.learning.parametric;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.core.utils.Serialization;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.accumulators.DoubleCounter;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.RichReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.configuration.Configuration;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * This class implements the {@link ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class ParallelMaximumLikelihood implements ParameterLearningAlgorithm {

    /**
     * Represents the directed acyclic graph {@link DAG}.
     */
    protected DAG dag;

    /**
     * Represents a {@link EF_BayesianNetwork} object
     */
    protected EF_BayesianNetwork efBayesianNetwork;

    /**
     * Represents the sufficient statistics used for parameter learning.
     */
    protected SufficientStatistics sumSS;


    double numInstances;

    public static String EFBN_NAME = "EFBN";

    public static String COUNTER_NAME = "COUNTER";


    public void initLearning() {
        efBayesianNetwork = new EF_BayesianNetwork(dag);
        sumSS = efBayesianNetwork.createInitSufficientStatistics();

    }

    @Override
    public void setBatchSize(int batchSize) {
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
    public double updateModel(DataFlink<DataInstance> dataUpdate) {
        try {
            Configuration config = new Configuration();
            config.setString(BN_NAME, this.dag.getName());
            config.setBytes(EFBN_NAME, Serialization.serializeObject(efBayesianNetwork));

            DataSet<DataInstance> dataset = dataUpdate.getDataSet();
            this.sumSS = dataset.map(new SufficientSatisticsMAP())
                    .withParameters(config)
                    .reduce(new SufficientSatisticsReduce())
                    .collect().get(0);

            //Add the prior
            sumSS.sum(efBayesianNetwork.createInitSufficientStatistics());

            JobExecutionResult result = dataset.getExecutionEnvironment().getLastJobExecutionResult();

            numInstances = result.getAccumulatorResult(ParallelMaximumLikelihood.COUNTER_NAME+"_"+this.dag.getName());
            numInstances++;//Initial counts

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput) {

    }


    static class SufficientSatisticsMAP extends RichMapFunction<DataInstance, SufficientStatistics> {


        private final DoubleCounter counterInstances = new DoubleCounter();
        EF_BayesianNetwork ef_bayesianNetwork;

        @Override
        public SufficientStatistics map(DataInstance dataInstance) throws Exception {
            this.counterInstances.add(1.0);
            return this.ef_bayesianNetwork.getSufficientStatistics(dataInstance);
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            String bnName = parameters.getString(BN_NAME, "");
            ef_bayesianNetwork = Serialization.deserializeObject(parameters.getBytes(EFBN_NAME,null));
            getRuntimeContext().addAccumulator(COUNTER_NAME+"_"+bnName, this.counterInstances);

        }

    }

    static class SufficientSatisticsReduce extends RichReduceFunction<SufficientStatistics> {
        @Override
        public SufficientStatistics reduce(SufficientStatistics value1, SufficientStatistics value2) throws Exception {
            value2.sum(value1);
            return value2;
        }

    }
}