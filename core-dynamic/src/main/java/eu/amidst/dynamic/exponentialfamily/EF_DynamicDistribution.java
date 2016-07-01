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
 ******************* ISSUE LIST **************************************************************************************
 *
 * 1. Make SufficientStatics an static class to avoid the creation of an object in each call to getSuffStatistics();
 * 2. Make naturalParameters and momentParameters statics?
 * *******************************************************************************************************************
 */

package eu.amidst.dynamic.exponentialfamily;

import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

/**
 * This class defines a Exponential Family (EF) distribution in canonical form for Dynamic Bayesian networks.
 */
public abstract class EF_DynamicDistribution {

    /** Represents the variable of the distribution. */
    protected Variable var;

    /** Represents the vector of natural parameters. */
    protected NaturalParameters naturalParameters;

    /** Represents the vector of moment parameters. */
    protected MomentParameters momentParameters;

    /**
     * Returns the variable of the distribution.
     * @return a {@link Variable} object.
     */
    public final Variable getVariable() {
        return this.var;
    }

    /**
     * Returns the vector of natural parameters of this EF_DynamicDistribution.
     * @return a {@link NaturalParameters} object.
     */
    public final NaturalParameters getNaturalParameters() {

        return this.naturalParameters;
    }

    /**
     * Returns the vector of moment parameters of this EF_DynamicDistribution.
     * @return a {@link MomentParameters} object.
     */
    public final MomentParameters getMomentParameters() {

        return this.momentParameters;
    }

    /**
     * Sets the vector of natural parameters for this EF_DynamicDistribution.
     * Note that this automatically and accordingly updates also the vector of moment parameters.
     * @param parameters a given {@link NaturalParameters} object.
     */
    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters=parameters;
        this.updateMomentFromNaturalParameters();
    }

    /**
     * Sets the vector of moment parameters for this EF_DynamicDistribution for given {@link SufficientStatistics} parameters.
     * Note that this automatically and accordingly updates also the vector of natural parameters.
     * @param parameters a given {@link SufficientStatistics} object.
     */
    public void setMomentParameters(SufficientStatistics parameters) {
        this.momentParameters=(MomentParameters)parameters;
        this.updateNaturalFromMomentParameters();
    }

    /**
     * Sets the vector of moment parameters for this EF_DynamicDistribution for given {@link MomentParameters}.
     * Note that this automatically and accordingly updates also the vector of natural parameters.
     * @param parameters a given {@link MomentParameters} object.
     */
    public void setMomentParameters(MomentParameters parameters) {
        this.momentParameters=parameters;// .copy(parameters);
        this.updateNaturalFromMomentParameters();
    }

    /**
     * Updates the vector of natural parameters from the current vector of moment parameters.
     */
    public abstract void updateNaturalFromMomentParameters();

    /**
     * Updates the vector of moment parameters from the current vector of natural parameters.
     */
    public abstract void updateMomentFromNaturalParameters();

    /**
     * Returns the vector of sufficient statistics for a given {@link DynamicDataInstance} object.
     * @param data a {@link DynamicDataInstance} object.
     * @return a {@link SufficientStatistics} object.
     */
    public abstract SufficientStatistics getSufficientStatistics(DynamicDataInstance data);

    /**
     * Returns the size of the sufficient statistics vector of this EF_DynamicDistribution.
     * @return an {@code int} that represents the size of the sufficient statistics vector.
     */
    public abstract int sizeOfSufficientStatistics();

    /**
     * Computes the logarithm of the base measure function for a given DynamicDataInstance.
     * @param dataInstance a {@link DynamicDataInstance} object.
     * @return a {@code double} that represents the logarithm of the base measure.
     */
    public abstract double computeLogBaseMeasure(DynamicDataInstance dataInstance);

    /**
     * Computes the log-normalizer function of this EF_Distribution.
     * @return a {@code double} that represents the log-normalizer value.
     */
    public abstract double computeLogNormalizer();

    /**
     * Computes the probability for a given DynamicDataInstance.
     * @param dataInstance a {@link DynamicDataInstance} object.
     * @return a {@code double} that represents the probability value.
     */
    public double computeProbabilityOf(DynamicDataInstance dataInstance){
        return Math.exp(this.computeLogProbabilityOf(dataInstance));
    }

    /**
     * Computes the log probability for a given DynamicDataInstance.
     * @param dataInstance a {@link DynamicDataInstance} object.
     * @return a {@code double} that represents the log probability value.
     * TODO: the logbasemeasure and the lognormalizer are positives or negatives terms (Andres)
     */
    public double computeLogProbabilityOf(DynamicDataInstance dataInstance){
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(dataInstance)) + this.computeLogBaseMeasure(dataInstance) - this.computeLogNormalizer();
    }

    /**
     * Creates a zero vector (i.e., a vector filled with zeros).
     * @return a {@link Vector} object.
     */
    public abstract Vector createZeroVector();

    /**
     * Creates a zero moment parameter vector (i.e., a vector filled with zeros).
     * @return a {@link MomentParameters} object.
     */
    public MomentParameters createZeroMomentParameters(){
        return (MomentParameters)this.createZeroVector();
    }

    /**
     * Creates a zero sufficient statistics vector (i.e., a vector filled with zeros).
     * @return a {@link SufficientStatistics} object.
     */
    public SufficientStatistics createZeroSufficientStatistics(){
        return (SufficientStatistics)this.createZeroVector();
    }

    /**
     * Creates a zero natural parameter vector (i.e., a vector filled with zeros).
     * @return a {@link NaturalParameters} object.
     */
    public NaturalParameters createZeroNaturalParameters(){
        return (NaturalParameters)this.createZeroVector();
    }

    /**
     * Creates the initial sufficient statistics vector (i.e., a vector with the initial counts).
     * It is used to implement MAP learning.
     * @return a {@link SufficientStatistics} object.
     */
    public abstract SufficientStatistics createInitSufficientStatistics();

}
