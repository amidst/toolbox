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

import java.util.List;

/**
 * Created by andresmasegosa on 10/12/15.
 */
public class DAGTime0FromDynamicDAG implements DAG{

    DynamicDAG dynamicDAG;
    Variables variablesTime0;

    public DAGTime0FromDynamicDAG(DynamicDAG dynamicDAG) {
        this.dynamicDAG = dynamicDAG;
        this.variablesTime0 = dynamicDAG.getDynamicVariables().toVariablesTime0();
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
        return this.variablesTime0;
    }

    @Override
    public ParentSet getParentSet(Variable var) {
        if (var.isInterfaceVariable())
            throw new IllegalArgumentException("Interface Variable is not present at time 0");
        return this.dynamicDAG.getParentSetTime0(var);
    }

    @Override
    public List<ParentSet> getParentSets() {
        return this.dynamicDAG.getParentSetsTime0();
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
