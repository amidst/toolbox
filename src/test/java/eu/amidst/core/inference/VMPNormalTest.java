package eu.amidst.core.inference;

//import cern.jet.random.Normal;
import com.google.common.base.Stopwatch;
import eu.amidst.core.distribution.*;
import eu.amidst.core.exponentialfamily.EF_Multinomial;
import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 10/02/15.
 */
public class VMPNormalTest extends TestCase {



/*
    public static void test1() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenGaussianVariable("A");
        Variable varB = variables.addHiddenGaussianVariable("B");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal_MultinomialParents distA = bn.getDistribution(varA);
        Normal_NormalParents distB = bn.getDistribution(varB);


        distA.getNormal(0).setMean(0);
        distA.getNormal(0).setSd(1);
        distB.setIntercept(1);
        distB.setSd(1);
        distB.setCoeffParents(new double[]{1});


        System.out.println(bn.toString());


        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.nodes.get(0).getQDist());
        EF_Normal qBDist = ((EF_Normal) vmp.nodes.get(1).getQDist());


        double[] qA = new double[2];
        qA[0] = qADist.getMomentParameters().get(0);
        qA[1] = qADist.getMomentParameters().get(1);

        double[] qB = new double[2];
        qB[0] = qBDist.getMomentParameters().get(0);
        qB[1] = qBDist.getMomentParameters().get(1);


        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        Normal postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = InferenceEngineForBN.getPosterior(varB);
        System.out.println("P(B) = " + postB.toString());


    }*/


    /*
    public static void test2() {
        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenGaussianVariable("A");
        Variable varB = variables.addHiddenGaussianVariable("B");
        Variable varC = variables.addHiddenGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal_MultinomialParents distA = bn.getDistribution(varA);
        Normal_MultinomialParents distB = bn.getDistribution(varB);
        Normal_NormalParents distC = bn.getDistribution(varC);


        distA.getNormal(0).setMean(0);
        distA.getNormal(0).setSd(1);
        distB.getNormal(0).setMean(0);
        distB.getNormal(0).setSd(1);
        distC.setIntercept(1);
        distC.setSd(1);
        distC.setCoeffParents(new double[]{1, 1});


        System.out.println(bn.toString());


        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);


        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        Normal postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = InferenceEngineForBN.getPosterior(varB);
        System.out.println("P(B) = " + postB.toString());
        Normal postC = InferenceEngineForBN.getPosterior(varC);
        System.out.println("P(C) = " + postC.toString());
    }
    */
    /*
    public static void test3() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenGaussianVariable("B");
        Variable varC = variables.addHiddenGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Normal_MultinomialParents distB = bn.getDistribution(varB);
        Normal_MultinomialNormalParents distC = bn.getDistribution(varC);

        distA.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});
        distB.getNormal(0).setMean(0);
        distB.getNormal(0).setSd(1);
        distC.getNormal_NormalParentsDistribution(0).setIntercept(1);
        distC.getNormal_NormalParentsDistribution(0).setSd(1);
        distC.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[] {1});
        distC.getNormal_NormalParentsDistribution(0).setIntercept(2);
        distC.getNormal_NormalParentsDistribution(0).setSd(1);
        distC.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[] {2}); //Does it have to be 1 too?

        //bn.randomInitialization(new Random(0));

        double[] pA = distA.getMultinomial(0).getProbabilities();
        double[][] pB = new double[2][];

        System.out.println(bn.toString());


        //HashMapAssignment assignment = new HashMapAssignment(1);
        //assignment.setValue(varA, 0.0);
        //assignment.setValue(varB, 1.0);
        //assignment.setValue(varC, 1.0);

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        EF_Multinomial qADist = ((EF_Multinomial) vmp.nodes.get(0).getQDist());
        EF_Normal qBDist = ((EF_Normal) vmp.nodes.get(1).getQDist());
        EF_Normal qCDist = ((EF_Normal) vmp.nodes.get(2).getQDist());


        double[] qA = new double[2];
        qA[0] = qADist.getMomentParameters().get(0);
        qA[1] = qADist.getMomentParameters().get(1);

        double[] qB = new double[2];
        qB[0] = qBDist.getMomentParameters().get(0);
        qB[1] = qBDist.getMomentParameters().get(1);

        double[] qC = new double[2];
        qC[0] = qCDist.getMomentParameters().get(0);
        qC[1] = qCDist.getMomentParameters().get(1);

        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        Multinomial postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        eu.amidst.core.distribution.Normal postB = InferenceEngineForBN.getPosterior(varB);
        System.out.println("P(B) = " + postB.toString());
        eu.amidst.core.distribution.Normal postC = InferenceEngineForBN.getPosterior(varC);
        System.out.println("P(B) = " + postC.toString());


        boolean convergence = false;
        double oldvalue = 0;
        while (!convergence) {

            qA[0] = Math.exp(qB[0] * Math.log(pB[0][0]) + qB[1] * Math.log(pB[0][1]) + Math.log(pA[0]));
            qA[1] = Math.exp(qB[0] * Math.log(pB[1][0]) + qB[1] * Math.log(pB[1][1]) + Math.log(pA[1]));

            Utils.normalize(qA);

            qB[0] = Math.exp(qA[0] * Math.log(pB[0][0] * pA[0]) + qA[1] * Math.log(pB[1][0] * pA[1]));
            qB[1] = Math.exp(qA[0] * Math.log(pB[0][1] * pA[0]) + qA[1] * Math.log(pB[1][1] * pA[1]));

            Utils.normalize(qB);

            if (Math.abs(qA[0] + qB[0] - oldvalue) < 0.001) {
                convergence = true;
            }

            oldvalue = qA[0] + qB[0];


        }
        System.out.println(qA[0]);
        System.out.println(qB[0]);
        System.out.println(qC[0]);

        assertEquals(postA.getProbabilities()[0], qA[0], 0.01);
        assertEquals(postB.getProbability(1.0), qB[0], 0.01);
        assertEquals(postC.getProbability(1.0), qC[0], 0.01);

    }*/

    public static void test4() throws IOException, ClassNotFoundException{

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");
        for (int i = 0; i < 1; i++) {

            //bn.randomInitialization(new Random(i));
            //System.out.println(bn.toString());

            InferenceEngineForBN.setModel(bn);

            Stopwatch watch = Stopwatch.createStarted();
            InferenceEngineForBN.compileModel();
            System.out.println(watch.stop());

            bn.getStaticVariables().getListOfVariables().forEach( var -> System.out.println(var.getName()+": "+InferenceEngineForBN.getPosterior(bn.getStaticVariables().getVariableByName(var.getName())).toString()));
        }
    }
}
