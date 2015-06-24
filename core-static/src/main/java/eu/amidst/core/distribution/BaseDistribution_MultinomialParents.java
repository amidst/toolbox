/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_BaseDistribution_MultinomialParents;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_Distribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 24/02/15.
 */
public class BaseDistribution_MultinomialParents<E extends Distribution> extends ConditionalDistribution {

    /**
     * The list of multinomial parents
     */
    private List<Variable> multinomialParents;

    private List<Variable> nonMultinomialParents;

    private List<E> baseDistributions;

    private boolean isBaseConditionalDistribution;

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
        this.multinomialParents = Collections.unmodifiableList(this.multinomialParents);
        this.nonMultinomialParents = Collections.unmodifiableList(this.nonMultinomialParents);
        this.parents = Collections.unmodifiableList(this.parents);
    }

    public BaseDistribution_MultinomialParents(Variable var_, List<Variable> parents_) {

        this.var = var_;
        this.multinomialParents = new ArrayList<Variable>();
        this.nonMultinomialParents = new ArrayList<Variable>();
        this.parents = parents_;

        for (Variable parent : parents) {

            if (parent.isMultinomial()) {
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
        this.multinomialParents = Collections.unmodifiableList(this.multinomialParents);
        this.nonMultinomialParents = Collections.unmodifiableList(this.nonMultinomialParents);
        this.parents = Collections.unmodifiableList(this.parents);
    }

    public List<Variable> getMultinomialParents() {
        return multinomialParents;
    }

    public List<Variable> getNonMultinomialParents() {
        return nonMultinomialParents;
    }

    public boolean isBaseConditionalDistribution() {
        return isBaseConditionalDistribution;
    }

    public int getNumberOfBaseDistributions(){
        return this.baseDistributions.size();
    }
    public E getBaseDistribution(Assignment parentAssignment){
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.multinomialParents, parentAssignment);
        return baseDistributions.get(position);
    }

    public void setBaseDistribution(Assignment parentAssignment, E baseDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentAssignment);
        this.setBaseDistribution(position,baseDistribution);
    }

    public void setBaseDistribution(int position, E baseDistribution){
        this.baseDistributions.set(position, baseDistribution);
    }

    public E getBaseDistribution(int position){
        return this.baseDistributions.get(position);
    }

    public List<E> getBaseDistributions() {
        return baseDistributions;
    }

    public ConditionalDistribution getBaseConditionalDistribution(int position) {
        return (ConditionalDistribution)this.getBaseDistribution(position);
    }

    public UnivariateDistribution getBaseUnivariateDistribution(int position) {
        return (UnivariateDistribution)this.getBaseDistribution(position);
    }

    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        return this.getBaseDistribution(assignment).getLogProbability(assignment);
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        if (this.isBaseConditionalDistribution)
            return ((ConditionalDistribution)this.getBaseDistribution(assignment)).getUnivariateDistribution(assignment);
        else
            return (UnivariateDistribution)this.getBaseDistribution(assignment);
    }

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

    @Override
    public int getNumberOfParameters() {
        return this.baseDistributions.stream().mapToInt(dist -> dist.getNumberOfParameters()).sum();
    }

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

    @Override
    public void randomInitialization(Random random) {
        this.baseDistributions.stream().forEach(dist -> dist.randomInitialization(random));
    }

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
