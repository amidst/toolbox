package eu.amidst.core.huginlink;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.distribution.*;
import eu.amidst.core.header.StaticModelHeader;
import eu.amidst.core.header.Variable;
import eu.amidst.core.modelstructure.BayesianNetwork;
import eu.amidst.core.modelstructure.DAG;
import java.util.List;

/**
 * Created by afa on 18/11/14.
 */
public class ConverterToHuginTest {


    public static void main(String args[]) throws ExceptionHugin {

        //**************************************** Synthetic data ******************************************************

        WekaDataFileReader fileReader = new WekaDataFileReader(new String("datasets/syntheticData.arff"));
        StaticModelHeader modelHeader = new StaticModelHeader(fileReader.getAttributes());

        //***************************************** Network structure **************************************************
        //Create the structure by hand

        DAG dag = new DAG(modelHeader);
        List<Variable> variables = dag.getModelHeader().getVariables();

        System.out.print("\nVariables: ");
        for(Variable v: variables){
            System.out.print(v.getName()+ " ");
        }

        Variable A, B, C, D, E, G, H, I;

        A = variables.get(0);
        B = variables.get(1);
        C = variables.get(2);
        D = variables.get(3);
        E = variables.get(4);
        G = variables.get(5);
        H = variables.get(6);
        I = variables.get(7);

        //Example

        dag.getParentSet(E).addParent(A);
        dag.getParentSet(E).addParent(B);

        dag.getParentSet(H).addParent(A);
        dag.getParentSet(H).addParent(B);

        dag.getParentSet(I).addParent(A);
        dag.getParentSet(I).addParent(B);
        dag.getParentSet(I).addParent(C);
        dag.getParentSet(I).addParent(D);

        dag.getParentSet(G).addParent(C);
        dag.getParentSet(G).addParent(D);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

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

        

        // Variable A
        ((Multinomial_MultinomialParents)bn.getDistribution(A)).getMultinomial(0).setProbabilities(new double[]{0.3,0.7});

        // Variable B
        ((Multinomial_MultinomialParents)bn.getDistribution(B)).getMultinomial(0).setProbabilities(new double[]{0.4,0.1,0.5});

        // Variable C
        ((Normal_MultinomialParents)bn.getDistribution(C)).getNormal(0).setMean(0.8);
        ((Normal_MultinomialParents)bn.getDistribution(C)).getNormal(0).setSd(1.5);

        // Variable D
        ((Normal_MultinomialParents)bn.getDistribution(D)).getNormal(0).setMean(1.3);
        ((Normal_MultinomialParents)bn.getDistribution(D)).getNormal(0).setSd(0.9);

        // Variable E
        ((Multinomial_MultinomialParents)bn.getDistribution(E)).getMultinomial(0).setProbabilities(new double[]{0.2,0.8});
        ((Multinomial_MultinomialParents)bn.getDistribution(E)).getMultinomial(1).setProbabilities(new double[]{0.1,0.9});
        ((Multinomial_MultinomialParents)bn.getDistribution(E)).getMultinomial(2).setProbabilities(new double[]{0.8,0.2});
        ((Multinomial_MultinomialParents)bn.getDistribution(E)).getMultinomial(3).setProbabilities(new double[]{0.45,0.55});
        ((Multinomial_MultinomialParents)bn.getDistribution(E)).getMultinomial(4).setProbabilities(new double[]{0.35,0.65});
        ((Multinomial_MultinomialParents)bn.getDistribution(E)).getMultinomial(5).setProbabilities(new double[]{0.9,0.1});

        // Variable H
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(0).setMean(2);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(0).setSd(1.5);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(1).setMean(-1);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(1).setSd(0.5);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(2).setMean(3);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(2).setSd(0.8);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(3).setMean(2);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(3).setSd(1);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(4).setMean(5);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(4).setSd(0.8);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(5).setMean(1.5);
        ((Normal_MultinomialParents)bn.getDistribution(H)).getNormal(5).setSd(0.7);

        //Variable I
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(0).setIntercept(0.5);
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.25,0.4});
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(0).setSd(0.9);

        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(1).setIntercept(-0.1);
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{-0.5,0.2});
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(1).setSd(0.6);

        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(2).setIntercept(2.1);
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(2).setCoeffParents(new double[]{1.2,-0.3});
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(2).setSd(1.1);

        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(3).setIntercept(2.1);
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(3).setCoeffParents(new double[]{1.25,0.9});
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(3).setSd(0.95);

        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(4).setIntercept(1.5);
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(4).setCoeffParents(new double[]{-0.41,0.5});
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(4).setSd(1.5);

        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(5).setIntercept(0);
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(5).setCoeffParents(new double[]{0.0,0.3});
        ((Normal_MultinomialNormalParents)bn.getDistribution(I)).getNormal_NormalParentsDistribution(5).setSd(0.25);

        //Variable G
        ((Normal_NormalParents)bn.getDistribution(G)).setIntercept(0.7);
        ((Normal_NormalParents)bn.getDistribution(G)).setCoeffParents(new double[]{0.3,-0.8});
        ((Normal_NormalParents)bn.getDistribution(G)).setSd(0.9);

        //**************************************************************************************************************


        System.out.println("\n\nConverting the AMIDST network into Hugin format ...");
        ConverterToHugin converter = new ConverterToHugin();
        converter.setBayesianNetwork(bn);


        String outFile = new String("networks/huginNetworkFromAMIDST.net");
        converter.getHuginNetwork().saveAsNet(new String(outFile));
        System.out.println("Hugin network saved in \"" + outFile + "\""+".");

    }
}
