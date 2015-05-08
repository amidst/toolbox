/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Random;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public abstract class EF_UnivariateDistribution extends EF_Distribution {

    public abstract double computeLogBaseMeasure(double val);

    public abstract SufficientStatistics getSufficientStatistics(double val);

    public abstract Vector getExpectedParameters();

    public double computeProbabilityOf(double val){
        return Math.exp(this.computeLogProbabilityOf(val));
    }

    public double computeLogProbabilityOf(double val){
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(val)) + this.computeLogBaseMeasure(val) + this.computeLogNormalizer();
    }

    @Override
    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters = parameters;//.copy(parameters);
        this.fixNumericalInstability();
        this.updateMomentFromNaturalParameters();
    }

    public abstract void fixNumericalInstability();

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data){
        return this.getSufficientStatistics(data.getValue(this.getVariable()));
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance){
        return this.computeLogBaseMeasure(dataInstance.getValue(this.getVariable()));
    }

    public abstract EF_UnivariateDistribution deepCopy(Variable variable);

    public EF_UnivariateDistribution deepCopy(){
        return this.deepCopy(this.getVariable());
    }

    public abstract EF_UnivariateDistribution randomInitialization(Random rand);

    public <E extends UnivariateDistribution> E toUnivariateDistribution(){
        throw new UnsupportedOperationException("This EF distribution is not convertible to standard form");
    }
}
