/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_BaseDistribution_MultinomialParents;
import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * <h2>This class implements a conditional distribution of a normal variable given a set of multinomial parents.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-4
 */
public class Normal_MultinomialParents extends ConditionalDistribution {



    /**
     * An array of normal distribution, one for each assignment of the multinomial parents
     */
    private BaseDistribution_MultinomialParents<Normal> base;

    public Normal_MultinomialParents(BaseDistribution_MultinomialParents<Normal> base_) {
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
    public Normal_MultinomialParents(Variable var1, List<Variable> parents1) {
        this.var = var1;
        this.parents = parents1;

        this.base = new BaseDistribution_MultinomialParents<>(var1,parents1);

        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);

    }

    public Normal getNormal(int position) {
        return base.getBaseDistribution(position);
    }


    @Override
    public double[] getParameters() {
        return this.base.getParameters();
    }

    @Override
    public int getNumberOfParameters() {
        int n=0;
        for(Normal dist:this.getNormalDistributions()){
            n+=dist.getNumberOfParameters();
        }
        return n;
    }

    /**
     * Gets the corresponding univariate normal distribution after conditioning the distribution to a multinomial
     * parent assignment.
     * @param parentsAssignment An <code>Assignment</code> for the parents.
     * @return A <code>Normal</code> object with the univariate distribution.
     */
     public Normal getNormal(Assignment parentsAssignment) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentsAssignment);
        return this.getNormal(position);
    }

    /**
     * Sets a <code>Normal</code> distribution in a given position in the array of distributions.
     * @param position The position in which the distribution is set.
     * @param normalDistribution The <code>Normal</code> distribution to be set.
     */
    public void setNormal(int position, Normal normalDistribution) {
        this.base.setBaseDistribution(position,normalDistribution);
    }

    /**
     * Sets a <code>Multinomial</code> distribution in a position in the array of distributions determined by a given
     * parents assignment.
     * @param parentsAssignment An <code>Assignment</code> for the parents.
     * @param normalDistribution The <code>Normal</code> distribution to be set.
     */
    public void setNormal(Assignment parentsAssignment, Normal normalDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentsAssignment);
        this.setNormal(position, normalDistribution);
    }

    /**
     * Computes the logarithm of the evaluated density function in a point after conditioning the distribution to a
     * given parent <code>Assignment</code>.
     * @param assignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> with the logarithm of the corresponding density value.
     */
    @Override
    public double getLogConditionalProbability(Assignment assignment) {

        double value = assignment.getValue(this.var);
        return this.getNormal(assignment).getLogProbability(value);
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getNormal(assignment);
    }

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

    @Override
    public void randomInitialization(Random random) {
        for (int i = 0; i < this.getNumberOfParentAssignments(); i++) {
            this.base.getBaseDistribution(i).randomInitialization(random);
        }
    }

    public List<Normal> getNormalDistributions() {
        return this.base.getBaseDistributions();
    }

    public int getNumberOfParentAssignments(){
        return getNormalDistributions().size();
    }

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

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist instanceof Normal_MultinomialParents)
            return this.equalDist((Normal_MultinomialParents)dist,threshold);
        return false;
    }

    public boolean equalDist(Normal_MultinomialParents dist, double threshold) {
        boolean equals = true;
        for (int i = 0; i < this.getNormalDistributions().size(); i++) {
            equals = equals && this.getNormal(i).equalDist(dist.getNormal(i),threshold);
        }
        return equals;
    }

    @Override
    public EF_BaseDistribution_MultinomialParents<EF_Normal> toEFConditionalDistribution(){
        return (EF_BaseDistribution_MultinomialParents<EF_Normal>)this.base.toEFConditionalDistribution();
    }

}
