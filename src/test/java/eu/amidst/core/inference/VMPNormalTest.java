package eu.amidst.core.inference;

//import cern.jet.random.Normal;
import com.google.common.base.Stopwatch;
import eu.amidst.core.distribution.*;
import eu.amidst.core.exponentialfamily.EF_Multinomial;
import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.exponentialfamily.EF_Normal_NormalParents;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
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


    public static void test1() throws IOException, ClassNotFoundException{

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");
        for (int i = 0; i < 1; i++) {

            //bn.randomInitialization(new Random(i));
            //System.out.println(bn.toString());

            InferenceEngineForBN.setModel(bn);

            Stopwatch watch = Stopwatch.createStarted();
            InferenceEngineForBN.runInference();
            System.out.println(watch.stop());

            bn.getStaticVariables().getListOfVariables().forEach( var -> System.out.println(var.getName()+": "+InferenceEngineForBN.getPosterior(bn.getStaticVariables().getVariableByName(var.getName())).toString()));
        }
    }


    //Test with a BN containing 2 Guassian variables A->B
    public static void test2() throws IOException, ClassNotFoundException{

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenGaussianVariable("A");
        Variable varB = variables.addHiddenGaussianVariable("B");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal_MultinomialParents distA = bn.getDistribution(varA);
        Normal_NormalParents distB = bn.getDistribution(varB);

        distA.getNormal(0).setMean(1);
        distA.getNormal(0).setSd(0.5);
        distB.setIntercept(1);
        distB.setCoeffParents(new double[]{1});
        distB.setSd(0.5);

        System.out.println(bn.toString());

        //bn.randomInitialization(new Random(0));

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varA, 1.0);

        double pMeanA =  distA.getNormal(0).getMean();
        double pSdA =  distA.getNormal(0).getSd();

        double pMeanB =  distB.getNormal(assignment).getMean();
        double pSdB =  distB.getNormal(assignment).getSd();

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
        InferenceEngineForBN.runInference();

        Normal postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = ((Normal)InferenceEngineForBN.getPosterior(varB));
        System.out.println("P(B) = " + postB.toString());


        boolean convergence = false;
        double oldvalue = 0;

        //to be finished
    }


    //Test with a BN containing 3 Guassian variables A->C<-B
    public static void test3() throws IOException, ClassNotFoundException{

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

        distA.getNormal(0).setMean(1);
        distA.getNormal(0).setSd(0.5);

        distB.getNormal(0).setMean(1.2);
        distB.getNormal(0).setSd(0.8);

        distC.setIntercept(1);
        distC.setCoeffParents(new double[]{1, 1});
        distC.setSd(0.5);

        System.out.println(bn.toString());

        //bn.randomInitialization(new Random(0));

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varA, 1.0);

        double pMeanA =  distA.getNormal(0).getMean();
        double pSdA =  distA.getNormal(0).getSd();

        double pMeanB =  distB.getNormal(0).getMean();
        double pSdB =  distB.getNormal(0).getSd();

        double pMeanC =  distC.getNormal(assignment).getMean();
        double pSdC =  distC.getNormal(assignment).getSd();

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.nodes.get(0).getQDist());
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
        InferenceEngineForBN.runInference();

        Normal postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = ((Normal)InferenceEngineForBN.getPosterior(varB));
        System.out.println("P(B) = " + postB.toString());
        Normal postC = ((Normal)InferenceEngineForBN.getPosterior(varB));
        System.out.println("P(C) = " + postC.toString());

        boolean convergence = false;
        double oldvalue = 0;

        //to be finished
    }


    //Test with a BN containing 3 Guassian variables such that C has two parents A->C<-B, and B has one parent A->B
    public static void test4() throws IOException, ClassNotFoundException{

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenGaussianVariable("A");
        Variable varB = variables.addHiddenGaussianVariable("B");
        Variable varC = variables.addHiddenGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);
        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal_MultinomialParents distA = bn.getDistribution(varA);
        Normal_NormalParents distB = bn.getDistribution(varB);
        Normal_NormalParents distC = bn.getDistribution(varC);

        distA.getNormal(0).setMean(1);
        distA.getNormal(0).setSd(0.5);

        distB.setIntercept(1.5);
        distB.setCoeffParents(new double[]{1});
        distB.setSd(0.8);

        distC.setIntercept(1);
        distC.setCoeffParents(new double[]{1, 1});
        distC.setSd(0.5);

        System.out.println(bn.toString());

        //bn.randomInitialization(new Random(0));

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varA, 1.0);

        double pMeanA =  distA.getNormal(0).getMean();
        double pSdA =  distA.getNormal(0).getSd();

        double pMeanB =  distB.getNormal(assignment).getMean();
        double pSdB =  distB.getNormal(assignment).getSd();

        double pMeanC =  distC.getNormal(assignment).getMean();
        double pSdC =  distC.getNormal(assignment).getSd();

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.nodes.get(0).getQDist());
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
        InferenceEngineForBN.runInference();

        Normal postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = ((Normal)InferenceEngineForBN.getPosterior(varB));
        System.out.println("P(B) = " + postB.toString());
        Normal postC = ((Normal)InferenceEngineForBN.getPosterior(varB));
        System.out.println("P(C) = " + postC.toString());

        boolean convergence = false;
        double oldvalue = 0;

        //to be finished
    }

    //Test with a BN containing 3 Guassian variables such that C has two children A and B: i.e., C->A and C->B
    public static void test5() throws IOException, ClassNotFoundException{

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenGaussianVariable("A");
        Variable varB = variables.addHiddenGaussianVariable("B");
        Variable varC = variables.addHiddenGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal_NormalParents distA = bn.getDistribution(varA);
        Normal_NormalParents distB = bn.getDistribution(varB);
        Normal_MultinomialParents distC = bn.getDistribution(varC);

        distA.setIntercept(1);
        distA.setCoeffParents(new double[]{1});
        distA.setSd(0.5);

        distB.setIntercept(1.5);
        distB.setCoeffParents(new double[]{1});
        distB.setSd(0.8);

        distC.getNormal(0).setMean(1);
        distC.getNormal(0).setSd(0.5);

        System.out.println(bn.toString());

        //bn.randomInitialization(new Random(0));

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varA, 1.0);

        double pMeanA =  distA.getNormal(assignment).getMean();
        double pSdA =  distA.getNormal(assignment).getSd();

        double pMeanB =  distB.getNormal(assignment).getMean();
        double pSdB =  distB.getNormal(assignment).getSd();

        double pMeanC =  distC.getNormal(0).getMean();
        double pSdC =  distC.getNormal(0).getSd();

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.nodes.get(0).getQDist());
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
        InferenceEngineForBN.runInference();

        Normal postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = ((Normal)InferenceEngineForBN.getPosterior(varB));
        System.out.println("P(B) = " + postB.toString());
        Normal postC = ((Normal)InferenceEngineForBN.getPosterior(varB));
        System.out.println("P(C) = " + postC.toString());

        boolean convergence = false;
        double oldvalue = 0;

        //to be finished
    }

}
