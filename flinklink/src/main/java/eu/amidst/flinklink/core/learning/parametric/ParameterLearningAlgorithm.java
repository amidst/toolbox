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
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.flinklink.core.data.DataFlink;

/**
 * This interface defines the Algorithm for learning the {@link BayesianNetwork} parameters.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#learningexample"> http://amidst.github.io/toolbox/CodeExamples.html#learningexample </a>  </p>
 */
public interface ParameterLearningAlgorithm {

    String BN_NAME="BN_NAME";

    /**
     * Returns the log marginal probability.
     * @return the log marginal probability.
     */
    double getLogMarginalProbability();


    /**
     * Updates the model using a given {@link DataFlink} object.
     * @param dataFlink a {@link DataFlink} object.
     * @return the log-probability of the data instances of the
     * data. Or Double.NaN if this log-probability can not be estimated.
     */
    double updateModel(DataFlink<DataInstance> dataFlink);


    /**
     * Initializes the parameter learning process.
     */
    void initLearning();


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
     * Sets the Output.
     * @param activateOutput {@code true} if the output is activated, {@code false} otherwise.
     */
    void setOutput(boolean activateOutput);

    /**
     * Sets the batch size.
     * @param windowsSize the window size.
     */
    void setBatchSize(int windowsSize);
}
