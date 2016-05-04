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

package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_BaseDistribution_MultinomialParents;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_SparseMultinomial;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class extends the abstract class {@link DistributionType} and defines the SparseMultinomial parameter type.
 */
public class SparseMultinomialType extends DistributionType {

    /**
     * Creates a new SparseMultinomialType for the given variable.
     * @param var_ the Variable to which the Gamma distribution will be assigned.
     */
    public SparseMultinomialType(Variable var_) {
        super(var_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParentCompatible(Variable parent) {
        if (parent.getDistributionTypeEnum()== DistributionTypeEnum.MULTINOMIAL)
            return true;
        else
            return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateDistribution newUnivariateDistribution() {
        throw new UnsupportedOperationException("SparseMultinomial Parameter Type does not allow standard distributions");
    }

    /**
     * Creates a new exponential family univariate distribution.
     * @return an exponential family Gamma distribution.
     */
    @Override
    public EF_SparseMultinomial newEFUnivariateDistribution() {
        return new EF_SparseMultinomial(this.variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Sparse Multinomial Parameter Type does not allow conditional distributions");
    }

    /**
     * Creates a new exponential family conditional distribution given the input list of parents.
     * @param parents the list of parents.
     * @param <E> the type of elements.
     * @return an exponential family conditional distribution.
     */
    public <E extends EF_ConditionalDistribution> E newEFConditionalDistribution(List<Variable> parents){
        if (!this.areParentsCompatible(parents))
            throw new IllegalArgumentException("Parents are not compatible");

        if (parents.isEmpty())
            return (E)new EF_SparseMultinomial(this.variable);
        else {
            List<EF_SparseMultinomial> base_ef_dists = new ArrayList<>();

            int size = MultinomialIndex.getNumberOfPossibleAssignments(parents);

            for (int i = 0; i < size; i++) {
                base_ef_dists.add(this.variable.getDistributionType().newEFUnivariateDistribution());
            }

            return (E) new EF_BaseDistribution_MultinomialParents<EF_SparseMultinomial>(parents, base_ef_dists);
        }
    }

}
