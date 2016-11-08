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

import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * This class extends the abstract class {@link DistributionType} and defines the Normal distribution.
 */
public class NormalType extends DistributionType{

    /**
     * Creates a new NormalParameterType for the given variable.
     * @param variable the Variable to which the Normal distribution type will be assigned.
     */
    public NormalType(Variable variable){
        super(variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParentCompatible(Variable parent) {
        if (parent.getDistributionTypeEnum()==DistributionTypeEnum.MULTINOMIAL ||
                parent.getDistributionTypeEnum()==DistributionTypeEnum.MULTINOMIAL_LOGISTIC ||
                parent.getDistributionTypeEnum()==DistributionTypeEnum.NORMAL||
                parent.getDistributionTypeEnum()==DistributionTypeEnum.INDICATOR)
            return true;
        else
            return false;
    }

    /**
     * Creates a new univariate distribution.
     * @return a normal distribution for this Variable.
     */
    @Override
    public Normal newUnivariateDistribution() {
        Normal normal = new Normal(variable);
        normal.setMean(0);
        normal.setVariance(1);

        return normal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        if (!this.areParentsCompatible(parents))
            throw new IllegalArgumentException("Parents are not compatible");

        boolean multinomialParents = false;
        boolean normalParents = false;
        for (Variable v : parents) {
            //TODO MultinomialLogist as parent
            if (v.getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
                multinomialParents = true;
            } else if (v.isNormal()) {
                normalParents = true;
            }
        }

        if (!multinomialParents && !normalParents){
            return (E) new Normal(this.variable);
        } else if (multinomialParents && !normalParents) {
            return (E)new Normal_MultinomialParents(this.variable, parents);
        } else if (!multinomialParents && normalParents) {
            return (E)new ConditionalLinearGaussian(this.variable, parents);
        } else if (multinomialParents && normalParents) {
            return (E)new Normal_MultinomialNormalParents(this.variable, parents);
        } else{
            return null;
        }
    }
}
