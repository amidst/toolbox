/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.distribution;


import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * <h2>This interface generalizes the set of possible conditional distributions.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-3
 */
public abstract class ConditionalDistribution extends Distribution {

    /**
     * The list of parents of the variable
     */
    protected List<Variable> parents;

    /**
     * Gets the set of conditioning variables
     *
     * @return An <code>unmodifiable List</code> object with the set of conditioning variables.
     */
    public List<Variable> getConditioningVariables() {
        return this.parents;
    }

    /**
     * Evaluates the conditional distribution given a value of the variable and an assignment of the parents.
     *
     * @param assignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> value with the evaluated distribution.
     */
    public double getConditionalProbability(Assignment assignment) {
        return Math.exp(this.getLogConditionalProbability(assignment));
    }

    /**
     * Evaluates the conditional distribution given a value of the variable and an assignment of the parents.
     *
     * @param assignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> value with the logarithm of the evaluated distribution.
     */
    public abstract double getLogConditionalProbability(Assignment assignment);

    public abstract UnivariateDistribution getUnivariateDistribution(Assignment assignment);


    @Override
    public double getLogProbability(Assignment assignment){
        return this.getLogConditionalProbability(assignment);
    }

    public <E extends EF_ConditionalDistribution> E toEFConditionalDistribution(){
        throw new UnsupportedOperationException("This distribution is not convertible to EF form");
    }
}