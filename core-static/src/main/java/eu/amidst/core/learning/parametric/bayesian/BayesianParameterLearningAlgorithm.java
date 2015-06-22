/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.core.variables.Variable;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public interface BayesianParameterLearningAlgorithm extends ParameterLearningAlgorithm {

    default <E extends UnivariateDistribution> E getParameterPosterior(Variable parameter){
        if (!parameter.isParameterVariable())
            throw new IllegalArgumentException("Non a parameter variable");

        throw new UnsupportedOperationException("Non implemented yet");
    }
}
