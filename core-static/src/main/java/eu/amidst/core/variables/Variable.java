/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.variables;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;

import java.util.List;

/**
 * TODO Implements outputString method
 * Created by afa on 02/07/14.
 */
public interface Variable {

    String getName();

    int getVarID();

    boolean isObservable();

    <E extends StateSpaceType> E getStateSpaceType();

    default StateSpaceTypeEnum getStateSpaceTypeEnum(){
        return this.getStateSpaceType().getStateSpaceTypeEnum();
    }

    int getNumberOfStates();

    <E extends DistributionType> E getDistributionType();

    DistributionTypeEnum getDistributionTypeEnum();

    boolean isInterfaceVariable();

    boolean isDynamicVariable();

    boolean isParameterVariable();

    default Variable getInterfaceVariable(){
        throw new UnsupportedOperationException("This type of variable cannot have an interface variable");
    }

    Attribute getAttribute();

    default <E extends UnivariateDistribution> E newUnivariateDistribution(){
        return this.getDistributionType().newUnivariateDistribution();
    }

    default <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents){
        return this.getDistributionType().newConditionalDistribution(parents);
    }

    @Override
    int hashCode();

    @Override
    boolean equals(Object o);

    default boolean isNormal(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.NORMAL)==0);
    }

    default boolean isMultinomial(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.MULTINOMIAL)==0);
    }

    default boolean isMultinomialLogistic(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.MULTINOMIAL_LOGISTIC)==0);
    }

    default boolean isInverseGammaParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.INV_GAMMA_PARAMETER)==0);
    }

    default boolean isGammaParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.GAMMA_PARAMETER)==0);
    }

    default  boolean isDirichletParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.DIRICHLET_PARAMETER)==0);
    }

    default  boolean isNormalParameter(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.NORMAL_PARAMETER)==0);
    }

        //default boolean isIndicator(){
    //    return(this.getDistributionTypeEnum().compareTo(DistType.INDICATOR)==0);
    //}


}
