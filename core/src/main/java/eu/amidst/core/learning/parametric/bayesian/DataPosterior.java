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

package eu.amidst.core.learning.parametric.bayesian;


import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class stores the posterior probabilities over a set of latent variables for an
 * item with a given id. It can be used to store the result of an inference/learning operation.
 */
public class DataPosterior implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    /** Represents the id of item */
    final long id;

    /** Represent the list of posterior probabilities */
    final List<UnivariateDistribution> posteriors;

    /** Represent the list of latent variables*/
    final List<Variable> latentVariables;

    /**
     * Creates a new object using a given id_ and a list of univariate posteriors.
     * @param id_, a unique id.
     * @param posteriors_, a list of {@link UnivariateDistribution} distributions.
     */
    public DataPosterior(long id_, List<UnivariateDistribution> posteriors_){
        this.id=id_;
        this.posteriors = posteriors_;
        latentVariables = new ArrayList<>();
        for (UnivariateDistribution univariateDistribution : posteriors_) {
            latentVariables.add(univariateDistribution.getVariable());
        }
    }

    /**
     * Returns the id of the item
     * @return a long value
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the list of posterior probabilities distributions.
     * @return, a list of {@link UnivariateDistribution} objects
     */
    public List<UnivariateDistribution> getPosteriors() {
        return posteriors;
    }

    /**
     * Returns the list of latent variables.
     * @return, a list of {@link Variable} objects
     */
    public List<Variable> getLatentVariables() {
        return latentVariables;
    }
}
