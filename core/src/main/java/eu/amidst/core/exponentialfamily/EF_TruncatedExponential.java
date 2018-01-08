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

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by andresmasegosa on 14/4/16.
 */
public class EF_TruncatedExponential extends EF_TruncatedUnivariateDistribution {


    /**
     * Creates a new EF_TruncatedExponential distribution, for a given {@link Variable} object.
     * @param var1 a {@link Variable} object with a Gamma distribution type.
     */
    public EF_TruncatedExponential(Variable var1) {

        this.parents = new ArrayList();

        this.var = var1;
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();
        this.naturalParameters.set(0, 0.1 );
        this.setNaturalParameters(naturalParameters);
    }

    /**
     * Creates a new EF_TruncatedExponential distribution, for a given {@link Variable} object.
     * @param var1 a {@link Variable} object with a Gamma distribution type.
     * @param initialDelta the initial delta value of the truncated exponential.
     */
    public EF_TruncatedExponential(Variable var1, double initialDelta) {

        this.parents = new ArrayList();

        this.var = var1;
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();
        this.naturalParameters.set(0, initialDelta);
        this.setNaturalParameters(naturalParameters);
    }


    @Override
    public double computeLogBaseMeasure(double val) {
        return 0;
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics sufficientStatistics = this.createZeroSufficientStatistics();
        sufficientStatistics.set(0,val);
        return sufficientStatistics;
    }

    @Override
    public Vector getExpectedParameters() {
        return this.momentParameters;
    }

    @Override
    public void fixNumericalInstability() {

    }

    @Override
    public EF_UnivariateDistribution deepCopy(Variable variable) {
        EF_TruncatedExponential copy = new EF_TruncatedExponential(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());
        return copy;
    }

    @Override
    public EF_UnivariateDistribution randomInitialization(Random rand) {

        double randomDelta = rand.nextGaussian()*rand.nextInt(100);

        this.getNaturalParameters().set(0, randomDelta);
        this.fixNumericalInstability();
        this.updateMomentFromNaturalParameters();

        return this;
    }

    @Override
    public <E extends UnivariateDistribution> E toUnivariateDistribution() {
        throw new UnsupportedOperationException("TruncatedExponential is not included yet in the Distributions package.");
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("Not Implemented");
    }


    @Override
    public void updateMomentFromNaturalParameters() {
        double delta =this.getNaturalParameters().get(0);
        double width = (this.upperInterval - this.lowerInterval);

        if (Math.exp(width*delta)>Double.MAX_VALUE) {
            this.momentParameters.set(0, this.upperInterval - 1/delta);
        } else {

            double val = this.upperInterval -
                    width/(1 - Math.exp(delta*width))
                    - 1/delta;

            this.momentParameters.set(0, val);

//            this.momentParameters.set(0, Math.exp(delta) / (Math.exp(delta) - 1) - 1 / delta);
        }
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 1;
    }

    @Override
    public double computeLogNormalizer() {
        double delta =this.getNaturalParameters().get(0);

        //return Mt(Math.exp(delta*this.upperInterval)/this.upperInterval - Math.exp(delta*this.lowerInterval)/this.lowerInterval)

        if (delta>100){
            return this.upperInterval*delta - Math.log(delta);
        }else {
            return Math.log((Math.exp(delta * this.upperInterval) - Math.exp(delta * this.lowerInterval)) / delta);
        }

//        return Math.log((Math.exp(delta)-1)/delta);

    }

    @Override
    public Vector createZeroVector() {
        return new ArrayVector(1);
    }

    @Override
    public SufficientStatistics createInitSufficientStatistics() {
        SufficientStatistics sufficientStatistics = this.createZeroSufficientStatistics();
        sufficientStatistics.set(0,0.1);
        return sufficientStatistics;
    }
}
