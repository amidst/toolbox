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
 * 1. The number of states should be parsed and stored.
 *
 *
 * ********************************************************
 */

package eu.amidst.core.datastream;

import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.io.Serializable;

/**
 *
 * If we look at a data sets as a data  matrix, an Attribute class would
 * represent a column of the matrix. This Attribute contains information such as
 * the name of the column and the type of data (discrete, continuous, etc.) it contains.
 *
 */

public final class Attribute implements Serializable {

    /** The serial vesion ID for serializing the object */
    private static final long serialVersionUID = -2932037991574118651L;

    /** The index of the column**/
    private final int index;

    /** The name of the column */
    private final String name;

    /** The states values in case the column represent a discrete attribute*/
    private final StateSpaceType stateSpaceType;

    /**
     * A builder for attributes
     * @param index, the index of column which the Attribute object refers to
     * @param name, the name of the attribute,
     * @param unit, the name of the unit of the attribute,
     * @param stateSpaceTypeEnum1, the state space of the attribute (finite or real).
     * @param numberOfStates, the number of states of the attribute in case its state space is finite
     */
    public Attribute(int index, String name, String unit, StateSpaceTypeEnum stateSpaceTypeEnum1, int numberOfStates) {

        this.index = index;
        this.name = name;
        if (stateSpaceTypeEnum1 == StateSpaceTypeEnum.FINITE_SET) {
            this.stateSpaceType = new FiniteStateSpace(numberOfStates);
            this.stateSpaceType.setUnit(unit);
        }else if (stateSpaceTypeEnum1 == StateSpaceTypeEnum.REAL) {
            this.stateSpaceType = new RealStateSpace();
            this.stateSpaceType.setUnit(unit);
        }else {
            throw new IllegalArgumentException("State Space not defined");
        }
    }

    /**
     * A builder for attributes
     * @param index, the index of column which the Attribute object refers to
     * @param name, the name of the attribute,
     * @param stateSpaceTypeEnum1, the state space of the attribute (finite or real).
     * @param numberOfStates, the number of states of the attribute in case its state space is finite
     */
    public Attribute(int index, String name, StateSpaceTypeEnum stateSpaceTypeEnum1, int numberOfStates) {

        this.index = index;
        this.name = name;
        if (stateSpaceTypeEnum1 == StateSpaceTypeEnum.FINITE_SET) {
            this.stateSpaceType = new FiniteStateSpace(numberOfStates);
        }else if (stateSpaceTypeEnum1 == StateSpaceTypeEnum.REAL) {
            this.stateSpaceType = new RealStateSpace();
        }else {
            throw new IllegalArgumentException("State Space not defined");
        }
    }

    /**
     * A builder for attributes
     * @param index, the index of column which the Attribute object refers to
     * @param name, the name of the attribute,
     * @param stateSpaceType1, an  StateSpaceType object defining the attribute.
     */
    public Attribute(int index, String name, StateSpaceType stateSpaceType1) {
        this.index = index;
        this.name = name;
        this.stateSpaceType = stateSpaceType1;
    }

    /**
     * Return the index of the attribute
     * @return a integer i>=0
     */
    public int getIndex() {
        return index;
    }

    /**
     * Return the name of the attribute
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Return an StateSpaceType object describing the attribute
     * @param <E>
     * @return
     */
    public <E extends StateSpaceType> E getStateSpaceType() {
        return (E) stateSpaceType;
    }

    /**
     * Two attributes are considered to be equal if they have
     * the same name and the same StateSpaceType
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }

        if (o == null || getClass() != o.getClass()){
            return false;
        }

        Attribute attribute = (Attribute) o;

        if (stateSpaceType.getStateSpaceTypeEnum() != attribute.stateSpaceType.getStateSpaceTypeEnum()){return false;}
        if (!name.equals(attribute.name)) {return false;}

        return true;
    }

    /**
     * The hasCode of the object.
     * @return
     */
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + stateSpaceType.hashCode();
        return result;
    }
}
