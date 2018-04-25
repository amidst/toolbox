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

package eu.amidst.core.models;
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
public class DAG implements Serializable {

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
    public DAG(Variables variables) {
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

        DAG dag = (DAG) o;

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


    /**
     * This class implements the interface {@link ParentSet}.
     * It is used to handle the parent set operations.
     */
    private static final class ParentSetImpl implements ParentSet, Serializable {

        /**
         * Represents a serialVersionUID value.
         */
        private static final long serialVersionUID = 3580889238865345208L;

        /**
         * Represents the main variable.
         */
        private Variable mainVar;

        /**
         * Represents the parent set of mainVar.
         */
        private List<Variable> vars;

        /**
         * Creates a new empty parent set for a given variable.
         * @param mainVar1 the main variable.
         */
        private ParentSetImpl(Variable mainVar1) {
            mainVar = mainVar1;
            this.vars = new ArrayList<Variable>();
        }

        /**
         * Returns the mainVar.
         * @return the mainVar.
         */
        @Override
        public Variable getMainVar() {
            return mainVar;
        }

        /**
         * Adds a given Variable as a new parent of the main variable.
         * That is, it adds a directed link from the new parent to the mainVar in the DAG.
         * @param var the new parent that will be added.
         */
        public void addParent(Variable var) {
            if (!mainVar.getDistributionType().isParentCompatible(var)){
                throw new IllegalArgumentException("Adding a parent var " +var.getName()+ " of type "+var.getDistributionTypeEnum().toString()+" which is not compatible " +
                        "with children variable "+this.getMainVar().getName()+" of type "+this.mainVar.getDistributionTypeEnum().toString());
            }

            if (this.contains(var)) {
                throw new IllegalArgumentException("Trying to add a duplicated parent");
            }

            vars.add(var);
        }

        /**
         * Removes a given Variable from the parent set of the main variable.
         * That is, it removes the directed link between a given Variable and the mainVar in the DAG.
         * @param var the parent variable that will be removed.
         */
        public void removeParent(Variable var) {
            vars.remove(var);
        }

        /**
         * Returns a list of parents.
         * @return the list of parents.
         */
        public List<Variable> getParents() {
            return vars;
        }

        /**
         * Returns the number of parents.
         * @return the number of parents.
         */
        public int getNumberOfParents() {
            return vars.size();
        }

        /**
         * Returns a textual representation of the parent set of this mainVar.
         * @return a String description of the parent set of this mainVar.
         */
        @Override
        public String toString() {

            int numParents = getNumberOfParents();
            StringBuilder str = new StringBuilder();
            str.append("{");

            for (int i = 0; i < numParents; i++) {
                Variable parent = getParents().get(i);
                str.append(parent.getName());
                if (i < numParents - 1) {
                    str.append(", ");
                }
            }
            str.append("}");
            return str.toString();
        }

        /**
         * Defines the set of parent as unmodifiable, i.e., no add or remove operations are allowed.
         */
        public void blockParents() {
            //vars = Collections.unmodifiableList(vars);
        }

        /**
         * Tests if a given variable pertains to this parent set.
         * @param var a variable to be tested.
         * @return a boolean indicating if the variable pertains to this parent set (true) or not (false).
         */
        public boolean contains(Variable var) {
            return this.vars.contains(var);
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

            ParentSet parentset = (ParentSet) o;

            if (this.getNumberOfParents() != parentset.getNumberOfParents()) {
                return false;
            } else {
                int i = 0;
                boolean eqs = true;
                while (i < this.getNumberOfParents() && eqs) {
                    if (this.getParents().get(i).equals(parentset.getParents().get(i))) {
                        i++;
                    } else {
                        eqs = false;
                    }
                }
                return eqs;
            }
        }
    }
}