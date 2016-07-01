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

/**
 ******************* ISSUE LIST **************************
 *
 * 1. Do we need here the min and max of the variable, for instance, to check that the input value in computeProbabilityOf(value) is in the range [min,max]?
 *
 *
 * ********************************************************
 */

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.variables.Variable;
import java.util.Random;

/**
 * This class extends the abstract class {@link UnivariateDistribution} and defines the univariate Normal distribution.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class Normal extends UnivariateDistribution {

    private static final long serialVersionUID = -5498079146465770364L;

    /** Represents the mean of the Normal distribution. */
    private double mean;

    /** represents the standard deviation of the Normal distribution. */
    private double variance;

    /**
     * Creates a new Normal distribution for a given variable.
     * @param var1 a continuous {@link Variable} object.
     */
    public Normal(Variable var1) {
        this.var = var1;
        this.mean = 0;
        this.variance = 1;
    }

    /**
     * Returns the mean of this Normal distribution.
     * @return a double value corresponding to the mean.
     */
    public double getMean() {
        return mean;
    }

    /**
     * Sets the mean of this Normal distribution.
     * @param mean1 a value for the mean.
     */
    public void setMean(double mean1) {
        this.mean = mean1;
    }

    /**
     * Returns the variance of this Normal distribution.
     * @return a double value corresponding to the variance.
     */
    public double getVariance(){
        return this.variance;
    }

    /**
     * Sets the variance of this Normal distribution.
     * @param var a value for the variance.
     */
    public void setVariance(double var){
        this.variance=var;
    }

    /**
     * Returns the standard deviation of this Normal distribution.
     * @return a double value corresponding to the standard deviation.
     */
    public double getSd() {
        return Math.sqrt(variance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParameters() {
        return new double[]{this.mean, this.getVariance()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getProbability(double value) {
        return Math.exp(getLogProbability(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogProbability(double value) {
        return (-0.5*Math.log(variance) - 0.5 * Math.log(2 * Math.PI) - 0.5 * Math.pow(value - mean, 2)/variance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double sample(Random rand) {
        return rand.nextGaussian()*this.getSd()+this.getMean();
    }

    @Override
    public UnivariateDistribution deepCopy(Variable variable) {
        Normal copy = new Normal(variable);
        copy.setMean(this.getMean());
        copy.setVariance(this.getVariance());
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label(){
        return "Normal";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
        this.setMean(random.nextGaussian()*10);
        this.setVariance(random.nextDouble()*10+0.5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Normal)
            return this.equalDist((Normal)dist,threshold);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        //return "Normal ("+this.getVariable().getName()+") [ mu = " + this.getMean() + ", var = "+ this.getVariance() +" ]";
        return "Normal [ mu = " + this.getMean() + ", var = "+ this.getVariance() +" ]";
    }

    /**
     * Tests if a given Normal distribution is equal to this Normal distribution.
     * @param dist a given Normal distribution.
     * @param threshold a threshold.
     * @return true if the two Normal distributions are equal, false otherwise.
     */
    public boolean equalDist(Normal dist, double threshold){
        return Math.abs(this.getMean() - dist.getMean()) <= threshold && Math.abs(this.getSd() - dist.getSd()) <= threshold;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_Normal toEFUnivariateDistribution() {

        EF_Normal efNormal = new EF_Normal(this.getVariable());
        MomentParameters momentParameters = efNormal.createZeroMomentParameters();
        momentParameters.set(EF_Normal.EXPECTED_MEAN, this.getMean());
        momentParameters.set(EF_Normal.EXPECTED_SQUARE, this.getMean() * this.getMean() + this.getVariance());
        efNormal.setMomentParameters(momentParameters);
        efNormal.updateNaturalFromMomentParameters();
        return efNormal;
    }
}
