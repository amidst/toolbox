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

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * This class extends the abstract class {@link EF_UnivariateDistribution} and defines an inverse Gamma distribution in exponential family canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox take a look at the following paper:
 *  <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_InverseGamma extends EF_UnivariateDistribution {

    public static final int LOGX = 0;
    public static final int INVX = 1;
    public static final double DELTA = 0.0001;

    /**
     * Creates a new EF_InverseGamma distribution for a given {@link Variable} object.
     * @param var1 a {@link Variable} object.
     */
    public EF_InverseGamma(Variable var1) {

        if (!var1.isInverseGammaParameter())
            throw new IllegalArgumentException("The variable is not Inverse Gamma parameter");

        this.parents = new ArrayList();

        this.var = var1;
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();

        this.naturalParameters.set(0, -2.1); //alpha = 1.1
        this.naturalParameters.set(1, -1);   //beta = 1
        this.setNaturalParameters(naturalParameters);
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
        SufficientStatistics vec = this.createZeroSufficientStatistics();
        vec.set(LOGX, Math.log(val));
        vec.set(INVX, 1.0 / val);
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {
        EF_InverseGamma copy = new EF_InverseGamma(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());

        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {
        double alpha = random.nextGaussian() * 2 + 1;
        double beta = random.nextDouble() * 1 + 0.1;

        this.getNaturalParameters().set(0, -alpha - 1);
        this.getNaturalParameters().set(1, -beta);
        this.fixNumericalInstability();
        this.updateMomentFromNaturalParameters();

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E toUnivariateDistribution() {
        throw new UnsupportedOperationException("Inverse Gamma is not included yet in the Distributions package.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        double m0 = this.getMomentParameters().get(0);
        double m1 = this.getMomentParameters().get(1);
        // Coordinate ascent until convergence
        double newalpha = 2.0, alpha = 0.0;
        double newbeta = 2.0, beta = 0.0;
        while (Math.abs(newalpha - alpha) > DELTA || Math.abs(newbeta - beta) > DELTA) {
            alpha = newalpha; beta = newbeta;
            newalpha = Utils.invDigamma(Math.log(beta) - m0);
            newbeta = newalpha / m1;
        }
        this.naturalParameters.set(0, newalpha);
        this.naturalParameters.set(1, newbeta);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        double alpha = -this.naturalParameters.get(0) - 1;
        double beta = -this.naturalParameters.get(1);
        this.momentParameters.set(0, Math.log(beta) - Gamma.digamma(alpha));
        this.momentParameters.set(1, alpha / beta);
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
    public double computeLogNormalizer() {
        double alpha = -this.naturalParameters.get(0) - 1;
        double beta = -this.naturalParameters.get(1);
        return Gamma.logGamma(alpha) - alpha * Math.log(beta);
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
    public Vector getExpectedParameters() {
        //if ((-this.naturalParameters.get(0)-2)<=0){
        //    throw new UnsupportedOperationException("The expected value is negative, so we do not have a valid inverse gamma distributions ");
        //}
        Vector vec = new ArrayVector(1);
        vec.set(0, -this.naturalParameters.get(1)/(-this.naturalParameters.get(0)-1));
        return vec;
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
    public SufficientStatistics createInitSufficientStatistics() {

        ArrayVector vector = new ArrayVector(this.sizeOfSufficientStatistics());

        double alpha = 1.0;
        double beta = 1.0;
        vector.set(0, Math.log(beta) - Gamma.digamma(alpha));
        vector.set(1, alpha / beta);

        return vector;
    }

}