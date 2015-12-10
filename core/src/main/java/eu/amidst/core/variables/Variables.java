/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.variables;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;

import java.util.List;

//TODO Remove method getVariableByVarID()!!

//TODO Does the best way to implement hashcode?

/**
 * This class is used to store and to handle the creation of all the variables of
 * a Bayesian network model.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#variablesexample"> http://amidst.github.io/toolbox/CodeExamples.html#variablesexample </a>  </p>
 */
public interface Variables extends Iterable<Variable> {



    /**
     * Sets a new set of attributes. Links current variables with this new set by matching
     * variable names with attributes names.
     * @param attributes an object of class {@link Attributes}
     */
    void setAttributes(Attributes attributes);

    /**
     * Creates a new multionomial Variable from a given Attribute.
     * @param att a given Attribute.
     * @return a new multionomial Variable.
     */
    Variable newMultionomialVariable(Attribute att);

    /**
     * Creates a new multionomial Variable from a given name and number of states.
     * @param name a given name.
     * @param nOfStates number of states.
     * @return a new multionomial Variable.
     */
    Variable newMultionomialVariable(String name, int nOfStates);

    /**
     * Creates a new multionomial Variable from a given name and a list of states.
     * @param name a given name.
     * @param states a list of states.
     * @return a new multionomial Variable.
     */
    Variable newMultionomialVariable(String name, List<String> states);

    /**
     * Creates a new multionomial logistic Variable from a given Attribute.
     * @param att a given Attribute.
     * @return a new multinomial logistic Variable.
     */
    Variable newMultinomialLogisticVariable(Attribute att);

    /**
     * Creates a new multionomial logistic Variable from a given name and number of states.
     * @param name a given name.
     * @param nOfStates number of states.
     * @return a new multionomial logistic Variable.
     */
    Variable newMultinomialLogisticVariable(String name, int nOfStates);

    /**
     * Creates a new multionomial logistic Variable from a given name and a list of states.
     * @param name a given name.
     * @param states a list of states.
     * @return a new multionomial logistic Variable.
     */
    Variable newMultinomialLogisticVariable(String name, List<String> states);

    /**
     * Creates a new gaussian Variable from a given Attribute.
     * @param att a given Attribute.
     * @return a new gaussian logistic Variable.
     */
    Variable newGaussianVariable(Attribute att);

    /**
     * Creates a new Gaussian Variable from a given name.
     * @param name a given name.
     * @return a new gaussian Variable.
     */
    Variable newGaussianVariable(String name);

    /**
     * Creates a new Variable given an Attribute and a distribution type.
     * @param att an Attribute.
     * @param distributionTypeEnum a distribution type.
     * @return a new {@link Variable}.
     */
    Variable newVariable(Attribute att, DistributionTypeEnum distributionTypeEnum);

    /**
     * Creates a new Variable given an Attribute.
     * @param att an Attribute.
     * @return a new {@link Variable}.
     */
    Variable newVariable(Attribute att);

    /**
     * Creates a new Variable given a {@link VariableBuilder} object.
     * @param builder a {@link VariableBuilder} object.
     * @return a new {@link Variable}.
     */
    Variable newVariable(VariableBuilder builder);

    /**
     * Returns a Variable given its ID.
     * @param varID the ID of the Variable to be returned.
     * @return a {@link Variable}.
     */
    Variable getVariableById(int varID);

    /**
     * Returns a Variable given its name.
     * @param name the name of the Variable to be returned.
     * @return a {@link Variable}.
     */
    Variable getVariableByName(String name);

    /**
     * Returns the number of all variables.
     * @return the total number of variables.
     */
    int getNumberOfVars();

    /**
     * Defines the list of Variables as an unmodifiable list.
     */
    void block();

    /**
     * Returns the list of Variables.
     * @return the list of Variables.
     */
    List<Variable> getListOfVariables();

}
