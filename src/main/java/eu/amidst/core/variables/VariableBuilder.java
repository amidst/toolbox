/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.variables;

import eu.amidst.core.datastream.Attribute;


/**
 * Created by andresmasegosa on 04/11/14.
 */
public class VariableBuilder {
    private String name;
    private boolean observable;
    private StateSpaceType stateSpaceType;
    private DistributionTypeEnum distributionType;
    private Attribute attribute;

    public VariableBuilder() {
    }

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
            default:
                throw new IllegalArgumentException(" The string \"" + att.getStateSpaceType() + "\" does not map to any Type.");
        }
        this.attribute = att;
    }

    public VariableBuilder(Attribute att, DistributionTypeEnum typeDist){
        this.name = att.getName();
        this.observable = true;
        this.stateSpaceType = att.getStateSpaceType();
        this.distributionType = typeDist;
        this.attribute = att;
    }

    public String getName() {
        return name;
    }

    public boolean isObservable() {
        return observable;
    }

    public StateSpaceType getStateSpaceType() {
        return stateSpaceType;
    }

    public DistributionTypeEnum getDistributionType() {
        return distributionType;
    }

    public Attribute getAttribute() { return attribute; }

    public void setName(String name) {
        this.name = name;
    }

    public void setObservable(boolean observable) {
        this.observable = observable;
    }

    public void setStateSpaceType(StateSpaceType stateSpaceType) {
        this.stateSpaceType = stateSpaceType;
    }

    public void setDistributionType(DistributionTypeEnum distributionType) {
        this.distributionType = distributionType;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }
}
