/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
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
 * The BayesianNetwork class represents a Bayesian network model.
 */
public final class BayesianNetwork implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    /** Represents the list of conditional probability distributions defining the Bayesian network parameters. */
    private List<ConditionalDistribution> distributions;

    /** Represents the Directed Acyclic Graph ({@link DAG}) defining the Bayesian network graphical structure. */
    private DAG dag;

    /**
     * Creates a new BayesianNetwork from a dag.
     * @param dag a directed acyclic graph.
     * @return a BayesianNetwork.
     */
    public static BayesianNetwork newBayesianNetwork(DAG dag) {
        return new BayesianNetwork(dag);
    }

    /**
     * Creates a new BayesianNetwork from a dag and a list of distributions.
     * @param dag a directed acyclic graph.
     * @param dists a list of conditional probability distributions.
     * @return a BayesianNetwork.
     */
    public static BayesianNetwork newBayesianNetwork(DAG dag, List<ConditionalDistribution> dists) {
        return new BayesianNetwork(dag, dists);
    }

    /**
     * Creates a new BayesianNetwork from a dag.
     * @param dag a directed acyclic graph.
     */
    private BayesianNetwork(DAG dag) {
        this.dag = dag;
        initializeDistributions();
    }

    /**
     * Creates a new BayesianNetwork from a dag and a list of distributions.
     * @param dag a directed acyclic graph.
     * @param dists a list of conditional probability distributions.
     */
    private BayesianNetwork(DAG dag, List<ConditionalDistribution> dists) {
        this.dag = dag;
        this.distributions = dists;
    }

    /**
     * Returns the conditional probability distribution of a variable.
     * @param var a variable of type {@link Variable}.
     * @return a conditional probability distribution.
     */
    public <E extends ConditionalDistribution> E getConditionalDistribution(Variable var) {
        return (E) distributions.get(var.getVarID());
    }

    /**
     * Sets the conditional probability distribution of a variable.
     * @param var a variable of type {@link Variable}.
     * @param dist Conditional probability distribution of type {@link ConditionalDistribution}.
     */
    public void setConditionalDistribution(Variable var, ConditionalDistribution dist){
        this.distributions.set(var.getVarID(),dist);
    }

    /**
     * Returns the total number of variables in this BayesianNetwork.
     * @return the number of variables.
     */
    public int getNumberOfVars() {
        return this.getDAG().getStaticVariables().getNumberOfVars();
    }

    /**
     * Returns the set of variables in this BayesianNetwork.
     * @return set of variables of type {@link Variables}.
     */
    public Variables getStaticVariables() {
        return this.getDAG().getStaticVariables();
    }

    /**
     * Returns the directed acyclic graph of this BayesianNetwork.
     * @return a directed acyclic graph of type {@link DAG}.
     */
    public DAG getDAG() {
        return dag;
    }

    /**
     * Returns the parameter values of this BayesianNetwork.
     * @return an array containing the parameter values of all distributions.
     */
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

    /**
     * Initializes the distributions of this BayesianNetwork.
     * The initialization is performed for each variable depending on its distribution type.
     * as well as the distribution type of its parent set (if that variable has parents).
     */
    private void initializeDistributions() {

        this.distributions = new ArrayList(this.getNumberOfVars());

        for (Variable var : getStaticVariables()) {
            ParentSet parentSet = this.getDAG().getParentSet(var);
            int varID = var.getVarID();
            this.distributions.add(varID, var.newConditionalDistribution(parentSet.getParents()));
            parentSet.blockParents();
        }

        this.distributions = Collections.unmodifiableList(this.distributions);
    }

    /**
     * Returns the log probability of a valid assignment.
     * @param assignment an object of type {@link Assignment}.
     * @return the log probability of an assignment.
     */
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

    /**
     * Returns the list of the conditional probability distributions of this BayesianNetwork.
     * @return a list of {@link ConditionalDistribution}.
     */
    public List<ConditionalDistribution> getConditionalDistributions() {
        return this.distributions;
    }

    /**
     * Returns a textual representation of this BayesianNetwork.
     * @return a String description of this BayesianNetwork.
     */
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

    /**
     * Initializes the distributions of this BayesianNetwork randomly.
     * @param random an object of type {@link java.util.Random}.
     */
    public void randomInitialization(Random random) {
        this.distributions.stream().forEach(w -> w.randomInitialization(random));
    }

    /**
     * Tests if two Bayesian networks are equals.
     * A two Bayesian networks are considered equals if they have an equal conditional distribution for each variable.
     * @param bnet a given BayesianNetwork to be compared with this BayesianNetwork.
     * @param threshold a threshold value.
     * @return a boolean indicating if the two BNs are equals or not.
     */
    public boolean equalBNs(BayesianNetwork bnet, double threshold) {
        boolean equals = true;
        if (this.getDAG().equals(bnet.getDAG())){
            for (Variable var : this.getStaticVariables()) {
                equals = equals && this.getConditionalDistribution(var).equalDist(bnet.getConditionalDistribution(var), threshold);
            }
        }
        return equals;
    }

    /**
     * Returns this class name.
     * @return a String representing this class name.
     */
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

