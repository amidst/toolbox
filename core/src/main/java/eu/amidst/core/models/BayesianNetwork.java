/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.models;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.List;
import java.util.Random;

/**
 * The BayesianNetwork class represents a Bayesian network model.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnexample </a>  </p>
 *
 * <p> For further details about the implementation of this class using Java 8 functional-style programming look at the following paper: </p>
 *
 * <i> Masegosa et al. Probabilistic Graphical Models on Multi-Core CPUs using Java 8. IEEE-CIM (2015). </i>
 *
 */
public interface BayesianNetwork {

    /**
     * Returns the name of the BN
     * @return a String object
     */
    String getName();

    /**
     * Sets the name of the BN
     * @param name, a String object
     */
    void setName(String name);
    /**
     * Returns the conditional probability distribution of a variable.
     * @param <E> a class extending {@link ConditionalDistribution}.
     * @param var a variable of type {@link Variable}.
     * @return a conditional probability distribution.
     */
    <E extends ConditionalDistribution> E getConditionalDistribution(Variable var);

    /**
     * Sets the conditional probability distribution of a variable.
     * @param var a variable of type {@link Variable}.
     * @param dist Conditional probability distribution of type {@link ConditionalDistribution}.
     */
    void setConditionalDistribution(Variable var, ConditionalDistribution dist);

    /**
     * Returns the total number of variables in this BayesianNetwork.
     * @return the number of variables.
     */
    int getNumberOfVars();

    /**
     * Returns the set of variables in this BayesianNetwork.
     * @return set of variables of type {@link Variables}.
     */
    Variables getVariables();

    /**
     * Returns the directed acyclic graph of this BayesianNetwork.
     * @return a directed acyclic graph of type {@link DAG}.
     */
    DAG getDAG();

    /**
     * Returns the parameter values of this BayesianNetwork.
     * @return an array containing the parameter values of all distributions.
     */
    double[] getParameters();



    /**
     * Returns the log probability of a valid assignment.
     * @param assignment an object of type {@link Assignment}.
     * @return the log probability of an assignment.
     */
    double getLogProbabiltyOf(Assignment assignment);

    /**
     * Returns the list of the conditional probability distributions of this BayesianNetwork.
     * @return a list of {@link ConditionalDistribution}.
     */
    List<ConditionalDistribution> getConditionalDistributions();

   /**
     * Initializes the distributions of this BayesianNetwork randomly.
     * @param random an object of type {@link java.util.Random}.
     */
    void randomInitialization(Random random);

    /**
     * Tests if two Bayesian networks are equals.
     * A two Bayesian networks are considered equals if they have an equal conditional distribution for each variable.
     * @param bnet a given BayesianNetwork to be compared with this BayesianNetwork.
     * @param threshold a threshold value.
     * @return a boolean indicating if the two BNs are equals or not.
     */
    boolean equalBNs(BayesianNetwork bnet, double threshold);

}

