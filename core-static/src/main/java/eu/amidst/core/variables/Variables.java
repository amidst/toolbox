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
 * 1. Remove method getVariableByVarID()!!
 *
 * ********************************************************
 */

package eu.amidst.core.variables;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by afa on 02/07/14.
 */
public class Variables implements Iterable<Variable>, Serializable {

    private static final long serialVersionUID = 5077959998533923231L;

    private List<Variable> allVariables;

    private Map<String, Integer> mapping;

    Attributes attributes;

    public Variables() {
        this.allVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();
    }

    /**
     * Constructor where the distribution type of random variables is initialized by default.
     *
     */
    public Variables(Attributes atts) {
        this.attributes= new Attributes(atts.getList());
        this.allVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();

        for (Attribute att : atts.getListExceptTimeAndSeq()) {
            VariableBuilder builder = new VariableBuilder(att);
            VariableImplementation var = new VariableImplementation(builder, allVariables.size());
            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
            this.mapping.put(var.getName(), var.getVarID());
            allVariables.add(var.getVarID(), var);
        }
    }

    /**
     * Constructor where the distribution type of random variables is provided as an argument.
     *
     */
    public Variables(Attributes atts, HashMap<Attribute, DistributionTypeEnum> typeDists) {

        this.allVariables = new ArrayList<>();

        for (Attribute att : atts.getListExceptTimeAndSeq()) {
            Variable var;
            if (typeDists.containsKey(att)) {
                var = this.newVariable(att, typeDists.get(att));
            }else{
                var = this.newVariable(att);
            }

            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
            this.mapping.put(var.getName(), var.getVarID());
            allVariables.add(var.getVarID(), var);

        }
    }

    public Attributes getAttributes() {
        return attributes;
    }

    /*
    public Variable addIndicatorVariable(Variable var) {
        if (!var.isObservable()) {
            throw new IllegalArgumentException("An indicator variable should be created from an observed variable");
        }

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Indicator");
        builder.setDistributionType(DistType.INDICATOR);

        VariableImplementation varNew = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(varNew.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(varNew.getName(), varNew.getVarID());
        allVariables.add(varNew);
        return varNew;
    }*/



    public Variable newMultionomialVariable(Attribute att) {
        return this.newVariable(att, DistributionTypeEnum.MULTINOMIAL);
    }

    public Variable newMultionomialVariable(String name, int nOfStates) {
        return this.newVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(nOfStates));
    }

    public Variable newMultionomialVariable(String name, List<String> states) {
        return this.newVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(states));
    }

    public Variable newMultinomialLogisticVariable(Attribute att) {
        return this.newVariable(att, DistributionTypeEnum.MULTINOMIAL_LOGISTIC);
    }

    public Variable newMultinomialLogisticVariable(String name, int nOfStates) {
        return this.newVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(nOfStates));
    }

    public Variable newMultinomialLogisticVariable(String name, List<String> states) {
        return this.newVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(states));
    }

    public Variable newGaussianVariable(Attribute att) {
        return this.newVariable(att, DistributionTypeEnum.NORMAL);
    }

    public Variable newGaussianVariable(String name) {
        return this.newVariable(name, DistributionTypeEnum.NORMAL, new RealStateSpace());
    }

    public Variable newVariable(Attribute att, DistributionTypeEnum distributionTypeEnum) {
        VariableBuilder builder = new VariableBuilder(att);
        builder.setDistributionType(distributionTypeEnum);
        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);
        return var;

    }

    public Variable newVariable(Attribute att) {
        VariableBuilder builder = new VariableBuilder(att);
        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);
        return var;
    }

    private Variable newVariable(String name, DistributionTypeEnum distributionTypeEnum, StateSpaceType stateSpaceType) {
        VariableBuilder builder = new VariableBuilder();
        builder.setName(name);
        builder.setDistributionType(distributionTypeEnum);
        builder.setStateSpaceType(stateSpaceType);
        builder.setObservable(false);

        return this.newVariable(builder);
    }

    private Variable newVariable(VariableBuilder builder) {
        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names: " + var.getName());
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);
        return var;

    }

    //public List<Variable> getListOfVariables() {
    //    return this.allVariables;
    //}

    public Variable getVariableById(int varID) {
        return this.allVariables.get(varID);
    }

    public Variable getVariableByName(String name) {
        Integer index = this.mapping.get(name);
        if (index==null) {
            throw new UnsupportedOperationException("Variable " + name + " is not part of the list of Variables");
        }
        else {
            return this.getVariableById(index.intValue());
        }
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }

    @Override
    public Iterator<Variable> iterator() {
        return this.allVariables.iterator();
    }

    public void block(){
        this.allVariables = Collections.unmodifiableList(this.allVariables);
    }

    public List<Variable> getListOfVariables(){
        return this.allVariables;
    }

    //TODO Implements hashCode method!!

    private static class VariableImplementation implements Variable, Serializable {

        private static final long serialVersionUID = 4656207896676444152L;

        private String name;
        private int varID;
        private boolean observable;
        private StateSpaceType stateSpaceType;
        private DistributionTypeEnum distributionTypeEnum;
        private DistributionType distributionType;

        private Attribute attribute;
        private int numberOfStates = -1;


        public VariableImplementation(VariableBuilder builder, int varID) {
            this.name = builder.getName();
            this.varID = varID;
            this.observable = builder.isObservable();
            this.stateSpaceType = builder.getStateSpaceType();
            this.distributionTypeEnum = builder.getDistributionType();
            this.attribute = builder.getAttribute();

            if (this.getStateSpaceType().getStateSpaceTypeEnum() == StateSpaceTypeEnum.FINITE_SET) {
                this.numberOfStates = ((FiniteStateSpace) this.stateSpaceType).getNumberOfStates();
            }

            this.distributionType=distributionTypeEnum.newDistributionType(this);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public int getVarID() {
            return varID;
        }

        @Override
        public boolean isObservable() {
            return observable;
        }

        @Override
        public <E extends StateSpaceType> E getStateSpaceType() {
            return (E) stateSpaceType;
        }

        @Override
        public DistributionTypeEnum getDistributionTypeEnum() {
            return distributionTypeEnum;
        }

        @Override
        public <E extends DistributionType> E getDistributionType() {
            return (E)this.distributionType;
        }

        @Override
        public boolean isInterfaceVariable() {
            throw new UnsupportedOperationException("In a static context a variable cannot be temporal.");
        }

        @Override
        public Attribute getAttribute() {
            return attribute;
        }

        @Override
        public boolean isDynamicVariable() {
            return false;
        }

        @Override
        public boolean isParameterVariable() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()){
                return false;
            }

            Variable var = (Variable) o;

            return this.getVarID()==var.getVarID();
        }

        @Override
        public int getNumberOfStates() {
            return this.numberOfStates;
        }


        //TODO Does the best way to implement hashcode?
        @Override
        public int hashCode(){
            return this.name.hashCode();
        }

    }
}
