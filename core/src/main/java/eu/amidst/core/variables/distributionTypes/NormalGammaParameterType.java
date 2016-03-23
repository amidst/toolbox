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
import eu.amidst.core.exponentialfamily.EF_Gamma;
import eu.amidst.core.exponentialfamily.EF_JointNormalGamma;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * This class extends the abstract class {@link DistributionType} and defines the Gamma parameter type.
 */
public class NormalGammaParameterType extends DistributionType {

    /**
     * Creates a new GammaParameterType for the given variable.
     * @param var_ the Variable to which the Gamma distribution will be assigned.
     */
    public NormalGammaParameterType(Variable var_) {
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
     * {@inheritDoc}
     */
    @Override
    public Normal newUnivariateDistribution() {
        throw new UnsupportedOperationException("NormalGamma Parameter Type does not allow standard distributions");
    }

    /**
     * Creates a new exponential family univariate distribution.
     * @return an exponential family Gamma distribution.
     */
    @Override
    public EF_JointNormalGamma newEFUnivariateDistribution() {
        EF_JointNormalGamma gamma = new EF_JointNormalGamma(this.variable);
        double alpha = 1;
        double beta = 1;
        double mean = 0;
        double precision = 1e-10;

        gamma.getNaturalParameters().set(EF_JointNormalGamma.MU_GAMMA, mean*precision);
        gamma.getNaturalParameters().set(EF_JointNormalGamma.MUSQUARE_GAMMA, -0.5*precision);
        gamma.getNaturalParameters().set(EF_JointNormalGamma.GAMMA, -beta-0.5*precision*mean*mean);
        gamma.getNaturalParameters().set(EF_JointNormalGamma.LOGGAMMA, alpha-0.5);

        gamma.fixNumericalInstability();

        gamma.updateMomentFromNaturalParameters();

        return gamma;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Inverse Gamma Parameter Type does not allow conditional distributions");
    }
}
