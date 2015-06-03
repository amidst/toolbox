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
 * 1. Rename to DynamicVariables
 * 2. We can/should remove all setters from VariableImplementation right?
 * 3. Is there any need for the field atts? It is only used in the constructor.
 * 4. If the fields in VariableImplementation are all objects then the Interface variable only contains
 *    pointers, which would ensure consistency, although we are not planing to modify these values.
 *
 * 5. Remove method getVariableByVarID();
 *
 * ********************************************************
 */



package eu.amidst.examples.core.variables;

import eu.amidst.examples.core.datastream.Attribute;
import eu.amidst.examples.core.datastream.Attributes;
import eu.amidst.examples.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.examples.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by afa on 02/07/14.
 */
public class DynamicVariables  implements Iterable<Variable>, Serializable {

    private static final long serialVersionUID = -4959625141445606681L;

    private List<Variable> allVariables;
    private List<Variable> interfaceVariables;

    private Map<String, Integer> mapping;

    public DynamicVariables() {
        this.allVariables = new ArrayList();
        this.interfaceVariables = new ArrayList();
        this.mapping = new ConcurrentHashMap<>();
    }

    public DynamicVariables(Attributes atts) {

        this.allVariables = new ArrayList<>();
        this.interfaceVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();

        for (Attribute att : atts.getListExceptTimeAndSeq()) {
            VariableBuilder builder = new VariableBuilder(att);
            VariableImplementation var = new VariableImplementation(builder, allVariables.size());
            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
            this.mapping.put(var.getName(), var.getVarID());
            allVariables.add(var.getVarID(), var);


            VariableImplementation interfaceVariable = new VariableImplementation(var);
            var.setInterfaceVariable(interfaceVariable);
            interfaceVariables.add(var.getVarID(), interfaceVariable);
        }
    }

    /**
     * Constructor where the distribution type of random variables is provided as an argument.
     *
     */
    public DynamicVariables(Attributes atts, Map<Attribute, DistributionTypeEnum> typeDists) {

        this.allVariables = new ArrayList<>();
        this.interfaceVariables = new ArrayList<>();

        for (Attribute att : atts.getListExceptTimeAndSeq()) {
            VariableBuilder builder;
            if (typeDists.containsKey(att)) {
                builder = new VariableBuilder(att, typeDists.get(att));
            }else{
                builder = new VariableBuilder(att);
            }

            VariableImplementation var = new VariableImplementation(builder, allVariables.size());
            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
            this.mapping.put(var.getName(), var.getVarID());
            allVariables.add(var.getVarID(), var);

            VariableImplementation interfaceVariable = new VariableImplementation(var);
            var.setInterfaceVariable(interfaceVariable);
            interfaceVariables.add(var.getVarID(), interfaceVariable);

        }
    }


    public Variable getInterfaceVariable(Variable var){
        return interfaceVariables.get(var.getVarID());
    }

    public Variable getVariableFromInterface(Variable var){
        return allVariables.get(var.getVarID());
    }

    /*
    public Variable addIndicatorDynamicVariable(Variable var) {
        if (!var.isObservable()) {
            throw new IllegalArgumentException("An indicator variable should be created from an observed variable");
        }

        if (var.getStateSpace().getStateSpaceType()!=StateSpaceType.REAL) {
            throw new IllegalArgumentException("An indicator variable should be created from an real variable");
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

        VariableImplementation interfaceVariable = new VariableImplementation(varNew);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(varNew.getVarID(),interfaceVariable);

        return varNew;

    }
*/

    public Variable newMultinomialLogisticDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.MULTINOMIAL_LOGISTIC);
    }

    public Variable newMultinomialLogisticDynamicVariable(String name, int nOfStates) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(nOfStates));
    }

    public Variable newMultinomialLogisticDynamicVariable(String name, List<String> states) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(states));
    }

    public Variable newMultionomialDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.MULTINOMIAL);
    }

    public Variable newMultinomialDynamicVariable(String name, int nOfStates) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(nOfStates));
    }

    public Variable newMultinomialDynamicVariable(String name, List<String> states) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(states));
    }

    public Variable newGaussianDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.NORMAL);
    }

    public Variable newGaussianDynamicVariable(String name) {
        return this.newDynamicVariable(name, DistributionTypeEnum.NORMAL, new RealStateSpace());
    }


    public Variable newDynamicVariable(Attribute att) {

        VariableImplementation var = new VariableImplementation(new VariableBuilder(att), allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation interfaceVariable = new VariableImplementation(var);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    public Variable newDynamicVariable(String name, DistributionTypeEnum distributionTypeEnum, StateSpaceType stateSpaceType) {
        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName(name);
        variableBuilder.setDistributionType(distributionTypeEnum);
        variableBuilder.setStateSpaceType(stateSpaceType);
        variableBuilder.setObservable(false);
        VariableImplementation var = new VariableImplementation(variableBuilder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation interfaceVariable = new VariableImplementation(var);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    public Variable newDynamicVariable(Attribute att, DistributionTypeEnum distributionTypeEnum) {
        VariableBuilder variableBuilder = new VariableBuilder(att);
        variableBuilder.setDistributionType(distributionTypeEnum);
        VariableImplementation var = new VariableImplementation(variableBuilder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation interfaceVariable = new VariableImplementation(var);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    public Variable newDynamicVariable(VariableBuilder builder) {

        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);

        VariableImplementation interfaceVariable = new VariableImplementation(var);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    public Variable newRealDynamicVariable(Variable var){
        if (!var.isObservable()) {
            throw new IllegalArgumentException("A Real variable should be created from an observed variable");
        }

        if (var.getStateSpaceType().getStateSpaceTypeEnum()!= StateSpaceTypeEnum.REAL) {
            throw new IllegalArgumentException("An Real variable should be created from a real variable");
        }

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Real");

        VariableImplementation varNew = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(varNew.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(varNew.getName(), varNew.getVarID());
        allVariables.add(varNew);

        VariableImplementation interfaceVariable = new VariableImplementation(varNew);
        varNew.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(varNew.getVarID(), interfaceVariable);

        return varNew;
    }

    public List<Variable> getListOfDynamicVariables() {
        return this.allVariables;
    }

    //public List<Variable> getListOfInterfaceVariables() {
    //    return this.interfaceVariables;
    //}

    public Variable getVariableById(int varID) {
       return this.allVariables.get(varID);
    }


    //public Variable getInterfaceVariablesById(int varID) {
    //    return this.interfaceVariables.get(varID);
    //}

    public Variable getVariableByName(String name) {
        Integer index = this.mapping.get(name);
        if (index==null) {
            throw new UnsupportedOperationException("Variable " + name + " is not part of the list of Variables");
        }
        else {
            return this.getVariableById(index.intValue());
        }
    }

    public Variable getInterfaceVariableByName(String name) {
        return this.getInterfaceVariable(this.getVariableByName(name));
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }

    public void block(){
        this.allVariables = Collections.unmodifiableList(this.allVariables);
    }

    @Override
    public Iterator<Variable> iterator() {
        return this.allVariables.iterator();
    }


    //TODO Implements hashCode method!!
    private static class VariableImplementation implements Variable, Serializable {

        private static final long serialVersionUID = 7934186475276412196L;

        private String name;
        private int varID;
        private boolean observable;
        private StateSpaceType stateSpaceType;
        private DistributionTypeEnum distributionTypeEnum;
        private DistributionType distributionType;
        private Attribute attribute;
        private final boolean isInterfaceVariable;
        private Variable interfaceVariable;
        private int numberOfStates = -1;

        /*
         * Constructor for a Variable (not a interface Variable)
         */
        public VariableImplementation(VariableBuilder builder, int varID) {
            this.name = builder.getName();
            this.varID = varID;
            this.observable = builder.isObservable();
            this.stateSpaceType = builder.getStateSpaceType();
            this.distributionTypeEnum = builder.getDistributionType();
            this.attribute = builder.getAttribute();
            this.isInterfaceVariable = false;

            if (this.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
                this.numberOfStates = ((FiniteStateSpace) this.stateSpaceType).getNumberOfStates();
            }

            this.distributionType=distributionTypeEnum.newDistributionType(this);
        }

        /*
         * Constructor for an Interface (based on a variable)
         */
        public VariableImplementation(Variable variable) {
            this.name = variable.getName()+"_Interface";
            this.varID = variable.getVarID();
            this.observable = variable.isObservable();
            this.stateSpaceType = variable.getStateSpaceType();
            this.distributionTypeEnum = variable.getDistributionTypeEnum();
            this.attribute = variable.getAttribute();
            this.isInterfaceVariable = true;

            if (this.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
                this.numberOfStates = ((FiniteStateSpace) this.stateSpaceType).getNumberOfStates();
            }

            this.distributionType=distributionTypeEnum.newDistributionType(this);

        }

        public String getName() {
            return this.name;
        }

        public int getVarID() {
            return varID;
        }

        public boolean isObservable() {
            return observable;
        }

        @Override
        public <E extends StateSpaceType> E getStateSpaceType() {
            return (E) stateSpaceType;
        }

        @Override
        public int getNumberOfStates() {
            return this.numberOfStates;
        }

        public DistributionTypeEnum getDistributionTypeEnum() {
            return distributionTypeEnum;
        }

        @Override
        public <E extends DistributionType> E getDistributionType() {
            return (E)this.distributionType;
        }

        @Override
        public boolean isInterfaceVariable(){
            return isInterfaceVariable;
        }

        @Override
        public Variable getInterfaceVariable(){
            return this.interfaceVariable;
        }

        private void setInterfaceVariable(Variable interfaceVariable_){
            this.interfaceVariable = interfaceVariable_;
        }

        public Attribute getAttribute(){return attribute;}

        public boolean isDynamicVariable(){
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o){
                return true;
            }
            if (o == null || getClass() != o.getClass()){
                return false;
            }

            Variable var = (Variable) o;

            return this.isInterfaceVariable()==var.isInterfaceVariable() && this.getVarID()==var.getVarID();
        }


        @Override
        public int hashCode(){
            return this.name.hashCode();
        }


        @Override
        public boolean isParameterVariable() {
            return false;
        }

    }
}
