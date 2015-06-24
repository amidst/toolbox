/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.inference.messagepassing;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by andresmasegosa on 03/02/15.
 */
public class Node {

    List<Node> parents;

    List<Node> children;

    Assignment assignment;

    EF_UnivariateDistribution QDist;

    EF_ConditionalDistribution PDist;

    boolean observed=false;

    SufficientStatistics sufficientStatistics;

    boolean isDone = false;

    boolean active = true;

    Variable mainVar;

    boolean parallelActivated = true;

    Map<Variable, Node> variableToParentsNodeMap;

    Map<Node, Variable> nodeParentsToVariableMap;

    String name;

    public Node(EF_ConditionalDistribution PDist) {
         this(PDist, PDist.getVariable().getName());
    }

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

    public String getName() {
        return name;
    }

    public boolean isParallelActivated() {
        return parallelActivated;
    }

    public void setParallelActivated(boolean parallelActivated) {
        this.parallelActivated = parallelActivated;
    }

    public void resetQDist(Random random){
        this.QDist= this.mainVar.getDistributionType().newEFUnivariateDistribution().randomInitialization(random);
    }

    public void setPDist(EF_ConditionalDistribution PDist) {
        this.PDist = PDist;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public List<Node> getParents() {
        return parents;
    }

    public void setParents(List<Node> parents) {
        this.parents = parents;
        variableToParentsNodeMap = new ConcurrentHashMap();
        nodeParentsToVariableMap = new ConcurrentHashMap();

        for( Node node: parents){
            variableToParentsNodeMap.put(node.getMainVariable(), node);
            nodeParentsToVariableMap.put(node, node.getMainVariable());
        }

    }

    public Assignment getAssignment() {
        return assignment;
    }

    public boolean isObserved() {
        return observed;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
        if (this.assignment==null || Utils.isMissingValue(this.assignment.getValue(this.getMainVariable()))){
            this.observed=false;
            sufficientStatistics=null;
            //if (this.isActive()) resetQDist();
        }else {
            this.observed=true;
            sufficientStatistics = this.QDist.getSufficientStatistics(assignment);
        }
    }

    public SufficientStatistics getSufficientStatistics() {
        return sufficientStatistics;
    }

    public EF_UnivariateDistribution getQDist() {
        return (isObserved())? null: QDist;
    }

    public void setQDist(EF_UnivariateDistribution QDist) {
        this.QDist = QDist;
    }

    public MomentParameters getQMomentParameters(){
        return (isObserved())? (MomentParameters) this.sufficientStatistics: QDist.getMomentParameters();
    }

    public EF_ConditionalDistribution getPDist() {
        return PDist;
    }

    public Variable getMainVariable(){
        return this.mainVar;
    }

    public Map<Variable, MomentParameters> getMomentParents(){
        Map<Variable, MomentParameters> momentParents = new ConcurrentHashMap<>();

        //this.getParents().stream().forEach(parent -> momentParents.put(parent.getMainVariable(), parent.getQMomentParameters()));

        this.getPDist().getConditioningVariables().stream().forEach(var -> momentParents.put(var,this.variableToNodeParent(var).getQMomentParameters()));

        momentParents.put(this.getMainVariable(), this.getQMomentParameters());

        return momentParents;
    }

    public Variable nodeParentToVariable(Node parent){
        return this.nodeParentsToVariableMap.get(parent);
    }

    public Node variableToNodeParent(Variable var){
        return this.variableToParentsNodeMap.get(var);
    }

    public void setVariableToNodeParent(Variable var, Node parent){
        this.variableToParentsNodeMap.put(var, parent);
        this.nodeParentsToVariableMap.put(parent, var);
    }

    public boolean messageDoneToParent(Variable parent){

        if (!this.isObserved())
            return false;

        for (Node node : this.getParents()){
            if (node.isActive() && node.getMainVariable().getVarID()!=parent.getVarID() && !node.isObserved())
                return false;
        }

        return true;
    }

    public boolean messageDoneFromParents(){

        for (Node node : this.getParents()){
            if (node.isActive() && !node.isObserved())
                return false;
        }

        return true;
    }

    public boolean isDone(){
        return isDone || this.observed;
    }

    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }

}
