package eu.amidst.huginlink.inference;

import eu.amidst.corestatic.distribution.Multinomial;
import eu.amidst.corestatic.distribution.Normal;
import eu.amidst.corestatic.inference.InferenceEngineForBN;
import eu.amidst.corestatic.inference.messagepassing.VMP;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.io.BayesianNetworkLoader;
import eu.amidst.corestatic.variables.HashMapAssignment;
import eu.amidst.corestatic.variables.StaticVariables;
import eu.amidst.corestatic.variables.Variable;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * Created by Hanen on 19/02/15.
 */
public class VMPHuginTest extends TestCase {

    public static void testMultinomialBN() throws IOException, ClassNotFoundException{

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/asia.bn");

        System.out.println(bn.toString());

        StaticVariables variables = bn.getDAG().getStaticVariables();

        Variable varX = variables.getVariableByName("X");
        Variable varB = variables.getVariableByName("B");
        Variable varD = variables.getVariableByName("D");
        Variable varA = variables.getVariableByName("A");
        Variable varS = variables.getVariableByName("S");
        Variable varL = variables.getVariableByName("L");
        Variable varT = variables.getVariableByName("T");
        Variable varE = variables.getVariableByName("E");

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varX, 0.0);
        assignment.setValue(varB, 0.0);
        assignment.setValue(varD, 0.0);
        assignment.setValue(varA, 0.0);
        assignment.setValue(varS, 0.0);
        assignment.setValue(varT, 0.0);
        assignment.setValue(varE, 0.0);

        InferenceEngineForBN.setEvidence(assignment);

        InferenceEngineForBN.runInference();

        Multinomial postL = ((Multinomial)InferenceEngineForBN.getPosterior(varL));
        System.out.println("Prob of having lung cancer P(L) = " + postL.toString());

        //test with Hugin inference
        HuginInferenceForBN inferenceHuginForBN = new HuginInferenceForBN();
        inferenceHuginForBN.setModel(bn);
        inferenceHuginForBN.setEvidence(assignment);
        inferenceHuginForBN.runInference();

        Multinomial postHuginL = ((Multinomial)inferenceHuginForBN.getPosterior(varL));
        System.out.println("Prob of having lung cancer P(L) = " + postHuginL.toString());

        assertTrue(postL.equalDist(postHuginL, 0.01));
    }


    public static void testMultinomialNormalBN() throws IOException, ClassNotFoundException{

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");

        System.out.println(bn.toString());

        StaticVariables variables = bn.getDAG().getStaticVariables();

        Variable varB = variables.getVariableByName("B");
        Variable varF = variables.getVariableByName("F");
        Variable varW = variables.getVariableByName("W");
        Variable varE = variables.getVariableByName("E");
        Variable varD = variables.getVariableByName("D");
        Variable varC = variables.getVariableByName("C");
        Variable varL = variables.getVariableByName("L");
        Variable varMin = variables.getVariableByName("Min");
        Variable varMout = variables.getVariableByName("Mout");

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varB, 1.0);
        assignment.setValue(varF, 0.0);
        assignment.setValue(varW, 1.0);
        assignment.setValue(varE, -3.0);
        assignment.setValue(varD, 1.5);
        assignment.setValue(varC, -1.0);
        assignment.setValue(varL, 1.0);
        assignment.setValue(varMin, -0.5);

        InferenceEngineForBN.setEvidence(assignment);

        InferenceEngineForBN.runInference();

        Normal postMout = ((Normal)InferenceEngineForBN.getPosterior(varMout));
        System.out.println("Prob of metals emission P(Mout) = " + postMout.toString());

        //test with Hugin inference
        HuginInferenceForBN inferenceHuginForBN = new HuginInferenceForBN();
        inferenceHuginForBN.setModel(bn);
        inferenceHuginForBN.setEvidence(assignment);
        inferenceHuginForBN.runInference();

        Normal postHuginMout = ((Normal)inferenceHuginForBN.getPosterior(varMout));
        System.out.println("Prob of metals emission P(Mout) = " + postHuginMout.toString());

        assertTrue(postMout.equalDist(postHuginMout, 0.01));
    }

}
