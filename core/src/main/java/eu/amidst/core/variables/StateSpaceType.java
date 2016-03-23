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

import java.io.Serializable;

/**
 * This class defines the state space type.
 */
public abstract class StateSpaceType implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4158293895929418259L;

    /** Represents an enum of type {@link StateSpaceTypeEnum}. */
    private StateSpaceTypeEnum stateSpaceTypeEnum;

    /** Represents a String equal to "NA". */
    private String unit="NA";

    /**
     * An empty constructor.
     */
    public StateSpaceType(){}

    /**
     * Creates a new StateSpaceType for a given type.
     * @param type the state space type.
     */
    public StateSpaceType(StateSpaceTypeEnum type){
        this.stateSpaceTypeEnum = type;
    }

    /**
     * Returns the state space type.
     * @return the state space type.
     */
    public StateSpaceTypeEnum getStateSpaceTypeEnum(){
        return this.stateSpaceTypeEnum;
    }

    /**
     * Returns the unit of this StateSpaceType.
     * @return the unit of this StateSpaceType.
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the unit of this StateSpaceType.
     * @param unit the unit.
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Returns an string representation of the associated value.
     * @param value, a valid value of the state space.
     * @return a string object representing the value.
     */
    public abstract String stringValue(double value);

}
