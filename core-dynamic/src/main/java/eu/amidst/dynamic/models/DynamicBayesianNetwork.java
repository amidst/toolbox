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
 * 1. (Andres) Implement DynamicBN with two BNs: one for time 0 and another for time T.
 *
 * ********************************************************
 */

package eu.amidst.dynamic.models;


import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * The DynamicBayesianNetwork class represents a Bayesian network model.
 */
public final class DynamicBayesianNetwork implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4968590066969071698L;

    /** Represents the list of conditional probability distributions defining the Dynamic Bayesian network parameters at Time 0. */
    private List<ConditionalDistribution> distributionsTime0;

    /** Represents the list of conditional probability distributions defining the Dynamic Bayesian network parameters at Time T. */
    private List<ConditionalDistribution> distributionsTimeT;

    /** Represents the Dynamic Directed Acyclic Graph ({@link DynamicDAG}) defining the Dynamic Bayesian network graphical structure. */
    private DynamicDAG dynamicDAG;

    /**
     * Creates a new DynamicBayesianNetwork from a DynamicDAG object.
     * @param dynamicDAG1 a Dynamic directed acyclic graph.
     */
    public DynamicBayesianNetwork(DynamicDAG dynamicDAG1){
        dynamicDAG = dynamicDAG1;
        this.initializeDistributions();
    }

    /**
     * Creates a new DynamicBayesianNetwork from a DynamicDAG and a list of distributions for both at Time 0 and T.
     * @param dynamicDAG1 a Dynamic directed acyclic graph.
     * @param distsTime0 a list of conditional probability distributions at Time 0.
     * @param distsTimeT a list of conditional probability distributions at Time T.
     */
    public DynamicBayesianNetwork(DynamicDAG dynamicDAG1, List<ConditionalDistribution> distsTime0, List<ConditionalDistribution> distsTimeT){
        dynamicDAG = dynamicDAG1;
        this.distributionsTime0=distsTime0;
        this.distributionsTimeT=distsTimeT;
    }

    /**
     * Returns the Bayesian network at Time 0.
     * @return a {@link BayesianNetwork} object.
     */
    public BayesianNetwork toBayesianNetworkTime0(){
        DAG dagTime0 = this.getDynamicDAG().toDAGTime0();
        BayesianNetwork bnTime0 = new BayesianNetwork(dagTime0);
        for (Variable dynamicVar : this.getDynamicVariables()) {
            Variable staticVar = dagTime0.getVariables().getVariableByName(dynamicVar.getName());
            ConditionalDistribution deepCopy = Serialization.deepCopy(this.getConditionalDistributionTime0(dynamicVar));
            deepCopy.setVar(staticVar);
            List<Variable> newParents = deepCopy.getConditioningVariables().stream().map(var -> dagTime0.getVariables().getVariableByName(var.getName())).collect(Collectors.toList());
            deepCopy.setConditioningVariables(newParents);
            bnTime0.setConditionalDistribution(staticVar,deepCopy);
        }
        return bnTime0;
    }

    /**
     * Returns the Bayesian network at Time T.
     * @return a {@link BayesianNetwork} object.
     */
    public BayesianNetwork toBayesianNetworkTimeT(){
        DAG dagTimeT = this.getDynamicDAG().toDAGTimeT();
        BayesianNetwork bnTimeT = new BayesianNetwork(dagTimeT);
        for (Variable dynamicVar : this.getDynamicVariables()) {
            Variable staticVar = dagTimeT.getVariables().getVariableByName(dynamicVar.getName());
            ConditionalDistribution deepCopy = Serialization.deepCopy(this.getConditionalDistributionTimeT(dynamicVar));
            deepCopy.setVar(staticVar);
            List<Variable> newParents = deepCopy.getConditioningVariables().stream().map(var -> dagTimeT.getVariables().getVariableByName(var.getName())).collect(Collectors.toList());
            deepCopy.setConditioningVariables(newParents);
            bnTimeT.setConditionalDistribution(staticVar,deepCopy);
        }
        return bnTimeT;
    }

    /**
     * Initializes the distributions of this DynamicBayesianNetwork.
     * The initialization is performed for each variable at time ) and T depending on its distribution type
     * as well as the distribution type of its parent set (if that variable has parents).
     */
    private void initializeDistributions() {

        //Parents should have been assigned before calling this method (from dynamicmodelling.models)
        this.distributionsTime0 = new ArrayList(this.getDynamicVariables().getNumberOfVars());
        this.distributionsTimeT = new ArrayList(this.getDynamicVariables().getNumberOfVars());

        for (Variable var : this.getDynamicVariables()) {
            int varID = var.getVarID();

            /* Distributions at time t */
            this.distributionsTimeT.add(varID, var.newConditionalDistribution(this.dynamicDAG.getParentSetTimeT(var).getParents()));
            this.dynamicDAG.getParentSetTimeT(var).blockParents();

            /* Distributions at time 0 */
            this.distributionsTime0.add(varID, var.newConditionalDistribution(this.dynamicDAG.getParentSetTime0(var).getParents()));
            this.dynamicDAG.getParentSetTime0(var).blockParents();
        }

        //distributionsTimeT = Collections.unmodifiableList(this.distributionsTimeT);
        //distributionsTime0 = Collections.unmodifiableList(this.distributionsTime0);
    }

    /**
     * Sets the conditional probability distribution of a variable at Time 0.
     * @param var a variable of type {@link Variable}.
     * @param dist a Conditional probability distribution of type {@link ConditionalDistribution}.
     */
    public void setConditionalDistributionTime0(Variable var, ConditionalDistribution dist){
        this.distributionsTime0.set(var.getVarID(),dist);
    }

    /**
     * Sets the conditional probability distribution of a variable at Time T.
     * @param var a variable of type {@link Variable}.
     * @param dist a Conditional probability distribution of type {@link ConditionalDistribution}.
     */
    public void setConditionalDistributionTimeT(Variable var, ConditionalDistribution dist){
        this.distributionsTimeT.set(var.getVarID(),dist);
    }

    /**
     * Sets the name of the dynamic BN.
     * @param name a {@code String} object.
     */
    public void setName(String name) {
        this.dynamicDAG.setName(name);
    }

    /**
     * Returns the name of the dynamic BN.
     * @return a {@code String} object.
     */
    public String getName() {
        return this.dynamicDAG.getName();
    }

    /**
     * Returns the total number of variables in this DynamicBayesianNetwork.
     * @return an {@code int} that represents the number of variables.
     */
    public int getNumberOfDynamicVars() {
        return this.getDynamicVariables().getNumberOfVars();
    }

    /**
     * Returns the set of dynamic variables in this DynamicBayesianNetwork.
     * @return set of variables of type {@link DynamicVariables}.
     */
    public DynamicVariables getDynamicVariables() {
        return this.dynamicDAG.getDynamicVariables();
    }

    /**
     * Returns the conditional probability distribution at Time T of a given {@link Variable}.
     * @param var a variable of type {@link Variable}.
     * @param <E> a class extending {@link ConditionalDistribution}.
     * @return a conditional probability distribution.
     */
    public <E extends ConditionalDistribution> E getConditionalDistributionTimeT(Variable var) {
        return (E) this.distributionsTimeT.get(var.getVarID());
    }

    /**
     * Returns the conditional probability distribution at Time 0 of a given {@link Variable}.
     * @param var a variable of type {@link Variable}.
     * @param <E> a class extending {@link ConditionalDistribution}.
     * @return a conditional probability distribution.
     */
    public <E extends ConditionalDistribution> E getConditionalDistributionTime0(Variable var) {
        return (E) this.distributionsTime0.get(var.getVarID());
    }

    /**
     * Returns the dynamic directed acyclic graph of this DynamicBayesianNetwork.
     * @return a dynamic directed acyclic graph of type {@link DynamicDAG}.
     */
    public DynamicDAG getDynamicDAG (){
        return this.dynamicDAG;
    }

    /**
     * Returns the total number of variables in this DynamicBayesianNetwork.
     * @return an {@code int} that represents the number of variables.
     */
    public int getNumberOfVars() {
        return this.getDynamicVariables().getNumberOfVars();
    }

    /**
     * Returns the log probability of a valid assignment at Time T.
     * @param assignment an object of type {@link Assignment}.
     * @return a {@code double} that represents the log probability of the assignment.
     */
    public double getLogProbabiltyOfFullAssignmentTimeT(Assignment assignment){
        double logProb = 0;
        for (Variable var: this.getDynamicVariables()){
            if (assignment.getValue(var)== Utils.missingValue()) {
                throw new UnsupportedOperationException("This method can not compute the probabilty of a partial assignment.");
            }
            logProb += this.distributionsTimeT.get(var.getVarID()).getLogConditionalProbability(assignment);
        }
        return logProb;
    }

    /**
     * Returns the log probability of a valid assignment at Time 0.
     * @param assignment an object of type {@link Assignment}.
     * @return a {@code double} that represents the log probability of the assignment.
     */
    public double getLogProbabiltyOfFullAssignmentTime0(Assignment assignment){
        double logProb = 0;
        for (Variable var: this.getDynamicVariables()){
            if (assignment.getValue(var) == Utils.missingValue()) {
                throw new UnsupportedOperationException("This method can not compute the probabilty of a partial assignment.");
            }
            logProb += this.distributionsTime0.get(var.getVarID()).getLogConditionalProbability(assignment);
        }
        return logProb;
    }

    /**
     * Returns the list of the conditional probability distributions at Time T of this DynamicBayesianNetwork.
     * @return a list of {@link ConditionalDistribution}.
     */
    public List<ConditionalDistribution> getConditionalDistributionsTimeT(){
        return this.distributionsTimeT;
    }

    /**
     * Returns the list of the conditional probability distributions at Time 0 of this DynamicBayesianNetwork.
     * @return a list of {@link ConditionalDistribution}.
     */
    public List<ConditionalDistribution> getConditionalDistributionsTime0(){
        return this.distributionsTime0;
    }

    /**
     * Returns a textual representation of this DynamicBayesianNetwork.
     * @return a String description of this DynamicBayesianNetwork.
     */
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("Dynamic Bayesian Network Time 0:\n");
        for (Variable var: this.getDynamicVariables()){

            if (this.getDynamicDAG().getParentSetTime0(var).getNumberOfParents()==0){
                str.append("P(" + var.getName()+") follows a ");
                str.append(this.getConditionalDistributionTime0(var).label()+"\n");
            }else {
                str.append("P(" + var.getName() + " | ");

                for (Variable parent : this.getDynamicDAG().getParentSetTime0(var)) {
                    str.append(parent.getName() + " , ");
                }
                if (this.getDynamicDAG().getParentSetTime0(var).getNumberOfParents() > 0){
                    str.delete(str.length() - 3, str.length());
                }
                str.append(") follows a ");
                str.append(this.getConditionalDistributionTime0(var).label() + "\n");
            }
            //Variable distribution
            str.append(this.getConditionalDistributionTime0(var).toString() + "\n");
        }

        str.append("\nDynamic Bayesian Network Time T:\n");

        for (Variable var: this.getDynamicVariables()){

            if (this.getDynamicDAG().getParentSetTimeT(var).getNumberOfParents()==0){
                str.append("P(" + var.getName()+") follows a ");
                str.append(this.getConditionalDistributionTimeT(var).label()+"\n");
            }else {
                str.append("P(" + var.getName() + " | ");

                for (Variable parent : this.getDynamicDAG().getParentSetTimeT(var)) {
                    str.append(parent.getName() + " , ");
                }
                if (this.getDynamicDAG().getParentSetTimeT(var).getNumberOfParents() > 0){
                    str.delete(str.length() - 3, str.length());
                }
                str.append(") follows a ");
                str.append(this.getConditionalDistributionTimeT(var).label() + "\n");
            }
            //Variable distribution
            str.append(this.getConditionalDistributionTimeT(var).toString() + "\n");
        }
        return str.toString();
    }

    /**
     * Initializes the distributions of this DynamicBayesianNetwork randomly.
     * @param random an object of type {@link java.util.Random}.
     */
    public void randomInitialization(Random random){
        this.distributionsTimeT.stream().forEach(w -> w.randomInitialization(random));
        this.distributionsTime0.stream().forEach(w -> w.randomInitialization(random));
    }

    /**
     * Tests if two Dynamic Bayesian networks are equals.
     * Two Dynamic Bayesian networks are considered equals if they have an equal conditional distribution for each variable at both Time 0 and T.
     * @param bnet a given DynamicBayesianNetwork to be compared with this DynamicBayesianNetwork.
     * @param threshold a threshold value.
     * @return a boolean indicating if the two DynamicBNs are equals or not.
     */
    public boolean equalDBNs(DynamicBayesianNetwork bnet, double threshold) {
        boolean equals = true;
        if (this.getDynamicDAG().equals(bnet.getDynamicDAG())){
            for (Variable var : this.getDynamicVariables()) {
                equals = equals && this.getConditionalDistributionTime0(var).equalDist(bnet.getConditionalDistributionTime0(var), threshold) && this.getConditionalDistributionTimeT(var).equalDist(bnet.getConditionalDistributionTimeT(var), threshold);
            }
        }
        return equals;
    }

}
