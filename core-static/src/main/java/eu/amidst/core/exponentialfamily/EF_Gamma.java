/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 23/02/15.
 */
public class EF_Gamma extends EF_UnivariateDistribution {

    public static final int LOGX = 0;
    public static final int INVX = 1;
    public static final double DELTA = 0.0001;

    public EF_Gamma(Variable var1) {

        if (!var1.isGammaParameter())
            throw new IllegalArgumentException("The variable is not Gamma parameter");

        this.parents = new ArrayList();

        this.var = var1;
        this.naturalParameters = this.createZeroedNaturalParameters();
        this.momentParameters = this.createZeroedMomentParameters();
        double alpha = 1;
        double beta = 1;
        this.naturalParameters.set(0, alpha -1 );
        this.naturalParameters.set(1, -beta);
        this.setNaturalParameters(naturalParameters);
    }

    @Override
    public double computeLogBaseMeasure(double val) {
        return 0;
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroedSufficientStatistics();
        vec.set(LOGX, Math.log(val));
        vec.set(INVX, val);
        return vec;
    }

    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {
        EF_Gamma copy = new EF_Gamma(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());

        return copy;
    }

    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        double randomVar = random.nextDouble()*2 + 1 + 0.01;

        double alpha = 10;
        double beta = randomVar*alpha ;

        this.getNaturalParameters().set(0, alpha - 1);
        this.getNaturalParameters().set(1, -beta);

        this.updateMomentFromNaturalParameters();

        return this;
    }

    @Override
    public <E extends UnivariateDistribution> E toUnivariateDistribution() {
        throw new UnsupportedOperationException("Gamma is not included yet in the Distributions package.");
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        double alpha = this.naturalParameters.get(0) + 1;
        double beta = -this.naturalParameters.get(1);
        this.momentParameters.set(0, Gamma.digamma(alpha) - Math.log(beta));
        this.momentParameters.set(1, alpha / beta);
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        return this.getSufficientStatistics(data.getValue(this.var));
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return 2;
    }



    @Override
    public double computeLogNormalizer() {
        double alpha = this.naturalParameters.get(0) + 1;
        double beta = -this.naturalParameters.get(1);
        return Gamma.logGamma(alpha) - alpha * Math.log(beta);
    }

    @Override
    public Vector createZeroedVector() {
        return new ArrayVector(2);
    }


    @Override
    public Vector getExpectedParameters() {

        Vector vec = new ArrayVector(1);
        vec.set(0, -(this.naturalParameters.get(0)+1)/this.naturalParameters.get(1));
        return vec;
    }

    @Override
    public void fixNumericalInstability() {

    }


}