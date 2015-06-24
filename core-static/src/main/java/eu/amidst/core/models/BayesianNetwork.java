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
 * 1. (Andres) getListOfVariables should return a Set instead of a List.
 *
 * ********************************************************
 */

package eu.amidst.core.models;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by afa on 02/07/14.
 */


public final class BayesianNetwork implements Serializable {


    private static final long serialVersionUID = 4107783324901370839L;
    private List<ConditionalDistribution> distributions;

    private DAG dag;

    public static BayesianNetwork newBayesianNetwork(DAG dag) {
        return new BayesianNetwork(dag);
    }

    public static BayesianNetwork newBayesianNetwork(DAG dag, List<ConditionalDistribution> dists) {
        return new BayesianNetwork(dag, dists);
    }

    private BayesianNetwork(DAG dag) {
        this.dag = dag;
        initializeDistributions();
    }

    private BayesianNetwork(DAG dag, List<ConditionalDistribution> dists) {
        this.dag = dag;
        this.distributions = dists;
    }

    public <E extends ConditionalDistribution> E getConditionalDistribution(Variable var) {
        return (E) distributions.get(var.getVarID());
    }

    public void setConditionalDistribution(Variable var, ConditionalDistribution dist){
        this.distributions.set(var.getVarID(),dist);
    }

    public int getNumberOfVars() {
        return this.getDAG().getStaticVariables().getNumberOfVars();
    }

    public Variables getStaticVariables() {
        return this.getDAG().getStaticVariables();
    }

    public DAG getDAG() {
        return dag;
    }

    // public List<Variable> getListOfVariables() {
    //     return this.getStaticVariables().getListOfVariables();
    // }


    public double[] getParameters(){

        int size = this.distributions.stream().mapToInt(dist -> dist.getNumberOfParameters()).sum();

        double[] param = new double[size];

        int count = 0;

        for (Distribution dist : this.distributions){
            System.arraycopy(dist.getParameters(), 0, param, count, dist.getNumberOfParameters());
            count+=dist.getNumberOfParameters();
        }

        return param;
    }


    private void initializeDistributions() {


        this.distributions = new ArrayList(this.getNumberOfVars());


        /* Initialize the distribution for each variable depending on its distribution type
        as well as the distribution type of its parent set (if that variable has parents)
         */
        for (Variable var : getStaticVariables()) {
            ParentSet parentSet = this.getDAG().getParentSet(var);

            int varID = var.getVarID();
            this.distributions.add(varID, var.newConditionalDistribution(parentSet.getParents()));
            parentSet.blockParents();
        }

        this.distributions = Collections.unmodifiableList(this.distributions);
    }

    public double getLogProbabiltyOf(Assignment assignment) {
        double logProb = 0;
        for (Variable var : this.getStaticVariables()) {
            if (assignment.getValue(var) == Utils.missingValue()) {
                throw new UnsupportedOperationException("This method can not compute the probabilty of a partial assignment.");
            }

            logProb += this.distributions.get(var.getVarID()).getLogConditionalProbability(assignment);
        }
        return logProb;
    }

    public List<ConditionalDistribution> getConditionalDistributions() {
        return this.distributions;
    }

    public String toString() {

        StringBuilder str = new StringBuilder();
        str.append("Bayesian Network:\n");

        for (Variable var : this.getStaticVariables()) {

            if (this.getDAG().getParentSet(var).getNumberOfParents() == 0) {
                str.append("P(" + var.getName() + ") follows a ");
                str.append(this.getConditionalDistribution(var).label() + "\n");
            } else {
                str.append("P(" + var.getName() + " | ");

                for (Variable parent : this.getDAG().getParentSet(var)) {
                    str.append(parent.getName() + ", ");
                }
                str.delete(str.length()-2,str.length());
                if (this.getDAG().getParentSet(var).getNumberOfParents() > 0) {
                    str.substring(0, str.length() - 2);
                    str.append(") follows a ");
                    str.append(this.getConditionalDistribution(var).label() + "\n");
                }
            }
            //Variable distribution
            str.append(this.getConditionalDistribution(var).toString() + "\n");
        }
        return str.toString();
    }

    public void randomInitialization(Random random) {
        this.distributions.stream().forEach(w -> w.randomInitialization(random));
    }

    public boolean equalBNs(BayesianNetwork bnet, double threshold) {
        boolean equals = true;
        if (this.getDAG().equals(bnet.getDAG())){
            for (Variable var : this.getStaticVariables()) {
                equals = equals && this.getConditionalDistribution(var).equalDist(bnet.getConditionalDistribution(var), threshold);
            }
        }
        return equals;
    }

    public static String listOptions() {
        return  classNameID();
    }

    public static String listOptionsRecursively() {
        return listOptions()
                + "\n" +  "test";
    }

    public static String classNameID() {
        return "BayesianNetwork";
    }

    public static void loadOptions() {

    }
}

