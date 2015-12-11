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

package eu.amidst.dynamic.variables.impl;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.VariableBuilder;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.util.Iterator;
import java.util.List;

/**
 * Created by andresmasegosa on 10/12/15.
 */
public class StaticTime0FromDynamicVarsImpl implements Variables {

    DynamicVariables dynamicVariables;

    public StaticTime0FromDynamicVarsImpl(DynamicVariables dynamicVariables) {
        this.dynamicVariables = dynamicVariables;
    }

    @Override
    public void setAttributes(Attributes attributes) {
        this.dynamicVariables.setAttributes(attributes);
    }

    @Override
    public Variable newMultionomialVariable(Attribute att) {
        return null;
    }

    @Override
    public Variable newMultionomialVariable(String name, int nOfStates) {
        return null;
    }

    @Override
    public Variable newMultionomialVariable(String name, List<String> states) {
        return null;
    }

    @Override
    public Variable newMultinomialLogisticVariable(Attribute att) {
        return null;
    }

    @Override
    public Variable newMultinomialLogisticVariable(String name, int nOfStates) {
        return null;
    }

    @Override
    public Variable newMultinomialLogisticVariable(String name, List<String> states) {
        return null;
    }

    @Override
    public Variable newGaussianVariable(Attribute att) {
        return null;
    }

    @Override
    public Variable newGaussianVariable(String name) {
        return null;
    }

    @Override
    public Variable newVariable(Attribute att, DistributionTypeEnum distributionTypeEnum) {
        return null;
    }

    @Override
    public Variable newVariable(Attribute att) {
        return null;
    }

    @Override
    public Variable newVariable(VariableBuilder builder) {
        return null;
    }

    @Override
    public Variable getVariableById(int varID) {
        return this.dynamicVariables.getVariableById(varID);
    }

    @Override
    public Variable getVariableByName(String name) {
        Variable var =  this.dynamicVariables.getVariableByName(name);
        if (var.isInterfaceVariable())
            throw new IllegalArgumentException("Querying the name of a interface variable at Time 0");
        else
            return var;
    }

    @Override
    public int getNumberOfVars() {
        return this.dynamicVariables.getNumberOfVars();
    }

    @Override
    public void block() {

    }

    @Override
    public List<Variable> getListOfVariables() {
        return this.dynamicVariables.getListOfDynamicVariables();
    }

    @Override
    public boolean equals(Variables variables) {
        boolean equals = true;
        for (int i = 0; i < this.getNumberOfVars() && equals; i++) {
            equals = equals && this.getVariableById(i).equals(variables.getVariableById(i));
        }
        return equals;
    }

    @Override
    public Iterator<Variable> iterator() {
        return this.getListOfVariables().iterator();
    }
}
