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
 *
 * This class extends the abstract class {@link EF_UnivariateDistribution} and defines a Normal distribution in exponential family canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_Normal extends EF_UnivariateDistribution {

    public static final int EXPECTED_MEAN = 0;
    public static final int EXPECTED_SQUARE = 1;

    private static final double LIMIT = 1000000;
    /**
     * Creates a new EF_Normal distribution for a given variable.
     * @param var1 a {@link Variable} object with a Normal distribution type.
     */
    public EF_Normal(Variable var1) {
        if (!var1.isNormal() && !var1.isParameterVariable()) {
            throw new UnsupportedOperationException("Creating a Gaussian EF distribution for a non-gaussian variable.");
        }

        this.parents = new ArrayList();

        this.var=var1;
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();

        this.momentParameters.set(EXPECTED_MEAN,0);
        this.momentParameters.set(EXPECTED_SQUARE,1);
        this.setMomentParameters(momentParameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fixNumericalInstability() {

        if (this.naturalParameters.get(1)<(-0.5*LIMIT)){ //To avoid numerical problems!
            double x = -0.5*this.naturalParameters.get(0)/this.getNaturalParameters().get(1);
            this.naturalParameters.set(0,x*LIMIT);
            this.naturalParameters.set(1,-0.5*LIMIT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(double val) {
        return -0.5*Math.log(2 * Math.PI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        double m0=this.momentParameters.get(EXPECTED_MEAN);
        double m1=this.momentParameters.get(EXPECTED_SQUARE);
        double variance =m1-m0*m0;
        if (variance < 1/LIMIT) variance = 1/LIMIT;

        return m0*m0/(2*(variance)) + 0.5*Math.log(variance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        return new ArrayVector(2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroSufficientStatistics();
        vec.set(EXPECTED_MEAN, val);
        vec.set(EXPECTED_SQUARE, val * val);
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector getExpectedParameters() {
        Vector vec = new ArrayVector(1);
        vec.set(0, this.momentParameters.get(0));
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogProbabilityOf(double val) {
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(val)) + this.computeLogBaseMeasure(val) - this.computeLogNormalizer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {

        EF_Normal copy = new EF_Normal(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());

        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy() {
        return this.deepCopy(this.var);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        double mean = random.nextGaussian()*10;
        double var = random.nextDouble()*10+1;

        this.getNaturalParameters().set(0,mean/(var));
        this.getNaturalParameters().set(1,-1/(2*var));
        this.fixNumericalInstability();

        this.updateMomentFromNaturalParameters();

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Normal toUnivariateDistribution() {

        Normal normal = new Normal(this.getVariable());
        double mean = this.getMomentParameters().get(EF_Normal.EXPECTED_MEAN);
        double variance = this.getMomentParameters().get(EF_Normal.EXPECTED_SQUARE) - mean * mean;
        if (variance < 1/LIMIT) variance = 1/LIMIT;

        normal.setMean(mean);
        normal.setVariance(variance);

        return normal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        double m0=this.momentParameters.get(EXPECTED_MEAN);
        double m1=this.momentParameters.get(EXPECTED_SQUARE);
        double variance =m1-m0*m0;
        if (variance < 1/LIMIT) variance = 1/LIMIT;

        // var = E(X^2) - E(X)^2 = m1 - m0*m0
        this.naturalParameters.set(0, m0 / (variance));
        this.naturalParameters.set(1, -0.5 / (variance));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        double n0 = this.naturalParameters.get(0);
        double n1 = this.naturalParameters.get(1);
        this.momentParameters.set(EXPECTED_MEAN,-0.5*n0/n1);
        this.momentParameters.set(EXPECTED_SQUARE,-0.5/n1 + 0.25*Math.pow(n0/n1,2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        return this.getSufficientStatistics(data.getValue(this.var));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        return this.computeLogBaseMeasure(dataInstance.getValue(this.var));
    }

    /**
     * {@inheritDoc}
     */
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
