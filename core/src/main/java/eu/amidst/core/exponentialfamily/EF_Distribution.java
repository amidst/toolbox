/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
//TODO: Make SufficientStatics an static class to avoid the creation of an object in each call to getSuffStatistics();
//TODO: Make naturalParameters and momentParameters statics?
//TODO: the logbasemeasure and the lognormalizer are positives or negatives terms (Andres)


package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;

import java.util.List;


/**
 *
 * This class represents an exponential family (EF) distribution in canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox look at the following paper </p>
 * <p> <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
 * </p>
 */
public abstract class EF_Distribution {

    /** The vector of natural parameters */
    protected NaturalParameters naturalParameters;

    /** The vector of moment parameters */
    protected MomentParameters momentParameters;

    /**
     * Gets the vector of natural parameters of the distribution
     *
     * @return A <code>NaturalParameters</code> object.
     */
    public NaturalParameters getNaturalParameters() {

        return this.naturalParameters;
    }

    /**
     * Gets the vector of moment parameters of the distribution
     *
     * @return A <code>MomentParameters</code> object.
     */
    public MomentParameters getMomentParameters() {

        return this.momentParameters;
    }

    /**
     * Set the vector of natural parameters of the distribution. Automatically updates accordingly
     * the vector of moment parameters.
     * @param parameters, a <code>NaturalParameters</code> object
     */
    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters = parameters;//.copy(parameters);
        this.updateMomentFromNaturalParameters();
    }

    /**
     * Set the vector of moment parameters of the distribution from a vector with sufficient statistics.
     * Automatically updates accordingly the vector of natural parameters.
     * @param parameters, a <code>SufficientStatistics</code> object
     */
    public void setMomentParameters(SufficientStatistics parameters) {
        this.momentParameters = (MomentParameters) parameters;
        this.updateNaturalFromMomentParameters();
    }

    /**
     * Set the vector of moment parameters of the distribution.
     * Automatically updates accordingly the vector of natural parameters.
     * @param parameters, a <code>MomentParameters</code> object
     */
    public void setMomentParameters(MomentParameters parameters) {
        this.momentParameters = parameters;// .copy(parameters);
        this.updateNaturalFromMomentParameters();
    }

    /**
     * Update the vector of natural parameter from the current vector of moment parameters.
     *
     * <p> For further details about this procedure look at the following paper </p>
     * <p> Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.
     * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
     * </p>
     */
    public abstract void updateNaturalFromMomentParameters();

    /**
     * Update the vector of moment parameter from the current vector of natural parameters.
     *
     * <p> For further details about this procedure look at the following paper </p>
     * <p> Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.
     * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
     * </p>
     */
    public abstract void updateMomentFromNaturalParameters();

    /**
     * Gets the vector of sufficient statistics for a given assignment.
     * @param data, an <code>Assignment</code> object
     * @return A <code>SufficientStatistics</code> object
     */
    public abstract SufficientStatistics getSufficientStatistics(Assignment data);

    /**
     * Return the size of the sufficient statistics vector of the distribution of the associated exponential family distribution.
     * @return A positive integer value.
     */
    public abstract int sizeOfSufficientStatistics();

    /**
     * Computes the logarithm of the base measure function of a given assignment for the associated exponential
     * family distribution.
     *
     * @param dataInstance, an <code>Assignment</code> object
     * @return A double value.
     */
    public abstract double computeLogBaseMeasure(Assignment dataInstance);

    /**
     * Computes the log-normalizer function of the associated exponential family distribution.
     *
     * @return A double value.
     */
    public abstract double computeLogNormalizer();

    /**
     * Returns the log probability of a given assignment according to the associated exponential family distribution.
     * @param dataInstance, an <code>Assignment</code> object
     * @return A positive double value.
     */
    public double computeLogProbabilityOf(Assignment dataInstance) {
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(dataInstance)) + this.computeLogBaseMeasure(dataInstance) - this.computeLogNormalizer();
    }

    /**
     * Creates a zero vector (ie a vector fills with zeros).
     * @return A <code>Vector</code> object
     */
    public abstract Vector createZeroVector();

    /**
     * Creates a zero moment parameter vector (ie a vector fills with zeros).
     * @return A <code>MomentParameters</code> object
     */
    public MomentParameters createZeroMomentParameters() {
        return (MomentParameters) this.createZeroVector();
    }

    /**
     * Creates a zero sufficient statistics vector (ie a vector fills with zeros).
     * @return A <code>SufficientStatistics</code> object
     */
    public SufficientStatistics createZeroSufficientStatistics() {
        return (SufficientStatistics) this.createZeroVector();
    }

    /**
     * Creates a zero natural parameter vector (ie a vector fills with zeros).
     * @return A <code>NaturalParameters</code> object
     */
    public NaturalParameters createZeroNaturalParameters() {
        return (NaturalParameters) this.createZeroVector();
    }


    /**
     * Returns a new conditional distribution which is part of a extended Bayesian network. This extended model
     * is the result of adding variables modelling the Bayesian prior probabilities for each of the parameters of
     * the distribution. This method can be seen as a part of the model pre-processing for performing Bayesian learning.
     *
     * <p> For example, if applied to a Multinomial distribution, it will return a new conditional distribution with a
     * Dirichlet parent variable and an unconditional Dirichlet distribution. It applied to a Normal distribution, it
     * will return a new conditional distribution with Normal and Gamma parent variables and the associated Normal and
     * Gamma unconditional distributions.</p>
     *
     * @param variables, a <code>ParameterVariables</code> object which allow to access to all the parameter prior variables
     *                   of the newly created model.
     * @return A non-empty list of <code>EF_ConditionalDistribution</code> objects.
     */
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables){
        throw new UnsupportedOperationException("Not convertible to Learning distribution");
    }

}