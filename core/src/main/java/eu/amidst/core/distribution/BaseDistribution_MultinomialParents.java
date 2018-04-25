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
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_Distribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class extends the abstract class {@link ConditionalDistribution}.
 * It defines the conditional distribution of a variable with a base distribution given a set of Multinomial parents.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnmodifyexample </a>  </p>
 */
public class BaseDistribution_MultinomialParents<E extends Distribution> extends ConditionalDistribution {

    /** Represents the list of multinomial parents. */
    private List<Variable> multinomialParents;

    /** Represents the list of non-multinomial parents. */
    private List<Variable> nonMultinomialParents;

    /** Represents the list of base distributions. */
    private List<E> baseDistributions;

    /** Indicates if this distribution is a base conditional distribution or not. */
    private boolean isBaseConditionalDistribution;

    /**
     * Creates a new BaseDistribution_MultinomialParents given the lists of multinomial parents and distributions.
     * @param multinomialParents1 the list of multinomial parents.
     * @param distributions1 the list of distributions.
     */
    public BaseDistribution_MultinomialParents(List<Variable> multinomialParents1, List<E> distributions1) {

        if (distributions1.size() == 0) throw new IllegalArgumentException("Size of base distributions is zero");

        int size = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents1);

        if (size != distributions1.size())
            throw new IllegalArgumentException("Size of base distributions list does not match with the number of parents configurations");

        this.var = distributions1.get(0).getVariable();
        parents = new ArrayList<>();
        this.multinomialParents = multinomialParents1;
        this.nonMultinomialParents = new ArrayList<>();
        this.baseDistributions = distributions1;

        if (!(baseDistributions.get(0) instanceof UnivariateDistribution)){
            this.isBaseConditionalDistribution=true;
            for (int i = 0; i < size; i++) {
                for (Variable v : this.getBaseConditionalDistribution(i).getConditioningVariables()) {
                    if (!this.nonMultinomialParents.contains(v))
                        this.nonMultinomialParents.add(v);
                }
            }
        }else{
            this.isBaseConditionalDistribution=false;
        }

        this.parents.addAll(this.multinomialParents);
        this.parents.addAll(this.nonMultinomialParents);

        //Make them unmodifiable
        //this.multinomialParents = Collections.unmodifiableList(this.multinomialParents);
        //this.nonMultinomialParents = Collections.unmodifiableList(this.nonMultinomialParents);
        //this.parents = Collections.unmodifiableList(this.parents);
    }

    /**
     * Creates a new BaseDistribution_MultinomialParents given a variable and the list of its parents.
     * @param var_ a {@link Variable} object.
     * @param parents_ the list of parents that may include both multinomial and non-multinomial parents.
     */
    public BaseDistribution_MultinomialParents(Variable var_, List<Variable> parents_) {

        this.var = var_;
        this.multinomialParents = new ArrayList<Variable>();
        this.nonMultinomialParents = new ArrayList<Variable>();
        this.parents = parents_;

        for (Variable parent : parents) {

            if (parent.getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
                this.multinomialParents.add(parent);
            } else {
                this.nonMultinomialParents.add(parent);
            }
        }

        int size = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);
        this.baseDistributions = new ArrayList(size);

        if (nonMultinomialParents.size()==0){
            this.isBaseConditionalDistribution=false;
            for (int i = 0; i < size; i++) {
                this.baseDistributions.add((E)this.var.newUnivariateDistribution());
            }
        }else{
            this.isBaseConditionalDistribution=true;
            for (int i = 0; i < size; i++) {
                this.baseDistributions.add((E)this.var.newConditionalDistribution(this.nonMultinomialParents));
            }
        }

        //Make them unmodifiable
        //this.multinomialParents = Collections.unmodifiableList(this.multinomialParents);
        //this.nonMultinomialParents = Collections.unmodifiableList(this.nonMultinomialParents);
        //this.parents = Collections.unmodifiableList(this.parents);
    }


    /**
     * Returns the list of the multinomial parents.
     * @return the list of the multinomial parents.
     */
    public List<Variable> getMultinomialParents() {
        return multinomialParents;
    }

    /**
     * Returns the list of the multinomial parents.
     * @return the list of the non-multinomial parents.
     */
    public List<Variable> getNonMultinomialParents() {
        return nonMultinomialParents;
    }

    /**
     * Tests if this distribution is a base conditional distribution or not.
     * @return true if this distribution is a base conditional distribution, false otherwise.
     */
    public boolean isBaseConditionalDistribution() {
        return isBaseConditionalDistribution;
    }

    /**
     * Returns the total number of base distributions.
     * @return the total number of base distributions.
     */
    public int getNumberOfBaseDistributions(){
        return this.baseDistributions.size();
    }

    /**
     * Returns a base distribution given a parent assignment.
     * @param parentAssignment an {@link Assignment} for the parent set of this variable.
     * @return a base distribution.
     */
    public E getBaseDistribution(Assignment parentAssignment){
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, parentAssignment);
        return baseDistributions.get(position);
    }

    /**
     * Sets a base distribution given a parent assignment and an input base distribution.
     * @param parentAssignment an {@link Assignment} for the parent set of this variable.
     * @param baseDistribution a base distribution.
     */
    public void setBaseDistribution(Assignment parentAssignment, E baseDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentAssignment);
        this.setBaseDistribution(position,baseDistribution);
    }

    /**
     * Sets a base distribution given a position and an input base distribution.
     * @param position the position of the base distribution.
     * @param baseDistribution a base distribution.
     */
    public void setBaseDistribution(int position, E baseDistribution){
        this.baseDistributions.set(position, baseDistribution);
    }

    /**
     * Returns a base distribution given its position.
     * @param position the position of the base distribution.
     * @return a base distribution.
     */
    public E getBaseDistribution(int position){
        return this.baseDistributions.get(position);
    }

    /**
     * Returns the list of base distributions.
     * @return the list of base distributions.
     */
    public List<E> getBaseDistributions() {
        return baseDistributions;
    }

    /**
     * Returns a base conditional distribution given its position.
     * @param position the position of the base conditional distribution.
     * @return a {@link ConditionalDistribution} object.
     */
    public ConditionalDistribution getBaseConditionalDistribution(int position) {
        return (ConditionalDistribution)this.getBaseDistribution(position);
    }

    /**
     * Returns a base univariate distribution given its position.
     * @param position the position of the base univariate distribution.
     * @return a {@link UnivariateDistribution} object.
     */
    public UnivariateDistribution getBaseUnivariateDistribution(int position) {
        return (UnivariateDistribution)this.getBaseDistribution(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConditioningVariables(List<Variable> parents){
        this.parents=parents;
        this.multinomialParents = new ArrayList<Variable>();
        this.nonMultinomialParents = new ArrayList<Variable>();

        for (Variable parent : parents) {

            if (parent.getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
                this.multinomialParents.add(parent);
            } else {
                this.nonMultinomialParents.add(parent);
            }
        }

        if (this.isBaseConditionalDistribution()) {
            for (int i = 0; i < this.getNumberOfBaseDistributions(); i++) {
                this.getBaseConditionalDistribution(i).setConditioningVariables(nonMultinomialParents);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVar(Variable var) {
        this.var = var;
        for (E baseDistribution : baseDistributions) {
            baseDistribution.setVar(var);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        return this.getBaseDistribution(assignment).getLogProbability(assignment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        if (this.isBaseConditionalDistribution)
            return ((ConditionalDistribution)this.getBaseDistribution(assignment)).getUnivariateDistribution(assignment);
        else
            return (UnivariateDistribution)this.getBaseDistribution(assignment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getParameters() {

        double[] param = new double[this.getNumberOfParameters()];

        int count = 0;
        for (int i = 0; i < this.getNumberOfBaseDistributions(); i++) {
            System.arraycopy(this.getBaseDistribution(i).getParameters(), 0, param, count, this.getBaseDistribution(i).getNumberOfParameters());
            count+=this.getBaseDistribution(i).getNumberOfParameters();
        }
        return param;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfParameters() {
        return this.baseDistributions.stream().mapToInt(dist -> dist.getNumberOfParameters()).sum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label() {
        if (this.getConditioningVariables().size() == 0 || this.multinomialParents.size() == 0) {
            return this.getBaseDistribution(0).label();
        } else if (!this.isBaseConditionalDistribution) {
            return this.getBaseDistribution(0).label() + "| Multinomial";
        } else {
            return this.getBaseDistribution(0).label() + ", Multinomial";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void randomInitialization(Random random) {
        this.baseDistributions.stream().forEach(dist -> dist.randomInitialization(random));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        BaseDistribution_MultinomialParents<E> newdist = (BaseDistribution_MultinomialParents<E>)dist;

        int size = this.getNumberOfBaseDistributions();

        if (newdist.getNumberOfBaseDistributions()!=size)
            return false;

        for (int i = 0; i < size; i++) {
            if (!this.getBaseDistribution(i).equalDist(newdist.getBaseDistribution(i), threshold))
                return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("");
        for (int i = 0; i < this.getNumberOfBaseDistributions(); i++) {
            str.append(this.getBaseDistribution(i).toString());
            if (getNumberOfBaseDistributions() > 1) {
                Assignment parentAssignment = MultinomialIndex.getVariableAssignmentFromIndex(this.getMultinomialParents(),i);
                str.append(" | "+parentAssignment.outputString());
                if(i < getNumberOfBaseDistributions() - 1) str.append("\n");
            }
        }
        return str.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EF_BaseDistribution_MultinomialParents<? extends EF_Distribution> toEFConditionalDistribution(){

        if (this.isBaseConditionalDistribution()){
            List<EF_ConditionalDistribution> base_ef_dists = new ArrayList<>();

            for (int i = 0; i < this.getNumberOfBaseDistributions(); i++) {
                base_ef_dists.add(this.getBaseConditionalDistribution(i).toEFConditionalDistribution());
            }

            return new EF_BaseDistribution_MultinomialParents<>(this.multinomialParents,base_ef_dists);

        }else{
            List<EF_UnivariateDistribution> base_ef_dists = new ArrayList<>();

            for (int i = 0; i < this.getNumberOfBaseDistributions(); i++) {
                base_ef_dists.add(this.getBaseUnivariateDistribution(i).toEFUnivariateDistribution());
            }

            return new EF_BaseDistribution_MultinomialParents<>(this.multinomialParents,base_ef_dists);
        }
    }
}
