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

package eu.amidst.core.distribution;

import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Random;

//TODO: Check getNumberOfParameters()! I'm not sure about the free parameters in this distribution!

/**
 * This class extends the abstract class {@link ConditionalDistribution}.
 * It defines the conditional distribution of a variable with a {@link Multinomial} distribution given a set of Logistic parents.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class Multinomial_LogisticParents extends ConditionalDistribution {

    /** Represents the set of intercept parameters of this distribution, one for each state. */
    private double[] intercept;

    /** Represents the set of coefficients, one for each state and parent. */
    private double[][] coeffParents;

    /**
     * Creates a new Multinomial_LogisticParents distribution for a given {@link Multinomial} Variable and the list of its parents.
     * @param var1 the variable of the distribution.
     * @param parents1 the list of Logistic parents of this variable.
     */
    public Multinomial_LogisticParents(Variable var1, List<Variable> parents1) {

        if (parents1.size() == 0) {
            throw new UnsupportedOperationException("A multinomial logistic distribution can not be created from a empty set of parents.");
        }

        this.var = var1;
        this.parents = parents1;
        this.intercept = new double[var.getNumberOfStates() - 1];
        this.coeffParents = new double[var.getNumberOfStates() - 1][parents.size()];

        for (int k = 0; k < var.getNumberOfStates() - 1; k++) {
            intercept[k] = 0;
            coeffParents[k] = new double[parents.size() + 1];
            for (int i = 0; i < parents.size(); i++) {
                coeffParents[k][i] = 1;
            }
        }
        //Make them unmodifiable
        //this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParameters() {
        double[] param = new double[this.getNumberOfParameters()];

        System.arraycopy(this.intercept, 0, param, 0, this.intercept.length);
        int count = this.intercept.length;
        for (int i = 0; i <this.coeffParents.length; i++) {
            System.arraycopy(this.coeffParents[i],0,param,count,this.coeffParents[i].length);
            count+=this.coeffParents[i].length;
        }

        return new double[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {

        int n=0;
        for(int i=0;i<this.coeffParents.length;i++){
            n+=this.getCoeffParents(i).length;
        }
        return n + this.intercept.length;

    }

    /**
     * Returns the intercept for a given state.
     * @param state the state for which the intercept is extracted.
     * @return an intercept parameter.
     */
    public double getIntercept(int state) {
        return intercept[state];
    }

    /**
     * Sets the intercept for a given state.
     * @param state the state for which the intercept is set.
     * @param intercept an intercept parameter.
     */
    public void setIntercept(int state, double intercept) {
        this.intercept[state] = intercept;
    }

    /**
     * Returns the set of coefficients for a given state.
     * @param state the state for which  the set of coefficients is extracted.
     * @return an Array of doubles corresponding to the set of coefficients.
     */
    public double[] getCoeffParents(int state) {
        return coeffParents[state];
    }

    /**
     * Sets the set of coefficients for a given state.
     * @param state the state for which  the set of coefficients is updated.
     * @param coeffParents the set of coefficients.
     */
    public void setCoeffParents(int state, double[] coeffParents) {
        this.coeffParents[state] = coeffParents;
    }

    /**
     * Returns a {@link Multinomial} distribution given an {@link Assignment} for the set of parents.
     * @param parentsAssignment an {@link Assignment} for the set of parents.
     * @return a {@link Multinomial} distribution.
     */
    public Multinomial getMultinomial(Assignment parentsAssignment) {

        double[] probs = new double[this.var.getNumberOfStates()];

        for (int i = 0; i < var.getNumberOfStates() - 1; i++) {
            probs[i] = intercept[i];
            int cont = 0;
            for (Variable v : parents) {
                probs[i] += coeffParents[i][cont] * parentsAssignment.getValue(v);
                cont++;
            }
        }

        probs = Utils.logs2probs(probs);

        Multinomial multinomial = new Multinomial(this.var);
        multinomial.setProbabilities(probs);

        return multinomial;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        double value = assignment.getValue(this.var);
        return (getMultinomial(assignment).getLogProbability(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getMultinomial(assignment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label() {
        return "Multinomial Logistic";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
        for (int i = 0; i < this.coeffParents.length; i++) {
            this.intercept[i] = random.nextGaussian();
            for (int j = 0; j < this.coeffParents[i].length; j++) {
                this.coeffParents[i][j]=random.nextGaussian();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        StringBuilder str = new StringBuilder();
        str.append("");

        for (int i = 0; i < this.var.getNumberOfStates() - 1; i++) {
            str.append("[ alpha = " + this.getIntercept(i));
            for (int j = 0; j < this.getCoeffParents(i).length; j++) {
                str.append(", beta = " + this.getCoeffParents(i)[j]);
            }
            str.append("]\n");
        }
        return str.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Multinomial_LogisticParents)
            return this.equalDist((Multinomial_LogisticParents)dist,threshold);
        return false;
    }

    public boolean equalDist(Multinomial_LogisticParents dist, double threshold) {
        boolean equals = true;
        for (int i = 0; i < this.intercept.length; i++) {
            equals = equals && Math.abs(this.getIntercept(i) - dist.getIntercept(i)) <= threshold;
        }
        if (equals) {
            for (int i = 0; i < this.coeffParents.length; i++) {
                for (int j = 0; j < this.coeffParents[i].length; j++) {
                    equals = equals && Math.abs(this.coeffParents[i][j] - dist.coeffParents[i][j]) <= threshold;
                }
            }
        }
        return equals;
    }
}
