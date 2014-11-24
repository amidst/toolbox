package eu.amidst.core.huginlink;

import COM.hugin.HAPI.*;

import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.distribution.*;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by afa on 18/11/14.
 */
public class ConverterToHuginTest {

    private BayesianNetwork amidstBN;
    private Domain huginBN;

    public static BayesianNetwork getAmidstBayesianNetworkExample(){

        //**************************************** Synthetic data ******************************************************

        WekaDataFileReader fileReader = new WekaDataFileReader(new String("datasets/syntheticData.arff"));
        StaticVariables modelHeader = new StaticVariables(fileReader.getAttributes());

        //***************************************** Network structure **************************************************
        //Create the structure by hand

        DAG dag = new DAG(modelHeader);
        List<Variable> variables = dag.getStaticVariables().getListOfVariables();

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
        Multinomial_MultinomialParents distA = bn.getDistribution(A);
        distA.getMultinomial(0).setProbabilities(new double[]{0.3,0.7});

        // Variable B
        Multinomial_MultinomialParents distB = bn.getDistribution(B);
        distB.getMultinomial(0).setProbabilities(new double[]{0.4,0.1,0.5});

        // Variable C
        Normal_MultinomialParents distC = bn.getDistribution(C);
        distC.getNormal(0).setMean(0.8);
        distC.getNormal(0).setSd(1.5);

        // Variable D
        Normal_MultinomialParents distD = bn.getDistribution(D);
        distD.getNormal(0).setMean(1.3);
        distD.getNormal(0).setSd(0.9);

        // Variable E
        Multinomial_MultinomialParents distE=bn.getDistribution(E);
        distE.getMultinomial(0).setProbabilities(new double[]{0.2,0.8});
        distE.getMultinomial(1).setProbabilities(new double[]{0.1,0.9});
        distE.getMultinomial(2).setProbabilities(new double[]{0.8,0.2});
        distE.getMultinomial(3).setProbabilities(new double[]{0.45,0.55});
        distE.getMultinomial(4).setProbabilities(new double[]{0.35,0.65});
        distE.getMultinomial(5).setProbabilities(new double[]{0.9,0.1});

        // Variable H
        Normal_MultinomialParents distH = bn.getDistribution(H);
        distH.getNormal(0).setMean(2);
        distH.getNormal(0).setSd(1.5);
        distH.getNormal(1).setMean(-1);
        distH.getNormal(1).setSd(0.5);
        distH.getNormal(2).setMean(3);
        distH.getNormal(2).setSd(0.8);
        distH.getNormal(3).setMean(2);
        distH.getNormal(3).setSd(1);
        distH.getNormal(4).setMean(5);
        distH.getNormal(4).setSd(0.8);
        distH.getNormal(5).setMean(1.5);
        distH.getNormal(5).setSd(0.7);

        //Variable I
        Normal_MultinomialNormalParents distI = bn.getDistribution(I);
        distI.getNormal_NormalParentsDistribution(0).setIntercept(0.5);
        distI.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{0.25,0.4});
        distI.getNormal_NormalParentsDistribution(0).setSd(0.9);

        distI.getNormal_NormalParentsDistribution(1).setIntercept(-0.1);
        distI.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{-0.5,0.2});
        distI.getNormal_NormalParentsDistribution(1).setSd(0.6);

        distI.getNormal_NormalParentsDistribution(2).setIntercept(2.1);
        distI.getNormal_NormalParentsDistribution(2).setCoeffParents(new double[]{1.2,-0.3});
        distI.getNormal_NormalParentsDistribution(2).setSd(1.1);

        distI.getNormal_NormalParentsDistribution(3).setIntercept(2.1);
        distI.getNormal_NormalParentsDistribution(3).setCoeffParents(new double[]{1.25,0.9});
        distI.getNormal_NormalParentsDistribution(3).setSd(0.95);

        distI.getNormal_NormalParentsDistribution(4).setIntercept(1.5);
        distI.getNormal_NormalParentsDistribution(4).setCoeffParents(new double[]{-0.41,0.5});
        distI.getNormal_NormalParentsDistribution(4).setSd(1.5);

        distI.getNormal_NormalParentsDistribution(5).setIntercept(0);
        distI.getNormal_NormalParentsDistribution(5).setCoeffParents(new double[]{0.0,0.3});
        distI.getNormal_NormalParentsDistribution(5).setSd(0.25);

        //Variable G
        Normal_NormalParents distG  = bn.getDistribution(G);
        distG.setIntercept(0.7);
        distG.setCoeffParents(new double[]{0.3,-0.8});
        distG.setSd(0.9);


        return bn;


    }

    @Before
    public void setUp() throws ExceptionHugin {

        //AMIDST Bayesian network built by hand. Update the attribute amidstBN used next for the tests.
        this.amidstBN = getAmidstBayesianNetworkExample();

        //Conversion from AMIDST network into a Hugin network.
        System.out.println("\n\nConverting the AMIDST network into Hugin format ...");
        ConverterToHugin converter = new ConverterToHugin(this.amidstBN);
        converter.convertToHuginBN();
        String outFile = new String("networks/huginNetworkFromAMIDST.net");
        converter.getHuginNetwork().saveAsNet(new String(outFile));
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");

        //Update the attribute huginBN used next for the tests.
        this.huginBN = converter.getHuginNetwork();



        String netName = new String("networks/huginNetworkFromAMIDST");

        ParseListener parseListener2 = new DefaultClassParseListener();
        this.huginBN = new Domain (netName + ".net", parseListener2);
        System.out.println("\n\nConverting the Hugin network into AMIDST format ...");
        ConverterToAmidst converter2 = new ConverterToAmidst(this.huginBN);
        converter2.convertToAmidstBN();
        this.amidstBN = converter2.getAmidstNetwork();
        System.out.println("\nAMIDST network object created.");

/*

        System.out.println("\n\nConverting the AMIDST network into Hugin format ...");
        ConverterToHugin converter3 = new ConverterToHugin(this.amidstBN);
        converter3.convertToHuginBN();
        String outFile2 = new String("networks/huginNetworkFromAMIDST2.net");
        converter3.getHuginNetwork().saveAsNet(new String(outFile2));
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");

*/
    }

    @Test
    public void testHuginAndAmidstModels() throws ExceptionHugin {

        this.testNumberOfVariables();

        int numVars = amidstBN.getNumberOfVars();

        for (int i = 0; i < numVars; i++) {

            Variable amidstVar = amidstBN.getListOfVariables().get(i);
            Node huginVar = (Node) huginBN.getNodes().get(i);

            this.testName(amidstVar,huginVar);
            this.testVariableType(amidstVar,huginVar);
            this.testParents(amidstVar, huginVar);
            this.testConditionalDistribution(amidstVar,huginVar);
        }
    }

    private void testNumberOfVariables() throws ExceptionHugin {
        assertEquals(amidstBN.getNumberOfVars(), huginBN.getNodes().size());
    }

    private void testName(Variable amidstVar, Node huginVar) throws ExceptionHugin {
        assertEquals(amidstVar.getName(), huginVar.getName());
    }

    private void testVariableType(Variable amidstVar, Node huginVar) throws ExceptionHugin {
        boolean amidstMultinomialVar = amidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0;
        boolean amidstNormalVar = amidstVar.getDistributionType().compareTo(DistType.GAUSSIAN) == 0;
        boolean huginMultinomialVar = huginVar.getKind().compareTo(NetworkModel.H_KIND_DISCRETE) == 0;
        boolean huginNormalVar = huginVar.getKind().compareTo(NetworkModel.H_KIND_CONTINUOUS) == 0;

        assertEquals(amidstMultinomialVar, huginMultinomialVar);
        assertEquals(amidstNormalVar, huginNormalVar);
    }

    private void testParents(Variable amidstVar, Node huginVar) throws ExceptionHugin {
        ParentSet parentsAmidstVar = amidstBN.getDAG().getParentSet(amidstVar);
        NodeList parentsHuginVar = huginVar.getParents();
        int numParentsAmidstVar = parentsAmidstVar.getParents().size();
        int numParentsHuginVar = parentsHuginVar.size();

        // Number of parents
        assertEquals(numParentsAmidstVar, numParentsHuginVar);

        // IMPORTANT: Hugin stores the MULTINOMIAL parents in a reverse order wrt. AMIDST whilst the Normal parents
        // are stored in the same order.
        // System.out.println("\n HUGIN: "+ huginVar.getName() + " - " +huginVar.getParents().toString());
        //System.out.println("AMIDST: "+ amidstVar.getName() + " - " +parentsAmidstVar.toString());

        ArrayList<String> namesAmidstParents = new ArrayList<>();
        ArrayList<String> namesHuginParents = new ArrayList<>();

        for (int j = 0; j < numParentsAmidstVar; j++) {

            Variable parentAmidstVar = parentsAmidstVar.getParents().get(j);

            String parentNameHuginVar = ((Node) parentsHuginVar.get(j)).getName();
            String parentNameAmidstVar = parentAmidstVar.getName();

            if (parentAmidstVar.getDistributionType().compareTo(DistType.GAUSSIAN) == 0) {
                assertEquals(parentNameAmidstVar, parentNameHuginVar);
            } else if (parentAmidstVar.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                namesAmidstParents.add(parentNameAmidstVar);
                namesHuginParents.add(parentNameHuginVar);
            } else {
                throw new IllegalArgumentException("Unrecognized DistributionType.");
            }
        }
        Collections.reverse(namesHuginParents);
        assertEquals(namesAmidstParents, namesHuginParents);
    }

    private void testConditionalDistribution(Variable amidstVar, Node huginVar) throws ExceptionHugin {
        int type = Utils.getConditionalDistributionType(amidstVar, amidstBN);

        System.out.println("Testing distribution for " +amidstVar.getName());
        switch (type) {
            case 0:
                this.testMultinomial_MultinomialParents(huginVar, amidstVar);
                break;
            case 1:
                Normal_NormalParents dist1 = amidstBN.getDistribution(amidstVar);
                this.testNormal_NormalParents(huginVar, dist1, 0);
                break;
            case 2:
                this.testNormal_MultinomialParents (huginVar, amidstVar);
                break;
            case 3:
                this.testNormal_MultinomialNormalParents(huginVar, amidstVar);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized DistributionType. ");
        }
    }

    private void testMultinomial_MultinomialParents(Node huginVar, Variable amidstVar) throws ExceptionHugin{
        Multinomial_MultinomialParents dist0 = amidstBN.getDistribution(amidstVar);
        double[] huginProbabilities = huginVar.getTable().getData();
        Multinomial[] probabilities = dist0.getProbabilities();

        int nStates = amidstVar.getNumberOfStates();
        int numParentAssignments =
                MultinomialIndex.getNumberOfPossibleAssignments(dist0.getConditioningVariables());

        System.out.println(numParentAssignments);

        for (int j = 0; j < numParentAssignments; j++) {
            double[] amidstProbabilitiesAssignment_j = probabilities[j].getProbabilities();
            for (int k = 0; k < nStates; k++) {
                // Probability of the state k for the j assignment of the parents
                assertEquals(amidstProbabilitiesAssignment_j[k], huginProbabilities[j * nStates + k], 0.0);
            }
        }

    }

    private void testNormal_NormalParents(Node huginVar, Normal_NormalParents dist1, int assign_j) throws ExceptionHugin {


        // Intercept
        double interceptHugin = ((ContinuousChanceNode)huginVar).getAlpha(assign_j);
        double interceptAmidst = dist1.getIntercept();
        assertEquals(interceptHugin,interceptAmidst ,0.0);

        // Parents coefficients
        List<Variable> normalParents = dist1.getConditioningVariables();
        int numNormalParents = normalParents.size();
        for(int i=0;i<numNormalParents;i++) {
            ContinuousChanceNode huginParent =
                    (ContinuousChanceNode)this.huginBN.getNodeByName(normalParents.get(i).getName());

            double coeff_iHugin = ((ContinuousChanceNode)huginVar).getBeta(huginParent,assign_j);
            double coeff_iAmidst = dist1.getCoeffParents()[i];
            assertEquals(coeff_iHugin,coeff_iAmidst,0);
        }

        // Variance
        double varianceAmidst = Math.pow(dist1.getSd(),2);
        double varianceHugin = ((ContinuousChanceNode)huginVar).getGamma(assign_j);
        assertEquals(varianceAmidst,varianceHugin ,0.0);
    }

    private void testNormal_MultinomialParents (Node huginVar, Variable amidstVar) throws ExceptionHugin  {

        Normal_MultinomialParents dist2 = amidstBN.getDistribution(amidstVar);
        List<Variable> conditioningVariables = dist2.getConditioningVariables();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(conditioningVariables);

        for(int j=0;j<numParentAssignments;j++) {
            Normal normal =  dist2.getNormal(j);
            double mean_jAmidst = normal.getMean();
            double mean_jHugin = ((ContinuousChanceNode)huginVar).getAlpha(j);
            assertEquals(mean_jAmidst, mean_jHugin,0);

            double variance_jAmidst = Math.pow(normal.getSd(),2);
            double variance_jHugin = ((ContinuousChanceNode)huginVar).getGamma(j);
            assertEquals(variance_jAmidst, variance_jHugin,0.000001);
        }
    }

    private void testNormal_MultinomialNormalParents(Node huginVar, Variable amidstVar)throws ExceptionHugin {

        Normal_MultinomialNormalParents dist3 = amidstBN.getDistribution(amidstVar);

        List<Variable> multinomialParents = dist3.getMultinomialParents();
        int numParentAssignments = MultinomialIndex.getNumberOfPossibleAssignments(multinomialParents);

        for(int j=0;j <numParentAssignments;j++) {
            Normal_NormalParents dist1 = dist3.getNormal_NormalParentsDistribution(j);
            this.testNormal_NormalParents(huginVar, dist1 ,j);
        }
    }
}