/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

/**
 ******************* ISSUE LIST **************************
 *
 * 1. (Andres) Implement DynamicBN with two BNs: one for time 0 and another for time T.
 *
 * ********************************************************
 */

package eu.amidst.dynamic.models;


import eu.amidst.corestatic.distribution.ConditionalDistribution;
import eu.amidst.corestatic.utils.Utils;
import eu.amidst.corestatic.variables.Assignment;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.corestatic.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * <h2>This class implements a dynamic Bayesian network.</h2>
 *
 * @author afalvarez@ual.es, andres@cs.aau.dk & ana@cs.aau.dk
 * @version 1.0
 * @since 2014-07-3
 *
 */
public final class DynamicBayesianNetwork implements Serializable {


    private static final long serialVersionUID = 4968590066969071698L;
    /**
     * It contains the distributions for all variables at time 0.
     */
    private List<ConditionalDistribution> distributionsTime0;

    /**
     * It contains the distributions for all variables at time T.
     */
    private List<ConditionalDistribution> distributionsTimeT;

    private DynamicDAG dynamicDAG;

    private DynamicBayesianNetwork(DynamicDAG dynamicDAG1){
        dynamicDAG = dynamicDAG1;
        this.initializeDistributions();
    }

    private DynamicBayesianNetwork(DynamicDAG dynamicDAG1, List<ConditionalDistribution> distsTime0, List<ConditionalDistribution> distsTimeT){
        dynamicDAG = dynamicDAG1;
        this.distributionsTime0=distsTime0;
        this.distributionsTimeT=distsTimeT;
    }

    public static DynamicBayesianNetwork newDynamicBayesianNetwork(DynamicDAG dynamicDAG){
        return new DynamicBayesianNetwork(dynamicDAG);
    }

    public static DynamicBayesianNetwork newDynamicBayesianNetwork(DynamicDAG dynamicDAG, List<ConditionalDistribution> distsTime0, List<ConditionalDistribution> distsTimeT) {
        return new DynamicBayesianNetwork(dynamicDAG,distsTime0,distsTimeT);
    }

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

        distributionsTimeT = Collections.unmodifiableList(this.distributionsTimeT);
        distributionsTime0 = Collections.unmodifiableList(this.distributionsTime0);

    }

    public int getNumberOfDynamicVars() {
        return this.getDynamicVariables().getNumberOfVars();
    }

    public DynamicVariables getDynamicVariables() {
        return this.dynamicDAG.getDynamicVariables();
    }

    public <E extends ConditionalDistribution> E getConditionalDistributionTimeT(Variable var) {
        return (E) this.distributionsTimeT.get(var.getVarID());
    }

    public <E extends ConditionalDistribution> E getConditionalDistributionTime0(Variable var) {
        return (E) this.distributionsTime0.get(var.getVarID());
    }

    public DynamicDAG getDynamicDAG (){
        return this.dynamicDAG;
    }

    public int getNumberOfVars() {
        return this.getDynamicVariables().getNumberOfVars();
    }

    //public List<Variable> getListOfDynamicVariables() {
    //    return this.getDynamicVariables().getListOfDynamicVariables();
    //}

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


    /*
    public Variable getVariableById(int varID) {
        return this.getListOfDynamicVariables().getVariableById(varID);
    }

    public Variable getTemporalCloneById(int varID) {
        return this.getListOfDynamicVariables().getTemporalCloneById(varID);
    }

    public Variable getInterfaceVariable(Variable variable) {
        return this.getListOfDynamicVariables().getInterfaceVariable(variable);
    }
    */

    public List<ConditionalDistribution> getConditionalDistributionsTimeT(){
        return this.distributionsTimeT;
    }

    public List<ConditionalDistribution> getConditionalDistributionsTime0(){
        return this.distributionsTime0;
    }

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

    public void randomInitialization(Random random){
        this.distributionsTimeT.stream().forEach(w -> w.randomInitialization(random));
        this.distributionsTime0.stream().forEach(w -> w.randomInitialization(random));
    }

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
