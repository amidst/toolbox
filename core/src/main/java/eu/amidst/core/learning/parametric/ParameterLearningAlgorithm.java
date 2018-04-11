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



import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This interface defines the Algorithm for learning the {@link eu.amidst.core.models.BayesianNetwork} parameters.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#learningexample"> http://amidst.github.io/toolbox/CodeExamples.html#learningexample </a>  </p>
 */
public interface ParameterLearningAlgorithm {

    /**
     * Initializes the parameter learning process.
     */
    void initLearning();

    /**
     * Updates the model using a given {@link DataOnMemory} object.
     * @param batch a {@link DataOnMemory} object.
     * @return the log-probability of the data instances of the
     * batch. Or Double.NaN if this log-probability can not be estimated.
     */
    double updateModel(DataOnMemory<DataInstance> batch);


    /**
     * Updates the model using a given {@link DataStream} object.
     * @param dataStream a {@link DataStream} object.
     * @return the log-probability of the data instances of the
     * stream. Or Double.NaN if this log-probability can not be estimated.
     */
    default double updateModel(DataStream<DataInstance> dataStream){

        AtomicLong batchCount = new AtomicLong(0);
        AtomicLong instCount = new AtomicLong(0);

        return dataStream.streamOfBatches(this.getWindowsSize())
                .sequential().mapToDouble(b -> {


                    System.out.println("Processing batch "+batchCount.addAndGet(1)+":");
                    System.out.println("\tTotal instance count: "+ instCount.addAndGet(b.getNumberOfDataInstances()));

                    System.out.print("\t");
                    double ret =  this.updateModel(b);
                    return ret;

                }).sum();
    }

    /**
     * Returns the window size.
     * @return the window size.
     */
    int getWindowsSize();

    /**
     * Sets the window size.
     * @param windowsSize the window size.
     */
    void setWindowsSize(int windowsSize);

    /**
     * Sets the {@link DataStream} to be used by this ParameterLearningAlgorithm.
     * @param data a {@link DataStream} object.
     */
    void setDataStream(DataStream<DataInstance> data);

    /**
     * Returns the log marginal probability.
     * @return the log marginal probability.
     */
    double getLogMarginalProbability();

    /**
     * Runs the parameter learning process.
     */
    void runLearning();

    /**
     * Sets the {@link DAG} structure.
     * @param dag a directed acyclic graph {@link DAG}.
     */
    void setDAG(DAG dag);

    /**
     * Sets the seed using a single {@code int} seed.
     * @param seed the initial seed.
     */
    void setSeed(int seed);

    /**
     * Returns the learnt {@link BayesianNetwork} model.
     * @return the learnt {@link BayesianNetwork} model.
     */
    BayesianNetwork getLearntBayesianNetwork();

    /**
     * Sets the parallel processing mode.
     * @param parallelMode {@code true} if the learning is performed in parallel, {@code false} otherwise.
     */
    void setParallelMode(boolean parallelMode);

    /**
     * Sets the Output.
     * @param activateOutput {@code true} if the output is activated, {@code false} otherwise.
     */
    void setOutput(boolean activateOutput);
}
