/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.variables.Assignment;

import java.util.Random;

/**
 * <h2>This interface generalizes the set of univariate distributions.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-3
 */
public abstract class UnivariateDistribution extends Distribution {

    public UnivariateDistribution() {
    }

    /**
     * Evaluates the distribution in a given point.
     *
     * @param value The point to be evaluated.
     * @return A <code>double</code> value with the evaluated distribution.
     */
    public double getProbability(double value) {
        return Math.exp(this.getLogProbability(value));
    }

    @Override
    public double getLogProbability(Assignment assignment) {
        return this.getLogProbability(assignment.getValue(this.var));
    }

    /**
     * Evaluates the distribution in a given point.
     *
     * @param value The point to be evaluated.
     * @return A <code>double</code> value with the logarithm of the evaluated distribution.
     */
    public abstract double getLogProbability(double value);

    public abstract double sample(Random rand);

    public <E extends EF_UnivariateDistribution> E toEFUnivariateDistribution() {
            throw new UnsupportedOperationException("This distribution is not convertible to EF form");
    }
}