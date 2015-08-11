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

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.variables.*;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to store and to handle the creation of all the parameter variables of
 * a extended Bayesian network model, <code>EF_LearningBayesianNetwork</code>.
 *
 */
public class ParameterVariables implements Iterable<Variable>, Serializable {

    private static final long serialVersionUID = 5077959998533923231L;

    /** A list containing of the paramter variables*/
    private List<Variable> allParameterVariables;

    /** A mapping between the name of a paramter variable and its index*/
    private Map<String, Integer> mapping;

    /** Parameter variables are indexed which number higher than the number
     * of non-parameter variables to avoid collisions. The base index contains
     * the initial index number of parameter variables.*/
    int baseIndex;


    /**
     * Creates a new ParameterVariables object. The
     * current number of non-parameter variables should be provided.
     * @param numberOfVariables
     */
    public ParameterVariables(int numberOfVariables) {
        this.allParameterVariables = new ArrayList<>();
        this.mapping = new ConcurrentHashMap<>();
        this.baseIndex=numberOfVariables;
    }

    /**
     * Creates a new parameter Gaussian Variable from a given name.
     * @param name a given name.
     * @return a new gaussian Variable.
     */
    public Variable newGaussianParameter(String name) {
        return this.newVariable(name, DistributionTypeEnum.NORMAL_PARAMETER, new RealStateSpace());
    }


    /**
     * Creates a new parameter inverse Gamma Variable from a given name.
     * @param name a given name.
     * @return a new gaussian Variable.
     */
    public Variable newInverseGammaParameter(String name){
        return this.newVariable(name, DistributionTypeEnum.INV_GAMMA_PARAMETER, new RealStateSpace());
    }


    /**
     * Creates a new parameter Gamma Variable from a given name.
     * @param name a given name.
     * @return a new gaussian Variable.
     */
    public Variable newGammaParameter(String name){
        return this.newVariable(name, DistributionTypeEnum.GAMMA_PARAMETER, new RealStateSpace());
    }


    /**
     * Creates a new parameter Dirichlet Variable from a given name.
     * @param name a given name.
     * @return a new gaussian Variable.
     */
    public Variable newDirichletParameter(String name, int nOfStates) {
        return this.newVariable(name, DistributionTypeEnum.DIRICHLET_PARAMETER, new FiniteStateSpace(nOfStates));
    }

    /**
     * Creates a new parameter variable.
     * @param name, the name of the variable.
     * @param distributionTypeEnum, the distribution type of the variable.
     * @param stateSpaceType, the state space of the variable.
     * @return A new created <code>Variable</code> object.
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
     * Creates a new paramater variable with the information providec by a VariableBuilder object.
     * @param builder, a <code>VariableBuilder</code>
     * @return A new created <code>Variable</code> object.
     */
    private Variable newVariable(VariableBuilder builder) {
        ParameterVariable var = new ParameterVariable(builder, this.baseIndex + allParameterVariables.size());
        if (mapping.containsKey(var.getName())) {
            throw new IllegalArgumentException("Attribute list contains duplicated names: " + var.getName());
        }
        this.mapping.put(var.getName(), var.getVarID());
        allParameterVariables.add(var);
        return var;

    }


    /**
     * Returns the parameter variable with the given integer index,
     * @param varID, an integer index.
     * @return A <code>Variable</code> object.
     */
    public Variable getVariableById(int varID) {
        return this.allParameterVariables.get(varID - this.baseIndex);
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

    /**
     * Returns the number of parameter variables
     * @return A integer positive value
     */
    public int getNumberOfVars() {
        return this.allParameterVariables.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Variable> iterator() {
        return this.allParameterVariables.iterator();
    }

    /**
     * Return a list with all the parameter variables
     * @return A list of <code>Variable</code> objects.
     */
    public List<Variable> getListOfParamaterVariables(){
        return this.allParameterVariables;
    }

    //TODO Implements hashCode method!!

    private static class ParameterVariable implements Variable, Serializable {

        private static final long serialVersionUID = 4656207896676444152L;

        private String name;
        private int varID;
        private boolean observable;
        private StateSpaceType stateSpaceType;
        private DistributionTypeEnum distributionTypeEnum;
        private DistributionType distributionType;

        private Attribute attribute;
        private int numberOfStates = -1;


        public ParameterVariable(VariableBuilder builder, int varID) {
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
            return false;
            //throw new UnsupportedOperationException("A parameter variable cannot be temporal.");
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
            return true;
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

        @Override
        public int hashCode(){
            return this.name.hashCode();
        }


    }
}
