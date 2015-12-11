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

package eu.amidst.core.variables.impl;

/**
 * Created by andresmasegosa on 10/12/15.
 */

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.variables.*;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;

import java.io.Serializable;

/**
 * This class implements the interface {@link Variable}.
 */
class VariableImpl implements Variable, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4656207896676444152L;

    /** Represents the name of the Variable. */
    private String name;

    /** Represents the ID of the Variable. */
    private int varID;

    /** Indicates whether the Variable is observable or not. */
    private boolean observable;

    /** Represents the {@link StateSpaceType} of the Variable. */
    private StateSpaceType stateSpaceType;

    /** Represents the {@link DistributionTypeEnum} of the Variable. */
    private DistributionTypeEnum distributionTypeEnum;

    /** Represents the distribution type of the Variable. */
    private DistributionType distributionType;

    /** Represents the ID of the Variable. */
    private Attribute attribute;

    /** Represents the number of states of the Variable, by default equal to -1. */
    private int numberOfStates = -1;

    /**
     * Constructor that creates a new a VariableBuilder.
     * @param builder a VariableBuilder object.
     * @param varID a Variable ID.
     */
    public VariableImpl(VariableBuilder builder, int varID) {
        this.name = builder.getName();
        this.varID = varID;
        this.observable = builder.isObservable();
        this.stateSpaceType = builder.getStateSpaceType();
        this.distributionTypeEnum = builder.getDistributionType();
        this.attribute = builder.getAttribute();

        if (this.getStateSpaceType().getStateSpaceTypeEnum() == StateSpaceTypeEnum.FINITE_SET) {
            this.numberOfStates = ((FiniteStateSpace) this.stateSpaceType).getNumberOfStates();
        }

        this.distributionType=distributionTypeEnum.newDistributionType(this);
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVarID() {
        return varID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isObservable() {
        return observable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends StateSpaceType> E getStateSpaceType() {
        return (E) stateSpaceType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DistributionTypeEnum getDistributionTypeEnum() {
        return distributionTypeEnum;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends DistributionType> E getDistributionType() {
        return (E)this.distributionType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInterfaceVariable() {
        //throw new UnsupportedOperationException("In a static context a variable cannot be temporal.");
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamicVariable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParameterVariable() {
        return false;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        Variable var = (Variable) o;

        return this.getVarID()==var.getVarID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfStates() {
        return this.numberOfStates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode(){
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Variable variable) {
        return this.equals((Object)variable);
    }
}

    /*  public Variable addIndicatorVariable(Variable var) {
        if (!var.isObservable()) {
            throw new IllegalArgumentException("An indicator variable should be created from an observed variable");
        }

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Indicator");
        builder.setDistributionType(DistType.INDICATOR);

        VariableImplementation varNew = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(varNew.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(varNew.getName(), varNew.getVarID());
        allVariables.add(varNew);
        return varNew;
    }*/

