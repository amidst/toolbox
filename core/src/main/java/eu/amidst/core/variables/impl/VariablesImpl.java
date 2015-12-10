/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.core.variables.impl;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.*;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//TODO Remove method getVariableByVarID()!!

//TODO Does the best way to implement hashcode?

/**
 * This class is used to store and to handle the creation of all the variables of
 * a Bayesian network model.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#variablesexample"> http://amidst.github.io/toolbox/CodeExamples.html#variablesexample </a>  </p>
 */
public class VariablesImpl implements Variables, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 5077959998533923231L;

    /** Represents the list of all the variables. */
    private List<Variable> allVariables;

    /** Represents the set of variables as a {@link Map} object
     * that maps the Variable names ({@code String}) to their IDs ({@code Integer}). */
    private Map<String, Integer> mapping;

    /**
     * Creates a new list of Variables.
     */
    public VariablesImpl() {
        this.allVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new list of Variables given a list of Attributes.
     * @param atts a list of Attributes.
     */
    public VariablesImpl(Attributes atts) {
        this.allVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();

        for (Attribute att : atts.getListOfNonSpecialAttributes()) {
            VariableBuilder builder = new VariableBuilder(att);
            VariableImpl var = new VariableImpl(builder, allVariables.size());
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
     * @param typeDists a {@link HashMap} object that maps the Attributes to their distribution types.
     */
    public VariablesImpl(Attributes atts, HashMap<Attribute, DistributionTypeEnum> typeDists) {

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
        for (Variable variable : allVariables) {
            VariableImpl variableImpl = (VariableImpl)variable;
            variableImpl.setAttribute(attributes.getAttributeByName(variable.getName()));
        }
    }

    /**
     * Creates a new multionomial Variable from a given Attribute.
     * @param att a given Attribute.
     * @return a new multionomial Variable.
     */
    public Variable newMultionomialVariable(Attribute att) {
        return this.newVariable(att, DistributionTypeEnum.MULTINOMIAL);
    }

    /**
     * Creates a new multionomial Variable from a given name and number of states.
     * @param name a given name.
     * @param nOfStates number of states.
     * @return a new multionomial Variable.
     */
    public Variable newMultionomialVariable(String name, int nOfStates) {
        return this.newVariable(name, DistributionTypeEnum.MULTINOMIAL, new FiniteStateSpace(nOfStates));
    }

    /**
     * Creates a new multionomial Variable from a given name and a list of states.
     * @param name a given name.
     * @param states a list of states.
     * @return a new multionomial Variable.
     */
    public Variable newMultionomialVariable(String name, List<String> states) {
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
     * Creates a new Variable given an Attribute and a distribution type.
     * @param att an Attribute.
     * @param distributionTypeEnum a distribution type.
     * @return a new {@link Variable}.
     */
    public Variable newVariable(Attribute att, DistributionTypeEnum distributionTypeEnum) {
        VariableBuilder builder = new VariableBuilder(att);
        builder.setDistributionType(distributionTypeEnum);
        VariableImpl var = new VariableImpl(builder, allVariables.size());
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
        VariableImpl var = new VariableImpl(builder, allVariables.size());
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
        VariableImpl var = new VariableImpl(builder, allVariables.size());
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



}
