/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.models;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.List;

/**
 * The DAG class represents the Directed Acyclic Graph of a {@link BayesianNetwork}.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnexample </a>  </p>
 *
 * <p> For further details about the implementation of this class using Java 8 functional-style programming look at the following paper: </p>
 *
 * <i> Masegosa et al. Probabilistic Graphical Models on Multi-Core CPUs using Java 8. IEEE-CIM (2015). </i>
 *
 */
public interface DAG  {

    /**
     * Returns the name of the DAG
     * @return a String object
     */
    String getName();

    /**
     * Sets the name of the DAG
     * @param name, a String object
     */
    void setName(String name);

    /**
     * Returns the total number of links in this DAG.
     * @return the number of links.
     */
    long getNumberOfLinks();

    /**
     * Returns the set of Variables in this DAG.
     * @return Variables containing the set of variables in this DAG.
     */
    Variables getVariables();

    /**
     * Returns the parent set of a given variable.
     * @param var a variable of type {@link Variable}.
     * @return a ParentSet containing the set of parents of the variable.
     */
    ParentSet getParentSet(Variable var);

    /**
     * Returns the list of all parent sets in this DAG.
     * @return a list of ParentSet.
     */
    List<ParentSet> getParentSets();

    /**
     * Tests if this DAG contains cycles.
     * @return a boolean indicating if this DAG contains cycles (true) or not (false).
     */
    boolean containCycles();

}