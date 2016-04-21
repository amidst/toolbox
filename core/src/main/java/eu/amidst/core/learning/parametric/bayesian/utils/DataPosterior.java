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


import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class stores the posterior probabilities over a set of latent variables for an
 * item with a given id. It can be used to store the result of an inference/learning operation.
 */
public class DataPosterior implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    /** Represents the id of item */
    final long id;

    /** Represent a map between variables and UnivariateDistributions*/
    final Map<Variable,UnivariateDistribution> map;
    /**
     * Creates a new object using a given id_ and a list of univariate posteriors.
     * @param id_, a unique id.
     * @param posteriors_, a list of {@link UnivariateDistribution} distributions.
     */
    public DataPosterior(long id_, List<UnivariateDistribution> posteriors_){
        this.id=id_;
        map = new HashMap<>();
        for (UnivariateDistribution univariateDistribution : posteriors_) {
            map.put(univariateDistribution.getVariable(), univariateDistribution);
        }
    }

    /**
     * Returns the id of the item
     * @return a integer value
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the posterior of a given variable.
     * @param var, a {@link Variable} object
     * @return A {@link UnivariateDistribution} object
     */
    public UnivariateDistribution getPosterior(Variable var) {
        return map.get(var);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        StringBuilder string = new StringBuilder();
        string.append("ID: "+ id + " | ");
        for (Map.Entry<Variable, UnivariateDistribution> entry : map.entrySet()) {
            string.append(entry.getKey().getName()+", ");
            string.append(" |  ");
            string.append(entry.getValue().toString()+", ");
        }

        return string.toString();
    }

    /**
     * Returns whethers a given variable is present in the object.
     * @param var, a {@link Variable} object.
     * @return A boolean value.
     */
    public boolean isPresent(Variable var){
        return this.map.containsKey(var);
    }

}
