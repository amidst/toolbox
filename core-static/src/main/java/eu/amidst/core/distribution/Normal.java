/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
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
 * <h2>This class implements a univariate Normal distribution.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-3
 */
public class Normal extends UnivariateDistribution {


    /**
     * The mean of the Normal distribution.
     */
    private double mean;

    /**
     * The standard deviation of the Normal distribution.
     */
    private double variance;

    /**
     * The class constructor.
     * @param var1 The variable of the distribution.
     */
    public Normal(Variable var1) {
        this.var = var1;
        this.mean = 0;
        this.variance = 1;
    }

    /**
     * Gets the mean of the distribution.
     * @return A <code>double</code> value with the mean.
     */
    public double getMean() {
        return mean;
    }

    /**
     * Sets the mean of the distribution.
     * @param mean1 A value for the mean.
     */
    public void setMean(double mean1) {
        this.mean = mean1;
    }

    /**
     * Gets the standard deviation of the distribution.
     * @return A <code>double</code> value with the standar deviation.
     */
    public double getSd() {
        return Math.sqrt(variance);
    }


    public void setVariance(double var){
        this.variance=var;
    }

    public double getVariance(){
        return this.variance;
    }


    @Override
    public double[] getParameters() {
        return new double[]{this.mean, this.getVariance()};
    }

    @Override
    public int getNumberOfParameters() {
        return 2;
    }

    /**
     * Evaluates the density function in a given point.
     * @param value An value for the variable.
     * @return A <code>double</code> with the value of the density.
     */


    @Override
    public double getProbability(double value) {
        return Math.exp(getLogProbability(value));
    }

    /**
     * Computes the logarithm of the density function in a given point.
     * @param value An value for the variable.
     * @return A <code>double</code> with the logarithm of the density value.
     */
    @Override
    public double getLogProbability(double value) {
        return (-0.5*Math.log(variance) - 0.5 * Math.log(2 * Math.PI) - 0.5 * Math.pow(value - mean, 2)/variance);
    }

    @Override
    public double sample(Random rand) {
        return rand.nextGaussian()*this.getSd()+this.getMean();
    }

    public String label(){
        return "Normal";
    }

    @Override
    public void randomInitialization(Random random) {
        this.setMean(random.nextGaussian()*10);
        this.setVariance(random.nextDouble()*10+0.5);
    }

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Normal)
            return this.equalDist((Normal)dist,threshold);
        return false;
    }

    @Override
    public String toString() {
        //return "Normal ("+this.getVariable().getName()+") [ mu = " + this.getMean() + ", var = "+ this.getVariance() +" ]";
        return "Normal [ mu = " + this.getMean() + ", var = "+ this.getVariance() +" ]";
    }

    public boolean equalDist(Normal dist, double threshold){
        return Math.abs(this.getMean() - dist.getMean()) <= threshold && Math.abs(this.getSd() - dist.getSd()) <= threshold;
    }

    @Override
    public EF_Normal toEFUnivariateDistribution() {

        EF_Normal efNormal = new EF_Normal(this.getVariable());
        MomentParameters momentParameters = efNormal.createZeroedMomentParameters();
        momentParameters.set(EF_Normal.EXPECTED_MEAN, this.getMean());
        momentParameters.set(EF_Normal.EXPECTED_SQUARE, this.getMean() * this.getMean() + this.getSd() * this.getSd());
        efNormal.setMomentParameters(momentParameters);
        return efNormal;
    }
}
