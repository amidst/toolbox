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

package eu.amidst.flinklink.core.learning.dynamic;


import eu.amidst.core.learning.parametric.bayesian.utils.DataPosterior;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.flinklink.core.data.DataFlink;
import org.apache.flink.api.java.DataSet;

import java.util.List;

/**
 * This interface defines the Algorithm for learning the {@link eu.amidst.core.models.BayesianNetwork} parameters.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#learningexample"> http://amidst.github.io/toolbox/CodeExamples.html#learningexample </a>  </p>
 */
public interface ParameterLearningAlgorithm {

    public void initLearning();

    void updateModelWithNewTimeSlice(int timeSlice, DataFlink<DynamicDataInstance> data);

    public DataSet<DataPosterior> computePosterior(List<Variable> latentVariables);

    void setDAG(DynamicDAG dag);

    void setSeed(int seed);

    DynamicBayesianNetwork getLearntDynamicBayesianNetwork();

    void setOutput(boolean activateOutput);

}
