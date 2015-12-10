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
import eu.amidst.core.variables.*;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.Serializable;

//TODO Implements hashCode method!!
public class VariableImpl implements Variable, Serializable {

    private static final long serialVersionUID = 7934186475276412196L;

    private String name;
    private int varID;
    private boolean observable;
    private StateSpaceType stateSpaceType;
    private DistributionTypeEnum distributionTypeEnum;
    private DistributionType distributionType;
    private Attribute attribute;
    private final boolean isInterfaceVariable;
    private Variable interfaceVariable;
    private int numberOfStates = -1;

    /*
     * Constructor for a Variable (not a interface Variable)
     */
    public VariableImpl(VariableBuilder builder, int varID) {
        this.name = builder.getName();
        this.varID = varID;
        this.observable = builder.isObservable();
        this.stateSpaceType = builder.getStateSpaceType();
        this.distributionTypeEnum = builder.getDistributionType();
        this.attribute = builder.getAttribute();
        this.isInterfaceVariable = false;

        if (this.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
            this.numberOfStates = ((FiniteStateSpace) this.stateSpaceType).getNumberOfStates();
        }

        this.distributionType=distributionTypeEnum.newDistributionType(this);
    }


    /*
     * Constructor for an Interface (based on a variable)
     */
    private VariableImpl(Variable variable) {
        this.name = variable.getName()+ DynamicVariables.INTERFACE_SUFFIX;
        this.varID = variable.getVarID();
        this.observable = variable.isObservable();
        this.stateSpaceType = variable.getStateSpaceType();
        this.distributionTypeEnum = variable.getDistributionTypeEnum();
        this.attribute = variable.getAttribute();
        this.isInterfaceVariable = true;

        if (this.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
            this.numberOfStates = ((FiniteStateSpace) this.stateSpaceType).getNumberOfStates();
        }

        this.distributionType=distributionTypeEnum.newDistributionType(this);

    }

    public static VariableImpl newInterfaceVariable(Variable variable){
        return new VariableImpl(variable);
    }

    public String getName() {
        return this.name;
    }

    public int getVarID() {
        return varID;
    }

    public boolean isObservable() {
        return observable;
    }

    @Override
    public <E extends StateSpaceType> E getStateSpaceType() {
        return (E) stateSpaceType;
    }

    @Override
    public int getNumberOfStates() {
        return this.numberOfStates;
    }

    public DistributionTypeEnum getDistributionTypeEnum() {
        return distributionTypeEnum;
    }

    @Override
    public <E extends DistributionType> E getDistributionType() {
        return (E)this.distributionType;
    }

    @Override
    public boolean isInterfaceVariable(){
        return isInterfaceVariable;
    }

    @Override
    public Variable getInterfaceVariable(){
        return this.interfaceVariable;
    }

    public void setInterfaceVariable(Variable interfaceVariable_){
        this.interfaceVariable = interfaceVariable_;
    }

    public Attribute getAttribute(){return attribute;}

    public void setAttribute(Attribute att) {
        this.attribute=att;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VariableBuilder getVariableBuilder(){
        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setAttribute(this.getAttribute());
        variableBuilder.setDistributionType(this.getDistributionTypeEnum());
        variableBuilder.setName(this.getName());
        variableBuilder.setObservable(this.observable);
        variableBuilder.setStateSpaceType(this.getStateSpaceType());

        return variableBuilder;
    }

    public boolean isDynamicVariable(){
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        Variable var = (Variable) o;

        return this.isInterfaceVariable()==var.isInterfaceVariable() && this.getVarID()==var.getVarID();
    }


    @Override
    public int hashCode(){
        return this.name.hashCode();
    }


    @Override
    public boolean isParameterVariable() {
        return false;
    }

}