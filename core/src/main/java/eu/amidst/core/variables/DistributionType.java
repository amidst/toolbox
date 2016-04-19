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

package eu.amidst.core.variables;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;

import java.io.Serializable;
import java.util.List;

/**
 * This class handles the different distribution types supported in AMIDST.
 */
public abstract class DistributionType  implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4158293895929418259L;

    /** Represents the variable. */
    protected Variable variable;

    /**
     * Creates a new DistributionType for the given variable.
     * @param var_ the variable.
     */
    public DistributionType(Variable var_){
        this.variable=var_;
    }

    /**
     * Test whether the given parent is compatible or not.
     * @param parent the parent variable.
     * @return true if the parent is compatible, false otherwise.
     */
    public abstract boolean isParentCompatible(Variable parent);

    /**
     * Tests whether the given parents are compatible or not.
     * @param parents the list of parent variables.
     * @return true if all parents are compatible, false otherwise.
     */
    public boolean areParentsCompatible(List<Variable> parents){
        for(Variable parent: parents){
            if (!isParentCompatible(parent))
                return false;
        }
        return true;
    }

    /**
     * Creates a new univariate distribution.
     * @param <E> the type of elements.
     * @return a univariate distribution.
     */
    public abstract <E extends UnivariateDistribution> E newUnivariateDistribution();

    /**
     * Creates a new exponential family univariate distribution.
     * @param <E> the type of elements.
     * @return an exponential family univariate distribution.
     */
    public <E extends EF_UnivariateDistribution> E newEFUnivariateDistribution(){
        return this.newUnivariateDistribution().toEFUnivariateDistribution();
    }


    /**
     * Creates a new exponential family univariate distribution.
     * @param <E> the type of elements.
     * @return an exponential family univariate distribution.
     */

    /**
     * Creates a new exponential family univariate distribution with initial natural parameters
     * @param args, a sequence with the initial natural parameters.
     * @param <E> the type of elements.
     * @return
     */
    public <E extends EF_UnivariateDistribution> E newEFUnivariateDistribution(double... args) {
        return newEFUnivariateDistribution();
    }

    /**
     * Creates a new conditional distribution given the input list of parents.
     * @param parents the list of parents.
     * @param <E> the type of elements.
     * @return a conditional distribution.
     */
    public abstract <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents);

    /**
     * Creates a new exponential family conditional distribution given the input list of parents.
     * @param parents the list of parents.
     * @param <E> the type of elements.
     * @return an exponential family conditional distribution.
     */
    public <E extends EF_ConditionalDistribution> E newEFConditionalDistribution(List<Variable> parents){
        return this.newConditionalDistribution(parents).toEFConditionalDistribution();
    }

    /**
     * Tests whether the list of parents contains this DistributionType.
     * @param parents the list of parent variables.
     * @param distributionTypeEnum the DistributionType.
     * @return true if the list of parents contains this DistributionType, false otherwise.
     */
    public static boolean containsParentsThisDistributionType(List<Variable> parents, DistributionTypeEnum distributionTypeEnum){
        for (Variable var : parents){
            if (var.getDistributionTypeEnum()==distributionTypeEnum)
                return true;
        }
        return false;
    }

}

