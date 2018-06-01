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

import eu.amidst.core.distribution.Normal;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * This class extends the abstract class {@link EF_UnivariateDistribution} and defines a Normal distribution in exponential family canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_NormalParameter extends EF_UnivariateDistribution {


    public static final int EXPECTED_MEAN = 0;
    public static final int EXPECTED_SQUARE = 1;

    public static final int INDEX_MEAN = 0;
    public static final int INDEX_PRECISION = 1;

    private static final double LIMIT = 100000;

    /**
     * Creates a new EF_Normal distribution for a given variable.
     *
     * @param var1 a {@link Variable} object with a Normal distribution type.
     */
    public EF_NormalParameter(Variable var1) {
        if (!var1.isNormal() && !var1.isParameterVariable()) {
            throw new UnsupportedOperationException("Creating a Gaussian EF distribution for a non-gaussian variable.");
        }

        this.parents = new ArrayList();

        this.var = var1;
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();

        this.momentParameters.set(EXPECTED_MEAN, 0);
        this.momentParameters.set(EXPECTED_SQUARE, 1);
        this.setMomentParameters(momentParameters);
    }


    public double getMean() {
        return this.naturalParameters.get(INDEX_MEAN)/this.getPrecision();
    }

    public double getPrecision() {
        return -2*this.naturalParameters.get(INDEX_PRECISION);
    }

    public void setNaturalWithMeanPrecision(double mean, double precision) {
        this.naturalParameters.set(INDEX_MEAN, mean*precision);
        this.naturalParameters.set(INDEX_PRECISION, -0.5*precision);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fixNumericalInstability() {

        /*if (this.getPrecision()>LIMIT) {
            this.naturalParameters.set(INDEX_PRECISION,LIMIT);
        }*/

        /*if (this.naturalParameters.get(1)<(-0.5*LIMIT)){ //To avoid numerical problems!
            double x = -0.5*this.naturalParameters.get(0)/this.getNaturalParameters().get(1);
            this.naturalParameters.set(0,x*LIMIT);
            this.naturalParameters.set(1,-0.5*LIMIT);
        }*/

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(double val) {
        return -0.5 * Math.log(2 * Math.PI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        return -0.5 * Math.log(this.getPrecision()) + 0.5 * this.getPrecision() * Math.pow(this.getMean(), 2);
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
    public NaturalParameters createZeroNaturalParameters() {
        return new ArrayVector(2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics() {
        ArrayVector vector = new ArrayVector(this.sizeOfSufficientStatistics());

        vector.set(0, 0);
        vector.set(1, 0.000001);

        return vector;

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
        throw new UnsupportedOperationException("No implemented yet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {

        EF_NormalParameter copy = new EF_NormalParameter(var);
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

        double mean = random.nextGaussian() * 10;
        double var = random.nextDouble() * 10 + 1;

        this.setNaturalWithMeanPrecision(mean, 1 / var);

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

        normal.setMean(this.getMean());
        normal.setVariance(1/this.getPrecision());

        return normal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        double m0 = this.momentParameters.get(EXPECTED_MEAN);
        double m1 = this.momentParameters.get(EXPECTED_SQUARE);
        double variance = m1 - m0 * m0;

        if (variance < 0)
            throw new IllegalStateException("Negative variance value");

        if (variance < 1 / LIMIT)
            variance = 1 / LIMIT;

        this.setNaturalWithMeanPrecision(m0, 1 / variance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        this.momentParameters.set(EXPECTED_MEAN, this.getMean());
        double expt_square = 1 / this.getPrecision() + Math.pow(this.getMean(), 2);

        if (expt_square <= 0)
            throw new IllegalStateException("Zero or Negative expected square value");

        this.momentParameters.set(EXPECTED_SQUARE, expt_square);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void perMultiplyHessian(Vector vector) {
        double mu =  this.getMean();
        double prec = this.getPrecision();

        double H_11 = 1.0/prec;
        double H_12 = 2.0*mu/prec;
        double H_21 = H_12;
        double H_22 = 2.0/Math.pow(prec,2) + 4.0*Math.pow(mu,2)/prec;

        double R0 = H_11 * vector.get(0) + H_12 * vector.get(1);
        double R1 = H_21 * vector.get(0) + H_22 * vector.get(1);

        vector.set(0, R0);
        vector.set(1, R1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perMultiplyInverseHessian(Vector vector) {
        double mu =  this.getMean();
        double prec = this.getPrecision();

        double invH_11 = prec * ( 1.0 + 2.0 * prec * Math.pow(mu,2) );
        double invH_12 = - Math.pow(prec,2) * mu;
        double invH_21 = invH_12;
        double invH_22 = 0.5 * Math.pow(prec,2);

        double R0 = invH_11 * vector.get(0) + invH_12 * vector.get(1);
        double R1 = invH_21 * vector.get(0) + invH_22 * vector.get(1);

        vector.set(0, R0);
        vector.set(1, R1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double checkGradient(double learningRate, Vector gradient) {
        while (this.naturalParameters.get(1) + learningRate*gradient.get(1) >= 0)
            learningRate*=0.95;
        return learningRate;
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
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables, String nameSuffix) {
        throw new UnsupportedOperationException("No valid operation for a normal parameter distribution");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {
        NaturalParameters out = new ArrayVector(2);
        out.copy(this.getNaturalParameters());
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double kl(NaturalParameters naturalParameters, double logNormalizer) {
        double precisionQ = -2*naturalParameters.get(INDEX_PRECISION);
        double meanQ = naturalParameters.get(INDEX_MEAN)/precisionQ;


        //double kl =  0.5*Math.log(this.getPrecision()) - 0.5*Math.log(precisionQ) + 0.5*precisionQ/this.getPrecision()
        //        + 0.5*precisionQ*Math.pow(this.getMean()-meanQ,2) -0.5;

        double factor = precisionQ/this.getPrecision();
        double kl = 0.5*(-Math.log(factor) + factor + precisionQ*Math.pow(this.getMean()-meanQ,2) -1);


        if (Double.isNaN(kl)){
            throw new IllegalStateException("NaN KL");
        }

        if (kl<0) {
            kl=0;
        }

        return kl;
    }

}