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

import eu.amidst.core.exponentialfamily.EF_BaseDistribution_MultinomialParents;
import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Random;

/**
 * This class extends the abstract class {@link ConditionalDistribution}.
 * It defines the conditional distribution of a variable with a {@link Normal} distribution given a set of Multinomial parents.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 *
 */
public class Normal_MultinomialParents extends ConditionalDistribution {

    /** Represents an array of {@link Normal} distributions, one for each assignment of the Multinomial parents. */
    private BaseDistribution_MultinomialParents<Normal> base;

    /**
     * Creates a new Normal_MultinomialParents distribution for a given BaseDistribution_MultinomialParents&lt;Normal&gt;.
     * @param base_ an array of {@link Normal} distributions, one for each configuration of the Multinomial parents.
     */
    public Normal_MultinomialParents(BaseDistribution_MultinomialParents<Normal> base_) {
        this.base=base_;
        this.var=this.base.getVariable();
        this.parents=this.base.getConditioningVariables();
        //this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Creates a new Normal_MultinomialParents distribution for a given Variable and its parents.
     * @param var1 the continuous variable of the distribution.
     * @param parents1 the set of Multinomial parents of this variable.
     */
    public Normal_MultinomialParents(Variable var1, List<Variable> parents1) {
        this.var = var1;
        this.parents = parents1;
        this.base = new BaseDistribution_MultinomialParents<>(var1,parents1);
        //Make them unmodifiable
        //this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Returns a {@link Normal} distribution given an input position in the array of distributions.
     * @param position a position in the array of distributions.
     * @return a {@link Normal} distribution.
     */
    public Normal getNormal(int position) {
        return base.getBaseDistribution(position);
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
    public double[] getParameters() {
        return this.base.getParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {
        int n=0;
        for(Normal dist:this.getNormalDistributions()){
            n+=dist.getNumberOfParameters();
        }
        return n;
    }

    /**
     * Returns a {@link Normal} distribution given a multinomial parent assignment.
     * @param parentsAssignment an {@link Assignment} for the Multinomial parents.
     * @return a {@link Normal} distribution.
     */
     public Normal getNormal(Assignment parentsAssignment) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentsAssignment);
        return this.getNormal(position);
    }

    /**
     * Sets a {@link Normal}distribution in a given position in the array of distributions.
     * @param position the position in which the distribution is set.
     * @param normalDistribution the {@link Normal} distribution to be set.
     */
    public void setNormal(int position, Normal normalDistribution) {
        this.base.setBaseDistribution(position,normalDistribution);
    }

    /**
     * Sets a {@link Normal} distribution in a position in the array of distributions determined by a given
     * parents assignment.
     * @param parentsAssignment an {@link Assignment} for the Multinomial parents.
     * @param normalDistribution the {@link Normal} distribution to be set.
     */
    public void setNormal(Assignment parentsAssignment, Normal normalDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentsAssignment);
        this.setNormal(position, normalDistribution);
    }

    /**
     * Returns the logarithm of the evaluated density function in a point after conditioning the distribution to a
     * given parent {@link Assignment}.
     * @param assignment an {@link Assignment} for the Multinomial parents.
     * @return a double value corresponding to the logarithm of the density value.
     */
    @Override
    public double getLogConditionalProbability(Assignment assignment) {

        double value = assignment.getValue(this.var);
        return this.getNormal(assignment).getLogProbability(value);
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
    @Override
    public String label(){
        //TODO Explain this !!!
        // if (this.getConditioningVariables().size() == 0) {
        //Both ifs are equivalent but when reading a serializable object the first gives a NullPointerException. WHY?
        if (this.getNormalDistributions().size()==1){
            return "Normal";
        }
        else {
            return "Normal|Multinomial";
        }
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
     * Returns the list of Normal distributions.
     * @return the list of Normal distributions.
     */
    public List<Normal> getNormalDistributions() {
        return this.base.getBaseDistributions();
    }

    /**
     * Returns the number of parent assignments.
     * @return the number of parent assignments.
     */
    public int getNumberOfParentAssignments(){
        return getNormalDistributions().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("");
        for(int i=0;i<getNumberOfParentAssignments();i++){
            Assignment parentAssignment = MultinomialIndex.getVariableAssignmentFromIndex(this.getConditioningVariables(), i);
            str.append(this.getNormal(i).toString()+" | "+parentAssignment.outputString() +"\n");
        }
        return str.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Normal_MultinomialParents)
            return this.equalDist((Normal_MultinomialParents)dist,threshold);
        return false;
    }

    /**
     * Tests if a given Normal_MultinomialParents distribution is equal to this Normal_MultinomialParents distribution.
     * @param dist a given Normal_MultinomialParents distribution.
     * @param threshold a threshold.
     * @return true if the two Normal_MultinomialParents distributions are equal, false otherwise.
     */
    public boolean equalDist(Normal_MultinomialParents dist, double threshold) {
        boolean equals = true;
        for (int i = 0; i < this.getNormalDistributions().size(); i++) {
            equals = equals && this.getNormal(i).equalDist(dist.getNormal(i),threshold);
        }
        return equals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_BaseDistribution_MultinomialParents<EF_Normal> toEFConditionalDistribution(){
        return (EF_BaseDistribution_MultinomialParents<EF_Normal>)this.base.toEFConditionalDistribution();
    }

}
