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
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * This class extends the abstract class {@link ConditionalDistribution}.
 * It defines and handles the set of univariate distributions.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public abstract class UnivariateDistribution extends ConditionalDistribution {

    private static final long serialVersionUID = -5983759861664273449L;

    /**
     * Returns the log probability for a given input value.
     * @param value a double value.
     * @return the log probability for this value.
     */
    public abstract double getLogProbability(double value);

    /**
     * Returns the probability for a given input value.
     * @param value a double value.
     * @return the probability for this value.
     */
    public double getProbability(double value) {
        return Math.exp(this.getLogProbability(value));
    }

    /**
     * Returns a randomly sampled double value.
     * @param rand a {@link java.util.Random} object.
     * @return a randomly sampled double value.
     */
    public abstract double sample(Random rand);

    /**
     * Converts this UnivariateDistribution to an Exponential Family (EF) univariate distribution.
     * @param <E> any class extending {@link EF_UnivariateDistribution}
     * @return an {@link EF_UnivariateDistribution} object.
     * @exception UnsupportedOperationException if this distribution is not convertible to the EF form.
     */
    public abstract <E extends EF_UnivariateDistribution> E toEFUnivariateDistribution();

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Variable> getConditioningVariables() {
        return Arrays.asList();
    }

    /**
     * Converts this UnivariateDistribution to an Exponential Family (EF) conditional distribution.
     * @return an {@link EF_ConditionalDistribution} object.
     * @exception UnsupportedOperationException if this distribution is not convertible to the EF form.
     */
    public <E extends EF_ConditionalDistribution> E toEFConditionalDistribution(){
        return (E)this.toEFUnivariateDistribution();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        return this.getLogProbability(assignment.getValue(this.var));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogProbability(Assignment assignment) {
        return this.getLogProbability(assignment.getValue(this.var));
    }

    /**
     * Returns a deep copy of this UnivariateDistribution and changes the current main variable to the
     * one given as input parameter.
     * @param variable a {@link Variable} object.
     * @return a {@link UnivariateDistribution} object.
     */
    public abstract UnivariateDistribution deepCopy(Variable variable);


}