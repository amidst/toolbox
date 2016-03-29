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
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * This class extends the abstract class {@link EF_UnivariateDistribution} and defines a Gamma distribution in exponential family canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>) </p>
 */
public class EF_JointNormalGamma extends EF_UnivariateDistribution {

    public static final int MU_GAMMA = 0;
    public static final int MUSQUARE_GAMMA = 1;
    public static final int GAMMA = 2;
    public static final int LOGGAMMA = 3;

    /**
     * Creates a new EF_Gamma distribution, i.e., Gamma(1,1), for a given {@link Variable} object.
     * @param var1 a {@link Variable} object with a Gamma distribution type.
     */
    public EF_JointNormalGamma(Variable var1) {

        if (!var1.isNormalGammaParameter())
            throw new IllegalArgumentException("The variable is not a Gamma parameter!");

        this.parents = new ArrayList();

        this.var = var1;
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();
        double alpha = 1;
        double beta = 1;
        double mean = 0;
        double precision = 1e-10;

        naturalParameters.set(EF_JointNormalGamma.MU_GAMMA, mean*precision);
        naturalParameters.set(EF_JointNormalGamma.MUSQUARE_GAMMA, -0.5*precision);
        naturalParameters.set(EF_JointNormalGamma.GAMMA, -beta-0.5*precision*mean*mean);
        naturalParameters.set(EF_JointNormalGamma.LOGGAMMA, alpha-0.5);

        this.setNaturalParameters(naturalParameters);
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
    public SufficientStatistics getSufficientStatistics(double val) {
        throw new UnsupportedOperationException("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution deepCopy(Variable var) {
        EF_JointNormalGamma copy = new EF_JointNormalGamma(var);
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());

        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {

        double randomVar = random.nextDouble()*2 + 1 + 0.01;

        double alpha = 10;
        double beta = randomVar*alpha ;
        double mean = random.nextGaussian() * 10;
        double var = random.nextDouble() * 10 + 1;
        double precision = 1/var;

        naturalParameters.set(EF_JointNormalGamma.MU_GAMMA, mean*precision);
        naturalParameters.set(EF_JointNormalGamma.MUSQUARE_GAMMA, -0.5*precision);
        naturalParameters.set(EF_JointNormalGamma.GAMMA, -beta-0.5*precision*mean*mean);
        naturalParameters.set(EF_JointNormalGamma.LOGGAMMA, alpha-0.5);

        this.fixNumericalInstability();
        this.updateMomentFromNaturalParameters();

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E toUnivariateDistribution() {
        throw new UnsupportedOperationException("Gamma is not included yet in the Distributions package.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMomentFromNaturalParameters() {
        double alpha = this.naturalParameters.get(EF_JointNormalGamma.LOGGAMMA) + 0.5 ;
        double precision =  this.naturalParameters.get(EF_JointNormalGamma.MUSQUARE_GAMMA)/(-0.5);
        double mean =  this.naturalParameters.get(EF_JointNormalGamma.MU_GAMMA)/precision;
        double beta = -this.naturalParameters.get(EF_JointNormalGamma.GAMMA) -0.5*precision*mean*mean;

        this.momentParameters.set(EF_JointNormalGamma.MU_GAMMA, mean*alpha/beta);
        this.momentParameters.set(EF_JointNormalGamma.MUSQUARE_GAMMA, 1/precision + mean*mean*alpha/beta);
        this.momentParameters.set(EF_JointNormalGamma.GAMMA, alpha/beta);
        this.momentParameters.set(EF_JointNormalGamma.LOGGAMMA, Gamma.digamma(alpha) - Math.log(beta));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sizeOfSufficientStatistics() {
        return 4;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLogNormalizer() {
        double alpha = this.naturalParameters.get(EF_JointNormalGamma.LOGGAMMA) + 0.5 ;
        double precision =  this.naturalParameters.get(EF_JointNormalGamma.MUSQUARE_GAMMA)/(-0.5);
        double mean =  this.naturalParameters.get(EF_JointNormalGamma.MU_GAMMA)/precision;
        double beta = -this.naturalParameters.get(EF_JointNormalGamma.GAMMA) -0.5*precision*mean*mean;

        return -0.5*Math.log(precision) - alpha*Math.log(beta) + Gamma.logGamma(alpha);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector createZeroVector() {
        return new ArrayVector(4);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector getExpectedParameters() {

        double precision =  this.momentParameters.get(EF_JointNormalGamma.GAMMA);
        double mean =  this.momentParameters.get(EF_JointNormalGamma.MU_GAMMA)/precision;

        Vector vec = new ArrayVector(2);
        vec.set(0,mean);
        vec.set(1,precision);

        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fixNumericalInstability() {



        /*double alpha = this.naturalParameters.get(0) + 1;
        double beta = -this.naturalParameters.get(1);


        if ((alpha/beta)>PRECISION_LIMIT){
            double K = (alpha/beta)/PRECISION_LIMIT;
            double alphaPrime = alpha/K;
            this.naturalParameters.set(0,alphaPrime-1);
        }*/

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SufficientStatistics createInitSufficientStatistics() {

        ArrayVector vector = new ArrayVector(this.sizeOfSufficientStatistics());

        double alpha = 1;
        double beta = 1;
        double mean = 0;
        double precision = 1e-10;

        this.momentParameters.set(EF_JointNormalGamma.MU_GAMMA, mean*alpha/beta);
        this.momentParameters.set(EF_JointNormalGamma.MUSQUARE_GAMMA, 1/precision + mean*mean*alpha/beta);
        this.momentParameters.set(EF_JointNormalGamma.GAMMA, alpha/beta);
        this.momentParameters.set(EF_JointNormalGamma.LOGGAMMA, Gamma.digamma(alpha) - Math.log(beta));

        return vector;
    }
}