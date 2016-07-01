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
//TODO: Make SufficientStatics an static class to avoid the creation of an object in each call to getSuffStatistics();
//TODO: Make naturalParameters and momentParameters statics?
//TODO: the logbasemeasure and the lognormalizer are positives or negatives terms (Andres)


package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;

import java.io.Serializable;
import java.util.List;

/**
 *
 * This class defines an Exponential Family (EF) distribution in canonical form.
 *
 * <p> For further details about how exponential family models are considered in this toolbox take a look at the following paper:
 * <i>Representation, Inference and Learning of Bayesian Networks as Conjugate Exponential Family Models. Technical Report.</i>
 * (<a href="http://amidst.github.io/toolbox/docs/ce-BNs.pdf">pdf</a>)
 * </p>
 */
public abstract class EF_Distribution implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = -3436599636425587512L;

    /** Represents the vector of natural parameters. */
    protected NaturalParameters naturalParameters;

    /** Represents the vector of moment parameters. */
    protected MomentParameters momentParameters;

    /**
     * Returns the vector of natural parameters of this EF_Distribution.
     * @return a {@link NaturalParameters} object.
     */
    public NaturalParameters getNaturalParameters() {

        return this.naturalParameters;
    }

    /**
     * Returns the vector of moment parameters of this EF_Distribution.
     * @return a {@link MomentParameters} object.
     */
    public MomentParameters getMomentParameters() {
        return this.momentParameters;
    }

    /**
     * Sets the vector of natural parameters for this EF_Distribution.
     * Note that this automatically and accordingly updates also the vector of moment parameters.
     * @param parameters a given {@link NaturalParameters} object.
     */
    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters = parameters;//.copy(parameters);
        this.updateMomentFromNaturalParameters();
    }

    /**
     * Sets the vector of moment parameters for this EF_Distribution for given {@link SufficientStatistics} parameters.
     * Note that this automatically and accordingly updates also the vector of natural parameters.
     * @param parameters a given {@link SufficientStatistics} object.
     */
    public void setMomentParameters(SufficientStatistics parameters) {
        this.momentParameters = (MomentParameters) parameters;
        this.updateNaturalFromMomentParameters();
    }

    /**
     * Sets the vector of moment parameters for this EF_Distribution.
     * Note that this automatically and accordingly updates also the vector of natural parameters.
     * @param parameters  a given {@link MomentParameters} object.
     */
    public void setMomentParameters(MomentParameters parameters) {
        this.momentParameters = parameters;// .copy(parameters);
        this.updateNaturalFromMomentParameters();
    }

    /**
     * Updates the vector of natural parameter from the current vector of moment parameters.
     */
    public abstract void updateNaturalFromMomentParameters();

    /**
     * Updates the vector of moment parameter from the current vector of natural parameters.
     */
    public abstract void updateMomentFromNaturalParameters();

    /**
     * Returns the vector of sufficient statistics for a given {@link Assignment} object.
     * @param assignment an {@link Assignment} object.
     * @return a {@link SufficientStatistics} object.
     */
    public abstract SufficientStatistics getSufficientStatistics(Assignment assignment);

    /**
     * Returns the size of the sufficient statistics vector of this EF_Distribution.
     * @return an {@code int} that represents the size of the sufficient statistics vector.
     */
    public abstract int sizeOfSufficientStatistics();

    /**
     * Computes the logarithm of the base measure function for a given assignment.
     * @param assignment an {@link Assignment} object.
     * @return a {@code double} that represents the logarithm of the base measure.
     */
    public abstract double computeLogBaseMeasure(Assignment assignment);

    /**
     * Computes the log-normalizer function of this EF_Distribution.
     * @return a {@code double} that represents the log-normalizer value.
     */
    public abstract double computeLogNormalizer();

    /**
     * Computes the log probability for a given assignment.
     * @param assignment an {@link Assignment} object.
     * @return a {@code double} that represents the log probability value.
     */
    public double computeLogProbabilityOf(Assignment assignment) {
        return this.naturalParameters.dotProduct(this.getSufficientStatistics(assignment)) + this.computeLogBaseMeasure(assignment) - this.computeLogNormalizer();
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
    public MomentParameters createZeroMomentParameters() {
        return (MomentParameters) this.createZeroVector();
    }

    /**
     * Creates a zero sufficient statistics vector (i.e., a vector filled with zeros).
     * @return a {@link SufficientStatistics} object.
     */
    public SufficientStatistics createZeroSufficientStatistics() {
        return (SufficientStatistics) this.createZeroVector();
    }

    /**
     * Creates the initial sufficient statistics vector (i.e., a vector with the initial counts).
     * It is used to implement MAP learning.
     * @return a {@link SufficientStatistics} object.
     */
    public abstract SufficientStatistics createInitSufficientStatistics();

    /**
     * Creates a zero natural parameter vector (i.e., a vector filled with zeros).
     * @return a {@link NaturalParameters} object.
     */
    public NaturalParameters createZeroNaturalParameters() {
        return (NaturalParameters) this.createZeroVector();
    }

    /**
     * Converts this EF_ConditionalDistribution to an extended learning distribution.
     * This new extended conditional distribution is a part of an extended Bayesian network, which is the result of adding
     * extra variables modelling the Bayesian prior probabilities for each of the parameters of the distribution.
     * This method can be then seen as a part of the model pre-processing for performing Bayesian learning.
     *
     * <p> For example, if applied to a Multinomial distribution, this method will return a new conditional distribution with a
     * Dirichlet parent variable and an unconditional Dirichlet distribution. If applied to a Normal distribution, it
     * will return a new conditional distribution with Normal and Gamma parent variables and the associated Normal and
     * Gamma unconditional distributions.</p>
     *
     * @param variables a {@link ParameterVariables} object which allow to access to all the parameter prior variables
     *                   of the newly created model.
     * @param nameSuffix an string variable
     * @return a non-empty {@code List} of {@link EF_ConditionalDistribution} objects.
     */
    protected List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables, String nameSuffix){
        throw new UnsupportedOperationException("Not convertible to Learning distribution");
    }


    /**
     * Converts this EF_ConditionalDistribution to an extended learning distribution.
     * This new extended conditional distribution is a part of an extended Bayesian network, which is the result of adding
     * extra variables modelling the Bayesian prior probabilities for each of the parameters of the distribution.
     * This method can be then seen as a part of the model pre-processing for performing Bayesian learning.
     *
     * <p> For example, if applied to a Multinomial distribution, this method will return a new conditional distribution with a
     * Dirichlet parent variable and an unconditional Dirichlet distribution. If applied to a Normal distribution, it
     * will return a new conditional distribution with Normal and Gamma parent variables and the associated Normal and
     * Gamma unconditional distributions.</p>
     *
     * @param variables a {@link ParameterVariables} object which allow to access to all the parameter prior variables
     *                   of the newly created model.
     * @return a non-empty {@code List} of {@link EF_ConditionalDistribution} objects.
     */
    public List<EF_ConditionalDistribution> toExtendedLearningDistribution(ParameterVariables variables){
        return this.toExtendedLearningDistribution(variables,"");
    }
}