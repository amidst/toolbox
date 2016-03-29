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

package eu.amidst.core.inference.messagepassing;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class defines and handles a Node used for message passing inference algorithms.
 */
public class Node implements Serializable{

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    /** Represents the list of parents of this Node. */
    List<Node> parents;

    /** Represents the list of children of this Node. */
    List<Node> children;

    /** Represents the {@link Assignment} associated with this Node. */
    Assignment assignment;

    /** Represents the exponential family univariate distribution of this Node. */
    EF_UnivariateDistribution QDist;

    /** Represents the exponential family conditional distribution of this Node. */
    EF_ConditionalDistribution PDist;

    /** Indicates if this Node is observed or not. */
    boolean observed=false;

    /** Represents the {@link SufficientStatistics}. */
    SufficientStatistics sufficientStatistics;

    /** Indicates whether this Node is done or not, initialized to {@code false}. */
    boolean isDone = false;

    /** Indicates whether this Node is active or not, initialized to {@code true}. */
    boolean active = true;

    /** Represents the main variable. */
    Variable mainVar;

    /** Indicates if the parallel mode is activated, initialized to {@code true}. */
    boolean parallelActivated = true;

    /** Represents a {@code Map} object that maps variables to parent nodes. */
    Map<Variable, Node> variableToParentsNodeMap;

    /** Represents a {@code Map} object that maps parent nodes to variables. */
    Map<Node, Variable> nodeParentsToVariableMap;

    /** Represents the name of this Node. */
    String name;

    /**
     * Creates a new Node given an input {@link EF_ConditionalDistribution}.
     * @param PDist an input {@link EF_ConditionalDistribution}.
     */
    public Node(EF_ConditionalDistribution PDist) {
         this(PDist, PDist.getVariable().getName());
    }

    /**
     * Creates a new Node given an input {@link EF_ConditionalDistribution} and name.
     * @param PDist an input {@link EF_ConditionalDistribution}.
     * @param name_ a {@code String} that represents the name of the node.
     */
    public Node(EF_ConditionalDistribution PDist, String name_) {
        this.PDist = PDist;
        this.mainVar = this.PDist.getVariable();
        this.QDist= this.mainVar.getDistributionType().newEFUnivariateDistribution();
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.observed=false;
        sufficientStatistics=null;
        this.name = name_;
    }

    /**
     * Returns the name of this Node.
     * @return the name of this Node.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the main {@link Variable}.
     * @return the main {@link Variable}.
     */
    public Variable getMainVariable(){
        return this.mainVar;
    }

    /**
     * Tests whether the parallel mode is activated for this Node or not.
     * @return {@code true} if the parallel mode is activated, {@code false} otherwise.
     */
    public boolean isParallelActivated() {
        return parallelActivated;
    }

    /**
     * Sets the parallel mode for this Node.
     * @param parallelActivated the parallel mode value to be set.
     */
    public void setParallelActivated(boolean parallelActivated) {
        this.parallelActivated = parallelActivated;
    }

    /**
     * Resets the exponential family univariate distribution of this Node.
     * @param random a {@link Random} object.
     */
    public void resetQDist(Random random){
        this.QDist= this.mainVar.getDistributionType().newEFUnivariateDistribution().randomInitialization(random);
    }

    /**
     * Tests whether this Node is active or not.
     * @return {@code true} if this Node is active, {@code false} otherwise.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets this Node as either active or not active.
     * @param active a boolean value to be set.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns the list of children nodes of this Node.
     * @return the list of children nodes.
     */
    public List<Node> getChildren() {
        return children;
    }

    /**
     * Sets the list of children nodes for this Node.
     * @param children the list of children nodes to be set.
     */
    public void setChildren(List<Node> children) {
        this.children = children;
    }

    /**
     * Returns the list of parent nodes of this Node.
     * @return the list of parent nodes.
     */
    public List<Node> getParents() {
        return parents;
    }

    /**
     * Sets the list of parent nodes for this Node.
     * @param parents the list of parent nodes to be set.
     */
    public void setParents(List<Node> parents) {
        this.parents = parents;
        variableToParentsNodeMap = new ConcurrentHashMap();
        nodeParentsToVariableMap = new ConcurrentHashMap();

        for( Node node: parents){
            variableToParentsNodeMap.put(node.getMainVariable(), node);
            nodeParentsToVariableMap.put(node, node.getMainVariable());
        }

    }

    /**
     * Returns the {@link Assignment} associated with this Node.
     * @return the {@link Assignment} associated with this Node.
     */
    public Assignment getAssignment() {
        return assignment;
    }

    /**
     * Sets the {@link Assignment} for this Node.
     * @param assignment the {@link Assignment} to be set.
     */
    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
        if (this.assignment==null || Utils.isMissingValue(this.assignment.getValue(this.getMainVariable()))){
            this.observed=false;
            sufficientStatistics=null;
        }else {
            this.observed=true;
            sufficientStatistics = this.QDist.getSufficientStatistics(assignment);
        }
    }

    /**
     * Tests whether this Node is observed or not.
     * @return {@code true} if this Node is observed, {@code false} otherwise.
     */
    public boolean isObserved() {
        return observed;
    }

    /**
     * Returns the {@link SufficientStatistics} for this Node.
     * @return a {@link SufficientStatistics} object.
     */
    public SufficientStatistics getSufficientStatistics() {
        return sufficientStatistics;
    }

    /**
     * Returns the exponential family conditional distribution of this Node.
     * @return the exponential family conditional distribution of this Node.
     */
    public EF_ConditionalDistribution getPDist() {
        return PDist;
    }

    /**
     * Sets the exponential family conditional distribution of this Node.
     * @param PDist the exponential family conditional distribution to be set.
     */
    public void setPDist(EF_ConditionalDistribution PDist) {
        this.PDist = PDist;
    }

    /**
     * Returns the exponential family univariate distribution of this Node.
     * @return the exponential family univariate distribution of this Node.
     */
    public EF_UnivariateDistribution getQDist() {
        return (isObserved())? null: QDist;
    }

    /**
     * Sets the exponential family univariate distribution for this Node.
     * @param QDist the exponential family univariate distribution to be set.
     */
    public void setQDist(EF_UnivariateDistribution QDist) {
        this.QDist = QDist;
    }

    /**
     * Returns the {@link MomentParameters} of the univariate exponential family distribution of this Node.
     * @return a {@link MomentParameters} object.
     */
    public MomentParameters getQMomentParameters(){
        return (isObserved())? (MomentParameters) this.sufficientStatistics: QDist.getMomentParameters();
    }

    /**
     * Returns the {@link MomentParameters} of the exponential family distributions of the parents of this Node.
     * @return a {@code Map} object that maps parent variables to their corresponding {@link MomentParameters}.
     */
    public Map<Variable, MomentParameters> getMomentParents(){
        Map<Variable, MomentParameters> momentParents = new ConcurrentHashMap<>();

        this.getPDist().getConditioningVariables().stream().forEach(var -> momentParents.put(var,this.variableToNodeParent(var).getQMomentParameters()));

        momentParents.put(this.getMainVariable(), this.getQMomentParameters());

        return momentParents;
    }

    /**
     * Converts a given node parent of this Node to a {@link Variable}.
     * @param parent a given node parent.
     * @return the parent {@link Variable}.
     */
    public Variable nodeParentToVariable(Node parent){
        return this.nodeParentsToVariableMap.get(parent);
    }

    /**
     * Converts a given variable parent of this Node to a {@link Node}.
     * @param var a given {@link Variable} parent.
     * @return the parent {@link Node}.
     */
    public Node variableToNodeParent(Variable var){
        return this.variableToParentsNodeMap.get(var);
    }

    /**
     * Sets a given {@link Variable} as a parent node for this Node.
     * @param var a given {@link Variable}.
     * @param parent a given parent {@link Node}.
     */
    public void setVariableToNodeParent(Variable var, Node parent){
        this.variableToParentsNodeMap.put(var, parent);
        this.nodeParentsToVariableMap.put(parent, var);
    }

    /**
     * Tests whether the message is sent or done from this Node to a given parent variable.
     * @param parent a given parent variable.
     * @return {@code true} if the message is done, {@code false} otherwise.
     */
    public boolean messageDoneToParent(Variable parent){

        if (!this.isObserved())
            return false;

        for (Node node : this.getParents()){
            if (node.isActive() && node.getMainVariable().getVarID()!=parent.getVarID() && !node.isObserved())
                return false;
        }

        return true;
    }

    /**
     * Tests whether the message is received or done from the parents of this Node.
     * @return {@code true} if the message is done, {@code false} otherwise.
     */
    public boolean messageDoneFromParents(){

        for (Node node : this.getParents()){
            if (node.isActive() && !node.isObserved())
                return false;
        }

        return true;
    }

    /**
     * Test whether this Node is done.
     * @return {@code true} if this Node is done, {@code false} otherwise.
     */
    public boolean isDone(){
        return isDone || this.observed;
    }

    /**
     * Sets this Node as either done or not.
     * @param isDone a boolean value to be set.
     */
    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }

}
