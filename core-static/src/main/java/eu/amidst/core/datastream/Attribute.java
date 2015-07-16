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
 * If we consider a data sets as a data  matrix, an Attribute class would represent a column of the matrix.
 * This Attribute contains information such as the column name and the type of data it contains (discrete, continuous, etc.).
 * <p> See {@code eu.amidst.core.examples.datastream.DataStreamExample} for an example of use. <p>
 */

public final class Attribute implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = -2932037991574118651L;

    /** Represents the index of this attribute. */
    private final int index;

    /** Represents the name of this attribute. */
    private final String name;

    /** Represents the type of the state space of this attribute, that could be either finite or real. */
    private final StateSpaceType stateSpaceType;

    /**
     * Creates a new Attribute.
     * @param index the index of column to which this Attribute refers.
     * @param name the name of this Attribute.
     * @param unit the name of the unit of this Attribute.
     * @param stateSpaceTypeEnum1 the state space type of this Attribute (i.e., finite or real).
     * @param numberOfStates the number of states of this Attribute in case its state space is finite.
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
     * Creates a new Attribute.
     * @param index the index of column to which this Attribute refers.
     * @param name the name of this Attribute.
     * @param stateSpaceTypeEnum1 the state space type of this Attribute (i.e., finite or real).
     * @param numberOfStates the number of states of this Attribute in case its state space is finite.
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
     * Creates a new Attribute.
     * @param index the index of column to which this Attribute object refers.
     * @param name the name of this Attribute.
     * @param stateSpaceType1 a {@link StateSpaceType} object defining this Attribute.
     */
    public Attribute(int index, String name, StateSpaceType stateSpaceType1) {
        this.index = index;
        this.name = name;
        this.stateSpaceType = stateSpaceType1;
    }

    /**
     * Returns the index of this Attribute.
     * @return an integer i>=0.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the name of this Attribute.
     * @return a String representing the name of this Attribute.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a StateSpaceType object describing this Attribute.
     * @return a StateSpaceType object describing this Attribute.
     */
    public <E extends StateSpaceType> E getStateSpaceType() {
        return (E) stateSpaceType;
    }

    /**
     * Test whether two attributes are equal or not.
     * Two attributes are considered to be equal if they have the same name and the same StateSpaceType.
     * @param o an Attribute object to be compared with this Attribute.
     * @return true if the two attributes are equals, false otherwise.
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
     * Returns the hashCode of this Attribute.
     * @return the hashCode of this Attribute.
     */
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + stateSpaceType.hashCode();
        return result;
    }
}