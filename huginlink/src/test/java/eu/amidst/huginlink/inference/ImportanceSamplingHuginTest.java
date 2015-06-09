package eu.amidst.huginlink.inference;
import eu.amidst.corestatic.distribution.*;
import eu.amidst.corestatic.inference.ImportanceSampling;
import eu.amidst.corestatic.io.BayesianNetworkLoader;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.variables.HashMapAssignment;
import eu.amidst.corestatic.variables.StaticVariables;
import eu.amidst.corestatic.variables.Variable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import static org.junit.Assert.assertTrue;

/**
 * Created by afa on 1/3/15.
 * Test to check the Importance Sampling algorithm against Hugin inference engine (without evidences!!)
 */
public class ImportanceSamplingHuginTest {

    @Before
    public void setUp() {

    }

    @Test
    public void test() throws IOException, ClassNotFoundException {

        BayesianNetwork model = BayesianNetworkLoader.loadFromFile("networks/IS.bn");

        //**************************************************************************************************************
        // MODEL DISTRIBUTIONS
        //**************************************************************************************************************
        //System.out.println("MODEL DISTRIBUTIONS");
        //model.getConditionalDistributions().stream().forEach(e-> {
        //     System.out.println(e.getVariable().getName());
        //     System.out.println(e);
        //});

        StaticVariables variables = model.getStaticVariables();
        Variable varA = variables.getVariableByName("A");
        Variable varB = variables.getVariableByName("B");
        Variable varC = variables.getVariableByName("C");
        Variable varD = variables.getVariableByName("D");
        Variable varE = variables.getVariableByName("E");

        HashMapAssignment evidence = new HashMapAssignment(0);
        //evidence.setValue(varA,1.0);

        //**************************************************************************************************************
        // HUGIN INFERENCE
        //**************************************************************************************************************

        HuginInferenceForBN huginInferenceForBN = new HuginInferenceForBN();
        huginInferenceForBN.setModel(model);
        huginInferenceForBN.setEvidence(evidence);
        huginInferenceForBN.runInference();

        //**************************************************************************************************************
        // IMPORTANCE SAMPLING INFERENCE
        //**************************************************************************************************************

        BayesianNetwork samplingModel = ImportanceSamplingHuginTest.getNoisyModel();
        //System.out.println("  SAMPLING DISTRIBUTIONS (MODEL DISTRIBUTIONS WITH NOISE) ");
        //samplingModel.getConditionalDistributions().stream().forEach(e-> {
        //    System.out.println(e.getVariable().getName());
        //    System.out.println(e);
        //});
        ImportanceSampling IS = new ImportanceSampling();
        IS.setModel(model);
        IS.setSamplingModel(samplingModel);
        IS.setSampleSize(200000);
        IS.setEvidence(evidence);
        IS.setParallelMode(true);

        //**************************************************************************************************************

        double threshold = 0.005;

        /* runInference() method must be called each time we compute a posterior because the Stream of Weighted
           Assignments is closed (reduced) in method getPosterior(var).*/
        IS.runInference(); assertTrue(IS.getPosterior(varA).equalDist(huginInferenceForBN.getPosterior(varA),threshold));
        IS.runInference(); assertTrue(IS.getPosterior(varB).equalDist(huginInferenceForBN.getPosterior(varB),threshold));
        IS.runInference(); assertTrue(IS.getPosterior(varC).equalDist(huginInferenceForBN.getPosterior(varC),threshold));
        IS.runInference(); assertTrue(IS.getPosterior(varD).equalDist(huginInferenceForBN.getPosterior(varD),threshold));
        IS.runInference(); assertTrue(IS.getPosterior(varE).equalDist(huginInferenceForBN.getPosterior(varE),threshold));

        }

    private static BayesianNetwork getNoisyModel() throws IOException, ClassNotFoundException {

        BayesianNetwork samplingBN = BayesianNetworkLoader.loadFromFile("networks/IS.bn");
        StaticVariables variables = samplingBN.getStaticVariables();
        Variable A = variables.getVariableByName("A");
        Variable B = variables.getVariableByName("B");
        Variable C = variables.getVariableByName("C");
        Variable D = variables.getVariableByName("D");
        Variable E = variables.getVariableByName("E");

        // Variable A
        Multinomial distA = samplingBN.getConditionalDistribution(A);
        distA.setProbabilities(new double[]{0.15, 0.85});

        // Variable B
        Multinomial_MultinomialParents distB = samplingBN.getConditionalDistribution(B);
        distB.getMultinomial(0).setProbabilities(new double[]{0.15,0.85});
        distB.getMultinomial(1).setProbabilities(new double[]{0.75,0.25});

        // Variable C
        Normal_MultinomialParents distC = samplingBN.getConditionalDistribution(C);
        distC.getNormal(0).setMean(3.1);
        distC.getNormal(0).setVariance(0.93320508059375);
        distC.getNormal(1).setMean(2.1);
        distC.getNormal(1).setVariance(0.720262834489);

        //Variable D
        Normal_MultinomialNormalParents distD = samplingBN.getConditionalDistribution(D);
        distD.getNormal_NormalParentsDistribution(0).setIntercept(2.1);
        //distD.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{2.1});
        distD.getNormal_NormalParentsDistribution(0).setCoeffForParent(C, 2.1);
        distD.getNormal_NormalParentsDistribution(0).setVariance(1.21);

        distD.getNormal_NormalParentsDistribution(1).setIntercept(0.6);
        //distD.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.6});
        distD.getNormal_NormalParentsDistribution(1).setCoeffForParent(C, 1.6);
        distD.getNormal_NormalParentsDistribution(1).setVariance(2.29280164);

        //Variable E
        ConditionalLinearGaussian distE  = samplingBN.getConditionalDistribution(E);
        distE.setIntercept(2.4);
        //distE.setCoeffParents(new double[]{4.1});
        distE.setCoeffForParent(C, 4.1);
        distE.setVariance(1.64660224);

        return(samplingBN);
    }

}