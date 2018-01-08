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

package eu.amidst.core.learning.parametric;


import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;

import java.util.stream.Stream;

/**
 * This class implements the {@link ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class ParallelMaximumLikelihood implements ParameterLearningAlgorithm{

    /** Represents the batch size used for learning the parameters. */
    protected int windowsSize = 1000;

    /** Indicates the parallel processing mode, initialized here as {@code true}. */
    protected boolean parallelMode = true;

    /** Represents the {@link DataStream} used for learning the parameters. */
    protected DataStream<DataInstance> dataStream;

    /** Represents the directed acyclic graph {@link DAG}.*/
    protected DAG dag;

    /** Represents the data instance count. */
    protected AtomicDouble dataInstanceCount;

    /** Represents the sufficient statistics used for parameter learning. */
    protected SufficientStatistics sumSS;

    /** Represents a {@link EF_BayesianNetwork} object */
    protected EF_BayesianNetwork efBayesianNetwork;

    /** Represents if the class is in debug mode*/
    protected boolean debug = false;

    /** Represents whether Laplace correction (i.e. MAP estimation) is used*/
    protected boolean laplace = true;


    /**
     * Sets whether Laplace correction (i.e. MAP estimation) is used
     * @param laplace, a boolean value.
     */
    public void setLaplace(boolean laplace) {
        this.laplace = laplace;
    }

    /**
     * Sets the debug mode of the class
     * @param debug a boolean setting whether to execute in debug mode or not.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Sets the windows size.
     * @param windowsSize the batch size.
     */
    public void setWindowsSize(int windowsSize) {
        this.windowsSize = windowsSize;
    }

    /**
     * Sets the windows size.
     */
    public int getWindowsSize() {
        return windowsSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initLearning() {
        efBayesianNetwork = new EF_BayesianNetwork(dag);
        if (laplace) {
            sumSS = efBayesianNetwork.createInitSufficientStatistics();
            dataInstanceCount = new AtomicDouble(1.0); //Initial counts
        }else {
            sumSS = efBayesianNetwork.createZeroSufficientStatistics();
            dataInstanceCount = new AtomicDouble(0.0); //Initial counts
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataOnMemory<DataInstance> batch) {

        this.sumSS.sum(batch.stream()
                    .map(efBayesianNetwork::getSufficientStatistics)
                    .reduce(SufficientStatistics::sumVectorNonStateless).get());

        dataInstanceCount.addAndGet(batch.getNumberOfDataInstances());


        System.out.println(this.sumSS.output());
        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataStream(DataStream<DataInstance> data) {
        this.dataStream=data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        throw new UnsupportedOperationException("Method not implemented yet");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double updateModel(DataStream<DataInstance> dataStream) {
        Stream<DataOnMemory<DataInstance>> stream = null;
        if (parallelMode){
            stream = dataStream.parallelStreamOfBatches(windowsSize);
        }else{
            stream = dataStream.streamOfBatches(windowsSize);
        }
        sumSS.sum(stream
                .peek(batch -> {
                    dataInstanceCount.getAndAdd(batch.getNumberOfDataInstances());
                    if (debug) System.out.println("Parallel ML procesando "+(int)dataInstanceCount.get() +" instances");
                })
                .map(batch ->  batch.stream()
                        .map(efBayesianNetwork::getSufficientStatistics)
                        .reduce(SufficientStatistics::sumVectorNonStateless)
                        .get())
                .reduce(SufficientStatistics::sumVectorNonStateless).get());

        return Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {

        this.initLearning();

        Stream<DataOnMemory<DataInstance>> stream = null;
        if (parallelMode){
            stream = dataStream.parallelStreamOfBatches(windowsSize);
        }else{
            stream = dataStream.streamOfBatches(windowsSize);
        }
        sumSS.sum(stream
                .peek(batch -> {
                    dataInstanceCount.getAndAdd(batch.getNumberOfDataInstances());
                    if (debug) System.out.println("Parallel ML procesando "+(int)dataInstanceCount.get() +" instances");
                })
                .map(batch ->  batch.stream()
                                    .map(efBayesianNetwork::getSufficientStatistics)
                                    .reduce(SufficientStatistics::sumVectorNonStateless)
                                    .get())
                .reduce(SufficientStatistics::sumVectorNonStateless).get());
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
        normalizedSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(normalizedSS);
        return efBayesianNetwork.toBayesianNetwork(dag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParallelMode(boolean parallelMode_) {
        parallelMode = parallelMode_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput) {

    }
}
