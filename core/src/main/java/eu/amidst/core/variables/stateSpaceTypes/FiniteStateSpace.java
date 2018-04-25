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

package eu.amidst.core.variables.stateSpaceTypes;

import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class extends the abstract class {@link StateSpaceType} and implements the interface {@code Iterable<String>}.
 * It defines and handles the state space for discrete variables, i.e., the finite state space.
 */
public class FiniteStateSpace extends StateSpaceType implements Iterable<String> {

    /** Represents the number of states. */
    private int numberOfStates;

    /** Represents the list of space state names. */
    private final List<String> statesNames;

    /** Represents a {@link java.util.Map} object that maps the state names ({@code String}) to their indices ({@code Integer}). */
    private final Map<String,Integer> mapStatesNames;

    /**
     * Creates a new FiniteStateSpace given the number of states.
     * @param numberOfStates1 the number of states.
     */
    public FiniteStateSpace(int numberOfStates1) {
        super(StateSpaceTypeEnum.FINITE_SET);
        this.numberOfStates=numberOfStates1;
        this.statesNames = new ArrayList<>();
        this.mapStatesNames = new ConcurrentHashMap<>();
        for (int i=0; i<numberOfStates1; i++){
            //this.statesNames.add("State_"+i);
            //this.mapStatesNames.put("State_"+i, i);
            this.statesNames.add(i+".0");
            this.mapStatesNames.put(i+".0", i);
        }
    }

    /**
     * Creates a new FiniteStateSpace given a list of state space names.
     * @param statesNames1 a list of state space names
     */
    public FiniteStateSpace(List<String> statesNames1) {
        super(StateSpaceTypeEnum.FINITE_SET);
        this.numberOfStates=statesNames1.size();
        this.statesNames = new ArrayList<>();
        this.mapStatesNames = new ConcurrentHashMap<>();
        for (int i = 0; i < statesNames1.size(); i++) {
            this.statesNames.add(statesNames1.get(i));
            this.mapStatesNames.put(statesNames1.get(i),i);
        }
    }

    /**
     * Returns the number of states.
     * @return the number of states.
     */
    public int getNumberOfStates() {
        return numberOfStates;
    }

    /**
     * Returns the name of the state space given its index.
     * @param state the index of the state space.
     * @return the name of the state space.
     */
    public String getStatesName(int state) {
        return statesNames.get(state);
    }

    /**
     * Returns the index of the state space given its name.
     * @param stateName the name of the state space.
     * @return the index of the state space.
     */
    public int getIndexOfState(String stateName){
        return this.mapStatesNames.get(stateName);
    }

    /**
     * Returns an iterator over elements of type {@code String}, i.e. over the list of state names.
     * @return an Iterator over the state names.
     */
    @Override
    public Iterator<String> iterator() {
        return statesNames.iterator();
    }

    /**
     * Returns the list of the state names.
     * @return the list of the state names.
     */
    public List<String> getStatesNames(){
        return this.statesNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String stringValue(double value) {
        if (Double.isNaN(value))
            return "?";
        else
            return statesNames.get((int)value);
    }
}
