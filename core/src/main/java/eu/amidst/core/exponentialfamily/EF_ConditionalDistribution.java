/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;


/**
 * This class represents a conditional distribution in exponential family form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox look at the following paper </p>
 * <p> <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
 * </p>
 *
 */
public abstract class EF_ConditionalDistribution extends EF_Distribution {

    /** The main variable of the conditional distribution*/
    protected Variable var = null;

    /** The list of the conditioning/parents variables*/
    protected List<Variable> parents;

    /**
     * Gets the main variable of the conditional distribution
     *
     * @return A <code>Variable</code> object.
     */
    public Variable getVariable() {
        return this.var;
    }

    /**
     * Returns the list of conditioning/parents variables.
     * @return A list of <code>Variable</code> objects
     */
    public List<Variable> getConditioningVariables() {
        return this.parents;
    }

    /**
     * Returns the expected value of the log-normalizer for a given parent according to the given moment parameters of
     * the rest of parent variables.
     * @param parent, the reference parent. A <code>Variable</code> object.
     * @param momentChildCoParents, a map of <code>Variable</code> and <code>MomentParameters</code> objects.
     * @return A double value.
     */
    public abstract double getExpectedLogNormalizer(Variable parent, Map<Variable,MomentParameters> momentChildCoParents);

    /**
     * Returns the expected value of the log-normalizer according to the given moment parameters of the parent variables.
     * @param momentParents, a map of <code>Variable</code> and <code>MomentParameters</code> objects.
     * @return A double value.
     */
    public abstract double getExpectedLogNormalizer(Map<Variable,MomentParameters> momentParents);

    /**
     * Returns the expected natural parameter vector given the parent's moment parameters.
     * @param momentParents, a map of <code>Variable</code> and <code>MomentParameters</code> objects.
     * @return A <code>NaturalParameters</code> object.
     */
    public abstract NaturalParameters getExpectedNaturalFromParents(Map<Variable,MomentParameters> momentParents);

    /**
     * Returns the expected natural parameter vector for a given parent given the moment parameters of the rest of parent
     * variables.
     * @param parent, the reference parent. A <code>Variable</code> object.
     * @param momentChildCoParents, a map of <code>Variable</code> and <code>MomentParameters</code> objects.
     * @return A <code>NaturalParameters</code> object.
     */
    public abstract NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable,MomentParameters> momentChildCoParents);

    /**
     * This method converts the *this* object to its equivalent ConditionalDistribution object.
     * @param <E> The subtype of distribution of the final created object.
     * @return A <code>ConditionalDistribution</code> object.
     */
    public abstract <E extends ConditionalDistribution> E toConditionalDistribution();

    /**
     * This method creates a new ConditionalDistribution object from the *this* object. This method assumest that the
     * distribution is conditioned to parameter variables. Its values are replaced with their supplied expected values.
     * @param expectedValueParameterVariables, a map of <code>Variable</code> and <code>Vector</code> objects.
     * @return A <code>ConditionalDistribution</code> object.
     */
    public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedValueParameterVariables){
        throw new UnsupportedOperationException("This transformation does not make sense for variables with any parameter variable as parent.");
    }
}
