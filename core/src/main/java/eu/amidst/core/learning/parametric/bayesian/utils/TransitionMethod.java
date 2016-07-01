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

package eu.amidst.core.learning.parametric.bayesian.utils;

import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;

/**
 * This interface defines the transition method.
 */
public interface TransitionMethod {

    /**
     * Initializes the model.
     * @param bayesianNetwork an {@link EF_LearningBayesianNetwork} object.
     * @param plateuStructure a {@link PlateuStructure} object.
     * @return an {@link EF_LearningBayesianNetwork} object.
     */
    default EF_LearningBayesianNetwork initModel(EF_LearningBayesianNetwork bayesianNetwork, PlateuStructure plateuStructure){
        return bayesianNetwork;
    }

    /**
     * Transits the model.
     * @param bayesianNetwork an {@link EF_LearningBayesianNetwork} object.
     * @param plateuStructure a {@link PlateuStructure} object.
     * @return an {@link EF_LearningBayesianNetwork} object.
     */
    EF_LearningBayesianNetwork transitionModel(EF_LearningBayesianNetwork bayesianNetwork, PlateuStructure plateuStructure);
}
