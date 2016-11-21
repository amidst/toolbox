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

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.learning.parametric.bayesian.utils.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * This interface extends {@link ParameterLearningAlgorithm} and defines the Bayesian parameter learning algorithm.
 */
public interface BayesianParameterLearningAlgorithm extends ParameterLearningAlgorithm {

    /**
     * Compute the posterior over all the latent variables for a given set of data instances
     *
     * @param batch, a batch of data instances.
     * @return A list of {@link DataPosterior} objects.
     */
    public List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch);

    /**
     * Compute the posterior over a given set of latent variables for a given set of data instances
     *
     * @param batch,           a batch of data instances.
     * @param latentVariables, a list of Variable objects.
     * @return A list of {@link DataPosterior} objects.
     */
    List<DataPosterior> computePosterior(DataOnMemory<DataInstance> batch, List<Variable> latentVariables);

    /**
     * Returns the parameter posterior.
     *
     * @param parameter a {@link Variable} object.
     * @param <E>       a subtype of {@link UnivariateDistribution}.
     * @return the parameter posterior.
     */
    default <E extends UnivariateDistribution> E getParameterPosterior(Variable parameter) {
        if (!parameter.isParameterVariable())
            throw new IllegalArgumentException("Non a parameter variable");

        throw new UnsupportedOperationException("Non implemented yet");
    }

    /**
     * Computes the predictive log-likelihood of the data.
     *
     * @param batch a {@link DataOnMemory} object.
     * @return the predictive log-probability of the data instances of the
     * batch. Or Double.NaN if this log-probability can not be estimated.
     */
    double predictedLogLikelihood(DataOnMemory<DataInstance> batch);


    /**
     * Sets the plateu structure of this SVB.
     *
     * @param plateuStructure a valid {@link PlateuStructure} object.
     */
    void setPlateuStructure(PlateuStructure plateuStructure);


    /**
     * Randomly initializes the global parameters of the model.
     */
    void randomInitialize();

}