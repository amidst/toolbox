/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
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

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * <h2>This class implements a conditional distribution of a multinomial variable given a set of multinomial parents.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-4
 */
public class Multinomial_MultinomialParents extends ConditionalDistribution {

    /**
     * An array of <code>Multinomial</code> objects, one for each configuration of the parents. These objects are ordered
     * according to the criteria implemented in class utils.MultinomialIndex
     */
    private BaseDistribution_MultinomialParents<Multinomial> base;


    public Multinomial_MultinomialParents(BaseDistribution_MultinomialParents<Multinomial> base_) {
        this.base=base_;
        this.var=this.base.getVariable();
        this.parents=this.base.getConditioningVariables();
        this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
         * The class constructor.
         *
         * @param var1     The variable of the distribution.
         * @param parents1 The set of parents of the variable.
         */
    public Multinomial_MultinomialParents(Variable var1, List<Variable> parents1) {

        this.base = new BaseDistribution_MultinomialParents<>(var1, parents1);
        this.var = var1;
        this.parents = parents1;
        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);
    }

    public List<Multinomial> getMultinomialDistributions() {
        return this.base.getBaseDistributions();
    }

    /**
     * Sets a <code>Multinomial</code> distribution in a given position in the array of probabilities.
     *
     * @param position                The position in which the distribution is set.
     * @param multinomialDistribution A <code>Multinomial</code> object.
     */
    public void setMultinomial(int position, Multinomial multinomialDistribution) {
        this.base.setBaseDistribution(position, multinomialDistribution);
    }

    /**
     * Sets a <code>Multinomial</code> distribution in a position in the array of probabilities determined by a given
     * parents assignment.
     *
     * @param parentAssignment        An <code>Assignment</code> for the parents.
     * @param multinomialDistribution A <code>Multinomial</code> object.
     */
    public void setMultinomial(Assignment parentAssignment, Multinomial multinomialDistribution) {
        this.base.setBaseDistribution(parentAssignment, multinomialDistribution);
    }

    /**
     * Gets the <code>Multinomial</code> distribution for given a parents assignment.
     *
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @return A <code>Multinomial</code> object.
     */
    public Multinomial getMultinomial(Assignment parentAssignment) {
        return this.base.getBaseDistribution(parentAssignment);
    }

    public Multinomial getMultinomial(int position) {
        return this.base.getBaseDistribution(position);
    }

    /**
     * Computes the logarithm of the probability of the variable for a given state and a parent assignment.
     *
     * @param parentAssignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> value with the logarithm of the probability.
     */
    @Override
    public double getLogConditionalProbability(Assignment parentAssignment) {
        double value = parentAssignment.getValue(this.var);
        return this.getMultinomial(parentAssignment).getLogProbability(value);
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getMultinomial(assignment);
    }


    @Override
    public double[] getParameters() {
        return this.base.getParameters();
    }

    @Override
    public int getNumberOfParameters() {
        int n = 0;
        for (Multinomial dist : this.getMultinomialDistributions()) {
            n += dist.getNumberOfParameters();
        }
        return n;
    }

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

    @Override
    public void randomInitialization(Random random) {
        for (Multinomial multinomial : this.getMultinomialDistributions()) {
            multinomial.randomInitialization(random);
        }
    }

    public int getNumberOfParentAssignments() {
        return this.getMultinomialDistributions().size();
    }

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

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Multinomial_MultinomialParents)
            return this.equalDist((Multinomial_MultinomialParents) dist, threshold);
        return false;
    }

    public boolean equalDist(Multinomial_MultinomialParents dist, double threshold) {
        boolean equals = true;
        for (int i = 0; i < this.getNumberOfParentAssignments(); i++) {
            equals = equals && this.getMultinomial(i).equalDist(dist.getMultinomial(i), threshold);
        }
        return equals;
    }

    @Override
    public EF_BaseDistribution_MultinomialParents<EF_Multinomial> toEFConditionalDistribution() {
        return (EF_BaseDistribution_MultinomialParents<EF_Multinomial>)this.base.toEFConditionalDistribution();
    }
}