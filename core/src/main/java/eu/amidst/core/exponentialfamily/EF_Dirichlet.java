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

    /** Represents the number of parameter of this EF_Dirichlet distribution. */
    int nOfStates;

    /**
     * Creates a new uniform EF_Dirichlet distribution for a given {@link Variable} object.
     * @param var1 a {@link Variable} object.
     */
    public EF_Dirichlet(Variable var1) {
        if (!var1.isDirichletParameter())
            throw new IllegalArgumentException("Non Dirichlet var");
        this.var=var1;
        this.nOfStates = var.getNumberOfStates();
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();

        this.parents = new ArrayList();

        for (int i = 0; i < nOfStates; i++) {
            this.naturalParameters.set(i,1.0);
        }
        this.fixNumericalInstability();
        updateMomentFromNaturalParameters();
    }

    /**
     * Creates a new uniform EF_Dirichlet distribution for a given {@link Variable} object with a given scale.
     * @param var1 a {@link Variable} object with a Dirichlet distribution type
     * @param scale a positive double value defining the scale of the EF_Dirichlet.
     */
    public EF_Dirichlet(Variable var1, double scale) {
        if (!var1.isDirichletParameter())
            throw new IllegalArgumentException("The variable is not a Dirichlet parameter!");
        this.var=var1;
        this.nOfStates = var.getNumberOfStates();
        this.naturalParameters = this.createZeroNaturalParameters();
        this.momentParameters = this.createZeroMomentParameters();

        this.parents = new ArrayList();

        for (int i = 0; i < nOfStates; i++) {
            this.naturalParameters.set(i,scale - 1);
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
        SufficientStatistics vec = this.createZeroSufficientStatistics();
        vec.set((int)val, Math.log(val));
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector getExpectedParameters() {

        double sum =0;

        for (int i = 0; i < nOfStates; i++) {
            sum+=this.naturalParameters.get(i)+1;
        }

        Vector vector = new ArrayVector(this.nOfStates);
        for (int i = 0; i < nOfStates; i++) {
            vector.set(i,(this.naturalParameters.get(i)+1)/sum);
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
            this.getNaturalParameters().set(i, 5*random.nextDouble() + 1 + 1e-5);
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
            sumOfU_i += this.naturalParameters.get(i);
        }

        for (int i = 0; i < nOfStates; i++) {
            this.momentParameters.set(i, Gamma.digamma(this.naturalParameters.get(i)) - Gamma.digamma(sumOfU_i));
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
            sumOfU_i += naturalParameters.get(i);
            sumLogGammaOfU_i += Gamma.logGamma(naturalParameters.get(i));
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


}
