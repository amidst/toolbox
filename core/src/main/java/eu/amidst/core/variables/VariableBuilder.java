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

package eu.amidst.core.variables;

import eu.amidst.core.datastream.Attribute;

/**
 * This class allows to build and handle a Variable of a Bayesian network.
 */
public class VariableBuilder {

    /** Represents the name of the variable. */
    private String name;

    /** Indicates if a variable is observable or not. */
    private boolean observable;

    /** Represents the state space type of the variable. */
    private StateSpaceType stateSpaceType;

    /** Represents the distribution type of the variable. */
    private DistributionTypeEnum distributionType;

    /** Represents the {@link Attribute} associated with the Variable. */
    private Attribute attribute;

    /**
     * Empty constructor.
     */
    public VariableBuilder() {
    }

    /**
     * Creates a new Variable given an Attribute object.
     * @param att an {@link Attribute} object associated with the Variable.
     */
    public VariableBuilder(Attribute att){
        this.name = att.getName();
        this.observable = true;
        this.stateSpaceType = att.getStateSpaceType();
        switch (att.getStateSpaceType().getStateSpaceTypeEnum()) {
            case REAL:
                this.distributionType = DistributionTypeEnum.NORMAL;
                break;
            case FINITE_SET:
                this.distributionType = DistributionTypeEnum.MULTINOMIAL;
                break;
            case SPARSE_FINITE_SET:
                this.distributionType = DistributionTypeEnum.SPARSE_MULTINOMIAL;
                break;
            default:
                throw new IllegalArgumentException(" The string \"" + att.getStateSpaceType() + "\" does not map to any Type.");
        }
        this.attribute = att;
    }

    /**
     * Creates a new Variable given an Attribute object and a distribution type.
     * @param att an {@link Attribute} object associated with the Variable.
     * @param typeDist a distribution type.
     */
    public VariableBuilder(Attribute att, DistributionTypeEnum typeDist){
        this.name = att.getName();
        this.observable = true;
        this.stateSpaceType = att.getStateSpaceType();
        this.distributionType = typeDist;
        this.attribute = att;
    }

    /**
     * Returns the name of this Variable.
     * @return the name of this Variable.
     */
    public String getName() {
        return name;
    }

    /**
     * Tests whether this Variable is observable or not.
     * @return true if this Variable is observable, false otherwise.
     */
    public boolean isObservable() {
        return observable;
    }

    /**
     * Returns the state space type {@link StateSpaceType} of this variable.
     * @return the state space type of this variable.
     */
    public StateSpaceType getStateSpaceType() {
        return stateSpaceType;
    }

    /**
     * Returns the distribution type of this variable.
     * @return the distribution type of this variable.
     */
    public DistributionTypeEnum getDistributionType() {
        return distributionType;
    }

    /**
     * Returns the {@link Attribute} associated with this Variable.
     * @return the {@link Attribute} associated with this Variable.
     */
    public Attribute getAttribute() { return attribute; }

    /**
     * Sets the name of this Variable.
     * @param name the name of this Variable.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets whether this Variable is observable or not.
     * @param observable boolean which is equal to true if this Variable is observable, false otherwise.
     */
    public void setObservable(boolean observable) {
        this.observable = observable;
    }

    /**
     * Sets the state space type of this variable.
     * @param stateSpaceType the state space type of this variable.
     */
    public void setStateSpaceType(StateSpaceType stateSpaceType) {
        this.stateSpaceType = stateSpaceType;
    }

    /**
     * Sets the distribution type of this variable.
     * @param distributionType the distribution type of this variable.
     */
    public void setDistributionType(DistributionTypeEnum distributionType) {
        this.distributionType = distributionType;
    }

    /**
     * Sets the {@link Attribute} associated with this Variable.
     * @param attribute the {@link Attribute} associated with this Variable.
     */
    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }
}
