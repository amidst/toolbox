/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.core.models.impl;

import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.Serializable;
import java.util.ArrayList;
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
public class DAGImpl implements DAG, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 2889423026182605212L;

    /** Represents the set of variables. */
    private Variables variables;

    /** Represents the list of parents for each variable. */
    private List<ParentSet> parents;

    /** Represents the name of the DAG **/
    private String name = "DAG";

    /**
     * Creates a new DAG from a set of variables.
     * @param variables the set of variables of type {@link Variables}.
     */
    public DAGImpl(Variables variables) {
        this.variables = variables;
        this.parents = new ArrayList(variables.getNumberOfVars());

        for (Variable var : variables) {
            parents.add(var.getVarID(), new ParentSetImpl(var));
        }
        //this.parents = Collections.unmodifiableList(parents);
        this.variables.block();
    }

    /**
     * Returns the name of the DAG
     * @return a String object
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the DAG
     * @param name, a String object
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the total number of links in this DAG.
     * @return the number of links.
     */
    public long getNumberOfLinks(){
        return this.parents.stream().mapToInt(p -> p.getNumberOfParents()).sum();
    }

    /**
     * Returns the set of Variables in this DAG.
     * @return Variables containing the set of variables in this DAG.
     */
    public Variables getVariables() {
        return this.variables;
    }

    /**
     * Returns the parent set of a given variable.
     * @param var a variable of type {@link Variable}.
     * @return a ParentSet containing the set of parents of the variable.
     */
    public ParentSet getParentSet(Variable var) {
        return parents.get(var.getVarID());
    }

    /**
     * Returns the list of all parent sets in this DAG.
     * @return a list of ParentSet.
     */
    public List<ParentSet> getParentSets() {
        return this.parents;
    }

    /**
     * Tests if this DAG contains cycles.
     * @return a boolean indicating if this DAG contains cycles (true) or not (false).
     */
    public boolean containCycles() {

        boolean[] bDone = new boolean[this.variables.getNumberOfVars()];

        for (Variable var : this.variables) {
            bDone[var.getVarID()] = false;
        }

        for (Variable var : this.variables) {

            // find a node for which all parents are 'done'
            boolean bFound = false;

            for (Variable variable2 : this.variables) {
                if (!bDone[variable2.getVarID()]) {
                    boolean bHasNoParents = true;

                    for (Variable parent : this.getParentSet(variable2)) {
                        if (!bDone[parent.getVarID()]) {
                            bHasNoParents = false;
                        }
                    }

                    if (bHasNoParents) {
                        bDone[variable2.getVarID()] = true;
                        bFound = true;
                        break;
                    }
                }
            }

            if (!bFound) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DAGImpl dag = (DAGImpl) o;

        if (this.variables.getNumberOfVars() != dag.variables.getNumberOfVars()) {
            return false;
        } else {
            boolean eqs = true;
            for (Variable var : this.getVariables()) {
                if (!this.getParentSet(var).equals(dag.getParentSet(dag.getVariables().getVariableByName(var.getName())))) {
                    eqs = false;
                    break;
                }
            }
            return eqs;
        }
    }

    /**
     * Returns a textual representation of this DAG.
     * @return a String description of this DAG.
     */
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("DAG\n");
        for (Variable var : this.getVariables()) {
            str.append(var.getName() + " has "+ this.getParentSet(var).getNumberOfParents() + " parent(s): " + this.getParentSet(var).toString() + "\n");
        }
        return str.toString();
    }



}