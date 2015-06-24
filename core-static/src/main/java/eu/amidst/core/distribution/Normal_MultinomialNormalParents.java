/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
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

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * <h2>This class implements a conditional distribution of a normal variable given a set of multinomial and normal
 * parents.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-4
 */

public class Normal_MultinomialNormalParents extends ConditionalDistribution {



    /**
     * An array of <code>Normal_NormalParents</code> objects, one for each configuration of the multinomial parents. These objects are
     * ordered according to the criteria implemented in class utils.MultinomialIndex
     */
    private BaseDistribution_MultinomialParents<ConditionalLinearGaussian> base;


    public Normal_MultinomialNormalParents(BaseDistribution_MultinomialParents<ConditionalLinearGaussian> base_) {
        this.base=base_;
        this.var=this.base.getVariable();
        this.parents=this.base.getConditioningVariables();
        this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * The class constructor.
     * @param var1 The variable of the distribution.
     * @param parents1 The set of parent variables.
     */
    public Normal_MultinomialNormalParents(Variable var1, List<Variable> parents1) {

        this.var = var1;
        this.parents = parents1;

        this.base = new BaseDistribution_MultinomialParents<>(var1,parents1);

        this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Gets a <code>Normal_NormalParentsDistribution</code> distribution conditioned to an assignment over a set of
     * Multinomial parents. Let X and Y two sets of Normal variables, and Z a set of Multinomial. Then this method
     * computes f(X|Y,Z=z).
     * @param assignment An assignment over a set of parents. For generality reasons, apart from the Multinomial
     *                   parents, the assignment contains values for the Normal parents as well (although they are
     *                   not used in this case).
     * @return a <code>Normal_NormalParentsDistribution</code> distribution conditioned to the assignment given as
     * argument.
     */
    public ConditionalLinearGaussian getNormal_NormalParentsDistribution(Assignment assignment) {
        return this.base.getBaseDistribution(assignment);

    }
    public ConditionalLinearGaussian getNormal_NormalParentsDistribution(int i) {
        return this.base.getBaseDistribution(i);

    }

    /**
     * Sets a <code>ConditionalLinearGaussian</code> distribution to a given position in the array of distributions.
     * @param position The position in which the distribution is set.
     * @param distribution A <code>ConditionalLinearGaussian</code> distribution.
     */
    public void setNormal_NormalParentsDistribution(int position, ConditionalLinearGaussian distribution) {
        this.base.setBaseDistribution(position,distribution);
    }

    /**
     * Sets a <code>ConditionalLinearGaussian</code> distribution to the array of distributions in a position determined by
     * an given <code>Assignment</code>. Note that this assignment contains values for the Normal parents as well
     * (although they are not used in this case).
     * @param assignment An <code>Assignment</code> for the parents variables.
     * @param distribution A <code>ConditionalLinearGaussian</code> distribution.
     */
    public void setNormal_NormalParentsDistribution(Assignment assignment, ConditionalLinearGaussian distribution) {
        this.base.setBaseDistribution(assignment, distribution);
    }


    @Override
    public double[] getParameters() {
        return this.base.getParameters();
    }

    @Override
    public int getNumberOfParameters() {
        int n=0;
        for(ConditionalLinearGaussian dist:this.getDistribution()){
            n+= dist.getNumberOfParameters();
        }
        return n;
    }

    /**
     * Computes the logarithm of the evaluated density function in a point after restricting the distribution to a
     * given parent <code>Assignment</code>.
     * @param assignment An <code>Assignment</code>
     * @return A <code>double</code> with the logarithm of the corresponding density value.
     */
    public double getLogConditionalProbability(Assignment assignment) {
        return getNormal_NormalParentsDistribution(assignment).getLogConditionalProbability(assignment);
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getNormal_NormalParentsDistribution(assignment).getNormal(assignment);
    }

    public List<Variable> getMultinomialParents() {
        return base.getMultinomialParents();
    }


    public List<Variable> getNormalParents() {
        return base.getNonMultinomialParents();
    }

    public List<ConditionalLinearGaussian> getDistribution() {
        return base.getBaseDistributions();
    }

    public String label(){
        return "Normal|Multinomial,Normal";
    }

    @Override
    public void randomInitialization(Random random) {
        for (int i = 0; i < this.getNumberOfParentAssignments(); i++) {
            this.base.getBaseDistribution(i).randomInitialization(random);
        }
    }

    public int getNumberOfParentAssignments(){
        return this.base.getBaseDistributions().size();
    }


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

    @Override
    public EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents> toEFConditionalDistribution(){
        return (EF_BaseDistribution_MultinomialParents<EF_Normal_NormalParents>)this.base.toEFConditionalDistribution();
    }
}
