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
 * 1. (Andres) Implement DynamicDAG with two DAGs: one for time 0 and another for time T.
 *
 * ********************************************************
 */

package eu.amidst.dynamic.models;

import eu.amidst.corestatic.models.ParentSet;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.corestatic.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Hanen on 13/11/14.
 */
public class DynamicDAG implements Serializable {


    private static final long serialVersionUID = 123181485615649547L;

    /**
     * It contains a pointer to the variables (list of variables).
     */
    private DynamicVariables dynamicVariables;

    /**
     * It contains the ParentSets for all variables at time 0.
     */
    private List<ParentSet> parentSetTime0;

    /**
     * It contains the ParentSets for all variables at time T.
     */
    private List<ParentSet> parentSetTimeT;


    public DynamicDAG(DynamicVariables dynamicVariables1) {
        this.dynamicVariables = dynamicVariables1;
        this.parentSetTime0 = new ArrayList(dynamicVariables.getNumberOfVars());
        this.parentSetTimeT = new ArrayList(dynamicVariables.getNumberOfVars());

        for (Variable var : dynamicVariables) {
            parentSetTime0.add(var.getVarID(), new ParentSetImpl(var));
            parentSetTimeT.add(var.getVarID(), new ParentSetImpl(var));
        }

        this.parentSetTime0 = Collections.unmodifiableList(this.parentSetTime0);
        this.parentSetTimeT = Collections.unmodifiableList(this.parentSetTimeT);
        this.dynamicVariables.block();
    }

    public DynamicVariables getDynamicVariables() {
        return this.dynamicVariables;
    }

    /* Methods accessing structure at time T*/
    public ParentSet getParentSetTimeT(Variable var) {
        if (var.isInterfaceVariable()) {
            throw new UnsupportedOperationException("Parents of clone variables can not be queried. Just query the parents" +
                    "of its dynamic counterpart.");
        }
        return this.parentSetTimeT.get(var.getVarID());
    }

    public ParentSet getParentSetTime0(Variable var) {
        if (var.isInterfaceVariable()) {
            throw new UnsupportedOperationException("Parents of clone variables can not be queried. Just query the parents" +
                    "of its dynamic counterpart.");
        }
        return this.parentSetTime0.get(var.getVarID());
    }

    public boolean containCycles() {

        boolean[] bDone = new boolean[this.dynamicVariables.getNumberOfVars()];


        for (Variable var : this.dynamicVariables) {
            bDone[var.getVarID()] = false;
        }

        for (Variable var : this.dynamicVariables) {

            // find a node for which all parents are 'done'
            boolean bFound = false;

            for (Variable variable2 : this.dynamicVariables) {
                if (!bDone[variable2.getVarID()]) {
                    boolean bHasNoParents = true;

                    for (Variable parent : this.getParentSetTimeT(variable2)) {
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

    public List<ParentSet> getParentSetsTimeT() {
        return this.parentSetTimeT;
    }

    public List<ParentSet> getParentSetsTime0() {
        return this.parentSetTime0;
    }

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
                throw new IllegalArgumentException("Adding a parent of type "+var.getDistributionTypeEnum().toString()+"which is not compatible" +
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
            vars = Collections.unmodifiableList(vars);
        }

        public boolean contains(Variable var) {
            return this.vars.contains(var);
        }
    }

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