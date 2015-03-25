package eu.amidst.examples;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.io.DynamicDataStreamLoader;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

/**
 * Created by afa on 13/1/15.
 */
public class DBNExample {

    public static DynamicBayesianNetwork getAmidst_DBN_Example()  {

        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("datasets/syntheticDataDiscrete.arff");

        DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());
        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        Variable A = dynamicVariables.getVariable("A");
        Variable B = dynamicVariables.getVariable("B");
        Variable C = dynamicVariables.getVariable("C");
        Variable D = dynamicVariables.getVariable("D");
        Variable E = dynamicVariables.getVariable("E");
        Variable G = dynamicVariables.getVariable("G");

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

        DynamicBayesianNetwork amidstDBN = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);

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
        Multinomial distA_Time0 = amidstDBN.getDistributionTime0(A);
        distA_Time0.setProbabilities(new double[]{0.3, 0.7});

        // Variable B
        Multinomial_MultinomialParents distB_Time0 = amidstDBN.getDistributionTime0(B);
        distB_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.1, 0.5});
        distB_Time0.getMultinomial(1).setProbabilities(new double[]{0.2, 0.5, 0.3});

        // Variable C
        Multinomial_MultinomialParents distC_Time0 = amidstDBN.getDistributionTime0(C);
        distC_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
        distC_Time0.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});

        // Variable D
        Multinomial_MultinomialParents distD_Time0 = amidstDBN.getDistributionTime0(D);
        distD_Time0.getMultinomial(0).setProbabilities(new double[]{0.7, 0.3});
        distD_Time0.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable E
        Multinomial_MultinomialParents distE_Time0 = amidstDBN.getDistributionTime0(E);
        distE_Time0.getMultinomial(0).setProbabilities(new double[]{0.8, 0.2});
        distE_Time0.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable G
        Multinomial_MultinomialParents distG_Time0 = amidstDBN.getDistributionTime0(G);
        distG_Time0.getMultinomial(0).setProbabilities(new double[]{0.6, 0.4});
        distG_Time0.getMultinomial(1).setProbabilities(new double[]{0.7, 0.3});

        // ********************************************************************************************
        // ************************************** TIME T **********************************************
        // ********************************************************************************************

        // Variable A
        Multinomial_MultinomialParents distA_TimeT = amidstDBN.getDistributionTimeT(A);
        distA_TimeT.getMultinomial(0).setProbabilities(new double[]{0.15, 0.85});
        distA_TimeT.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable B
        Multinomial_MultinomialParents distB_TimeT = amidstDBN.getDistributionTimeT(B);
        distB_TimeT.getMultinomial(0).setProbabilities(new double[]{0.1, 0.2, 0.7});
        distB_TimeT.getMultinomial(1).setProbabilities(new double[]{0.6, 0.1, 0.3});
        distB_TimeT.getMultinomial(2).setProbabilities(new double[]{0.3, 0.4, 0.3});
        distB_TimeT.getMultinomial(3).setProbabilities(new double[]{0.2, 0.1, 0.7});
        distB_TimeT.getMultinomial(4).setProbabilities(new double[]{0.5, 0.1, 0.4});
        distB_TimeT.getMultinomial(5).setProbabilities(new double[]{0.1, 0.1, 0.8});

        // Variable C: equals to the distribution at time 0 (C does not have temporal clone)
        Multinomial_MultinomialParents distC_TimeT = amidstDBN.getDistributionTimeT(C);
        distC_TimeT.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
        distC_TimeT.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});

        // Variable D: equals to the distribution at time 0 (D does not have temporal clone)
        Multinomial_MultinomialParents distD_TimeT = amidstDBN.getDistributionTimeT(D);
        distD_TimeT.getMultinomial(0).setProbabilities(new double[]{0.7, 0.3});
        distD_TimeT.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // Variable E
        Multinomial_MultinomialParents distE_TimeT = amidstDBN.getDistributionTimeT(E);
        distE_TimeT.getMultinomial(0).setProbabilities(new double[]{0.3, 0.7});
        distE_TimeT.getMultinomial(1).setProbabilities(new double[]{0.6, 0.4});
        distE_TimeT.getMultinomial(2).setProbabilities(new double[]{0.7, 0.3});
        distE_TimeT.getMultinomial(3).setProbabilities(new double[]{0.9, 0.1});

        // Variable G
        Multinomial_MultinomialParents distG_TimeT = amidstDBN.getDistributionTimeT(G);
        distG_TimeT.getMultinomial(0).setProbabilities(new double[]{0.2, 0.8});
        distG_TimeT.getMultinomial(1).setProbabilities(new double[]{0.5, 0.5});
        distG_TimeT.getMultinomial(2).setProbabilities(new double[]{0.3, 0.7});
        distG_TimeT.getMultinomial(3).setProbabilities(new double[]{0.8, 0.2});

        return (amidstDBN);
    }

    public static DynamicBayesianNetwork getAmidst_DBN_Example2() {

        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("datasets/syntheticDataDiscrete.arff");

        DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());
        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        Variable A = dynamicVariables.getVariable("A");
        Variable B = dynamicVariables.getVariable("B");
        Variable C = dynamicVariables.getVariable("C");
        Variable D = dynamicVariables.getVariable("D");
        Variable E = dynamicVariables.getVariable("E");
        Variable G = dynamicVariables.getVariable("G");

        Variable A_TClone = dynamicVariables.getInterfaceVariable(A);


        // Time 0: Parents at time 0 are automatically created when adding parents at time t !!!
        // Time t
        dynamicDAG.getParentSetTimeT(D).addParent(A);
        dynamicDAG.getParentSetTimeT(D).addParent(B);
        dynamicDAG.getParentSetTimeT(D).addParent(C);

        dynamicDAG.getParentSetTimeT(E).addParent(B);
        dynamicDAG.getParentSetTimeT(E).addParent(C);

        dynamicDAG.getParentSetTimeT(G).addParent(C);

        dynamicDAG.getParentSetTimeT(A).addParent(A_TClone);


        DynamicBayesianNetwork amidstDBN = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);

        System.out.println(dynamicDAG.toString());

        //****************************************** Distributions *****************************************************

        // ********************************************************************************************
        // ************************************** TIME 0 **********************************************
        // ********************************************************************************************

        // Variable A
        Multinomial_MultinomialParents distA_Time0 = amidstDBN.getDistributionTime0(A);
        distA_Time0.getMultinomial(0).setProbabilities(new double[]{0.3, 0.7});

        // Variable B
        Multinomial_MultinomialParents distB_Time0 = amidstDBN.getDistributionTime0(B);
        distB_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.1, 0.5});

        // Variable C
        Multinomial_MultinomialParents distC_Time0 = amidstDBN.getDistributionTime0(C);
        distC_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});

        // Variable D
        Multinomial_MultinomialParents distD_Time0 = amidstDBN.getDistributionTime0(D);
        distD_Time0.getMultinomial(0).setProbabilities(new double[]{0.1,0.9});
        distD_Time0.getMultinomial(1).setProbabilities(new double[]{0.8,0.2});
        distD_Time0.getMultinomial(2).setProbabilities(new double[]{0.4,0.6});
        distD_Time0.getMultinomial(3).setProbabilities(new double[]{0.3,0.7});
        distD_Time0.getMultinomial(4).setProbabilities(new double[]{0.5,0.5});
        distD_Time0.getMultinomial(5).setProbabilities(new double[]{0.2,0.8});
        distD_Time0.getMultinomial(6).setProbabilities(new double[]{0.9,0.1});
        distD_Time0.getMultinomial(7).setProbabilities(new double[]{0.3,0.7});
        distD_Time0.getMultinomial(8).setProbabilities(new double[]{0.4,0.6});
        distD_Time0.getMultinomial(9).setProbabilities(new double[]{0.9,0.1});
        distD_Time0.getMultinomial(10).setProbabilities(new double[]{0.6,0.4});
        distD_Time0.getMultinomial(11).setProbabilities(new double[]{0.2,0.8});

        // Variable E
        Multinomial_MultinomialParents distE_Time0 = amidstDBN.getDistributionTime0(E);
        distE_Time0.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
        distE_Time0.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});
        distE_Time0.getMultinomial(2).setProbabilities(new double[]{0.3, 0.7});
        distE_Time0.getMultinomial(3).setProbabilities(new double[]{0.6, 0.4});
        distE_Time0.getMultinomial(4).setProbabilities(new double[]{0.1, 0.9});
        distE_Time0.getMultinomial(5).setProbabilities(new double[]{0.8, 0.2});

        // Variable G
        Multinomial_MultinomialParents distG_Time0 = amidstDBN.getDistributionTime0(G);
        distG_Time0.getMultinomial(0).setProbabilities(new double[]{0.6, 0.4});
        distG_Time0.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        // ********************************************************************************************
        // ************************************** TIME T **********************************************
        // ********************************************************************************************

        // Variable A
        Multinomial_MultinomialParents distA_TimeT = amidstDBN.getDistributionTimeT(A);
        distA_TimeT.getMultinomial(0).setProbabilities(new double[]{0.2, 0.8});
        distA_TimeT.getMultinomial(1).setProbabilities(new double[]{0.8, 0.2});


        // Variable B
        Multinomial_MultinomialParents distB_TimeT = amidstDBN.getDistributionTimeT(B);
        distB_TimeT.getMultinomial(0).setProbabilities(new double[]{0.4, 0.1, 0.5});

        // Variable C
        Multinomial_MultinomialParents distC_TimeT = amidstDBN.getDistributionTimeT(C);
        distC_TimeT.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});

        // Variable D
        Multinomial_MultinomialParents distD_TimeT = amidstDBN.getDistributionTimeT(D);
        distD_TimeT.getMultinomial(0).setProbabilities(new double[]{0.1,0.9});
        distD_TimeT.getMultinomial(1).setProbabilities(new double[]{0.8,0.2});
        distD_TimeT.getMultinomial(2).setProbabilities(new double[]{0.4,0.6});
        distD_TimeT.getMultinomial(3).setProbabilities(new double[]{0.3,0.7});
        distD_TimeT.getMultinomial(4).setProbabilities(new double[]{0.5,0.5});
        distD_TimeT.getMultinomial(5).setProbabilities(new double[]{0.2,0.8});
        distD_TimeT.getMultinomial(6).setProbabilities(new double[]{0.9,0.1});
        distD_TimeT.getMultinomial(7).setProbabilities(new double[]{0.3,0.7});
        distD_TimeT.getMultinomial(8).setProbabilities(new double[]{0.4,0.6});
        distD_TimeT.getMultinomial(9).setProbabilities(new double[]{0.9,0.1});
        distD_TimeT.getMultinomial(10).setProbabilities(new double[]{0.6,0.4});
        distD_TimeT.getMultinomial(11).setProbabilities(new double[]{0.2,0.8});

        // Variable E
        Multinomial_MultinomialParents distE_TimeT = amidstDBN.getDistributionTimeT(E);
        distE_TimeT.getMultinomial(0).setProbabilities(new double[]{0.4, 0.6});
        distE_TimeT.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});
        distE_TimeT.getMultinomial(2).setProbabilities(new double[]{0.3, 0.7});
        distE_TimeT.getMultinomial(3).setProbabilities(new double[]{0.6, 0.4});
        distE_TimeT.getMultinomial(4).setProbabilities(new double[]{0.1, 0.9});
        distE_TimeT.getMultinomial(5).setProbabilities(new double[]{0.8, 0.2});

        // Variable G
        Multinomial_MultinomialParents distG_TimeT = amidstDBN.getDistributionTimeT(G);
        distG_TimeT.getMultinomial(0).setProbabilities(new double[]{0.6, 0.4});
        distG_TimeT.getMultinomial(1).setProbabilities(new double[]{0.1, 0.9});

        return (amidstDBN);
    }
}
