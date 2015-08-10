/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.Normal;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public class EF_Normal extends EF_UnivariateDistribution {

    public static final int EXPECTED_MEAN = 0;
    public static final int EXPECTED_SQUARE = 1;

    public EF_Normal(Variable var1) {
        if (!var1.isNormal() && !var1.isParameterVariable()) {
            throw new UnsupportedOperationException("Creating a Gaussian EF distribution for a non-gaussian variable.");
        }

        this.parents = new ArrayList();

        this.var=var1;
        this.naturalParameters = this.createZeroedNaturalParameters();
        this.momentParameters = this.createZeroedMomentParameters();

        this.momentParameters.set(EXPECTED_MEAN,0);
        this.momentParameters.set(EXPECTED_SQUARE,1);
        this.setMomentParameters(momentParameters);
    }

    @Override
    public void fixNumericalInstability() {

        if (this.naturalParameters.get(1)<(-0.5*1000000)){ //To avoid numerical problems!
            double x = -0.5*this.naturalParameters.get(0)/this.getNaturalParameters().get(1);
            this.naturalParameters.set(0,x*1000000);
            this.naturalParameters.set(1,-0.5*1000000);
        }
    }

    @Override
    public double computeLogBaseMeasure(double val) {
        return -0.5*Math.log(2 * Math.PI);
    }

    @Override
    public double computeLogNormalizer() {
        double m0=this.momentParameters.get(EXPECTED_MEAN);
        double m1=this.momentParameters.get(EXPECTED_SQUARE);
        return m0*m0/(2*(m1-m0*m0)) + 0.5*Math.log(m1-m0*m0);
    }

    @Override
    public Vector createZeroedVector() {
        return new ArrayVector(2);
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroedSufficientStatistics();
        vec.set(EXPECTED_MEAN, val);
        vec.set(EXPECTED_SQUARE, val * val);
        return vec;
    }

    @Override
    public Vector getExpectedParameters() {
        Vector vec = new ArrayVector(1);
        vec.set(0, this.momentParameters.get(0));
        return vec;
    }

    @Override
    public double computeLogProbabilityOf(double val) {
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(val)) + this.computeLogBaseMeasure(val) - this.computeLogNormalizer();
    }

    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {

        EF_Normal copy = new EF_Normal(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());

        return copy;
    }

    @Override
    public EF_UnivariateDistribution deepCopy() {
        return this.deepCopy(this.var);
    }

    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        double mean = random.nextGaussian()*10;
        double var = random.nextDouble()*10+1;

        this.getNaturalParameters().set(0,mean/(var));
        this.getNaturalParameters().set(1,-1/(2*var));

        this.updateMomentFromNaturalParameters();

        return this;
    }

    @Override
    public Normal toUnivariateDistribution() {

        Normal normal = new Normal(this.getVariable());
        double mean = this.getMomentParameters().get(EF_Normal.EXPECTED_MEAN);
        double sigma = this.getMomentParameters().get(EF_Normal.EXPECTED_SQUARE) - mean * mean;

        normal.setMean(mean);
        normal.setVariance(sigma);

        return normal;
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        double m0=this.momentParameters.get(EXPECTED_MEAN);
        double m1=this.momentParameters.get(EXPECTED_SQUARE);
        // var = E(X^2) - E(X)^2 = m1 - m0*m0
        this.naturalParameters.set(0, m0 / (m1 - m0 * m0));
        this.naturalParameters.set(1, -0.5 / (m1 - m0 * m0));
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        double n0 = this.naturalParameters.get(0);
        double n1 = this.naturalParameters.get(1);
        this.momentParameters.set(EXPECTED_MEAN,-0.5*n0/n1);
        this.momentParameters.set(EXPECTED_SQUARE,-0.5/n1 + 0.25*Math.pow(n0/n1,2));
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
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return this.computeLogBaseMeasure(dataInstance.getValue(this.var));
    }

    @Override
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables) {
        List<EF_ConditionalDistribution> conditionalDistributions = new ArrayList<>();

        Variable varGamma = variables.newGammaParameter(this.var.getName()+"_Gamma_Parameter_"+variables.getNumberOfVars());
        Variable normalMean = variables.newGaussianParameter(this.var.getName() + "_Mean_Parameter_"+variables.getNumberOfVars());

        conditionalDistributions.add(varGamma.getDistributionType().newEFUnivariateDistribution());

        conditionalDistributions.add(normalMean.getDistributionType().newEFUnivariateDistribution());

        EF_NormalGamma dist = new EF_NormalGamma(this.var, normalMean, varGamma);
        conditionalDistributions.add(dist);

        return conditionalDistributions;
    }

  }
