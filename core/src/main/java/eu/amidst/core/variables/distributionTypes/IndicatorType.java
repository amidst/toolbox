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
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.exponentialfamily.EF_Multinomial;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * This class extends the abstract class {@link DistributionType} and defines the Indicator type.
 */
public class IndicatorType extends DistributionType {

    private double deltaValue = 0.0;

    /**
     * Creates a new IndicatorType for the given variable.
     * @param var_ the Variable to which the "Indicator" distribution will be assigned.
     */
    public IndicatorType(Variable var_) {
        super(var_);
    }

    public double getDeltaValue() {
        return deltaValue;
    }

    public void setDeltaValue(double deltaValue) {
        this.deltaValue = deltaValue;
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
    public Multinomial newUnivariateDistribution() {
        return new Multinomial(variable);
    }

    /**
     * Creates a new exponential family univariate distribution.
     * @return an exponential family Indicator distribution.
     */
    @Override
    public EF_Multinomial newEFUnivariateDistribution() {
        Multinomial multinomial = new Multinomial(variable);
        return multinomial.toEFUnivariateDistribution();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        //throw new UnsupportedOperationException("Indicator Type does not allow conditional distributions");
        return (E) new Multinomial(this.variable);
    }
}
