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

package eu.amidst.dynamic.models.impl;

import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.models.DynamicDAG;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 10/12/15.
 */
public class DAGTimeTFromDynamicDAG implements DAG{

    DynamicDAG dynamicDAG;
    Variables variablesTimeT;
    List<ParentSet> interfaceParents;
    List<ParentSet> parents;

    public DAGTimeTFromDynamicDAG(DynamicDAG dynamicDAG) {
        this.dynamicDAG = dynamicDAG;
        this.variablesTimeT = dynamicDAG.getDynamicVariables().toVariablesTimeT();


        interfaceParents = new ArrayList<>();

        List<Variable> vars = this.dynamicDAG.getDynamicVariables().getListOfDynamicVariables();

        for (int i = 0; i < vars.size(); i++) {
            interfaceParents.add(this.createEmptyParentSet(this.dynamicDAG.getDynamicVariables().getVariableById(i).getInterfaceVariable()));
        }

        parents = new ArrayList<>();
        parents.addAll(this.dynamicDAG.getParentSetsTimeT());
        parents.addAll(this.interfaceParents);
    }

    @Override
    public String getName() {
        return dynamicDAG.getName();
    }

    @Override
    public void setName(String name) {
        this.dynamicDAG.setName(name);
    }

    @Override
    public long getNumberOfLinks() {
        return (long) Double.NaN;
    }

    @Override
    public Variables getVariables() {
        return this.variablesTimeT;
    }


    private ParentSet createEmptyParentSet(Variable var){
        return new ParentSet() {
            @Override
            public Variable getMainVar() {
                return var;
            }

            @Override
            public void addParent(Variable var) {
                throw new IllegalStateException("Parent Set non-modifiable");
            }

            @Override
            public void removeParent(Variable var) {
                throw new IllegalStateException("Parent Set non-modifiable");
            }

            @Override
            public List<Variable> getParents() {
                return new ArrayList<>();
            }

            @Override
            public int getNumberOfParents() {
                return 0;
            }

            @Override
            public void blockParents() {

            }

            @Override
            public boolean contains(Variable var) {
                return false;
            }

            @Override
            public boolean equals(ParentSet parentSet) {
                return this.getMainVar().equals(parentSet.getMainVar()) && parentSet.getNumberOfParents()==0;
            }
        };
    }

    @Override
    public ParentSet getParentSet(Variable var) {
        if (var.isInterfaceVariable()) {
            return this.interfaceParents.get(var.getVarID());
        }else {
            return this.dynamicDAG.getParentSetTimeT(var);
        }
    }

    @Override
    public List<ParentSet> getParentSets() {
        return parents;
    }

    @Override
    public boolean containCycles() {
        return this.dynamicDAG.containCycles();
    }

    @Override
    public boolean equals(DAG dag) {
        if (this.getVariables().getNumberOfVars() != dag.getVariables().getNumberOfVars()) {
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
}
