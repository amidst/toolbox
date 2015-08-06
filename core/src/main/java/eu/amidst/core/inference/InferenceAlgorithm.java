/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.inference;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 30/01/15.
 */
public interface InferenceAlgorithm {

    void runInference();

    void setModel(BayesianNetwork model);

    BayesianNetwork getOriginalModel();

    void setEvidence(Assignment assignment);

    /**
     * This method is only implemented for those inference
     * algorithms which can be run in parallel.
     * @param parallelMode_
     */
    default void setParallelMode(boolean parallelMode_){

    }

    default double getExpectedValue(Variable var, Function<Double,Double> function){
        UnivariateDistribution univariateDistribution = this.getPosterior(var);

        Random random = new Random(0);
        int nSamples = 1000;
        double val = IntStream.range(0, nSamples).mapToDouble(i -> univariateDistribution.sample(random))
                                .map(sample -> function.apply(sample))
                                .sum();
        return val/nSamples;
    }

    <E extends UnivariateDistribution> E getPosterior(Variable var);

    default <E extends UnivariateDistribution> E getPosterior(int varID){
        return this.getPosterior(this.getOriginalModel().getStaticVariables().getVariableById(varID));
    }

    double getLogProbabilityOfEvidence();

    void setSeed(int seed);
}