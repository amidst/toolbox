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
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.exponentialfamily.EF_NormalParameter;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * This class extends the abstract class {@link DistributionType} and defines the Normal parameter type.
 */
public class NormalParameterType  extends DistributionType {

    /**
     * Creates a new NormalParameterType for the given variable.
     * @param var_ the Variable to which the Normal parameter type will be assigned.
     */
    public NormalParameterType(Variable var_) {
        super(var_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParentCompatible(Variable parent) {
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
        normal.setVariance(1e10);
        return normal;
    }

    /**
     * Creates a new exponential family univariate distribution.
     * @return an exponential family normal distribution.
     */
    @Override
    public EF_NormalParameter newEFUnivariateDistribution() {
        EF_NormalParameter ef_normal = new EF_NormalParameter(variable);
        ef_normal.setNaturalWithMeanPrecision(0,1e-10);
        ef_normal.fixNumericalInstability();
        ef_normal.updateMomentFromNaturalParameters();
        return ef_normal;
       //      //return this.newUnivariateDistribution().toEFUnivariateDistribution();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Normal Parameter Type does not allow conditional distributions");
    }
}
