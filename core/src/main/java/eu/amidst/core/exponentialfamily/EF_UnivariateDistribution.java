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
 * This class extends the abstract class {@link EF_ConditionalDistribution} and defines the univariate distribution in exponential family form.
 * It is considered as a special case of a conditional distribution with an empty list of conditioning variables.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public abstract class EF_UnivariateDistribution extends EF_ConditionalDistribution{

    /**
     * Computes the logarithm of the base measure function for a given value.
     * @param val a {@code double} value.
     * @return a {@code double} value.
     */
    public abstract double computeLogBaseMeasure(double val);

    /**
     * Returns the vector of sufficient statistics for a given value.
     * @param val a {@code double} value.
     * @return a {@link SufficientStatistics} object.
     */
    public abstract SufficientStatistics getSufficientStatistics(double val);

    /**
     * Returns the vector of expected parameters of this EF_UnivariateDistribution.
     * @return a {@code Vector} object.
     */
    public abstract Vector getExpectedParameters();

    /**
     * Fixes the numerical instability problems when invoked in this EF_UnivariateDistribution.
     */
    public abstract void fixNumericalInstability();

    /**
     * Returns a deep copy of this EF_UnivariateDistribution and changes the current main variable to the
     * one given as input parameter.
     * @param variable a {@link Variable} object.
     * @return a {@link EF_UnivariateDistribution} object.
     */
    public abstract EF_UnivariateDistribution deepCopy(Variable variable);

    /**
     * Randomly initializes this EF_UnivariateDistribution.
     * @param rand a {@link Random} object.
     * @return A {@link EF_UnivariateDistribution} object.
     */
    public abstract EF_UnivariateDistribution randomInitialization(Random rand);

    /**
     * Converts this EF_UnivariateDistribution to its equivalent {@link UnivariateDistribution} object.
     * @param <E> the subtype of distribution of the final created object.
     * @return a {@link UnivariateDistribution} object.
     */
    public abstract <E extends UnivariateDistribution> E toUnivariateDistribution();

    /**
     * Creates a deep copy of this this EF_UnivariateDistribution.
     * @return a {@link EF_UnivariateDistribution} object.
     */
    public EF_UnivariateDistribution deepCopy(){
        return this.deepCopy(this.var);
    }

    /**
     * Returns the log probability of a given value according to this EF_UnivariateDistribution.
     * @param val a {@code double} value. In case of finite state space distribution, the double value can be seen as an
     *             integer indexing the different states.
     * @return a positive {@code double} that represents the log probability value.
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

    /**
     * Compute the kullback-leibler distance between two distributions of the same
     * kind in exponential family form, KL(P,Q), where P is the object invoking the method
     * and Q is provided by argument.
     * @param naturalParameters, the natural parameter of the Q distribution.
     * @param logNormalizer, the logNormalizer of the Q distribution.
     * @return A positive double value
     */
    public double  kl(NaturalParameters naturalParameters, double logNormalizer){
        double kl = 0;
        NaturalParameters copy = this.createZeroNaturalParameters();
        copy.copy(this.naturalParameters);
        copy.substract(naturalParameters);
        kl+=copy.dotProduct(momentParameters);
        kl-=this.computeLogNormalizer();
        kl+=logNormalizer;

        if (Double.isNaN(kl)){
            throw new IllegalStateException("NaN KL");
        }

        if (kl<0) {
            kl=0;
        }


        return kl;
    }

}
