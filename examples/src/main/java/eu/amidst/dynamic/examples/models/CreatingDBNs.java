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

package eu.amidst.dynamic.examples.models;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

/**
 * This example creates a dynamic BN from a dynamic data stream, with randomly generated probability distributions, then saves it to a file.
 */
public class CreatingDBNs {

    public static void main(String[] args) throws Exception{

        //Open the data stream using the static class DynamicDataStreamLoader
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(
                "datasets/simulated/syntheticDataDiscrete.arff");

        /**
         * 1. Once the data is loaded, we create a random variable for each of the attributes (i.e. data columns)
         * in our data.
         *
         * 2. {@link DynamicVariables} is the class for doing that. It takes a list of Attributes and internally creates
         * all the variables. We create the variables using DynamicVariables class to guarantee that each variable
         * has a different ID number and make it transparent for the user. Each random variable has an associated
         * interface variable.
         *
         * 3. We can extract the Variable objects by using the method getVariableByName();
         */
        DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());
        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        Variable A = dynamicVariables.getVariableByName("A");
        Variable B = dynamicVariables.getVariableByName("B");
        Variable C = dynamicVariables.getVariableByName("C");
        Variable D = dynamicVariables.getVariableByName("D");
        Variable E = dynamicVariables.getVariableByName("E");
        Variable G = dynamicVariables.getVariableByName("G");

        Variable A_Interface = dynamicVariables.getInterfaceVariable(A);
        Variable B_Interface = dynamicVariables.getInterfaceVariable(B);

        //Note that C_Interface and D_Interface are also created although they will not be used
        //(we will not add temporal dependencies)

        Variable E_Interface = dynamicVariables.getInterfaceVariable(E);
        Variable G_Interface = dynamicVariables.getInterfaceVariable(G);

        // Example of the dynamic DAG structure
        // Time 0: Parents at time 0 are automatically created when adding parents at time T
        dynamicDAG.getParentSetTimeT(B).addParent(A);
        dynamicDAG.getParentSetTimeT(C).addParent(A);
        dynamicDAG.getParentSetTimeT(D).addParent(A);
        dynamicDAG.getParentSetTimeT(E).addParent(A);
        dynamicDAG.getParentSetTimeT(G).addParent(A);
        dynamicDAG.getParentSetTimeT(A).addParent(A_Interface);
        dynamicDAG.getParentSetTimeT(B).addParent(B_Interface);
        dynamicDAG.getParentSetTimeT(E).addParent(E_Interface);
        dynamicDAG.getParentSetTimeT(G).addParent(G_Interface);

        System.out.println(dynamicDAG.toString());

        /**
         * 1. We now create the Dynamic Bayesian network from the previous Dynamic DAG.
         *
         * 2. The DBN object is created from the DynamicDAG. It automatically looks at the distribution type
         * of each variable and their parents to initialize the Distributions objects that are stored
         * inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
         * properly initialized.
         *
         * 3. The network is printed and we can have a look at the kind of distributions stored in the DBN object.
         */
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dynamicDAG);
        System.out.printf(dbn.toString());

        /**
         * Finally teh Bayesian network is saved to a file.
         */
        DynamicBayesianNetworkWriter.save(dbn, "networks/simulated/DBNExample.dbn");
    }

}