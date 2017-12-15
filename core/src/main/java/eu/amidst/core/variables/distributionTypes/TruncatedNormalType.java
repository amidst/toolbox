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
import eu.amidst.core.exponentialfamily.EF_TruncatedNormal;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * This class extends the abstract class {@link DistributionType} and defines the Normal distribution.
 */
public class TruncatedNormalType extends DistributionType{

    /**
     * Creates a new NormalParameterType for the given variable.
     * @param variable the Variable to which the Normal distribution type will be assigned.
     */
    public TruncatedNormalType(Variable variable){
        super(variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParentCompatible(Variable parent) { return false; }

    /**
     * Creates a new univariate distribution.
     * @return a normal distribution for this Variable.
     */
    @Override
    public Normal newUnivariateDistribution() {
        throw new UnsupportedOperationException("Truncated Normal does not allow standard distributions");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        throw new UnsupportedOperationException("Truncated Normal does not allow standard distributions");

    }

    /**
     * Creates a new exponential family univariate distribution.
     * @return an exponential family Truncated Normal distribution.
     */
    @Override
    public EF_TruncatedNormal newEFUnivariateDistribution() {
        EF_TruncatedNormal ef_TruncatedNormal = new EF_TruncatedNormal(this.variable);
        //Standard Normal(0,1) --> natural parameters (0,-0.5)
        ef_TruncatedNormal.setNaturalWithMeanPrecision(0,1);
        ef_TruncatedNormal.fixNumericalInstability();
        ef_TruncatedNormal.updateMomentFromNaturalParameters();
        return ef_TruncatedNormal;
    }

    /**
     * Creates a new exponential family univariate distribution.
     * @param args, a sequence with the initial natural parameters.
     * @return an exponential family Truncated Normal distribution.
     */
    @Override
    public EF_TruncatedNormal newEFUnivariateDistribution(double... args) {
        EF_TruncatedNormal ef_TruncatedNormal = new EF_TruncatedNormal(this.variable);
        int i = 0;
        double[] val = new double[2];
        for (double a : args) {
            val[i++]=a;
        }
        ef_TruncatedNormal.setNaturalWithMeanPrecision(val[0],val[1]);
        ef_TruncatedNormal.fixNumericalInstability();
        ef_TruncatedNormal.updateMomentFromNaturalParameters();
        return ef_TruncatedNormal;
    }
}
