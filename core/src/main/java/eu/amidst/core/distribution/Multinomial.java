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
 * 1. In general, should we clone attributes in the constructor to avoid bad uses of input variables later on?
 *
 * 2. How are we going to update the probabilities? Value by value? Or directly with the whole set of probabilities? or both?
 * Two methods are included: setProbabilities(double[] probabilities) and setProbabilityOfState(int index, double value)
 *
 * 3. Is needed the method setProbabilityOfState ?
 * ********************************************************
 */
package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_Multinomial;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;

import java.util.Arrays;
import java.util.Random;


/**
 * This class extends the abstract class {@link UnivariateDistribution} and defines the univariate multinomial distribution.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class Multinomial extends UnivariateDistribution  {


    private static final long serialVersionUID = 8587756877237341367L;

    /** Represents a set of probabilities, one for each state of the variable. */
    private double[] probabilities;

    /**
     * Creates a new Multinomial distribution for a given variable.
     * @param var1 a discrete {@link Variable} object.
     */
    public Multinomial(Variable var1) {

        this.var = var1;
        this.probabilities = new double[var.getNumberOfStates()];

        for (int i = 0; i < var.getNumberOfStates(); i++) {
            this.probabilities[i] = 1.0 / var.getNumberOfStates();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParameters() {
        double[] param = new double[this.getNumberOfParameters()];
        System.arraycopy(this.probabilities, 0, param, 0, this.probabilities.length);
        return param;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {
        return probabilities.length;
    }

    /**
     * Sets the probability values of this Multinomial distribution.
     * @param probabilities1 an array of probability values having the same order as the variable states.
     */
    public void setProbabilities(double[] probabilities1) {
        this.probabilities = probabilities1;
    }

    /**
     * Sets a probability value in a given position in the array of probabilities.
     * @param state the position in which the probability is set.
     * @param prob a probability value.
     */
    public void setProbabilityOfState(int state, double prob) {
        this.probabilities[state] = prob;
    }

    /**
     * Returns the probability value of a given position in the array of probabilities.
     * @param state the position for which the probability is extracted.
     * @return a probability value.
     */
    public double getProbabilityOfState(int state) {
        return this.probabilities[state];
    }

    /**
     * Returns the probability value of a given multinomial state.
     * @param name the name of the state.
     * @return a probability value.
     */
    public double getProbabilityOfState(String name) {
        FiniteStateSpace stateSpace = this.var.getStateSpaceType();
        return this.probabilities[stateSpace.getIndexOfState(name)];
    }

    /**
     * Returns the set of probabilities for the different states of the variable.
     * @return an array of double corresponding to the probability values.
     */
    public double[] getProbabilities() {
        return probabilities;
    }

    /**
     * Returns the logarithm of the probability for a given variable state.
     * @param value The position of the variable state in the array of probabilities (represented as a
     *              double for generality reasons).
     * @return a double value corresponding to the logarithm of the probability.
     */
    @Override
    public double getLogProbability(double value) {
        return Math.log(this.probabilities[(int) value]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double sample(Random rand) {
        double b = 0, r = rand.nextDouble();
        for (int i = 0; i < probabilities.length; i++) {
            b += probabilities[i];
            if (b > r) {
                return i;
            }
        }
        return probabilities.length-1;
    }

    @Override
    public UnivariateDistribution deepCopy(Variable variable) {
        Multinomial copy = new Multinomial(variable);
        copy.probabilities = Arrays.copyOf(this.getProbabilities(),this.getProbabilities().length);
        return copy;
    }

    public String label() {
        return "Multinomial";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] = random.nextDouble()+0.2;
        }
        probabilities = Utils.normalize(probabilities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Multinomial)
            return this.equalDist((Multinomial)dist,threshold);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("[ ");
        int size = this.getProbabilities().length;
        for(int i=0;i<size;i++){
            str.append(this.getProbabilities()[i]);
            if(i<size-1) {
                str.append(", ");
            }
        }
        str.append(" ]");
        return str.toString();
    }

    /**
     * Tests if a given Multinomial distribution is equal to this Multinomial distribution.
     * @param dist a given Multinomial distribution.
     * @param threshold a threshold.
     * @return true if the two Multinomial distributions are equal, false otherwise.
     */
    public boolean equalDist(Multinomial dist, double threshold){
        boolean equals = true;
        for (int i = 0; i < this.probabilities.length; i++) {
           equals = equals && Math.abs(this.getProbabilityOfState(i) - dist.getProbabilityOfState(i)) <= threshold;
        }
        return equals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_Multinomial toEFUnivariateDistribution() {
        EF_Multinomial efMultinomial = new EF_Multinomial(this.getVariable());

        MomentParameters momentParameters = efMultinomial.createZeroMomentParameters();

        for (int i = 0; i < this.getVariable().getNumberOfStates(); i++) {
            momentParameters.set(i, this.getProbabilityOfState(i));
        }

        efMultinomial.setMomentParameters(momentParameters);

        return efMultinomial;
    }
}