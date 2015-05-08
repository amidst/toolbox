/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.variables;

import java.io.Serializable;

/**
 * Created by andresmasegosa on 25/11/14.
 */
public abstract class StateSpaceType implements Serializable {

    private static final long serialVersionUID = 4158293895929418259L;

    private StateSpaceTypeEnum stateSpaceTypeEnum;
    private String unit="NA";

    // This empty constructor is required because this class is the first non-serializable superclass in the inheritence
    // hierarchy for the classes FiniteStateSpace and RealStateSpace (both implements Serializable)
    public StateSpaceType(){}

    public StateSpaceType(StateSpaceTypeEnum type){
        this.stateSpaceTypeEnum =type;
    }

    public StateSpaceTypeEnum getStateSpaceTypeEnum(){
        return this.stateSpaceTypeEnum;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
