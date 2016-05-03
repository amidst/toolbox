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

/**
 ******************* ISSUE LIST **************************
 *
 * 1. The number of states should be parsed and stored.
 *
 *
 * ********************************************************
 */

package eu.amidst.core.datastream;

import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.SparseFiniteStateSpace;

import java.io.Serializable;
import java.text.NumberFormat;

/**
 * If we consider a data sets as a data  matrix, an Attribute class would represent a column of the matrix.
 * This Attribute contains information such as the column name and the type of data it contains (discrete, continuous, etc.).
 * <p> See {@code eu.amidst.core.examples.datastream.DataStreamExample} for an example of use. </p>
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

    /** Represents whether the attribute is a special one (e.g. refer to some id, date, etc) */
    private boolean specialAttribute = false;

    /** Represents whether the attribute is a seq_id**/
    private boolean seq_id = false;

    /** Represents whether the attribute is a time_id**/
    private boolean time_id = false;

    /** Represents the number format used to write the values of this attribute*/
    //private NumberFormat numberFormat=null;

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
     * Set number the number format of the attribute used to write values to a String.
     * @param numberFormat, a valid {@link NumberFormat} object.
     */
    //public void setNumberFormat(NumberFormat numberFormat) {
    //    this.numberFormat = numberFormat;
    //}

    /**
     * Returns number the number format of the attribute used to write values to a String.
     * if null, there is non specific number format.
     * @return a valid {@link NumberFormat} object.
     */
    //public NumberFormat getNumberFormat() {
    //    return numberFormat;
    //}

    /**
     * Indicates whether the attribute is a special one (e.g. an id, date, etc).
     * @return A boolean value
     */
    public boolean isSpecialAttribute() {
        return specialAttribute;
    }

    /**
     * Sets whether the attribute is a special one (e.g. an id, date, etc).
     * @param specialAttribute, a boolean value
     */
    public void setSpecialAttribute(boolean specialAttribute) {
        this.specialAttribute = specialAttribute;
    }

    /**
     * Indicates whether the attribute is a seq_id
     * @return A boolean value
     */
    public boolean isSeqId() {
        return seq_id;
    }

    /**
     * Sets whether the attribute is a seq_id
     * @param seq_id, a boolean value
     */
    public void setSeqId(boolean seq_id) {
        this.seq_id = seq_id;
    }

    /**
     * Indicates whether the attribute is a time_id
     * @return A boolean value
     */
    public boolean isTimeId() {
        return time_id;
    }

    /**
     * Sets whether the attribute is a time_id
     * @param time_id, a boolean value
     */
    public void setTimeId(boolean time_id) {
        this.time_id = time_id;
    }

    /**
     * Returns the index of this Attribute.
     * @return an integer i&gt;=0.
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
     * @param <E> any class extending {@link StateSpaceType}
     * @return a StateSpaceType object describing this Attribute.
     */
    public <E extends StateSpaceType> E getStateSpaceType() {
        return (E) stateSpaceType;
    }

    /**
     * Tests whether two attributes are equal or not.
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
     * Returns an string representation of the associated value of the attribute.
     * @param value, a valid value of the state space of the attribute.
     * @return a string object representing the value.
     */
    public String stringValue(double value){
        return this.getStateSpaceType().stringValue(value);
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

    /**
     * Returns the number of states of this attribute, in case it has a finite state space. Otherwise it returns -1.
     * @return the number of states of this attribute.
     */
    public int getNumberOfStates(){
        if (this.getStateSpaceType().getStateSpaceTypeEnum()==StateSpaceTypeEnum.FINITE_SET)
            return ((FiniteStateSpace)this.getStateSpaceType()).getNumberOfStates();
        else if (this.getStateSpaceType().getStateSpaceTypeEnum()==StateSpaceTypeEnum.SPARSE_FINITE_SET)
            return ((SparseFiniteStateSpace)this.getStateSpaceType()).getNumberOfStates();
        else
            return -1;
    }
}