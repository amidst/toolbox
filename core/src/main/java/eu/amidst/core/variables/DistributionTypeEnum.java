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

import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.distributionTypes.*;

/**
 * This class defines the different distribution types supported in AMIDST toolbox.
 */
public enum DistributionTypeEnum {

    /** The multinomial distribution. */
    MULTINOMIAL,

    /** The normal distribution. */
    NORMAL,

    /** The multinomial logistic distribution. */
    MULTINOMIAL_LOGISTIC,

    /** The normal parameter distribution. */
    NORMAL_PARAMETER,

    /** The inverse gamma parameter distribution. */
    INV_GAMMA_PARAMETER,

    /** The gamma parameter distribution. */
    GAMMA_PARAMETER,

    /** The dirichlet distribution. */
    DIRICHLET_PARAMETER,

    /** The normal-gamma parameter distribution. */
    NORMAL_GAMMA_PARAMETER,


    /** The truncated exponential distribution. */
    TRUNCATED_EXPONENTIAL,

    /** The truncated exponential distribution. */
    TRUNCATED_NORMAL,

    /** The sparse multinomial distribution**/
    SPARSE_MULTINOMIAL,

    /** The sparse Dirichlet distribution**/
    SPARSE_DIRICHLET_PARAMETER,

    /** The "indicator" distribution. */
    INDICATOR;

    /**
     * Creates a new distribution type for a given variable.
     * @param var the variable for which the distribution type will be associated.
     * @param <E> the type of elements.
     * @return a distribution type for the variable var.
     */
    public <E extends DistributionType> E newDistributionType(Variable var) {
        switch (this) {
            case MULTINOMIAL:
                return (E) new MultinomialType(var);
            case NORMAL:
                return (E) new NormalType(var);
            case MULTINOMIAL_LOGISTIC:
                return (E) new MultinomialLogisticType(var);
            case NORMAL_PARAMETER:
                return (E) new NormalParameterType(var);
            case INV_GAMMA_PARAMETER:
                return (E) new InverseGammaParameterType(var);
            case GAMMA_PARAMETER:
                return (E) new GammaParameterType(var);
            case DIRICHLET_PARAMETER:
                return (E) new DirichletParameterType(var);
            case NORMAL_GAMMA_PARAMETER:
                return (E) new NormalGammaParameterType(var);
            case TRUNCATED_EXPONENTIAL:
                return (E) new TruncatedExponentialType(var);
            case TRUNCATED_NORMAL:
                return (E) new TruncatedNormalType(var);
            case SPARSE_MULTINOMIAL:
                return (E) new SparseMultinomialType(var);
            case SPARSE_DIRICHLET_PARAMETER:
                return (E) new SparseDirichletParameterType(var);
            case INDICATOR:
                return (E) new IndicatorType(var);
            default:
                throw new IllegalArgumentException("Unknown Distribution Type");
        }
    }

    /**
     * Converts a {@link BaseDistribution_MultinomialParents} distribution to a {@link ConditionalDistribution}
     * @param base a base distribution.
     * @param <E> the type of elements.
     * @return a conditional distribution.
     */
    public static <E extends ConditionalDistribution> E FromBaseDistributionToConditionalDistribution(BaseDistribution_MultinomialParents base) {

            if (base.getBaseDistribution(0) instanceof Multinomial) {
                return (E)new Multinomial_MultinomialParents((BaseDistribution_MultinomialParents<Multinomial>)base);
            }else if (base.getBaseDistribution(0) instanceof Normal && base.getConditioningVariables().size()>0){
                return (E)new Normal_MultinomialParents((BaseDistribution_MultinomialParents<Normal>)base);
            }else  if (base.getBaseDistribution(0) instanceof ConditionalLinearGaussian) {
                return (E)new Normal_MultinomialNormalParents((BaseDistribution_MultinomialParents<ConditionalLinearGaussian>)base);
            }else{
                return (E) base;
            }
    }

}