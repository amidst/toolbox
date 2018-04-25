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
 * 1. In the constructor, should we initialize the CLG attributes in this way?
 *
 * 2. The name of the method computeProbabilityOf(..) is a bit confusing for continuous domains. It does not compute probabilities but the
 * value for the density function which is not a probability. However as this class implements this method of ConditionalDistribution,
 * we could leave like this.
 *
 * 3. QAPlug gives a warning when using the same name for a attribute and a given argument, e.g. this.var = var
 * ********************************************************
 */

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_Normal_NormalParents;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Random;

//TODO Re-implement the management of coffParents using a Map<Variale,Double>

/**
 * This class extends the abstract class {@link ConditionalDistribution}.
 * It defines the Conditional Linear Gaussian distribution, i.e.,
 * the conditional distribution of a normal variable given a set of normal parents.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class ConditionalLinearGaussian extends ConditionalDistribution {


    /** Represents the intercept parameter of this ConditionalLinearGaussian distribution. */
    private double intercept;

    /** Represents the set of coefficients, one for each parent. */
    private double[] coeffParents;

    /** Represents the standard deviation of the variable (it does not depends on the parents). */
    private double variance;

    /**
     * Creates a new ConditionalLinearGaussian distribution for a given variable and the list of its parents.
     * @param var1 the variable of this distribution.
     * @param parents1 the set of parents of this variable.
     */
    public ConditionalLinearGaussian(Variable var1, List<Variable> parents1) {

        this.var = var1;
        this.parents = parents1;
        this.intercept = 0;
        coeffParents = new double[parents.size()];
        for (int i = 0; i < parents.size(); i++) {
            coeffParents[i] = 1;
        }
        this.variance = 1;

        //Make them unmodifiable
        //this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Returns the intercept of this ConditionalLinearGaussian distribution.
     * @return a double value representing the intercept.
     */
    public double getIntercept() {
        return intercept;
    }

    /**
     * Sets the intercept of this ConditionalLinearGaussian distribution.
     * @param intercept1 a double value representing the intercept.
     */
    public void setIntercept(double intercept1) {
        this.intercept = intercept1;
    }

    /**
     * Returns the set of coefficients of the parent variables.
     * @return A array of doubles containing the set of coefficients of the parent variables.
     */
    public double[] getCoeffParents() {
        return coeffParents;
    }

    /**
     * Sets the set of coefficients of this ConditionalLinearGaussian distribution.
     * @param coeffParents1 an array of doubles containing the set of coefficients (i.e. Beta values), one for each parent.
     */
    public void setCoeffParents(double[] coeffParents1) {
        if(coeffParents1.length != this.coeffParents.length)
            throw new UnsupportedOperationException("The number of beta parametersParentVariables for the Normal_Normal distribution" +
                    " does not match with the number of parents");
        this.coeffParents = coeffParents1;
    }

    /**
     * Returns the coefficient of a given parent variable.
     * @param parentVar the parent variable.
     * @return the coefficient corresponding to this parent variable.
     */
    public double getCoeffForParent(Variable parentVar){

        int parentIndex = -1;
        for(int i=0; i<parents.size(); i++){
            Variable parent = parents.get(i);
            if(parentVar.equals(parent)){
                parentIndex = i;
                break;
            }
        }
        if(parentIndex == -1)
            throw new UnsupportedOperationException("Variable "+parentVar.getName()+" is not in the list of parents");
        return this.coeffParents[parentIndex];
    }

    /**
     * Sets the coefficient for a given parent variable.
     * @param parentVar the parent variable.
     * @param coeff the Beta value for this parent variable.
     */
    public void setCoeffForParent(Variable parentVar, double coeff) {

        int parentIndex = -1;
        for(int i=0; i<parents.size(); i++){
            Variable parent = parents.get(i);
            if(parentVar.equals(parent)){
                parentIndex = i;
                break;
            }
        }
        if(parentIndex == -1)
            throw new UnsupportedOperationException("Variable "+parentVar.getName()+" is not in the list of parents");
        this.coeffParents[parentIndex] = coeff;
    }

    /**
     * Returns the standard deviation of the variable.
     * @return a double value representing the standard deviation of the variable.
     */
    public double getSd() {
        return Math.sqrt(this.variance);
    }

    /**
     * Returns the variance of the variable.
     * @return a double value representing the variance of the variable.
     */
    public double getVariance() {
        return this.variance;
    }

    /**
     * Sets the variance of the variable.
     * @param variance_ a  double value representing the variance of the variable.
     */
    public void setVariance(double variance_) {
        this.variance = variance_;
    }

    /**
     * Returns the univariate {@link Normal} distribution after conditioning this distribution to a given parent assignment.
     * @param parentsAssignment an {@link Assignment} for the parents.
     * @return a {@link Normal} univariate distribution.
     */
    public Normal getNormal(Assignment parentsAssignment) {

        double mean = intercept;
        Normal univariateNormal = new Normal(var);
        int i = 0;

        for (Variable v : parents) {
            mean = mean + coeffParents[i] * parentsAssignment.getValue(v);
            i++;
        }

        univariateNormal.setVariance(this.variance);
        univariateNormal.setMean(mean);

        return (univariateNormal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParameters() {
        double[] param = new double[this.getNumberOfParameters()];

        param[0]=this.intercept;
        System.arraycopy(this.coeffParents,0, param,1, this.coeffParents.length);

        param[param.length-1] = this.variance;

        return param;
    }

    /**
     * {@inheritDoc}
     * Note that the intercept has not been included as a free parameter.
     */
    @Override
    public int getNumberOfParameters() {
        return(coeffParents.length + 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        double value = assignment.getValue(this.var);
        return (getNormal(assignment).getLogProbability(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getNormal(assignment);
    }

    /**
     * {@inheritDoc}
     */
    public String label(){
        return "Normal|Normal";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
        this.intercept = random.nextGaussian();
        for (int j = 0; j < this.coeffParents.length; j++) {
            this.coeffParents[j]=random.nextGaussian();
        }
        //this.sd = random.nextDouble()+0.1;
        this.variance = random.nextDouble()+0.5;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("[ alpha = " +this.getIntercept() + ", ");

        for (int i=0;i<this.getCoeffParents().length;i++){
            str.append("beta"+(i+1)+" = "+ this.getCoeffParents()[i] + ", ");
        }
        str.append("var = " + this.getVariance() + " ]");

        return str.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof ConditionalLinearGaussian)
            return this.equalDist((ConditionalLinearGaussian)dist,threshold);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equalDist(ConditionalLinearGaussian dist, double threshold) {
        boolean equals = false;
        if (Math.abs(this.getIntercept() - dist.getIntercept()) <= threshold && Math.abs(this.getSd() - dist.getSd()) <= threshold) {
            equals = true;
            for (int i = 0; i < this.getCoeffParents().length; i++) {
                equals = equals && Math.abs(this.coeffParents[i] - dist.coeffParents[i]) <= threshold;
            }
        }
        return equals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_Normal_NormalParents toEFConditionalDistribution() {

        EF_Normal_NormalParents ef_normal_normalParents = new EF_Normal_NormalParents(this.getVariable(), this.getConditioningVariables());

        ef_normal_normalParents.setBeta0(this.getIntercept());
        ef_normal_normalParents.setBetas(this.coeffParents);
        ef_normal_normalParents.setVariance(this.getVariance());

        return ef_normal_normalParents;

    }
}
