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
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;


/**
 * This class extends the abstract class {@link EF_Distribution} and defines the Conditional Distribution in Exponential Family (EF) form.
 *
 * <p> For further details about how exponential family models are designed in this toolbox, take look to the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public abstract class EF_ConditionalDistribution extends EF_Distribution {

    /** Represents the main variable of this EF_ConditionalDistribution. */
    protected Variable var = null;

    /** Represents the list of the conditioning parent variables. */
    protected List<Variable> parents;

    /**
     * Returns the main variable of this EF_ConditionalDistribution.
     * @return a {@link Variable} object.
     */
    public Variable getVariable() {
        return this.var;
    }

    /**
     * Returns the list of conditioning parent variables.
     * @return a list of {@link Variable} objects.
     */
    public List<Variable> getConditioningVariables() {
        return this.parents;
    }



    public double computeLogProbability(Map<Variable,MomentParameters> momentChildCoParents){
        return this.getExpectedNaturalFromParents(momentChildCoParents).dotProduct(momentChildCoParents.get(this.getVariable())) - this.getExpectedLogNormalizer(momentChildCoParents);
    }

    /**
     * Returns the expected value of the log-normalizer for a given parent according to the given moment parameters of
     * the rest of parent variables (i.e., Co-Parents).
     * @param parent the reference parent {@link Variable} object.
     * @param momentChildCoParents a {@code Map} object that maps parent {@link Variable}s to their corresponding {@link MomentParameters} objects.
     * @return a {@code double} value that represents the expected value of the log-normalizer.
     */
    public abstract double getExpectedLogNormalizer(Variable parent, Map<Variable,MomentParameters> momentChildCoParents);

    /**
     * Returns the expected value of the log-normalizer according to the given moment parameters of the parent variables.
     * @param momentParents a {@code Map} object that maps parent {@link Variable}s to their corresponding {@link MomentParameters} objects.
     * @return a {@code double} value that represents the expected value of the log-normalizer.
     */
    public abstract double getExpectedLogNormalizer(Map<Variable,MomentParameters> momentParents);

    /**
     * Returns the expected natural parameter vector given the parent moment parameters.
     * @param momentParents a {@code Map} object that maps parent {@link Variable}s to their corresponding {@link MomentParameters} objects.
     * @return a {@link NaturalParameters} object.
     */
    public abstract NaturalParameters getExpectedNaturalFromParents(Map<Variable,MomentParameters> momentParents);

    /**
     * Returns the expected natural parameter vector for a given parent variable given the moment parameters of the rest of parent
     * variables (i.e., Co-Parents).
     * @param parent the reference parent {@link Variable} object.
     * @param momentChildCoParents a {@code Map} object that maps parent {@link Variable}s to their corresponding {@link MomentParameters} objects.
     * @return a {@link NaturalParameters} object.
     */
    public abstract NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable,MomentParameters> momentChildCoParents);

    /**
     * Converts this EF_ConditionalDistribution to its equivalent {@link ConditionalDistribution} object.
     * @param <E> a subtype of conditional distribution of the final created object.
     * @return a {@link ConditionalDistribution} object.
     */
    public abstract <E extends ConditionalDistribution> E toConditionalDistribution();

    /**
     * Converts this EF_ConditionalDistribution to a {@link ConditionalDistribution} object given a map of expected values.
     * It assumes that the distribution is conditioned to parameter variables, thus its values are replaced with the given expected values.
     * @param expectedValueParameterVariables a {@code Map} object that maps {@link Variable} to their corresponding expected values, i.e. {@link Vector}s.
     * @return a {@link ConditionalDistribution} object.
     */
    public ConditionalDistribution toConditionalDistribution(Map<Variable, Vector> expectedValueParameterVariables){
        throw new UnsupportedOperationException("This transformation does not apply for variables with any parameter variable as parent.");
    }
}
