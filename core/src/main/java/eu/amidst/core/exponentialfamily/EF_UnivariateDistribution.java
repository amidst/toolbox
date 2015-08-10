/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class represents univariate distributions in exponential form. It is inherits form EF_ConditionalDistribution
 * because it is assumed to be a special case of a conditional distribution with empty list of conditioning variables.
 *
 * <p> For further details about how exponential family models are considered in this toolbox look at the following paper </p>
 * <p> <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
 * </p>
 */
public abstract class EF_UnivariateDistribution extends EF_ConditionalDistribution{

    /**
     * Computes the logarithm of the base measure function of a given assignment for the associated exponential
     * family distribution.
     *
     * @param val, a double value
     * @return A double value.
     */
    public abstract double computeLogBaseMeasure(double val);

    /**
     * Gets the vector of sufficient statistics for a given assignment.
     * @param val, a double value
     * @return A <code>SufficientStatistics</code> object
     */
    public abstract SufficientStatistics getSufficientStatistics(double val);

    /**
     * Gets the vector of expected parameters of the univariate distribution.
     * @return A <code>Vector</code> object.
     */
    public abstract Vector getExpectedParameters();

    /**
     * This method helps to fix numerical instability problems when invoked.
     */
    public abstract void fixNumericalInstability();

    /**
     * Returns a deep copy the univariate distribution but changing the main variable to the
     * one given as parameter.
     * @param variable, a <code>Variable</code> object.
     * @return A <code>EF_UnivariateDistribution</code> object.
     */
    public abstract EF_UnivariateDistribution deepCopy(Variable variable);

    /**
     * This method randomly initializes a univariate distribution.
     * @param rand, a <code>Random</code> object.
     * @return A <code>EF_UnivariateDistribution</code> object.
     */
    public abstract EF_UnivariateDistribution randomInitialization(Random rand);

    /**
     * This method converts the *this* object to its equivalent UnivariateDistribution object.
     * @param <E> The subtype of distribution of the final created object.
     * @return A <code>UnivariateDistribution</code> object.
     */
    public abstract <E extends UnivariateDistribution> E toUnivariateDistribution();

    /**
     * This method creates a deep copy of the object.
     * @return A <code>EF_UnivariateDistribution</code> object.
     */
    public EF_UnivariateDistribution deepCopy(){
        return this.deepCopy(this.var);
    }

    /**
     * Returns the log probability of a given value according to the associated exponential family distribution.
     * @param val, a double value. In case of finite state space distribution, the double value can be seen as an
     *             integer indexing the different states.
     * @return A positive double value.
     */
    public double computeLogProbabilityOf(double val){
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(val)) + this.computeLogBaseMeasure(val) - this.computeLogNormalizer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Variable> getConditioningVariables() {
        return Arrays.asList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters = parameters;
        this.fixNumericalInstability();
        this.updateMomentFromNaturalParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return this.computeLogBaseMeasure(dataInstance.getValue(this.var));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        return this.getSufficientStatistics(data.getValue(this.var));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("This operation does not make sense for univarite distributons with no parents");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        return this.computeLogNormalizer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        NaturalParameters out = this.createZeroNaturalParameters();
        out.copy(this.getNaturalParameters());
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("This operation does not make sense for univarite distributons with no parents");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ConditionalDistribution> E toConditionalDistribution() {
        return (E)this.toUnivariateDistribution();
    }
}
