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
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;

import java.util.List;

//TODO Implement outputString method

/**
 * This interface handles a Variable of a Bayesian network.
 * The Variable contains information such as observable or not, the state space type and the distribution type.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#bnexample"> http://amidst.github.io/toolbox/CodeExamples.html#bnexample </a>  </p>
 *
 */
public interface Variable {

    /**
     * Returns the name of this Variable.
     * @return a String representing the name of this Variable.
     */
    String getName();

    /**
     * Returns the ID of this Variable.
     * @return an int representing the ID of this Variable.
     */
    int getVarID();

    /**
     * Tests whether this Variable is observable or not.
     * @return true if this Variable is observable, false otherwise.
     */
    boolean isObservable();

    /**
     * Returns the state space type of this Variable.
     * @param <E> the type of elements.
     * @return the state space type of this Variable.
     */
    <E extends StateSpaceType> E getStateSpaceType();

    /**
     * Returns the state space type of this Variable.
     * @return the state space type of this Variable.
     */
    default StateSpaceTypeEnum getStateSpaceTypeEnum(){
        return this.getStateSpaceType().getStateSpaceTypeEnum();
    }

    /**
     * Returns the number of states of this Variable, in case it has a finite state space. Otherwise it returns -1.
     * @return the number of states of this Variable.
     */
    int getNumberOfStates();

    /**
     * Returns the distribution type of this Variable.
     * @param <E> the type of elements.
     * @return the distribution type of this Variable.
     */
    <E extends DistributionType> E getDistributionType();

    /**
     * Returns the distribution type of this Variable.
     * @return the distribution type of this Variable.
     */
    DistributionTypeEnum getDistributionTypeEnum();

    /**
     * Tests whether this Variable is an interface variable or not.
     * @return true if this Variable is an interface variable, false otherwise.
     */
    boolean isInterfaceVariable();

    /**
     * Tests whether this Variable is dynamic or not.
     * @return true if this Variable is dynamic, false otherwise.
     */
    boolean isDynamicVariable();

    /**
     * Tests whether this Variable is a parameter variable or not.
     * @return true if this Variable is a parameter variable, false otherwise.
     */
    boolean isParameterVariable();

    /**
     * Returns the interface variable.
     * @return the interface variable.
     */
    default Variable getInterfaceVariable(){
        throw new UnsupportedOperationException("This type of variable cannot have an interface variable");
    }

    /**
     * Returns the {@link Attribute} associated with this Variable.
     * @return the attribute associated with this Variable.
     */
    Attribute getAttribute();

    /**
     * Creates a new {@link UnivariateDistribution} for this Variable.
     * @param <E> the type of elements.
     * @return an univariate distribution for this Variable.
     */
    default <E extends UnivariateDistribution> E newUnivariateDistribution(){
        return this.getDistributionType().newUnivariateDistribution();
    }

    /**
     * Creates a new {@link ConditionalDistribution} for this Variable.
     * @param parents the list of parent variables.
     * @param <E> the type of elements.
     * @return a conditional distribution for this Variable.
     */
    default <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents){
        return this.getDistributionType().newConditionalDistribution(parents);
    }


    /**
     * Returns the associated, properly created, VariableBuilder object.
     * @return A {@link VariableBuilder} object.
     */
    VariableBuilder getVariableBuilder();

    /**
     * {@inheritDoc}
     */
    @Override
    int hashCode();

    /**
     * {@inheritDoc}
     */
    @Override
    boolean equals(Object o);

    /**
     * Tests whether this Variable follows a normal distribution.
     * @return true if this Variable follows a normal distribution, false otherwise.
     */
    default boolean isNormal(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.NORMAL)==0);
    }

    /**
     * Tests whether this Variable follows a multinomial distribution.
     * @return true if this Variable follows a multinomial distribution, false otherwise.
     */
    default boolean isMultinomial(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.MULTINOMIAL)==0);
    }


    /**
     * Tests whether this Variable follows a sparse multinomial distribution.
     * @return true if this Variable follows a multinomial distribution, false otherwise.
     */
    default boolean isSparseMultinomial(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.SPARSE_MULTINOMIAL)==0);
    }

    /**
     * Tests whether this Variable follows a multinomial logistic distribution.
     * @return true if this Variable follows a multinomial logistic distribution, false otherwise.
     */
    default boolean isMultinomialLogistic(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.MULTINOMIAL_LOGISTIC)==0);
    }

    /**
     * Tests whether this Variable follows an inverse gamma parameter distribution.
     * @return true if this Variable follows an inverse gamma parameter distribution, false otherwise.
     */
    default boolean isInverseGammaParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.INV_GAMMA_PARAMETER)==0);
    }

    /**
     * Tests whether this Variable follows a gamma parameter distribution.
     * @return true if this Variable follows a gamma parameter distribution, false otherwise.
     */
    default boolean isGammaParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.GAMMA_PARAMETER)==0);
    }

    /**
     * Tests whether this Variable follows a gamma parameter distribution.
     * @return true if this Variable follows a gamma parameter distribution, false otherwise.
     */
    default boolean isNormalGammaParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.NORMAL_GAMMA_PARAMETER)==0);
    }

    /**
     * Tests whether this Variable follows a Dirichlet parameter distribution.
     * @return true if this Variable follows a Dirichlet parameter distribution, false otherwise.
     */
    default  boolean isDirichletParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.DIRICHLET_PARAMETER)==0);
    }

    /**
     * Tests whether this Variable follows a Sparse Dirichlet parameter distribution.
     * @return true if this Variable follows a Dirichlet parameter distribution, false otherwise.
     */
    default  boolean isSparseDirichletParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.SPARSE_DIRICHLET_PARAMETER)==0);
    }


    /**
     * Tests whether this Variable follows a normal parameter distribution.
     * @return true if this Variable follows a normal parameter distribution, false otherwise.
     */
    default  boolean isNormalParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.NORMAL_PARAMETER)==0);
    }

    /**
     * Tests whether this Variable follows a "indicator" distribution.
     * @return true if this Variable follows a indicator distribution, false otherwise.
     */
    default  boolean isIndicator(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.INDICATOR)==0);
    }


}
