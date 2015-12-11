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
 * 1. Rename to DynamicVariables
 * 2. We can/should remove all setters from VariableImplementation right?
 * 3. Is there any need for the field atts? It is only used in the constructor.
 * 4. If the fields in VariableImplementation are all objects then the Interface variable only contains
 *    pointers, which would ensure consistency, although we are not planing to modify these values.
 *
 * 5. Remove method getVariableByVarID();
 *
 * ********************************************************
 */



package eu.amidst.dynamic.variables;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.*;

import java.util.List;

/**
 * Created by afa on 02/07/14.
 */
public interface DynamicVariables extends Iterable<Variable> {

    public static final String INTERFACE_SUFFIX = "_Interface";

    /**
     * Sets a new set of attributes. Links current variables with this new set by matching
     * variable names with attributes names.
     *
     * @param attributes an object of class {@link Attributes}
     */
    void setAttributes(Attributes attributes);

    Variable getInterfaceVariable(Variable var);

    Variable getVariableFromInterface(Variable var);

    Variable newMultinomialLogisticDynamicVariable(Attribute att);

    Variable newMultinomialLogisticDynamicVariable(String name, int nOfStates);

    Variable newMultinomialLogisticDynamicVariable(String name, List<String> states);

    Variable newMultionomialDynamicVariable(Attribute att);

    Variable newMultinomialDynamicVariable(String name, int nOfStates);

    Variable newMultinomialDynamicVariable(String name, List<String> states);

    Variable newGaussianDynamicVariable(Attribute att);

    Variable newGaussianDynamicVariable(String name);

    Variable newDynamicVariable(Attribute att);

    Variable newDynamicVariable(String name, DistributionTypeEnum distributionTypeEnum, StateSpaceType stateSpaceType);

    Variable newDynamicVariable(Attribute att, DistributionTypeEnum distributionTypeEnum);

    Variable newDynamicVariable(VariableBuilder builder);

    Variable newRealDynamicVariable(Variable var);

    List<Variable> getListOfDynamicVariables();

    List<Variable> getListOfDynamicAndInterfaceVariables();

    Variable getVariableById(int varID);

    Variable getVariableByName(String name);

    Variable getInterfaceVariableByName(String name);

    int getNumberOfVars();

    void block();

    Variables toVariablesTime0();

    Variables toVariablesTimeT();

    boolean equals(DynamicVariables variables);

}
