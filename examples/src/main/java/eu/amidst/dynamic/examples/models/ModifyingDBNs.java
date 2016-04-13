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
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

/**
 * In this example we show how to access and modify the conditional probabilities of a Dynamic Bayesian network model.
 *
 * Created by ana@cs.aau.dk on 02/12/15.
 */
public class ModifyingDBNs {
    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DynamicDataStreamLoader
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

        // EXAMPLE OF THE DAG STRUCTURE

        /*

        DAG Time 0
        A : {  }
        B : { A }
        C : { A }
        D : { A }
        E : { A }
        G : { A }

        DAG Time T
        A : { A_Interface }
        B : { A, B_Interface }
        C : { A }
        D : { A }
        E : { A, E_Interface }
        G : { A, G_Interface }

        */

        // Time 0: Parents at time 0 are automatically created when adding parents at time T
        // Time t
        dynamicDAG.getParentSetTimeT(B).addParent(A);
        dynamicDAG.getParentSetTimeT(C).addParent(A);
        dynamicDAG.getParentSetTimeT(D).addParent(A);
        dynamicDAG.getParentSetTimeT(E).addParent(A);
        dynamicDAG.getParentSetTimeT(G).addParent(A);
        dynamicDAG.getParentSetTimeT(A).addParent(A_Interface);
        dynamicDAG.getParentSetTimeT(B).addParent(B_Interface);
        dynamicDAG.getParentSetTimeT(E).addParent(E_Interface);
        dynamicDAG.getParentSetTimeT(G).addParent(G_Interface);

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

        /*
         * We now modify the conditional probability distributions to assigned the values we want
         */

        /* IMPORTANT: The parents are indexed according to Koller (Chapter 10. Pag. 358). Example:
           Parents: A = {A0,A1} and B = {B0,B1,B2}.
           NumberOfPossibleAssignments = 6

           Index   A    B
             0     A0   B0
             1     A1   B1
             2     A0   B2
             3     A1   B0
             4     A0   B1
             5     A1   B2
        */

        // ********************************************************************************************
        // ************************************** TIME 0 **********************************************
        // ********************************************************************************************

        // Variable A
        Multinomial distA_Time0 = dbn.getConditionalDistributionTime0(A);
        distA_Time0.setProbabilities(new double[]{0.3, 0.7});

        // Variable B
        Multinomial_MultinomialParents distB_Time0 = dbn.getConditionalDistributionTime0(B);
        distB_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.1, 0.5});
        distB_Time0.getMultinomial(1).setProbabilities(new double[]{0.2, 0.5, 0.3});

        // Variable C
        Multinomial_MultinomialParents distC_Time0 = dbn.getConditionalDistributionTime0(C);
        distC_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
        distC_Time0.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});

        // Variable D
        Multinomial_MultinomialParents distD_Time0 = dbn.getConditionalDistributionTime0(D);
        distD_Time0.getMultinomial(0).setProbabilities(new double[]{0.7, 0.3});
        distD_Time0.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable E
        Multinomial_MultinomialParents distE_Time0 = dbn.getConditionalDistributionTime0(E);
        distE_Time0.getMultinomial(0).setProbabilities(new double[]{0.8, 0.2});
        distE_Time0.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable G
        Multinomial_MultinomialParents distG_Time0 = dbn.getConditionalDistributionTime0(G);
        distG_Time0.getMultinomial(0).setProbabilities(new double[]{0.6, 0.4});
        distG_Time0.getMultinomial(1).setProbabilities(new double[]{0.7, 0.3});

        // ********************************************************************************************
        // ************************************** TIME T **********************************************
        // ********************************************************************************************

        // Variable A
        Multinomial_MultinomialParents distA_TimeT = dbn.getConditionalDistributionTimeT(A);
        distA_TimeT.getMultinomial(0).setProbabilities(new double[]{0.15, 0.85});
        distA_TimeT.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable B
        Multinomial_MultinomialParents distB_TimeT = dbn.getConditionalDistributionTimeT(B);
        distB_TimeT.getMultinomial(0).setProbabilities(new double[]{0.1, 0.2, 0.7});
        distB_TimeT.getMultinomial(1).setProbabilities(new double[]{0.6, 0.1, 0.3});
        distB_TimeT.getMultinomial(2).setProbabilities(new double[]{0.3, 0.4, 0.3});
        distB_TimeT.getMultinomial(3).setProbabilities(new double[]{0.2, 0.1, 0.7});
        distB_TimeT.getMultinomial(4).setProbabilities(new double[]{0.5, 0.1, 0.4});
        distB_TimeT.getMultinomial(5).setProbabilities(new double[]{0.1, 0.1, 0.8});

        // Variable C: equals to the distribution at time 0 (C does not have temporal clone)
        Multinomial_MultinomialParents distC_TimeT = dbn.getConditionalDistributionTimeT(C);
        distC_TimeT.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
        distC_TimeT.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});

        // Variable D: equals to the distribution at time 0 (D does not have temporal clone)
        Multinomial_MultinomialParents distD_TimeT = dbn.getConditionalDistributionTimeT(D);
        distD_TimeT.getMultinomial(0).setProbabilities(new double[]{0.7, 0.3});
        distD_TimeT.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable E
        Multinomial_MultinomialParents distE_TimeT = dbn.getConditionalDistributionTimeT(E);
        distE_TimeT.getMultinomial(0).setProbabilities(new double[]{0.3, 0.7});
        distE_TimeT.getMultinomial(1).setProbabilities(new double[]{0.6, 0.4});
        distE_TimeT.getMultinomial(2).setProbabilities(new double[]{0.7, 0.3});
        distE_TimeT.getMultinomial(3).setProbabilities(new double[]{0.9, 0.1});

        // Variable G
        Multinomial_MultinomialParents distG_TimeT = dbn.getConditionalDistributionTimeT(G);
        distG_TimeT.getMultinomial(0).setProbabilities(new double[]{0.2, 0.8});
        distG_TimeT.getMultinomial(1).setProbabilities(new double[]{0.5, 0.5});
        distG_TimeT.getMultinomial(2).setProbabilities(new double[]{0.3, 0.7});
        distG_TimeT.getMultinomial(3).setProbabilities(new double[]{0.8, 0.2});

        /*
         * We print the new DBN
         */
        System.out.println(dbn.toString());

    }
}
