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

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * This class extends the abstract class {@link Distribution}.
 * It defines the conditional distribution of a node (i.e., {@link Variable}) given the set of its parents.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 */
public abstract class ConditionalDistribution extends Distribution {

    /** Represents the list of parents of this Variable. */
    protected List<Variable> parents;

    /**
     * Returns the set of conditioning variables, i.e., the list of parents in this ConditionalDistribution.
     * @return a {@code List} object containing the set of parents.
     */
    public List<Variable> getConditioningVariables() {
        return this.parents;
    }


    /**
     * Sets the set of conditioning variables, i.e., the list of parents in this ConditionalDistribution.
     * WARNING: This method should only be used in exceptional cases. It may affect the coherence of
     * the graphical model.
     * @param parents a {@link List} of {@link Variable} representing the parents of this variable.
     */
    public void setConditioningVariables(List<Variable> parents){
        this.parents=parents;
    }

    /**
     * Returns the conditional probability of an {@link Assignment} given this ConditionalDistribution.
     * @param assignment an {@link Assignment} object.
     * @return a double representing the probability of an assignment.
     */
    public double getConditionalProbability(Assignment assignment) {
        return Math.exp(this.getLogConditionalProbability(assignment));
    }

    /**
     * Returns the log conditional probability of an {@link Assignment} given this ConditionalDistribution.
     * @param assignment an {@link Assignment} object.
     * @return a double representing the log probability of an assignment.
     */
    public abstract double getLogConditionalProbability(Assignment assignment);

    /**
     * Returns the univariate distribution of an {@link Assignment} given this ConditionalDistribution.
     * @param assignment an {@link Assignment} object.
     * @return an {@link UnivariateDistribution} object.
     */
    public abstract UnivariateDistribution getUnivariateDistribution(Assignment assignment);

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogProbability(Assignment assignment){
        return this.getLogConditionalProbability(assignment);
    }

    /**
     * Converts this ConditionalDistribution to an Exponential Family (EF) conditional distribution
     * @param <E> any class extending {@link EF_ConditionalDistribution}
     * @return an {@link EF_ConditionalDistribution} object.
     * @exception UnsupportedOperationException if this distribution is not convertible to EF form.
     */
    public <E extends EF_ConditionalDistribution> E toEFConditionalDistribution(){
        throw new UnsupportedOperationException("This distribution is not convertible to EF form");
    }
}