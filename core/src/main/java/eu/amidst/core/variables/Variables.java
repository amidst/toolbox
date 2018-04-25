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

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.distributionTypes.IndicatorType;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.SparseFiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

//TODO Remove method getVariableByVarID()!!

//TODO Does the best way to implement hashcode?

/**
 * This class is used to store and to handle the creation of all the variables of
 * a Bayesian network model.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#variablesexample"> http://amidst.github.io/toolbox/CodeExamples.html#variablesexample </a>  </p>
 */
public class Variables implements Iterable<Variable>, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 5077959998533923231L;

    /** Represents the list of all the variables. */
    private List<Variable> allVariables;

    /** Represents the set of variables as a {@link java.util.Map} object
     * that maps the Variable names ({@code String}) to their IDs ({@code Integer}). */
    private Map<String, Integer> mapping;

    /**
     * Creates a new list of Variables.
     */
    public Variables() {
        this.allVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new list of Variables given a list of Attributes.
     * @param atts a list of Attributes.
     */
    public Variables(Attributes atts) {
        this.allVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();

        for (Attribute att : atts.getListOfNonSpecialAttributes()) {
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
     * Creates a new list of Variables given a list of Attributes and their corresponding distribution types.
     * @param atts a list of Attributes.
     * @param typeDists a {@link java.util.HashMap} object that maps the Attributes to their distribution types.
     */
    public Variables(Attributes atts, HashMap<Attribute, DistributionTypeEnum> typeDists) {

        this.allVariables = new ArrayList<>();

        for (Attribute att : atts.getListOfNonSpecialAttributes()) {
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

    /**
     * Sets a new set of attributes. Links current variables with this new set by matching
     * variable names with attributes names.
     * @param attributes an object of class {@link Attributes}
     */
    public void setAttributes(Attributes attributes){
        for (Attribute att : attributes.getListOfNonSpecialAttributes()) {
            Variable variable = this.getVariableByName(att.getName());
            VariableImplementation variableImplementation = (VariableImplementation)variable;
            variableImplementation.setAttribute(att);
        }
    }

    /**
     * Creates a new multionomial Variable from a given Attribute.
     * @param att a given Attribute.
     * @return a new multionomial Variable.
     */
    public Variable newMultinomialVariable(Attribute att) {
        return this.newVariable(att, DistributionTypeEnum.MULTINOMIAL);
    }

    /**
     * Creates a new multionomial Variable from a given name and number of states.
     * @param name a given name.
     * @param nOfStates number of states.
     * @return a new multionomial Variable.
     */
    public Variable newMultinomialVariable(String name, int nOfStates) {
        return this.newVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(nOfStates));
    }


    /**
     * Creates a new sparse multionomial Variable from a given Attribute.
     * @param att a given Attribute.
     * @return a new multionomial Variable.
     */
    public Variable newSparseMultionomialVariable(Attribute att) {
        if (att.getStateSpaceType().getStateSpaceTypeEnum()!=StateSpaceTypeEnum.SPARSE_FINITE_SET)
            throw new UnsupportedOperationException("A Sparse Multinomial can not be created from a non-sparse attribute");
        return this.newVariable(att, DistributionTypeEnum.SPARSE_MULTINOMIAL);
    }


    /**
     * Creates a new sparse multionomial Variable from a given name and number of states.
     * @param name a given name.
     * @param nOfStates number of states.
     * @return a new multionomial Variable.
     */
    public Variable newSparseMultionomialVariable(String name, int nOfStates) {
        return this.newVariable(name, DistributionTypeEnum.SPARSE_MULTINOMIAL, new SparseFiniteStateSpace(nOfStates));
    }

    /**
     * Creates a new multionomial Variable from a given name and a list of states.
     * @param name a given name.
     * @param states a list of states.
     * @return a new multionomial Variable.
     */
    public Variable newMultinomialVariable(String name, List<String> states) {
        return this.newVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(states));
    }

    /**
     * Creates a new multionomial logistic Variable from a given Attribute.
     * @param att a given Attribute.
     * @return a new multinomial logistic Variable.
     */
    public Variable newMultinomialLogisticVariable(Attribute att) {
        return this.newVariable(att, DistributionTypeEnum.MULTINOMIAL_LOGISTIC);
    }

    /**
     * Creates a new multionomial logistic Variable from a given name and number of states.
     * @param name a given name.
     * @param nOfStates number of states.
     * @return a new multionomial logistic Variable.
     */
    public Variable newMultinomialLogisticVariable(String name, int nOfStates) {
        return this.newVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(nOfStates));
    }

    /**
     * Creates a new multionomial logistic Variable from a given name and a list of states.
     * @param name a given name.
     * @param states a list of states.
     * @return a new multionomial logistic Variable.
     */
    public Variable newMultinomialLogisticVariable(String name, List<String> states) {
        return this.newVariable(name, DistributionTypeEnum.MULTINOMIAL_LOGISTIC, new FiniteStateSpace(states));
    }

    /**
     * Creates a new gaussian Variable from a given Attribute.
     * @param att a given Attribute.
     * @return a new gaussian logistic Variable.
     */
    public Variable newGaussianVariable(Attribute att) {
        return this.newVariable(att, DistributionTypeEnum.NORMAL);
    }

    /**
     * Creates a new Gaussian Variable from a given name.
     * @param name a given name.
     * @return a new gaussian Variable.
     */
    public Variable newGaussianVariable(String name) {
        return this.newVariable(name, DistributionTypeEnum.NORMAL, new RealStateSpace());
    }


    /**
     * Creates a new Truncated ([0,1]) Exponential variable from a given name.
     * @param name a given name.
     * @return a new Truncated Exponential Variable.
     */
    public Variable newTruncatedExponential(String name) {
        return this.newVariable(name, DistributionTypeEnum.TRUNCATED_EXPONENTIAL, new RealStateSpace(0,1));
    }

    /**
     * Creates a new Truncated ([0,1]) Normal variable from a given name.
     * @param name a given name.
     * @return a new Truncated Normal Variable.
     */
    public Variable newTruncatedNormal(String name) {
        return this.newVariable(name, DistributionTypeEnum.TRUNCATED_NORMAL, new RealStateSpace(0,1));
    }

    /**
     * Creates a new Indicator Variable from a given Variable.
     * @param var a given Variable.
     * @return a new indicator Variable.
     */
    public Variable newIndicatorVariable(Variable var, double deltaValue) {
        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setAttribute(var.getAttribute());
        variableBuilder.setDistributionType(DistributionTypeEnum.INDICATOR);
            String newVarName = var.getName()+"_INDICATOR";
        variableBuilder.setName(newVarName);
        variableBuilder.setObservable(true);
        variableBuilder.setStateSpaceType(new FiniteStateSpace(Arrays.asList("Zero","NonZero")));
        Variable newVariable = new VariableImplementation(variableBuilder,this.getNumberOfVars());
        IndicatorType indicatorType = newVariable.getDistributionType();
        indicatorType.setDeltaValue(deltaValue);
        if (mapping.containsKey(newVarName)) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(newVarName, newVariable.getVarID());
        allVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Creates a new Indicator Variable from a given Variable.
     * @param var a given Variable.
     * @return a new indicator Variable.
     */
    public Variable newIndicatorVariable(Variable var, String deltaValueLabel) {
        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setAttribute(var.getAttribute());
        variableBuilder.setDistributionType(DistributionTypeEnum.INDICATOR);
        String newVarName = var.getName()+"_INDICATOR";
        variableBuilder.setName(newVarName);
        variableBuilder.setObservable(true);
        variableBuilder.setStateSpaceType(new FiniteStateSpace(Arrays.asList("Zero","NonZero")));
        Variable newVariable = new VariableImplementation(variableBuilder,this.getNumberOfVars());
        IndicatorType indicatorType = newVariable.getDistributionType();

        if(!(var.getStateSpaceType().getStateSpaceTypeEnum() == StateSpaceTypeEnum.FINITE_SET))
            throw new UnsupportedOperationException("String labels can only be used for multinomial variables");
        indicatorType.setDeltaValue(((FiniteStateSpace)var.getStateSpaceType()).getIndexOfState(deltaValueLabel));
        if (mapping.containsKey(newVarName)) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(newVarName, newVariable.getVarID());
        allVariables.add(newVariable);
        return newVariable;
    }

    /**
     * Creates a new Variable given an Attribute and a distribution type.
     * @param att an Attribute.
     * @param distributionTypeEnum a distribution type.
     * @return a new {@link Variable}.
     */
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

    /**
     * Creates a new Variable given an Attribute and a distribution type.
     * @param att an Attribute.
     * @param distributionTypeEnum a distribution type.
     * @param name the name of the variable to create
     * @return a new {@link Variable}.
     */
    public Variable newVariable(Attribute att, DistributionTypeEnum distributionTypeEnum, String name) {
        VariableBuilder builder = new VariableBuilder(att);
        builder.setDistributionType(distributionTypeEnum);
        builder.setName(name);
        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names");
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);
        return var;
    }

    /**
     * Creates a new Variable given an Attribute.
     * @param att an Attribute.
     * @return a new {@link Variable}.
     */
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

    /**
     * Creates a new Variable given an Attribute, a distribution type, and a state space type.
     * @param name a given name.
     * @param distributionTypeEnum a distribution type.
     * @param stateSpaceType a state space type.
     * @return a new {@link Variable}.
     */
    private Variable newVariable(String name, DistributionTypeEnum distributionTypeEnum, StateSpaceType stateSpaceType) {
        VariableBuilder builder = new VariableBuilder();
        builder.setName(name);
        builder.setDistributionType(distributionTypeEnum);
        builder.setStateSpaceType(stateSpaceType);
        builder.setObservable(false);

        return this.newVariable(builder);
    }

    /**
     * Creates a new Variable given a {@link VariableBuilder} object.
     * @param builder a {@link VariableBuilder} object.
     * @return a new {@link Variable}.
     */
    public Variable newVariable(VariableBuilder builder) {
        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names: " + var.getName());
        }
        this.mapping.put(var.getName(), var.getVarID());
        allVariables.add(var);
        return var;

    }

    /**
     * Returns a Variable given its ID.
     * @param varID the ID of the Variable to be returned.
     * @return a {@link Variable}.
     */
    public Variable getVariableById(int varID) {
        return this.allVariables.get(varID);
    }

    /**
     * Returns a Variable given its name.
     * @param name the name of the Variable to be returned.
     * @return a {@link Variable}.
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
     * Returns the number of all variables.
     * @return the total number of variables.
     */
    public int getNumberOfVars() {
        return this.allVariables.size();
    }

    /**
     * Returns an iterator over elements of type {@code Variable}, i.e. over all the Variables.
     * @return an Iterator over elements of type {@code Variable}.
     */
    @Override
    public Iterator<Variable> iterator() {
        return this.allVariables.iterator();
    }

    /**
     * Defines the list of Variables as an unmodifiable list.
     */
    public void block(){
        //this.allVariables = Collections.unmodifiableList(this.allVariables);
    }

    /**
     * Returns the list of Variables.
     * @return the list of Variables.
     */
    public List<Variable> getListOfVariables(){
        return this.allVariables;
    }


    /**
     * Auxiliar builder. It shoud not be used
     * @param variables list of variables
     * @return list of variables
     */
    public static Variables auxiliarBuilder(List<Variable> variables){
        return new Variables(variables);
    }

    /**
     * Auxiliar builder.
     * @param variables list of variables
     */
    private Variables(List<Variable> variables){
        this.allVariables = new ArrayList<>();
        this.allVariables.addAll(variables);
        this.mapping = new HashMap<>();
        for (Variable var : allVariables) {
            this.mapping.put(var.getName(),var.getVarID());
        }
    }

    /**
     * This class implements the interface {@link Variable}.
     */
    private static class VariableImplementation implements Variable, Serializable {

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = 4656207896676444152L;

        /** Represents the name of the Variable. */
        private String name;

        /** Represents the ID of the Variable. */
        private int varID;

        /** Indicates whether the Variable is observable or not. */
        private boolean observable;

        /** Represents the {@link StateSpaceType} of the Variable. */
        private StateSpaceType stateSpaceType;

        /** Represents the {@link DistributionTypeEnum} of the Variable. */
        private DistributionTypeEnum distributionTypeEnum;

        /** Represents the distribution type of the Variable. */
        private DistributionType distributionType;

        /** Represents the ID of the Variable. */
        private Attribute attribute;

        /** Represents the number of states of the Variable, by default equal to -1. */
        private int numberOfStates = -1;

        /**
         * Constructor that creates a new a VariableBuilder.
         * @param builder a VariableBuilder object.
         * @param varID a Variable ID.
         */
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
            if (this.getStateSpaceType().getStateSpaceTypeEnum() == StateSpaceTypeEnum.SPARSE_FINITE_SET) {
                this.numberOfStates = ((SparseFiniteStateSpace) this.stateSpaceType).getNumberOfStates();
            }
            this.distributionType=distributionTypeEnum.newDistributionType(this);
        }

        public void setAttribute(Attribute attribute) {
            this.attribute = attribute;
            if (attribute!=null)
                this.observable = true;
            else
                this.observable = false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return this.name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getVarID() {
            return varID;
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
        public boolean isInterfaceVariable() {
            //throw new UnsupportedOperationException("In a static context a variable cannot be temporal.");
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Attribute getAttribute() {
            return attribute;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDynamicVariable() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isParameterVariable() {
            return false;
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

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumberOfStates() {
            return this.numberOfStates;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode(){
            return this.name.hashCode();
        }
    }

    public List<Variable> getVariablesForListOfAttributes(List<Attribute> attributeList){
        return attributeList.parallelStream()
                .map(att->getVariableByName(att.getName()))
                .collect(Collectors.toList());
    }

    /*  public Variable addIndicatorVariable(Variable var) {
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

}
