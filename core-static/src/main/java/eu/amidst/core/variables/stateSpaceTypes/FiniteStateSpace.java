/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.variables.stateSpaceTypes;

import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StateSpaceTypeEnum;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by andresmasegosa on 25/11/14.
 */
public class FiniteStateSpace extends StateSpaceType implements Iterable<String> {

    private int numberOfStates;
    private final List<String> statesNames;
    private final Map<String,Integer> mapStatesNames;



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

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public String getStatesName(int state) {
        return statesNames.get(state);
    }

    public int getIndexOfState(String stateName) { return this.mapStatesNames.get(stateName);}

    @Override
    public Iterator<String> iterator() {
        return statesNames.iterator();
    }

    public List<String> getStatesNames(){
        return this.statesNames;
    }
}
