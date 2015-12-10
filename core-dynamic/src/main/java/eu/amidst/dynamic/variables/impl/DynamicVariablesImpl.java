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



package eu.amidst.dynamic.variables.impl;

import eu.amidst.core.ModelFactory;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.*;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by afa on 02/07/14.
 */
public class DynamicVariablesImpl implements DynamicVariables, Serializable {

    private static final long serialVersionUID = -4959625141445606681L;

    private List<Variable> nonInterfaceVariables;
    private List<Variable> interfaceVariables;

    private Map<String, Integer> mapping;

    public DynamicVariablesImpl() {
        this.nonInterfaceVariables = new ArrayList();
        this.interfaceVariables = new ArrayList();
        this.mapping = new ConcurrentHashMap<>();
    }

    public DynamicVariablesImpl(Attributes atts) {

        this.nonInterfaceVariables = new ArrayList<>();
        this.interfaceVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();

        for (Attribute att : atts.getListOfNonSpecialAttributes()) {
            VariableBuilder builder = new VariableBuilder(att);
            VariableImpl var = new VariableImpl(builder, nonInterfaceVariables.size());
            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
            this.mapping.put(var.getName(), var.getVarID());
            nonInterfaceVariables.add(var.getVarID(), var);


            VariableImpl interfaceVariable = VariableImpl.newInterfaceVariable(var);
            var.setInterfaceVariable(interfaceVariable);
            interfaceVariables.add(var.getVarID(), interfaceVariable);
        }
    }

    /**
     * Constructor where the distribution type of random variables is provided as an argument.
     *
     */
    public DynamicVariablesImpl(Attributes atts, Map<Attribute, DistributionTypeEnum> typeDists) {

        this.nonInterfaceVariables = new ArrayList<>();
        this.interfaceVariables = new ArrayList<>();


        for (Attribute att : atts.getListOfNonSpecialAttributes()) {
            VariableBuilder builder;
            if (typeDists.containsKey(att)) {
                builder = new VariableBuilder(att, typeDists.get(att));
            }else{
                builder = new VariableBuilder(att);
            }

            VariableImpl var = new VariableImpl(builder, nonInterfaceVariables.size());
            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
            this.mapping.put(var.getName(), var.getVarID());
            nonInterfaceVariables.add(var.getVarID(), var);

            VariableImpl interfaceVariable = VariableImpl.newInterfaceVariable(var);
            var.setInterfaceVariable(interfaceVariable);
            interfaceVariables.add(var.getVarID(), interfaceVariable);

        }
    }

    /**
     * Sets a new set of attributes. Links current variables with this new set by matching
     * variable names with attributes names.
     * @param attributes an object of class {@link Attributes}
     */
    public void setAttributes(Attributes attributes){
        for (Variable variable : nonInterfaceVariables) {
            VariableImpl variableImpl = (VariableImpl)variable;
            variableImpl.setAttribute(attributes.getAttributeByName(variable.getName()));
        }

        for (Variable variable : interfaceVariables) {
            VariableImpl variableImpl = (VariableImpl)variable;
            variableImpl.setAttribute(attributes.getAttributeByName(getVariableFromInterface(variable).getName()));
        }

    }

    public Variable getInterfaceVariable(Variable var){
        return interfaceVariables.get(var.getVarID());
    }

    public Variable getVariableFromInterface(Variable var){
        return nonInterfaceVariables.get(var.getVarID());
    }

    /*
    public Variable addIndicatorDynamicVariable(Variable var) {
        if (!var.isObservable()) {
            throw new IllegalArgumentException("An indicator variable should be created from an observed variable");
        }

        if (var.getStateSpace().getStateSpaceType()!=StateSpaceType.REAL) {
            throw new IllegalArgumentException("An indicator variable should be created from an real variable");
        }

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Indicator");
        builder.setDistributionType(DistType.INDICATOR);

        VariableImplementation varNew = new VariableImplementation(builder, nonInterfaceVariables.size());
        if (mapping.containsKey(varNew.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(varNew.getName(), varNew.getVarID());
        nonInterfaceVariables.add(varNew);

        VariableImplementation interfaceVariable = new VariableImplementation(varNew);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(varNew.getVarID(),interfaceVariable);

        return varNew;

    }
*/

    public Variable newMultinomialLogisticDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.MULTINOMIAL_LOGISTIC);
    }

    public Variable newMultinomialLogisticDynamicVariable(String name, int nOfStates) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(nOfStates));
    }

    public Variable newMultinomialLogisticDynamicVariable(String name, List<String> states) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(states));
    }

    public Variable newMultionomialDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.MULTINOMIAL);
    }

    public Variable newMultinomialDynamicVariable(String name, int nOfStates) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(nOfStates));
    }

    public Variable newMultinomialDynamicVariable(String name, List<String> states) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(states));
    }

    public Variable newGaussianDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.NORMAL);
    }

    public Variable newGaussianDynamicVariable(String name) {
        return this.newDynamicVariable(name, DistributionTypeEnum.NORMAL, new RealStateSpace());
    }


    public Variable newDynamicVariable(Attribute att) {

        VariableImpl var = new VariableImpl(new VariableBuilder(att), nonInterfaceVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        nonInterfaceVariables.add(var);

        VariableImpl interfaceVariable = VariableImpl.newInterfaceVariable(var);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    public Variable newDynamicVariable(String name, DistributionTypeEnum distributionTypeEnum, StateSpaceType stateSpaceType) {
        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName(name);
        variableBuilder.setDistributionType(distributionTypeEnum);
        variableBuilder.setStateSpaceType(stateSpaceType);
        variableBuilder.setObservable(false);
        VariableImpl var = new VariableImpl(variableBuilder, nonInterfaceVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        nonInterfaceVariables.add(var);

        VariableImpl interfaceVariable = VariableImpl.newInterfaceVariable(var);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    public Variable newDynamicVariable(Attribute att, DistributionTypeEnum distributionTypeEnum) {
        VariableBuilder variableBuilder = new VariableBuilder(att);
        variableBuilder.setDistributionType(distributionTypeEnum);
        VariableImpl var = new VariableImpl(variableBuilder, nonInterfaceVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        nonInterfaceVariables.add(var);

        VariableImpl interfaceVariable = VariableImpl.newInterfaceVariable(var);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    public Variable newDynamicVariable(VariableBuilder builder) {

        VariableImpl var = new VariableImpl(builder, nonInterfaceVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        nonInterfaceVariables.add(var);

        VariableImpl interfaceVariable = VariableImpl.newInterfaceVariable(var);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    public Variable newRealDynamicVariable(Variable var){
        if (!var.isObservable()) {
            throw new IllegalArgumentException("A Real variable should be created from an observed variable");
        }

        if (var.getStateSpaceType().getStateSpaceTypeEnum()!= StateSpaceTypeEnum.REAL) {
            throw new IllegalArgumentException("An Real variable should be created from a real variable");
        }

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Real");

        VariableImpl varNew = new VariableImpl(builder, nonInterfaceVariables.size());
        if (mapping.containsKey(varNew.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(varNew.getName(), varNew.getVarID());
        nonInterfaceVariables.add(varNew);

        VariableImpl interfaceVariable = VariableImpl.newInterfaceVariable(var);
        varNew.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(varNew.getVarID(), interfaceVariable);

        return varNew;
    }

    public List<Variable> getListOfDynamicVariables() {
        return this.nonInterfaceVariables;
    }

    public Variable getVariableById(int varID) {
       return this.nonInterfaceVariables.get(varID);
    }

    public Variable getVariableByName(String name) {
        Integer index = this.mapping.get(name);
        if (index==null) {
            throw new UnsupportedOperationException("Variable " + name + " is not part of the list of Variables");
        }
        else {
            return this.getVariableById(index.intValue());
        }
    }

    public Variable getInterfaceVariableByName(String name) {
        return this.getInterfaceVariable(this.getVariableByName(name));
    }

    public int getNumberOfVars() {
        return this.nonInterfaceVariables.size();
    }

    public void block(){
        //this.nonInterfaceVariables = Collections.unmodifiableList(this.nonInterfaceVariables);
    }

    @Override
    public Iterator<Variable> iterator() {
        return this.nonInterfaceVariables.iterator();
    }

    public Variables toVariablesTimeT(){

        Variables variables = ModelFactory.newVariables();

        for (Variable nonInterfaceVariable : nonInterfaceVariables) {
            variables.newVariable(nonInterfaceVariable.getVariableBuilder());
        }

        for (Variable interfaceVariable : interfaceVariables) {
            variables.newVariable(interfaceVariable.getVariableBuilder());
        }

        return variables;
    }


    public Variables toVariablesTime0(){

        Variables variables = ModelFactory.newVariables();

        for (Variable nonInterfaceVariable : nonInterfaceVariables) {
            variables.newVariable(nonInterfaceVariable.getVariableBuilder());
        }

        return variables;
    }
}
