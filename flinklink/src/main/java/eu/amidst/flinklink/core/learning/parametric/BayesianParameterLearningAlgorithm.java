/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.flinklink.core.learning.parametric;


import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.variables.Variable;

/**
 * This interface extends {@link ParameterLearningAlgorithm} and defines the Bayesian parameter learning algorithm.
 */
public interface BayesianParameterLearningAlgorithm extends ParameterLearningAlgorithm {

    /**
     * Returns the parameter posterior.
     * @param parameter a {@link Variable} object.
     * @param <E> a subtype of {@link UnivariateDistribution}.
     * @return the parameter posterior.
     */
    default <E extends UnivariateDistribution> E getParameterPosterior(Variable parameter){
        if (!parameter.isParameterVariable())
            throw new IllegalArgumentException("Non a parameter variable");

        throw new UnsupportedOperationException("Non implemented yet");
    }

    /**
     * Sets the plateu structure of this SVB.
     * @param plateuStructure a valid {@link PlateuStructure} object.
     */
    void setPlateuStructure(PlateuStructure plateuStructure);
}
