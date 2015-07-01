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
package eu.amidst.core.examples.variables;


import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;

import java.util.Arrays;

/**
 *
 * This example show the basic functionality of the classes Variables and Variable.
 *
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class VariablesExample {

    public static void main(String[] args) throws Exception {

        //We first create an empty Variables object
        Variables variables = new Variables();

        //We invoke the "new" methods of the object Variables to create new variables.
        //Now we create a Gaussian variables
        Variable gaussianVar = variables.newGaussianVariable("Gaussian");

        //Now we create a Multinomial variable with two states
        Variable multinomialVar = variables.newMultionomialVariable("Multinomial", 2);

        //Now we create a Multinomial variable with two states: TRUE and FALSE
        Variable multinomialVar2 = variables.newMultionomialVariable("Multinomial2", Arrays.asList("TRUE, FALSE"));

        //For Multinomial variables we can iterate over their different states
        FiniteStateSpace states = multinomialVar2.getStateSpaceType();
        states.getStatesNames().forEach(System.out::println);

        //Variable objects can also be used, for example, to know if one variable can be set as parent of some other variable
        System.out.println("Can a Gaussian variable be parent of Multinomial variable? " +
                (multinomialVar.getDistributionType().isParentCompatible(gaussianVar)));

        System.out.println("Can a Multinomial variable be parent of Gaussian variable? " +
                (gaussianVar.getDistributionType().isParentCompatible(multinomialVar)));

    }
}