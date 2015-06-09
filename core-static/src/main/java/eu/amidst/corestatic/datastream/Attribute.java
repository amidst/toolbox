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


package eu.amidst.corestatic.datastream;

import eu.amidst.corestatic.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.corestatic.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.corestatic.variables.StateSpaceType;
import eu.amidst.corestatic.variables.StateSpaceTypeEnum;

import java.io.Serializable;

/**
 * Created by sigveh on 10/20/14.
 */

public final class Attribute implements Serializable {

    private static final long serialVersionUID = -2932037991574118651L;

    private final int index;
    private final String name;
    private final StateSpaceType stateSpaceType;

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

    public Attribute(int index, String name, StateSpaceType stateSpaceType1) {
        this.index = index;
        this.name = name;
        this.stateSpaceType = stateSpaceType1;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public <E extends StateSpaceType> E getStateSpaceType() {
        return (E) stateSpaceType;
    }

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

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + stateSpaceType.hashCode();
        return result;
    }
}
