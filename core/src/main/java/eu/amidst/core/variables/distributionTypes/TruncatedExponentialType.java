/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.exponentialfamily.EF_TruncatedExponential;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * This class extends the abstract class {@link DistributionType} and defines the Gamma parameter type.
 */
public class TruncatedExponentialType extends DistributionType {

    /**
     * Creates a new GammaParameterType for the given variable.
     * @param var_ the Variable to which the Gamma distribution will be assigned.
     */
    public TruncatedExponentialType(Variable var_) {
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
        throw new UnsupportedOperationException("Truncated Exponential does not allow standard distributions");
    }

    /**
     * Creates a new exponential family univariate distribution.
     * @return an exponential family Gamma distribution.
     */
    @Override
    public EF_TruncatedExponential newEFUnivariateDistribution() {
        EF_TruncatedExponential ef_TruncatedExponential = new EF_TruncatedExponential(this.variable);
        ef_TruncatedExponential.getNaturalParameters().set(0, 0.1);
        ef_TruncatedExponential.fixNumericalInstability();
        ef_TruncatedExponential.updateMomentFromNaturalParameters();
        return ef_TruncatedExponential;
    }

    /**
     * Creates a new exponential family univariate distribution.
     * @param args, a sequence with the initial natural parameters.
     * @return an exponential family Gamma distribution.
     */
    @Override
    public EF_TruncatedExponential newEFUnivariateDistribution(double... args) {
        EF_TruncatedExponential ef_TruncatedExponential = new EF_TruncatedExponential(this.variable);
        for (double a : args) {
            ef_TruncatedExponential.getNaturalParameters().set(0, a);
        }
        ef_TruncatedExponential.fixNumericalInstability();
        ef_TruncatedExponential.updateMomentFromNaturalParameters();
        return ef_TruncatedExponential;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Truncated Exponential does not allow conditional distributions");
    }
}
