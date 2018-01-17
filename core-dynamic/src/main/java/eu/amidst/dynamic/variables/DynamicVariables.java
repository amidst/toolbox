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


package eu.amidst.dynamic.variables;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.*;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The DynamicVariables class defines and handles the operations related to the set of variables of
 * a {@link eu.amidst.dynamic.models.DynamicBayesianNetwork} model.
 */
public class DynamicVariables  implements Iterable<Variable>, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = -4959625141445606681L;

    /** Represents a suffix used for the name of interface variable. */
    public static final String INTERFACE_SUFFIX = "_Interface";

    /** Represents the list of non interface variables. */
    private List<Variable> nonInterfaceVariables;

    /** Represents the list of interface variables. */
    private List<Variable> interfaceVariables;

    /** Represents the set of variables as a {@link java.util.Map} object
     * that maps the Variable names ({@code String}) to their IDs ({@code Integer}). */
    private Map<String, Integer> mapping;

    /**
     * Creates a new DynamicVariables object.
     */
    public DynamicVariables() {
        this.nonInterfaceVariables = new ArrayList();
        this.interfaceVariables = new ArrayList();
        this.mapping = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new DynamicVariables object given a set of {@link Attributes}.
     * @param atts a set of {@link Attributes}.
     */
    public DynamicVariables(Attributes atts) {

        this.nonInterfaceVariables = new ArrayList<>();
        this.interfaceVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();

        for (Attribute att : atts.getListOfNonSpecialAttributes()) {
            VariableBuilder builder = new VariableBuilder(att);
            VariableImplementation var = new VariableImplementation(builder, nonInterfaceVariables.size());
            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
            this.mapping.put(var.getName(), var.getVarID());
            nonInterfaceVariables.add(var.getVarID(), var);

            VariableImplementation interfaceVariable = VariableImplementation.newInterfaceVariable(var);
            var.setInterfaceVariable(interfaceVariable);
            interfaceVariables.add(var.getVarID(), interfaceVariable);
        }
    }

    /**
     * Creates a new DynamicVariables object given a list of Attributes and their corresponding distribution types.
     * @param atts a set of {@link Attributes}.
     * @param typeDists a {@link java.util.HashMap} object that maps the Attributes to their distribution types.
     */
    public DynamicVariables(Attributes atts, Map<Attribute, DistributionTypeEnum> typeDists) {

        this.nonInterfaceVariables = new ArrayList<>();
        this.interfaceVariables = new ArrayList<>();

        for (Attribute att : atts.getListOfNonSpecialAttributes()) {
            VariableBuilder builder;
            if (typeDists.containsKey(att)) {
                builder = new VariableBuilder(att, typeDists.get(att));
            }else{
                builder = new VariableBuilder(att);
            }

            VariableImplementation var = new VariableImplementation(builder, nonInterfaceVariables.size());
            if (mapping.containsKey(var.getName())) {
                throw new IllegalArgumentException("Attribute list contains duplicated names");
            }
            this.mapping.put(var.getName(), var.getVarID());
            nonInterfaceVariables.add(var.getVarID(), var);

            VariableImplementation interfaceVariable = VariableImplementation.newInterfaceVariable(var);
            var.setInterfaceVariable(interfaceVariable);
            interfaceVariables.add(var.getVarID(), interfaceVariable);
        }
    }

    /**
     * Sets a new set of attributes. It links current variables with this new set by matching
     * variable names with attributes names.
     * @param attributes an object of class {@link Attributes}.
     */
    public void setAttributes(Attributes attributes){
        for (Variable variable : nonInterfaceVariables) {
            VariableImplementation variableImplementation = (VariableImplementation)variable;
            variableImplementation.setAttribute(attributes.getAttributeByName(variable.getName()));
        }
        for (Variable variable : interfaceVariables) {
            VariableImplementation variableImplementation = (VariableImplementation)variable;
            variableImplementation.setAttribute(attributes.getAttributeByName(getVariableFromInterface(variable).getName()));
        }

    }

    /**
     * Returns the interface variable of a corresponding given variable.
     * @param var a {@link Variable} object.
     * @return a {@link Variable} object.
     */
    public Variable getInterfaceVariable(Variable var){
        return interfaceVariables.get(var.getVarID());
    }

    /**
     * Returns the variable of a coressponding interface variable.
     * @param var a {@link Variable} object.
     * @return a {@link Variable} object.
     */
    public Variable getVariableFromInterface(Variable var){
        return nonInterfaceVariables.get(var.getVarID()-this.getNumberOfVars());
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

        VariableImplementation varNew = new VariableImplementation(builder, nonInterfaceVariables.size());
        if (mapping.containsKey(varNew.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(varNew.getName(), varNew.getVarID());
        nonInterfaceVariables.add(varNew);

        VariableImplementation interfaceVariable = new VariableImplementation(varNew);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(varNew.getVarID(),interfaceVariable);

        return varNew;
    }
    */

    /**
     * Creates a new multionomial logistic dynamic Variable from a given name and number of states.
     * @param name a {@code String} that represents the name of the dynamic variable.
     * @param nOfStates an {@code int} that represents the number of states of the dynamic variable.
     * @return a {@link Variable} object.
     */
    public Variable newMultinomialLogisticDynamicVariable(String name, int nOfStates) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(nOfStates));
    }

    /**
     * Creates a new multionomial logistic dynamic Variable from a given name and a list of states.
     * @param name a {@code String} that represents the name of the dynamic variable.
     * @param states a {@code List} of {@code String} that represents the different states of the dynamic variable.
     * @return a {@link Variable} object.
     */
    public Variable newMultinomialLogisticDynamicVariable(String name, List<String> states) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(states));
    }

    /**
     * Creates a new multionomial dynamic Variable from a given name and number of states.
     * @param name a {@code String} that represents the name of the dynamic variable.
     * @param nOfStates an {@code int} that represents the number of states of the dynamic variable.
     * @return a {@link Variable} object.
     */
    public Variable newMultinomialDynamicVariable(String name, int nOfStates) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(nOfStates));
    }

    /**
     * Creates a new multionomial dynamic Variable from a given name and a list of states.
     * @param name a {@code String} that represents the name of the dynamic variable.
     * @param states a {@code List} of {@code String} that represents the different states of the dynamic variable.
     * @return a {@link Variable} object.
     */
    public Variable newMultinomialDynamicVariable(String name, List<String> states) {
        return this.newDynamicVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(states));
    }

    /**
     * Creates a new multinomial dynamic Variable from a given Attribute.
     * @param att a given {@link Attribute}.
     * @return a new multinomial {@link Variable} object.
     */
    public Variable newMultinomialDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.MULTINOMIAL);
    }

    /**
     * Creates a new gaussian dynamic Variable from a given Attribute.
     * @param att a given {@link Attribute}.
     * @return a new gaussian {@link Variable} object.
     */
    public Variable newGaussianDynamicVariable(Attribute att) {
        return this.newDynamicVariable(att, DistributionTypeEnum.NORMAL);
    }

    /**
     * Creates a new gaussian dynamic Variable from a given Attribute.
     * @param name a given {@code String} that represents the name of the dynamic variable.
     * @return a new gaussian {@link Variable} object.
     */
    public Variable newGaussianDynamicVariable(String name) {
        return this.newDynamicVariable(name, DistributionTypeEnum.NORMAL, new RealStateSpace());
    }

    /**
     * Creates a new dynamic Variable from a given Attribute.
     * @param att a given {@link Attribute}.
     * @return a new {@link Variable} object.
     */
    public Variable newDynamicVariable(Attribute att) {

        VariableImplementation var = new VariableImplementation(new VariableBuilder(att), nonInterfaceVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        nonInterfaceVariables.add(var);

        VariableImplementation interfaceVariable = VariableImplementation.newInterfaceVariable(var);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    /**
     * Creates a new dynamic Variable given a name, a distribution type, and a state space type.
     * @param name a given {@code String} that represents the name of the dynamic variable.
     * @param distributionTypeEnum a {@link DistributionTypeEnum} object.
     * @param stateSpaceType a {@link StateSpaceType} object.
     * @return a new dynamic {@link Variable} object.
     */
    public Variable newDynamicVariable(String name, DistributionTypeEnum distributionTypeEnum, StateSpaceType stateSpaceType) {
        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName(name);
        variableBuilder.setDistributionType(distributionTypeEnum);
        variableBuilder.setStateSpaceType(stateSpaceType);
        variableBuilder.setObservable(false);
        VariableImplementation var = new VariableImplementation(variableBuilder, nonInterfaceVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        nonInterfaceVariables.add(var);

        VariableImplementation interfaceVariable = VariableImplementation.newInterfaceVariable(var);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    /**
     * Creates a new dynamic Variable given an {@link Attribute} object and a distribution type.
     * @param att a given {@link Attribute}.
     * @param distributionTypeEnum a {@link DistributionTypeEnum} object.
     * @return a new dynamic {@link Variable} object.
     */
    public Variable newDynamicVariable(Attribute att, DistributionTypeEnum distributionTypeEnum) {
        VariableBuilder variableBuilder = new VariableBuilder(att);
        variableBuilder.setDistributionType(distributionTypeEnum);
        VariableImplementation var = new VariableImplementation(variableBuilder, nonInterfaceVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        nonInterfaceVariables.add(var);

        VariableImplementation interfaceVariable = VariableImplementation.newInterfaceVariable(var);
        var.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(var.getVarID(), interfaceVariable);

        return var;
    }

    /**
     * Creates a new real dynamic Variable given an {@link Variable} object.
     * @param var a given {@link Variable} object.
     * @return a new dynamic {@link Variable} object.
     */
    public Variable newRealDynamicVariable(Variable var){
        if (!var.isObservable()) {
            throw new IllegalArgumentException("A Real variable should be created from an observed variable");
        }

        if (var.getStateSpaceType().getStateSpaceTypeEnum()!= StateSpaceTypeEnum.REAL) {
            throw new IllegalArgumentException("An Real variable should be created from a real variable");
        }

        VariableBuilder builder = new VariableBuilder(var.getAttribute());
        builder.setName(var.getName()+"_Real");

        VariableImplementation varNew = new VariableImplementation(builder, nonInterfaceVariables.size());
        if (mapping.containsKey(varNew.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(varNew.getName(), varNew.getVarID());
        nonInterfaceVariables.add(varNew);

        VariableImplementation interfaceVariable = VariableImplementation.newInterfaceVariable(var);
        varNew.setInterfaceVariable(interfaceVariable);
        interfaceVariables.add(varNew.getVarID(), interfaceVariable);

        return varNew;
    }

    /**
     * Returns the list of Dynamic Variables.
     * @return a list of {@link Variable} objects.
     */
    public List<Variable> getListOfDynamicVariables() {
        return this.nonInterfaceVariables;
    }

    /**
     * Returns the list of Interface Variables.
     * @return a list of {@link Variable} objects.
     */
    public List<Variable> getListOfInterfaceVariables() {
        return this.interfaceVariables;
    }

    /**
     * Returns a variable given its ID.
     * @param varID an {@code int} that represents a valid variable ID.
     * @return a {@link Variable} object.
     */
    public Variable getVariableById(int varID) {
       return this.nonInterfaceVariables.get(varID);
    }

    /**
     * Returns a variable given its name.
     * @param name a {@code String} that represents a valid variable name.
     * @return a {@link Variable} object.
     */
    public Variable getVariableByName(String name) {
        Integer index = this.mapping.get(name);
        if (index==null) {
            throw new UnsupportedOperationException("Variable " + name + " is not part of the list of Variables");
        }
        else {
            return this.getVariableById(index.intValue());
        }
    }

    /**
     * Returns an interface variable given its name.
     * @param name a {@code String} that represents a valid interface variable name.
     * @return a {@link Variable} object.
     */
    public Variable getInterfaceVariableByName(String name) {
        return this.getInterfaceVariable(this.getVariableByName(name));
    }

    /**
     * Returns the total number of variables.
     * @return the total number of variables.
     */
    public int getNumberOfVars() {
        return this.nonInterfaceVariables.size();
    }

    /**
     * Blocks this DynamicVariables.
     */
    public void block(){

        for (int i = 0; i < this.interfaceVariables.size(); i++) {
            VariableImplementation var = (VariableImplementation)this.interfaceVariables.get(i);
            var.setVarID(i+this.getNumberOfVars());
        }
    }

    public List<Variable> getVariablesForListOfAttributes(List<Attribute> attributeList){
        return attributeList.parallelStream()
                .map(att->getVariableByName(att.getName()))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Variable> iterator() {
        return this.nonInterfaceVariables.iterator();
    }



    /**
     * This class implements the interface {@link Variable}.
     * TODO: Implements hashCode method!!
     */
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
        private VariableImplementation(Variable variable) {
            this.name = variable.getName()+INTERFACE_SUFFIX;
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

        public static VariableImplementation newInterfaceVariable(Variable variable){
            return new VariableImplementation(variable);
        }

        public void setVarID(int varID) {
            this.varID = varID;
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

        /**
         * {@inheritDoc}
         */
        @Override
        public <E extends StateSpaceType> E getStateSpaceType() {
            return (E) stateSpaceType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumberOfStates() {
            return this.numberOfStates;
        }

        public DistributionTypeEnum getDistributionTypeEnum() {
            return distributionTypeEnum;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <E extends DistributionType> E getDistributionType() {
            return (E)this.distributionType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isInterfaceVariable(){
            return isInterfaceVariable;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Variable getInterfaceVariable(){
            return this.interfaceVariable;
        }

        private void setInterfaceVariable(Variable interfaceVariable_){
            this.interfaceVariable = interfaceVariable_;
        }

        public Attribute getAttribute(){return attribute;}

        public void setAttribute(Attribute att) {
            this.attribute=att;
            if (att!=null)
                this.observable = true;
            else
                this.observable = false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VariableBuilder getVariableBuilder(){
            VariableBuilder variableBuilder = new VariableBuilder();
            variableBuilder.setAttribute(this.getAttribute());
            variableBuilder.setDistributionType(this.getDistributionTypeEnum());
            variableBuilder.setName(this.getName());
            variableBuilder.setObservable(this.observable);
            variableBuilder.setStateSpaceType(this.getStateSpaceType());

            return variableBuilder;
        }

        public boolean isDynamicVariable(){
            return true;
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode(){
            return this.name.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isParameterVariable() {
            return false;
        }

    }
}
