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

package eu.amidst.huginlink.converters;

import COM.hugin.HAPI.Class;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by afa on 8/1/15.
 */
public class DBNConverterToHuginTest {


    public static DynamicBayesianNetwork getAmidst_DBN_Example()  {

        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("../datasets/simulated/syntheticDataDiscrete.arff");

        DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());
        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        Variable A = dynamicVariables.getVariableByName("A");
        Variable B = dynamicVariables.getVariableByName("B");
        Variable C = dynamicVariables.getVariableByName("C");
        Variable D = dynamicVariables.getVariableByName("D");
        Variable E = dynamicVariables.getVariableByName("E");
        Variable G = dynamicVariables.getVariableByName("G");

        Variable A_TClone = dynamicVariables.getInterfaceVariable(A);
        Variable B_TClone = dynamicVariables.getInterfaceVariable(B);

        //Note that C_TClone and D_TClone are created although they are not used (do not have temporal dependencies)

        Variable E_TClone = dynamicVariables.getInterfaceVariable(E);
        Variable G_TClone = dynamicVariables.getInterfaceVariable(G);

        // EXAMPLE OF A DAG STRUCTURE

        /*

        DAG Time 0
        A : {  }
        B : { A }
        C : { A }
        D : { A }
        E : { A }
        G : { A }

        DAG Time T
        A : { A_TClone }
        B : { A, B_TClone }
        C : { A }
        D : { A }
        E : { A, E_TClone }
        G : { A, G_TClone }

        */

        // Time 0: Parents at time 0 are automatically created when adding parents at time t !!!
        // Time t
        dynamicDAG.getParentSetTimeT(B).addParent(A);
        dynamicDAG.getParentSetTimeT(C).addParent(A);
        dynamicDAG.getParentSetTimeT(D).addParent(A);
        dynamicDAG.getParentSetTimeT(E).addParent(A);
        dynamicDAG.getParentSetTimeT(G).addParent(A);
        dynamicDAG.getParentSetTimeT(A).addParent(A_TClone);
        dynamicDAG.getParentSetTimeT(B).addParent(B_TClone);
        dynamicDAG.getParentSetTimeT(E).addParent(E_TClone);
        dynamicDAG.getParentSetTimeT(G).addParent(G_TClone);

        DynamicBayesianNetwork amidstDBN = new DynamicBayesianNetwork(dynamicDAG);

        //****************************************** Distributions *****************************************************

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
        Multinomial distA_Time0 = amidstDBN.getConditionalDistributionTime0(A);
        distA_Time0.setProbabilities(new double[]{0.3, 0.7});

        // Variable B
        Multinomial_MultinomialParents distB_Time0 = amidstDBN.getConditionalDistributionTime0(B);
        distB_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.1, 0.5});
        distB_Time0.getMultinomial(1).setProbabilities(new double[]{0.2, 0.5, 0.3});

        // Variable C
        Multinomial_MultinomialParents distC_Time0 = amidstDBN.getConditionalDistributionTime0(C);
        distC_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
        distC_Time0.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});

        // Variable D
        Multinomial_MultinomialParents distD_Time0 = amidstDBN.getConditionalDistributionTime0(D);
        distD_Time0.getMultinomial(0).setProbabilities(new double[]{0.7, 0.3});
        distD_Time0.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable E
        Multinomial_MultinomialParents distE_Time0 = amidstDBN.getConditionalDistributionTime0(E);
        distE_Time0.getMultinomial(0).setProbabilities(new double[]{0.8, 0.2});
        distE_Time0.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable G
        Multinomial_MultinomialParents distG_Time0 = amidstDBN.getConditionalDistributionTime0(G);
        distG_Time0.getMultinomial(0).setProbabilities(new double[]{0.6, 0.4});
        distG_Time0.getMultinomial(1).setProbabilities(new double[]{0.7, 0.3});

        // ********************************************************************************************
        // ************************************** TIME T **********************************************
        // ********************************************************************************************

        // Variable A
        Multinomial_MultinomialParents distA_TimeT = amidstDBN.getConditionalDistributionTimeT(A);
        distA_TimeT.getMultinomial(0).setProbabilities(new double[]{0.15, 0.85});
        distA_TimeT.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable B
        Multinomial_MultinomialParents distB_TimeT = amidstDBN.getConditionalDistributionTimeT(B);
        distB_TimeT.getMultinomial(0).setProbabilities(new double[]{0.1, 0.2, 0.7});
        distB_TimeT.getMultinomial(1).setProbabilities(new double[]{0.6, 0.1, 0.3});
        distB_TimeT.getMultinomial(2).setProbabilities(new double[]{0.3, 0.4, 0.3});
        distB_TimeT.getMultinomial(3).setProbabilities(new double[]{0.2, 0.1, 0.7});
        distB_TimeT.getMultinomial(4).setProbabilities(new double[]{0.5, 0.1, 0.4});
        distB_TimeT.getMultinomial(5).setProbabilities(new double[]{0.1, 0.1, 0.8});

        // Variable C: equals to the distribution at time 0 (C does not have temporal clone)
        Multinomial_MultinomialParents distC_TimeT = amidstDBN.getConditionalDistributionTimeT(C);
        distC_TimeT.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
        distC_TimeT.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});

        // Variable D: equals to the distribution at time 0 (D does not have temporal clone)
        Multinomial_MultinomialParents distD_TimeT = amidstDBN.getConditionalDistributionTimeT(D);
        distD_TimeT.getMultinomial(0).setProbabilities(new double[]{0.7, 0.3});
        distD_TimeT.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable E
        Multinomial_MultinomialParents distE_TimeT = amidstDBN.getConditionalDistributionTimeT(E);
        distE_TimeT.getMultinomial(0).setProbabilities(new double[]{0.3, 0.7});
        distE_TimeT.getMultinomial(1).setProbabilities(new double[]{0.6, 0.4});
        distE_TimeT.getMultinomial(2).setProbabilities(new double[]{0.7, 0.3});
        distE_TimeT.getMultinomial(3).setProbabilities(new double[]{0.9, 0.1});

        // Variable G
        Multinomial_MultinomialParents distG_TimeT = amidstDBN.getConditionalDistributionTimeT(G);
        distG_TimeT.getMultinomial(0).setProbabilities(new double[]{0.2, 0.8});
        distG_TimeT.getMultinomial(1).setProbabilities(new double[]{0.5, 0.5});
        distG_TimeT.getMultinomial(2).setProbabilities(new double[]{0.3, 0.7});
        distG_TimeT.getMultinomial(3).setProbabilities(new double[]{0.8, 0.2});

        return (amidstDBN);
    }

    @Before
    public void setUp() throws ExceptionHugin {

        DynamicBayesianNetwork amidstDBN = getAmidst_DBN_Example();
        System.out.println("\n\nConverting the AMIDST Dynamic BN into Hugin format ...");
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        String outFile = new String("../networks/simulated/huginDBNFromAMIDST.oobn");

        //The name of the DBN must be the same as the name of the out file !!!
        huginDBN.setName("huginDBNFromAMIDST");

        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");
        //--------------------------------------------------------------------------------------------------------------
    }

    @Test
    public void testModels() throws ExceptionHugin {

    }
}