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
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * This class extends the abstract class {@link EF_UnivariateDistribution} and defines a Dirichlet distribution in Exponential Family (EF) canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox, take a look at the following paper:
 *  <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
 * </p>
 */
public class EF_Dirichlet extends EF_UnivariateDistribution {

    /**
     * Represents the number of parameter of this EF_Dirichlet distribution.
     */
    int nOfStates;

    /**
     * Creates a new uniform EF_Dirichlet distribution for a given {@link Variable} object.
     *
     * @param var1 a {@link Variable} object.
     */
    public EF_Dirichlet(Variable var1) {
        if (!var1.isDirichletParameter())
            throw new IllegalArgumentException("Non Dirichlet var");
        this.var = var1;
        this.nOfStates = var.getNumberOfStates();
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();

        this.parents = new ArrayList();

        for (int i = 0; i < nOfStates; i++) {
            this.setAlphaParameter(i, 1.0);
        }
        this.fixNumericalInstability();
        updateMomentFromNaturalParameters();
    }

    /**
     * Creates a new uniform EF_Dirichlet distribution for a given {@link Variable} object with a given scale.
     *
     * @param var1  a {@link Variable} object with a Dirichlet distribution type
     * @param scale a positive double value defining the scale of the EF_Dirichlet.
     */
    public EF_Dirichlet(Variable var1, double scale) {
        if (!var1.isDirichletParameter())
            throw new IllegalArgumentException("The variable is not a Dirichlet parameter!");
        this.var = var1;
        this.nOfStates = var.getNumberOfStates();
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();

        this.parents = new ArrayList();

        for (int i = 0; i < nOfStates; i++) {
            this.setAlphaParameter(i, scale);
        }
        fixNumericalInstability();
        updateMomentFromNaturalParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogBaseMeasure(double val) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        if (val < 0 || val > 1)
            throw new IllegalArgumentException("Error");
        if (this.nOfStates > 2)
            throw new IllegalStateException("Error");
        SufficientStatistics vec = this.createZeroSufficientStatistics();
        vec.set(0, Math.log(val));
        vec.set(1, Math.log(1 - val));
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector getExpectedParameters() {

        double sum = 0;

        for (int i = 0; i < nOfStates; i++) {
            sum += this.getAlphaParameter(i);
        }

        Vector vector = new ArrayVector(this.nOfStates);
        for (int i = 0; i < nOfStates; i++) {
            vector.set(i, (this.getAlphaParameter(i)) / sum);
        }

        return vector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fixNumericalInstability() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {
        EF_Dirichlet copy = new EF_Dirichlet(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        for (int i = 0; i < this.nOfStates; i++) {
            this.getNaturalParameters().set(i, 5 * random.nextDouble() + 1 + 1e-5);
        }
        fixNumericalInstability();
        this.updateMomentFromNaturalParameters();

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E toUnivariateDistribution() {
        throw new UnsupportedOperationException("Dirichlet is not included yet in the Distributions package.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("No Implemented. EF_Dirichlet distribution should (right now) only be used for learning.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {

        double sumOfU_i = 0;
        for (int i = 0; i < nOfStates; i++) {
            sumOfU_i += this.getAlphaParameter(i);
        }

        for (int i = 0; i < nOfStates; i++) {
            this.momentParameters.set(i, Gamma.digamma(this.getAlphaParameter(i)) - Gamma.digamma(sumOfU_i));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return nOfStates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {

        double sumOfU_i = 0;
        double sumLogGammaOfU_i = 0;
        for (int i = 0; i < nOfStates; i++) {
            sumOfU_i += this.getAlphaParameter(i);
            sumLogGammaOfU_i += Gamma.logGamma(this.getAlphaParameter(i));
        }

        return sumLogGammaOfU_i - Gamma.logGamma(sumOfU_i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        return new ArrayVector(nOfStates);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics() {

        ArrayVector vector = new ArrayVector(this.sizeOfSufficientStatistics());

        for (int i = 0; i < this.sizeOfSufficientStatistics(); i++) {
            vector.set(i, Gamma.digamma(1.0) - Gamma.digamma(this.sizeOfSufficientStatistics()));
        }

        return vector;
    }


    /**
     * Get Alpha parameters of the Dirichlet
     */
    public double getAlphaParameter(int i) {
        return this.naturalParameters.get(i) + 1;
    }

    /**
     * Set Alpha parameters of the Dirichlet. Update to moment paramters must be performed.
     */
    public void setAlphaParameter(int i, double val) {
        this.naturalParameters.set(i, val - 1.0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perMultiplyInverseHessian(Vector vector) {
        if (this.nOfStates>2)
            throw new IllegalArgumentException("Non valid");
        double triGammaTotal = Gamma.trigamma(this.naturalParameters.sum() + this.naturalParameters.size());


        double a = Gamma.trigamma(this.getAlphaParameter(0)) - triGammaTotal;
        double b = - triGammaTotal;
        double c = - triGammaTotal;
        double d = Gamma.trigamma(this.getAlphaParameter(1)) - triGammaTotal;

        double quotien = (a*d - b*c);

        double[][] matrix = new double[2][2];
        matrix[0][0]=d/quotien;
        matrix[0][1]=-b/quotien;
        matrix[1][0]=-c/quotien;
        matrix[1][1]=a/quotien;

        double v0 = vector.get(0);
        double v1 = vector.get(1);

        vector.set(0, v0*matrix[0][0] + v1*matrix[0][1]);
        vector.set(1, v0*matrix[1][0] + v1*matrix[1][1]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perMultiplyHessian(Vector vector) {
        double vectorSum = vector.sum();
        double triGammaTotal = Gamma.trigamma(this.naturalParameters.sum() + this.naturalParameters.size());
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i, vector.get(i) * Gamma.trigamma(this.getAlphaParameter(i)) - triGammaTotal * vectorSum);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double checkGradient(double learningRate, Vector gradient) {
        for (int k = 0; k < this.nOfStates; k++) {
            while (this.getAlphaParameter(k)+learningRate*gradient.get(k)<0)
                learningRate*=0.95;
        }
        return learningRate;
    }
}