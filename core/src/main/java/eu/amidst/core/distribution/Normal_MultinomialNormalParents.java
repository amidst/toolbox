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
 ******************* ISSUES **************************
 *
 * 1. CODING: - this.multinomialParents or multinomialParents? Common criteria.
 *
 *             - methods are ordered? alphabetically?
 *
 *
 * ***************************************************
 */


package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_BaseDistribution_MultinomialParents;
import eu.amidst.core.exponentialfamily.EF_Normal_NormalParents;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Random;

/**
 * This class extends the abstract class {@link ConditionalDistribution}.
 * It defines the conditional distribution of a variable with a {@link Normal} distribution given a set of Multinomial and Normal parents.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class Normal_MultinomialNormalParents extends ConditionalDistribution {

    /**
     * An array of <code>Normal_NormalParents</code> objects, one for each configuration of the Multinomial parents.
     * These objects are ordered according to the criteria implemented in class {@link eu.amidst.core.utils.MultinomialIndex}.
     */
    private BaseDistribution_MultinomialParents<ConditionalLinearGaussian> base;

    /**
     * Creates a new Normal_MultinomialNormalParents distribution for a given BaseDistribution_MultinomialParents&lt;ConditionalLinearGaussian&gt;.
     * @param base_ an array of <code>Normal_NormalParents</code> objects, one for each configuration of the Multinomial parents.
     */
    public Normal_MultinomialNormalParents(BaseDistribution_MultinomialParents<ConditionalLinearGaussian> base_) {
        this.base=base_;
        this.var=this.base.getVariable();
        this.parents=this.base.getConditioningVariables();
        //this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Creates a new Normal_MultinomialNormalParents distribution for a given Variable and its parents.
     * @param var1 the variable of the distribution.
     * @param parents1 the set of parents of this variable.
     */
    public Normal_MultinomialNormalParents(Variable var1, List<Variable> parents1) {
        this.var = var1;
        this.parents = parents1;
        this.base = new BaseDistribution_MultinomialParents<>(var1,parents1);
        //this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Returns a {@link ConditionalLinearGaussian} distribution given an assignment over a set of Multinomial parents.
     * Assuming that X and Y are two sets of Normal variables, and Z is a set of Multinomial variables, this method computes P(X|Y,Z=z).
     * @param assignment An assignment over a set of parents. For generality reasons, apart from the Multinomial parents,
     *                   the assignment also contains values for the Normal parents (although they are not used in this case).
     * @return a {@link ConditionalLinearGaussian} distribution.
     */
    public ConditionalLinearGaussian getNormal_NormalParentsDistribution(Assignment assignment) {
        return this.base.getBaseDistribution(assignment);
    }

    /**
     * Returns a {@link ConditionalLinearGaussian} distribution given an input position in the array of distributions.
     * @param position a position in the array of distributions.
     * @return a {@link ConditionalLinearGaussian} distribution.
     */
    public ConditionalLinearGaussian getNormal_NormalParentsDistribution(int position) {
        return this.base.getBaseDistribution(position);
    }

    /**
     * Sets a {@link ConditionalLinearGaussian} distribution to a given position in the array of distributions.
     * @param position the position in which the distribution is set.
     * @param distribution a {@link ConditionalLinearGaussian} distribution.
     */
    public void setNormal_NormalParentsDistribution(int position, ConditionalLinearGaussian distribution) {
        this.base.setBaseDistribution(position,distribution);
    }

    /**
     * Sets a {@link ConditionalLinearGaussian} distribution to the array of distributions in a position determined by a given {@link Assignment}.
     * Note that this assignment also contains values for the Normal parents (although they are not used in this case).
     * @param assignment an {@link Assignment} for the parent variables.
     * @param distribution a {@link ConditionalLinearGaussian} distribution.
     */
    public void setNormal_NormalParentsDistribution(Assignment assignment, ConditionalLinearGaussian distribution) {
        this.base.setBaseDistribution(assignment, distribution);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParameters() {
        return this.base.getParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {
        int n=0;
        for(ConditionalLinearGaussian dist:this.getDistribution()){
            n+= dist.getNumberOfParameters();
        }
        return n;
    }

    /**
     * Returns the logarithm of the evaluated density function in a point after restricting the distribution to a
     * given parent {@link Assignment}.
     * @param assignment an {@link Assignment} for the parent variables.
     * @return a double value corresponding to the logarithm of the density value.
     */
    public double getLogConditionalProbability(Assignment assignment) {
        return getNormal_NormalParentsDistribution(assignment).getLogConditionalProbability(assignment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getNormal_NormalParentsDistribution(assignment).getNormal(assignment);
    }

    /**
     * Returns the list of the Multinomial parents.
     * @return the list of the Multinomial parents.
     */
    public List<Variable> getMultinomialParents() {
        return base.getMultinomialParents();
    }

    /**
     * Returns the list of the Normal parents.
     * @return the list of the Normal parents.
     */
    public List<Variable> getNormalParents() {
        return base.getNonMultinomialParents();
    }

    /**
     * Returns the list of the {@link ConditionalLinearGaussian} distributions.
     * @return the list of the {@link ConditionalLinearGaussian} distributions.
     */
    public List<ConditionalLinearGaussian> getDistribution() {
        return base.getBaseDistributions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVar(Variable var) {
        this.var= var;
        this.base.setVar(var);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConditioningVariables(List<Variable> parents) {
        this.parents = parents;
        this.base.setConditioningVariables(parents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label(){
        return "Normal|Multinomial,Normal";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
        for (int i = 0; i < this.getNumberOfParentAssignments(); i++) {
            this.base.getBaseDistribution(i).randomInitialization(random);
        }
    }

    /**
     * Returns the number of parent assignments.
     * @return the number of parent assignments.
     */
    public int getNumberOfParentAssignments(){
        return this.base.getBaseDistributions().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("");
        for (int i = 0; i < getNumberOfParentAssignments(); i++) {
            str.append(this.getNormal_NormalParentsDistribution(i).toString());
            Assignment parentAssignment = MultinomialIndex.getVariableAssignmentFromIndex(this.getMultinomialParents(), i);
            str.append(" | "+parentAssignment.outputString()+"\n");
        }

        return str.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Normal_MultinomialNormalParents)
            return this.equalDist((Normal_MultinomialNormalParents)dist,threshold);
        return false;
    }

    public boolean equalDist(Normal_MultinomialNormalParents dist, double threshold) {
        boolean equals = true;
        for (int i = 0; i < this.getDistribution().size(); i++) {
            equals = equals && this.getNormal_NormalParentsDistribution(i).equalDist(dist.getNormal_NormalParentsDistribution(i), threshold);
        }
        return equals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents> toEFConditionalDistribution(){
        return (EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents>)this.base.toEFConditionalDistribution();
    }
}
