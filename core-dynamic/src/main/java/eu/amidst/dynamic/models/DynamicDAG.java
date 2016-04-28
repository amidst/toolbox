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
 * 1. (Andres) Implement DynamicDAG with two DAGs: one for time 0 and another for time T.
 *
 * ********************************************************
 */

package eu.amidst.dynamic.models;

import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The DynamicDAG class defines the graphical structure a {@link DynamicBayesianNetwork}.
 */
public class DynamicDAG implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 123181485615649547L;

    /** Represents the set of dynamic variables. */
    private DynamicVariables dynamicVariables;


    /** Represents the list of parents for all variables at time 0. */
    private List<ParentSet> parentSetTime0;

    /** Represents the list of parents for all variables at time T. */
    private List<ParentSet> parentSetTimeT;

    /** Represents the name of the dynamic DAG. **/
    private String name = "DynamicDAG";

    /**
     * Creates a new DynamicDAG from a set of given dynamic variables.
     * @param dynamicVariables1 a set of variables of type {@link DynamicVariables}.
     */
    public DynamicDAG(DynamicVariables dynamicVariables1) {
        this.dynamicVariables = dynamicVariables1;
        this.parentSetTime0 = new ArrayList(dynamicVariables.getNumberOfVars());
        this.parentSetTimeT = new ArrayList(dynamicVariables.getNumberOfVars());

        for (Variable var : dynamicVariables) {
            parentSetTime0.add(var.getVarID(), new ParentSetImpl(var));
            parentSetTimeT.add(var.getVarID(), new ParentSetImpl(var));
        }
        this.dynamicVariables.block();
    }


    /**
     * Update the DAG with a new DynamicVariables. This must be the same object passed to
     * the builder, but with new variables created.
     * @param dynamicVariables1, object of the class DynamicVariables
     */
    public void updateDynamicVariables(DynamicVariables dynamicVariables1){
        this.dynamicVariables = dynamicVariables1;
        this.dynamicVariables.block();

        for (Variable var : dynamicVariables) {
            if (var.getVarID()<this.parentSetTime0.size())
                continue;
            this.parentSetTime0.add(var.getVarID(),new ParentSetImpl(var));
            this.parentSetTimeT.add(var.getVarID(),new ParentSetImpl(var));
        }
    }


    /**
     * Returns the set of dynamic variables in this DynamicDAG.
     * @return {@link DynamicVariables} object containing the set of dynamic variables in this DynmaicDAG.
     */
    public DynamicVariables getDynamicVariables() {
        return this.dynamicVariables;
    }

    /**
     * Returns the parent set of a given variable at time T.
     * @param var a variable of type {@link Variable}.
     * @return a {@link ParentSet} object containing the set of parents of the given variable.
     */
    public ParentSet getParentSetTimeT(Variable var) {
        if (var.isInterfaceVariable()) {
            throw new UnsupportedOperationException("Parents of clone variables can not be queried. Just query the parents" +
                    "of its dynamic counterpart.");
        }
        return this.parentSetTimeT.get(var.getVarID());
    }

    /**
     * Returns the parent set of a given variable at time 0.
     * @param var a variable of type {@link Variable}.
     * @return a {@link ParentSet} object containing the set of parents of the given variable.
     */
    public ParentSet getParentSetTime0(Variable var) {
        if (var.isInterfaceVariable()) {
            throw new UnsupportedOperationException("Parents of clone variables can not be queried. Just query the parents" +
                    "of its dynamic counterpart.");
        }
        return this.parentSetTime0.get(var.getVarID());
    }

    /**
     * Returns the DAG at time T of this DynamicDAG.
     * @return a {@link DAG} object.
     */
    public DAG toDAGTimeT(){
        List<Variable> allVariables = new ArrayList<>();
        allVariables.addAll(this.getDynamicVariables().getListOfDynamicVariables());
        allVariables.addAll(this.getDynamicVariables().getListOfInterfaceVariables());
        Variables staticVariables = Variables.auxiliarBuilder(allVariables);
        DAG dag = new DAG(staticVariables);
        dag.setName(this.getName());
        for (Variable dynamicVariable : dynamicVariables) {
            for (Variable parent : this.getParentSetTimeT(dynamicVariable)) {
                dag.getParentSet(dynamicVariable).addParent(parent);
            }
        }

        return dag;
    }

    /**
     * Returns the DAG at time 0 of this DynamicDAG.
     * @return a {@link DAG} object.
     */
    public DAG toDAGTime0(){
        Variables staticVariables = Variables.auxiliarBuilder(this.getDynamicVariables().getListOfDynamicVariables());
        DAG dag = new DAG(staticVariables);
        dag.setName(this.getName());

        for (Variable dynamicVariable : dynamicVariables) {
            for (Variable parent : this.getParentSetTime0(dynamicVariable)) {
                dag.getParentSet(dynamicVariable).addParent(parent);
            }
        }

        return dag;
    }

    /**
     * Tests if this DynamicDAG contains cycles.
     * @return a boolean indicating if this DynamicDAG contains cycles (true) or not (false).
     */
    public boolean containCycles() {
        return this.toDAGTimeT().containCycles() || this.toDAGTime0().containCycles();
    }

    /**
     * Returns a list of parents at time T.
     * @return the list of parents.
     */
    public List<ParentSet> getParentSetsTimeT() {
        return this.parentSetTimeT;
    }

    /**
     * Returns a list of parents at time 0.
     * @return the list of parents.
     */
    public List<ParentSet> getParentSetsTime0() {
        return this.parentSetTime0;
    }

    /**
     * Returns a textual representation of the parent set of this DynamicDAG.
     * @return a String description of the parent set of this DynamicDAG.
     */
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("\nDynamic DAG at Time 0\n");
        for (Variable var : this.getDynamicVariables()) {
            str.append(var.getName() + " has "+ this.getParentSetTime0(var).getNumberOfParents() + " parent(s): " + this.parentSetTime0.get(var.getVarID()).toString() + "\n");
        }

        str.append("\nDynamic DAG at Time T\n");
        for (Variable var : this.getDynamicVariables()) {
            str.append(var.getName()  + " has "+ this.getParentSetTimeT(var).getNumberOfParents() + " parent(s): " + this.getParentSetTimeT(var).toString() + "\n");
        }
        return str.toString();
    }

    /**
     * Sets the name of this DynamicDAG.
     * @param name, a String object.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this DynamicDAG.
     * @return a String object.
     */
    public String getName() {
        return name;
    }

    /**
     * This class implements the interface {@link ParentSet}.
     * It is used to handle the parent set operations.
     */
    private final class ParentSetImpl implements ParentSet, Serializable {

        private static final long serialVersionUID = 7416827986614255621L;

        private Variable mainVar;
        private List<Variable> vars;

        private ParentSetImpl(Variable mainVar1) {
            mainVar = mainVar1;
            this.vars = new ArrayList<Variable>();
        }

        @Override
        public Variable getMainVar() {
            return mainVar;
        }

        //TODO Gives an error trying to add a duplicate parent in the following structure: A -> B <- Aclone. Are are considering A and AClone the same variables?
        public void addParent(Variable var) {
            if (!mainVar.getDistributionType().isParentCompatible(var)){
                throw new IllegalArgumentException("Adding a parent of type "+var.getDistributionTypeEnum().toString()+ " which is not compatible " +
                        "with children variable of type "+this.mainVar.getDistributionTypeEnum().toString());
            }

            if (this.contains(var)) {
                throw new IllegalArgumentException("Trying to add a duplicated parent");
            }

            vars.add(var);

            if (!var.isInterfaceVariable() && !parentSetTime0.get(mainVar.getVarID()).contains(var)) {
                ((ParentSetImpl) parentSetTime0.get(mainVar.getVarID())).vars.add(var);
            }
        }

        public void removeParent(Variable var) {
            vars.remove(var);
        }

        public List<Variable> getParents() {
            return vars;
        }

        public int getNumberOfParents() {
            return vars.size();
        }

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
         * Is an ArrayList pointer to an ArrayList unmodifiable object still unmodifiable? I guess so right?
         */
        public void blockParents() {
            //vars = Collections.unmodifiableList(vars);
        }

        public boolean contains(Variable var) {
            return this.vars.contains(var);
        }
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

        DynamicDAG dyndag = (DynamicDAG) o;

        int i = 0;
        boolean eqs = true;

        if (this.parentSetTime0.size() != dyndag.getParentSetsTime0().size()) {
            return false;
        } else {
            while (i < this.parentSetTime0.size() && eqs) {
                if (this.getParentSetsTime0().get(i).equals(dyndag.getParentSetsTime0().get(i))) {
                    i++;
                } else {
                    eqs = false;
                }
            }
        }

        if (this.parentSetTimeT.size() != dyndag.getParentSetsTimeT().size()) {
            return false;
        } else {
            i = 0;
            while (i < this.parentSetTimeT.size() && eqs) {
                if (this.getParentSetsTime0().get(i).equals(dyndag.getParentSetsTime0().get(i))) {
                    i++;
                } else {
                    eqs = false;
                }
            }
            return eqs;
        }

    }
}
