package eu.amidst.huginlink.inference;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.corestatic.distribution.Normal;
import eu.amidst.corestatic.inference.InferenceEngineForBN;
import eu.amidst.corestatic.inference.messagepassing.VMP;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.variables.HashMapAssignment;
import eu.amidst.corestatic.variables.StaticVariables;
import eu.amidst.corestatic.variables.Variable;

import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.io.BNLoaderFromHugin;
import org.junit.Test;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by afa on 10/3/15.
 */
public class HuginInferenceForBNTest {


    @Test
    public void test() throws IOException, ClassNotFoundException, ExceptionHugin {

        Domain huginBN = BNLoaderFromHugin.loadFromFile("./networks/Example.net");
        BayesianNetwork bn = BNConverterToAMIDST.convertToAmidst(huginBN);
        System.out.println(bn.toString());

        StaticVariables variables = bn.getDAG().getStaticVariables();

        Variable varA = variables.getVariableByName("A");
        Variable varB = variables.getVariableByName("B");
        Variable varC = variables.getVariableByName("C");
        Variable varD = variables.getVariableByName("D");
        Variable varE = variables.getVariableByName("E");

        HashMapAssignment assignment = new HashMapAssignment(4);
        assignment.setValue(varA, 1.25);
        assignment.setValue(varB, 0.0);
        assignment.setValue(varC, 3.8);
        //assignment.setValue(varD, 7.0);
        assignment.setValue(varE, 2.5);

        //**************************************************************************************************************
        // HUGIN
        //**************************************************************************************************************

        HuginInferenceForBN inferenceHuginForBN = new HuginInferenceForBN();
        inferenceHuginForBN.setModel(bn);
        inferenceHuginForBN.setEvidence(assignment);
        inferenceHuginForBN.runInference();

        //Multinomial postHuginA = ((Multinomial)inferenceHuginForBN.getPosterior(varA));
        //Multinomial postHuginB = ((Multinomial)inferenceHuginForBN.getPosterior(varB));
        //Normal postHuginC = ((Normal)inferenceHuginForBN.getPosterior(varC));
        Normal postHuginD = ((Normal)inferenceHuginForBN.getPosterior(varD));
        //Normal postHuginE = ((Normal)inferenceHuginForBN.getPosterior(varE));


        //**************************************************************************************************************
        // AMIDST - VMP
        //**************************************************************************************************************

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);
        InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.runInference();

        //Multinomial postVMP_A = ((Multinomial)InferenceEngineForBN.getPosterior(varA));
        //Multinomial postVMP_B = ((Multinomial)InferenceEngineForBN.getPosterior(varB));
        //Normal postVMP_C = ((Normal)InferenceEngineForBN.getPosterior(varC));
        Normal postVMP_D = ((Normal)InferenceEngineForBN.getPosterior(varD));
        //Normal postVMP_E = ((Normal)InferenceEngineForBN.getPosterior(varE));

        //**************************************************************************************************************
        // TESTS
        //**************************************************************************************************************

        //assertTrue(postHuginA.equalDist(postVMP_A, 0.01));
        //assertTrue(postHuginB.equalDist(postVMP_B,0.01));
        //assertTrue(postHuginC.equalDist(postVMP_C,0.01));
        assertTrue(postHuginD.equalDist(postVMP_D,0.0001));
        //assertTrue(postHuginE.equalDist(postVMP_E,0.01));

    }
}