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
 * ****************** ISSUE LIST ******************************
 *
 *
 *
 * 1. getConditioningVariables change to getParentsVariables()
 *
 *
 *
 * **********************************************************
 */


package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_BaseDistribution_MultinomialParents;
import eu.amidst.core.exponentialfamily.EF_Multinomial;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Random;

/**
 * This class extends the abstract class {@link ConditionalDistribution}.
 * It defines the conditional distribution of a variable with a {@link Multinomial} distribution given a set of Multinomial parents.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class Multinomial_MultinomialParents extends ConditionalDistribution {

    /**
     * Represents an array of {@link Multinomial} objects, one for each configuration of the parents.
     * These objects are ordered according to the criteria implemented in class {@link eu.amidst.core.utils.MultinomialIndex}.
     */
    private BaseDistribution_MultinomialParents<Multinomial> base;

    /**
     * Creates a new Multinomial_MultinomialParents distribution for a given BaseDistribution_MultinomialParents&lt;Multinomial&gt;.
     * @param base_ an array of {@link Multinomial} objects, one for each configuration of the parents.
     */
    public Multinomial_MultinomialParents(BaseDistribution_MultinomialParents<Multinomial> base_) {
        this.base=base_;
        this.var=this.base.getVariable();
        this.parents=this.base.getConditioningVariables();
        //this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Creates a new Multinomial_MultinomialParents distribution for a given Variable and its parents.
     * @param var1 the variable of the distribution.
     * @param parents1 the set of parents of this variable.
     */
    public Multinomial_MultinomialParents(Variable var1, List<Variable> parents1) {

        this.base = new BaseDistribution_MultinomialParents<>(var1, parents1);
        this.var = var1;
        this.parents = parents1;
        //Make them unmodifiable
        //this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Returns the list of {@link Multinomial} distributions.
     * @return the list of {@link Multinomial} distributions.
     */
    public List<Multinomial> getMultinomialDistributions() {
        return this.base.getBaseDistributions();
    }

    /**
     * Returns the {@link Multinomial} distribution for a given position.
     * @param position the position in which the multinomial distribution is extracted.
     * @return a {@link Multinomial} distribution.
     */
    public Multinomial getMultinomial(int position) {
        return this.base.getBaseDistribution(position);
    }

    /**
     * Returns the {@link Multinomial} distribution for a given parents assignment.
     * @param parentAssignment an {@link Assignment} for the set of parents.
     * @return a {@link Multinomial} distribution.
     */
    public Multinomial getMultinomial(Assignment parentAssignment) {
        return this.base.getBaseDistribution(parentAssignment);
    }

    /**
     * Sets a {@link Multinomial} distribution in a given position in the array of probabilities.
     * @param position the position in which the multinomial distribution is set.
     * @param multinomialDistribution a {@link Multinomial} distribution.
     */
    public void setMultinomial(int position, Multinomial multinomialDistribution) {
        this.base.setBaseDistribution(position, multinomialDistribution);
    }

    /**
     * Sets a {@link Multinomial} distribution in a position in the array of probabilities
     * determined by a given parents assignment.
     * @param parentAssignment an {@link Assignment} for the set of parents.
     * @param multinomialDistribution a {@link Multinomial} distribution.
     */
    public void setMultinomial(Assignment parentAssignment, Multinomial multinomialDistribution) {
        this.base.setBaseDistribution(parentAssignment, multinomialDistribution);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVar(Variable var) {
        this.var = var;
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
    public double getLogConditionalProbability(Assignment parentAssignment) {
        double value = parentAssignment.getValue(this.var);
        return this.getMultinomial(parentAssignment).getLogProbability(value);
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
    public double[] getParameters() {
        return this.base.getParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {
        int n = 0;
        for (Multinomial dist : this.getMultinomialDistributions()) {
            n += dist.getNumberOfParameters();
        }
        return n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label() {
        //TODO Explain this !!!
        // if (this.getConditioningVariables().size() == 0) {
        //Both ifs are equivalent but when reading a serializable object the first gives a NullPointerException. WHY?
        if (this.getMultinomialDistributions().size() == 1) {
            return "Multinomial";
        } else {
            return "Multinomial|Multinomial";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
        for (Multinomial multinomial : this.getMultinomialDistributions()) {
            multinomial.randomInitialization(random);
        }
    }

    /**
     * Returns the number of parent assignments.
     * @return the number of parent assignments.
     */
    public int getNumberOfParentAssignments() {
        return this.getMultinomialDistributions().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("");
        for (int i = 0; i < getNumberOfParentAssignments(); i++) {
            str.append(this.getMultinomial(i).toString());
            if (getNumberOfParentAssignments() > 1) {
                Assignment parentAssignment = MultinomialIndex.getVariableAssignmentFromIndex(this.getConditioningVariables(), i);
                str.append(" | "+parentAssignment.outputString());
                if(i < getNumberOfParentAssignments() - 1) str.append("\n");
            }
        }
        return str.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Multinomial_MultinomialParents)
            return this.equalDist((Multinomial_MultinomialParents) dist, threshold);
        return false;
    }

    /**
     * Tests if a given Multinomial_MultinomialParents distribution is equal to this Multinomial_MultinomialParents distribution.
     * @param dist a given Multinomial_MultinomialParents distribution.
     * @param threshold a threshold.
     * @return true if the two Multinomial_MultinomialParents distributions are equal, false otherwise.
     */
    public boolean equalDist(Multinomial_MultinomialParents dist, double threshold) {
        boolean equals = true;
        for (int i = 0; i < this.getNumberOfParentAssignments(); i++) {
            equals = equals && this.getMultinomial(i).equalDist(dist.getMultinomial(i), threshold);
        }
        return equals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_BaseDistribution_MultinomialParents<EF_Multinomial> toEFConditionalDistribution() {
        return (EF_BaseDistribution_MultinomialParents<EF_Multinomial>)this.base.toEFConditionalDistribution();
    }
}