/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
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
 * Created by andresmasegosa on 12/11/14.
 */
public abstract class EF_UnivariateDistribution extends EF_ConditionalDistribution{

    public abstract double computeLogBaseMeasure(double val);

    public abstract SufficientStatistics getSufficientStatistics(double val);

    public abstract Vector getExpectedParameters();

    public abstract void fixNumericalInstability();

    public abstract EF_UnivariateDistribution deepCopy(Variable variable);

    public abstract EF_UnivariateDistribution randomInitialization(Random rand);

    public abstract <E extends UnivariateDistribution> E toUnivariateDistribution();

    @Override
    public List<Variable> getConditioningVariables() {
        return Arrays.asList();
    }

    @Override
    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters = parameters;
        this.fixNumericalInstability();
        this.updateMomentFromNaturalParameters();
    }

    public EF_UnivariateDistribution deepCopy(){
        return this.deepCopy(this.var);
    }

    public double computeLogProbabilityOf(double val){
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(val)) + this.computeLogBaseMeasure(val) - this.computeLogNormalizer();
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return this.computeLogBaseMeasure(dataInstance.getValue(this.var));
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        return this.getSufficientStatistics(data.getValue(this.var));
    }

    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("This operation does not make sense for univarite distributons with no parents");
    }

    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        return this.computeLogNormalizer();
    }

    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        NaturalParameters out = this.createZeroedNaturalParameters();
        out.copy(this.getNaturalParameters());
        return out;
    }

    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("This operation does not make sense for univarite distributons with no parents");
    }

    @Override
    public <E extends ConditionalDistribution> E toConditionalDistribution() {
        return (E)this.toUnivariateDistribution();
    }
}
