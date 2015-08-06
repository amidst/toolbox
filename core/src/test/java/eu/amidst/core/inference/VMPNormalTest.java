package eu.amidst.core.inference;

//import cern.jet.random.Normal;
import com.google.common.base.Stopwatch;
import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variables;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.distribution.Normal;
import junit.framework.TestCase;
import org.junit.Assert;

import java.io.IOException;

/**
 * Created by ana@cs.aau.dk on 10/02/15.
 */
public class VMPNormalTest extends TestCase {


    public static void test1() throws IOException, ClassNotFoundException{

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/Munin1.bn");
        for (int i = 0; i < 10; i++) {

            //bn.randomInitialization(new Random(i));
            //System.out.println(bn.outputString());
            VMP vmp = new VMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            vmp.setModel(bn);

            Stopwatch watch = Stopwatch.createStarted();

            vmp.runInference();
            System.out.println(watch.stop());

            //bn.getStaticVariables().getListOfVariables().forEach( var -> System.out.println(var.getName()+": "+InferenceEngineForBN.getPosterior(bn.getStaticVariables().getVariableByName(var.getName())).outputString()));
        }
    }

    //Test with a BN containing 2 Guassian variables A->B
    public static void test2() throws IOException, ClassNotFoundException{

        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal distA = bn.getConditionalDistribution(varA);
        ConditionalLinearGaussian distB = bn.getConditionalDistribution(varB);

        distA.setMean(1);
        distA.setVariance(0.25);
        distB.setIntercept(1);
        //distB.setCoeffParents(new double[]{1});
        distB.setCoeffForParent(varA, 1);
        distB.setVariance(0.25);

        System.out.println(bn.toString());

        double meanPA =  distA.getMean();
        double sdPA =  distA.getSd();

        double b0PB =  distB.getIntercept();
        //double b1PB = distB.getCoeffParents()[0];
        double b1PB = distB.getCoeffForParent(varA);
        double sdPB =  distB.getSd();

        VMP vmp = new VMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        vmp.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.getNodes().get(0).getQDist());
        EF_Normal qBDist = ((EF_Normal) vmp.getNodes().get(1).getQDist());

        double meanQA= qADist.getMomentParameters().get(0);
        double sdQA= Math.sqrt(qADist.getMomentParameters().get(1) - qADist.getMomentParameters().get(0) * qADist.getMomentParameters().get(0));

        double meanQB= qBDist.getMomentParameters().get(0);
        double sdQB= Math.sqrt(qBDist.getMomentParameters().get(1) - qBDist.getMomentParameters().get(0)*qBDist.getMomentParameters().get(0));

        vmp.runInference();

        Normal postA = vmp.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = ((Normal)vmp.getPosterior(varB));
        System.out.println("P(B) = " + postB.toString());

        boolean convergence = false;
        double oldvalue = 0;

        while(!convergence){

            sdQA = Math.sqrt(Math.pow(b1PB*b1PB/(sdPB*sdPB) + 1.0/(sdPA*sdPA),-1));
            meanQA = sdQA*sdQA*(b1PB*meanQB/(sdPB*sdPB) - b0PB*b1PB/(sdPB*sdPB) + meanPA/(sdPA*sdPA));

            sdQB = sdPB;
            meanQB = sdQB*sdQB*(b0PB/(sdPB*sdPB) + b1PB*meanQA/(sdPB*sdPB));

            if (Math.abs(sdQA + meanQA + sdQB + meanQB - oldvalue) < 0.001) {
                convergence = true;
            }

            oldvalue = sdQA + meanQA + sdQB + meanQB ;
        }

        System.out.println("Mean and Sd of A: " + meanQA +", " + sdQA );
        System.out.println("Mean and Sd of B: " + meanQB +", " + sdQB );

        Assert.assertEquals(postA.getMean(),meanQA,0.01);
        Assert.assertEquals(postA.getSd(),sdQA,0.01);
        Assert.assertEquals(postB.getMean(),meanQB,0.01);
        Assert.assertEquals(postB.getSd(),sdQB,0.01);
    }

    //Test with a BN containing 3 Guassian variables A->C<-B
    public static void test3() throws IOException, ClassNotFoundException{

        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal distA = bn.getConditionalDistribution(varA);
        Normal distB = bn.getConditionalDistribution(varB);
        ConditionalLinearGaussian distC = bn.getConditionalDistribution(varC);

        distA.setMean(1);
        distA.setVariance(0.25);

        distB.setMean(1.2);
        distB.setVariance(0.64);

        distC.setIntercept(1);
        //distC.setCoeffParents(new double[]{1, 1});
        distC.setCoeffForParent(varA, 1);
        distC.setCoeffForParent(varB, 1);
        distC.setVariance(0.25);

        System.out.println(bn.toString());

        double meanPA =  distA.getMean();
        double sdPA =  distA.getSd();

        double meanPB =  distB.getMean();
        double sdPB =  distB.getSd();

        double b0PC =  distC.getIntercept();
        //double b1PC = distC.getCoeffParents()[0];
        //double b2PC = distC.getCoeffParents()[1];
        double b1PC = distC.getCoeffForParent(varA);
        double b2PC = distC.getCoeffForParent(varB);
        double sdPC =  distC.getSd();

        VMP vmp = new VMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        vmp.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.getNodes().get(0).getQDist());
        EF_Normal qBDist = ((EF_Normal) vmp.getNodes().get(1).getQDist());
        EF_Normal qCDist = ((EF_Normal) vmp.getNodes().get(2).getQDist());

        double meanQA= qADist.getMomentParameters().get(0);
        double sdQA= Math.sqrt(qADist.getMomentParameters().get(1) - qADist.getMomentParameters().get(0) * qADist.getMomentParameters().get(0));

        double meanQB= qBDist.getMomentParameters().get(0);
        double sdQB= Math.sqrt(qBDist.getMomentParameters().get(1) - qBDist.getMomentParameters().get(0) * qBDist.getMomentParameters().get(0));

        double meanQC= qCDist.getMomentParameters().get(0);
        double sdQC= Math.sqrt(qCDist.getMomentParameters().get(1) - qCDist.getMomentParameters().get(0)*qCDist.getMomentParameters().get(0));

        //InferenceEngineForBN.setEvidence(assignment);
        vmp.runInference();

        Normal postA = vmp.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = ((Normal)vmp.getPosterior(varB));
        System.out.println("P(B) = " + postB.toString());
        Normal postC = ((Normal)vmp.getPosterior(varC));
        System.out.println("P(C) = " + postC.toString());

        boolean convergence = false;
        double oldvalue = 0;

        while(!convergence){

            sdQA = Math.sqrt(Math.pow(b1PC*b1PC/(sdPC*sdPC) + 1.0/(sdPA*sdPA),-1));
            meanQA = sdQA*sdQA*(b1PC*meanQC/(sdPC*sdPC) - b0PC*b1PC/(sdPC*sdPC) - b1PC*b2PC*meanQB/(sdPC*sdPC) + meanPA/(sdPA*sdPA));

            sdQB = Math.sqrt(Math.pow(b2PC*b2PC/(sdPC*sdPC) + 1.0/(sdPB*sdPB),-1));
            meanQB = sdQB*sdQB*(b2PC*meanQC/(sdPC*sdPC) - b0PC*b2PC/(sdPC*sdPC) - b1PC*b2PC*meanQA/(sdPC*sdPC) + meanPB/(sdPB*sdPB));

            sdQC = sdPC;//
            meanQC = sdQC*sdQC*(b0PC/(sdPC*sdPC) + b1PC*meanQA/(sdPC*sdPC) + b2PC*meanQB/(sdPC*sdPC));

            if (Math.abs(sdQA + meanQA + sdQB + meanQB + sdQC + meanQC - oldvalue) < 0.001) {
                convergence = true;
            }
            oldvalue = sdQA + meanQA + sdQB + meanQB + sdQC + meanQC;
        }

        System.out.println("Mean and Sd of A: " + meanQA +", " + sdQA );
        System.out.println("Mean and Sd of B: " + meanQB +", " + sdQB );
        System.out.println("Mean and Sd of C: " + meanQC +", " + sdQC );

        Assert.assertEquals(postA.getMean(),meanQA,0.01);
        Assert.assertEquals(postA.getSd(),sdQA,0.01);

        Assert.assertEquals(postB.getMean(),meanQB,0.01);
        Assert.assertEquals(postB.getSd(),sdQB,0.01);

        Assert.assertEquals(postC.getMean(),meanQC,0.01);
        Assert.assertEquals(postC.getSd(),sdQC,0.01);

   }

    //Test with a BN containing 3 Guassian variables A->C<-B  And C is Observed
    public static void test4() throws IOException, ClassNotFoundException{

        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal distA = bn.getConditionalDistribution(varA);
        Normal distB = bn.getConditionalDistribution(varB);
        ConditionalLinearGaussian distC = bn.getConditionalDistribution(varC);

        distA.setMean(1);
        distA.setVariance(0.25);

        distB.setMean(1.2);
        distB.setVariance(0.64);

        distC.setIntercept(1);
        //distC.setCoeffParents(new double[]{1, 1});
        distC.setCoeffForParent(varA, 1);
        distC.setCoeffForParent(varB, 1);
        distC.setVariance(0.25);

        System.out.println(bn.toString());

        double meanPA =  distA.getMean();
        double sdPA =  distA.getSd();

        double meanPB =  distB.getMean();
        double sdPB =  distB.getSd();

        double b0PC =  distC.getIntercept();
        //double b1PC = distC.getCoeffParents()[0];
        //double b2PC = distC.getCoeffParents()[1];
        double b1PC = distC.getCoeffForParent(varA);
        double b2PC = distC.getCoeffForParent(varB);
        double sdPC =  distC.getSd();

        VMP vmp = new VMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        vmp.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.getNodes().get(0).getQDist());
        EF_Normal qBDist = ((EF_Normal) vmp.getNodes().get(1).getQDist());
        EF_Normal qCDist = ((EF_Normal) vmp.getNodes().get(2).getQDist());

        double meanQA= qADist.getMomentParameters().get(0);
        double sdQA= Math.sqrt(qADist.getMomentParameters().get(1) - qADist.getMomentParameters().get(0) * qADist.getMomentParameters().get(0));

        double meanQB= qBDist.getMomentParameters().get(0);
        double sdQB= Math.sqrt(qBDist.getMomentParameters().get(1) - qBDist.getMomentParameters().get(0) * qBDist.getMomentParameters().get(0));

        double meanQC= 0.7;

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varC, 0.7);

        vmp.setEvidence(assignment);

        vmp.runInference();

        Normal postA = vmp.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = vmp.getPosterior(varB);
        System.out.println("P(B) = " + postB.toString());

        boolean convergence = false;
        double oldvalue = 0;

        while(!convergence){

            sdQA = Math.sqrt(Math.pow(b1PC*b1PC/(sdPC*sdPC) + 1.0/(sdPA*sdPA),-1));
            meanQA = sdQA*sdQA*(b1PC*meanQC/(sdPC*sdPC) - b0PC*b1PC/(sdPC*sdPC) - b1PC*b2PC*meanQB/(sdPC*sdPC) + meanPA/(sdPA*sdPA));

            sdQB = Math.sqrt(Math.pow(b2PC*b2PC/(sdPC*sdPC) + 1.0/(sdPB*sdPB),-1));
            meanQB = sdQB*sdQB*(b2PC*meanQC/(sdPC*sdPC) - b0PC*b2PC/(sdPC*sdPC) - b1PC*b2PC*meanQA/(sdPC*sdPC) + meanPB/(sdPB*sdPB));

            if (Math.abs(sdQA + meanQA + sdQB + meanQB - oldvalue) < 0.001) {
                convergence = true;
            }
            oldvalue = sdQA + meanQA + sdQB + meanQB;
        }

        System.out.println("Mean and Sd of A: " + meanQA +", " + sdQA );
        System.out.println("Mean and Sd of B: " + meanQB +", " + sdQB );

        Assert.assertEquals(postA.getMean(),meanQA,0.01);
        Assert.assertEquals(postA.getSd(),sdQA,0.01);
        Assert.assertEquals(postB.getMean(),meanQB,0.01);
        Assert.assertEquals(postB.getSd(),sdQB,0.01);
    }

    //Test with a BN containing 3 Guassian variables such that C has two children A and B: i.e., C->A and C->B
    public static void test5() throws IOException, ClassNotFoundException{

        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        ConditionalLinearGaussian distA = bn.getConditionalDistribution(varA);
        ConditionalLinearGaussian distB = bn.getConditionalDistribution(varB);
        Normal distC = bn.getConditionalDistribution(varC);

        distA.setIntercept(1);
        //distA.setCoeffParents(new double[]{1});
        distA.setCoeffForParent(varC, 1);
        distA.setVariance(0.25);

        distB.setIntercept(1.5);
        //distB.setCoeffParents(new double[]{1});
        distB.setCoeffForParent(varC, 1);
        distB.setVariance(0.64);

        distC.setMean(1);
        distC.setVariance(0.25);

        System.out.println(bn.toString());

        double b0PA =  distA.getIntercept();
        //double b1PA = distA.getCoeffParents()[0];
        double b1PA = distA.getCoeffForParent(varC);
        double sdPA =  distA.getSd();

        double b0PB =  distB.getIntercept();
        //double b1PB = distB.getCoeffParents()[0];
        double b1PB = distB.getCoeffForParent(varC);
        double sdPB =  distB.getSd();

        double meanPC =  distC.getMean();
        double sdPC =  distC.getSd();

        VMP vmp = new VMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        vmp.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.getNodes().get(0).getQDist());
        EF_Normal qBDist = ((EF_Normal) vmp.getNodes().get(1).getQDist());
        EF_Normal qCDist = ((EF_Normal) vmp.getNodes().get(2).getQDist());

        double meanQA= qADist.getMomentParameters().get(0);
        double sdQA= Math.sqrt(qADist.getMomentParameters().get(1) - qADist.getMomentParameters().get(0) * qADist.getMomentParameters().get(0));

        double meanQB= qBDist.getMomentParameters().get(0);
        double sdQB= Math.sqrt(qBDist.getMomentParameters().get(1) - qBDist.getMomentParameters().get(0) * qBDist.getMomentParameters().get(0));

        double meanQC= qCDist.getMomentParameters().get(0);
        double sdQC= Math.sqrt(qCDist.getMomentParameters().get(1) - qCDist.getMomentParameters().get(0)*qCDist.getMomentParameters().get(0));

        vmp.runInference();

        Normal postA = vmp.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = ((Normal)vmp.getPosterior(varB));
        System.out.println("P(B) = " + postB.toString());
        Normal postC = ((Normal)vmp.getPosterior(varC));
        System.out.println("P(C) = " + postC.toString());

        boolean convergence = false;
        double oldvalue = 0;

        while(!convergence){
            sdQA = sdPA;
            meanQA = sdQA*sdQA*(b0PA/(sdPA*sdPA) + b1PA*meanQC/(sdPA*sdPA));

            sdQB = sdPB;
            meanQB = sdQB*sdQB*(b0PB/(sdPB*sdPB) + b1PB*meanQC/(sdPB*sdPB));

            sdQC = Math.sqrt(Math.pow(b1PA*b1PA/(sdPA*sdPA) + b1PB*b1PB/(sdPB*sdPB) + 1.0/(sdPC*sdPC),-1));
            meanQC = sdQC*sdQC*(b1PA*meanQA/(sdPA*sdPA) - b0PA*b1PA/(sdPA*sdPA) + b1PB*meanQB/(sdPB*sdPB) - b0PB*b1PB/(sdPB*sdPB) + meanPC/(sdPC*sdPC));

            if (Math.abs(sdQA + meanQA + sdQB + meanQB + sdQC + meanQC - oldvalue) < 0.001) {
                convergence = true;
            }
            oldvalue = sdQA + meanQA + sdQB + meanQB + sdQC + meanQC;
        }

        System.out.println("Mean and Sd of A: " + meanQA +", " + sdQA );
        System.out.println("Mean and Sd of B: " + meanQB +", " + sdQB );
        System.out.println("Mean and Sd of C: " + meanQC +", " + sdQC );

        Assert.assertEquals(postA.getMean(),meanQA,0.01);
        Assert.assertEquals(postA.getSd(),sdQA,0.01);
        Assert.assertEquals(postB.getMean(),meanQB,0.01);
        Assert.assertEquals(postB.getSd(),sdQB,0.01);

        Assert.assertEquals(postC.getMean(),meanQC,0.01);
        Assert.assertEquals(postC.getSd(),sdQC,0.01);
    }

    //Test with a BN containing 3 Guassian variables such that C has two children A and B: i.e., C->A and C->B
    //In this test, Both A and B are observed
    public static void test6() throws IOException, ClassNotFoundException{

        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        ConditionalLinearGaussian distA = bn.getConditionalDistribution(varA);
        ConditionalLinearGaussian distB = bn.getConditionalDistribution(varB);
        Normal distC = bn.getConditionalDistribution(varC);

        distA.setIntercept(1);
        //distA.setCoeffParents(new double[]{1});
        distA.setCoeffForParent(varC, 1);
        distA.setVariance(0.25);

        distB.setIntercept(1.5);
        //distB.setCoeffParents(new double[]{1});
        distB.setCoeffForParent(varC, 1);
        distB.setVariance(0.64);

        distC.setMean(1);
        distC.setVariance(0.25);

        System.out.println(bn.toString());

        double b0PA =  distA.getIntercept();
        //double b1PA = distA.getCoeffParents()[0];
        double b1PA = distA.getCoeffForParent(varC);
        double sdPA =  distA.getSd();

        double b0PB =  distB.getIntercept();
        //double b1PB = distB.getCoeffParents()[0];
        double b1PB = distB.getCoeffForParent(varC);
        double sdPB =  distB.getSd();

        double meanPC =  distC.getMean();
        double sdPC =  distC.getSd();

        VMP vmp = new VMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        vmp.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.getNodes().get(0).getQDist());
        EF_Normal qBDist = ((EF_Normal) vmp.getNodes().get(1).getQDist());
        EF_Normal qCDist = ((EF_Normal) vmp.getNodes().get(2).getQDist());

        double meanQA= 0.7;
        double meanQB= 0.2;

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varA, 0.7);
        assignment.setValue(varB, 0.2);

        double meanQC= qCDist.getMomentParameters().get(0);
        double sdQC= Math.sqrt(qCDist.getMomentParameters().get(1) - qCDist.getMomentParameters().get(0)*qCDist.getMomentParameters().get(0));

        vmp.setEvidence(assignment);
        vmp.runInference();

        Normal postC = ((Normal)vmp.getPosterior(varC));
        System.out.println("P(C) = " + postC.toString());

        boolean convergence = false;
        double oldvalue = 0;

        while(!convergence){
            sdQC = Math.sqrt(Math.pow(b1PA*b1PA/(sdPA*sdPA) + b1PB*b1PB/(sdPB*sdPB) + 1.0/(sdPC*sdPC),-1));
            meanQC = sdQC*sdQC*(b1PA*meanQA/(sdPA*sdPA) - b0PA*b1PA/(sdPA*sdPA) + b1PB*meanQB/(sdPB*sdPB) - b0PB*b1PB/(sdPB*sdPB) + meanPC/(sdPC*sdPC));

            if (Math.abs(sdQC + meanQC - oldvalue) < 0.001) {
                convergence = true;
            }
            oldvalue = sdQC + meanQC;
        }

        System.out.println("Mean and Sd of C: " + meanQC +", " + sdQC );

        Assert.assertEquals(postC.getMean(),meanQC,0.01);
        Assert.assertEquals(postC.getSd(),sdQC,0.01);
    }


    //Test with a BN containing 3 Guassian variables with a dag structure defined as A->B->C
    public static void test7() throws IOException, ClassNotFoundException{

        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal distA = bn.getConditionalDistribution(varA);
        ConditionalLinearGaussian distB = bn.getConditionalDistribution(varB);
        ConditionalLinearGaussian distC = bn.getConditionalDistribution(varC);

        distA.setMean(1);
        distA.setVariance(0.25);

        distB.setIntercept(1);
        //distB.setCoeffParents(new double[]{1});
        distB.setCoeffForParent(varA, 1);
        distB.setVariance(0.04);

        distC.setIntercept(1);
        //distC.setCoeffParents(new double[]{1});
        distC.setCoeffForParent(varB, 1);
        distC.setVariance(0.25);


        System.out.println(bn.toString());

        double meanPA =  distA.getMean();
        double sdPA =  distA.getSd();

        double b0PB =  distB.getIntercept();
        //double b1PB = distB.getCoeffParents()[0];
        double b1PB = distB.getCoeffForParent(varA);
        double sdPB =  distB.getSd();

        double b0PC =  distC.getIntercept();
        //double b1PC = distC.getCoeffParents()[0];
        double b1PC = distC.getCoeffForParent(varB);
        double sdPC =  distC.getSd();

        VMP vmp = new VMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        vmp.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.getNodes().get(0).getQDist());
        EF_Normal qBDist = ((EF_Normal) vmp.getNodes().get(1).getQDist());
        EF_Normal qCDist = ((EF_Normal) vmp.getNodes().get(2).getQDist());

        double meanQA= qADist.getMomentParameters().get(0);
        double sdQA= Math.sqrt(qADist.getMomentParameters().get(1) - qADist.getMomentParameters().get(0) * qADist.getMomentParameters().get(0));

        double meanQB= qBDist.getMomentParameters().get(0);
        double sdQB= Math.sqrt(qBDist.getMomentParameters().get(1) - qBDist.getMomentParameters().get(0) * qBDist.getMomentParameters().get(0));

        double meanQC= qCDist.getMomentParameters().get(0);
        double sdQC= Math.sqrt(qCDist.getMomentParameters().get(1) - qCDist.getMomentParameters().get(0)*qCDist.getMomentParameters().get(0));

        vmp.runInference();

        Normal postA = vmp.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postB = ((Normal)vmp.getPosterior(varB));
        System.out.println("P(B) = " + postB.toString());
        Normal postC = ((Normal)vmp.getPosterior(varC));
        System.out.println("P(C) = " + postC.toString());

        boolean convergence = false;
        double oldvalue = 0;

        while(!convergence){

            sdQA = Math.sqrt(Math.pow(b1PB*b1PB/(sdPB*sdPB) + 1.0/(sdPA*sdPA),-1));
            meanQA = sdQA*sdQA*(b1PB*meanQB/(sdPB*sdPB) - b0PB*b1PB/(sdPB*sdPB) + meanPA/(sdPA*sdPA));

            sdQB = Math.sqrt(Math.pow(b1PC*b1PC/(sdPC*sdPC) + 1.0/(sdPB*sdPB),-1));
            meanQB = sdQB*sdQB*(b0PB/(sdPB*sdPB) + b1PB*meanQA/(sdPB*sdPB) - b0PC * b1PC/(sdPC*sdPC) + b1PC*meanQC/(sdPC*sdPC));

            sdQC = sdPC;
            meanQC = sdQC*sdQC*(b0PC/(sdPC*sdPC) + b1PC*meanQB/(sdPC*sdPC));

            if (Math.abs(sdQA + meanQA + sdQB + meanQB + sdQC + meanQC - oldvalue) < 0.001) {
                convergence = true;
            }
            oldvalue = sdQA + meanQA + sdQB + meanQB + sdQC + meanQC;
        }

        System.out.println("Mean and Sd of A: " + meanQA +", " + sdQA );
        System.out.println("Mean and Sd of B: " + meanQB +", " + sdQB );
        System.out.println("Mean and Sd of C: " + meanQC +", " + sdQC );

        Assert.assertEquals(postA.getMean(),meanQA,0.01);
        Assert.assertEquals(postA.getSd(),sdQA,0.01);
        Assert.assertEquals(postB.getMean(),meanQB,0.01);
        Assert.assertEquals(postB.getSd(),sdQB,0.01);
        Assert.assertEquals(postC.getMean(),meanQC,0.01);
        Assert.assertEquals(postC.getSd(),sdQC,0.01);
    }


    //Test with a BN containing 3 Guassian variables with a dag structure defined as A->B->C
    //In this test, variable B is observed
    public static void test8() throws IOException, ClassNotFoundException{

        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Normal distA = bn.getConditionalDistribution(varA);
        ConditionalLinearGaussian distB = bn.getConditionalDistribution(varB);
        ConditionalLinearGaussian distC = bn.getConditionalDistribution(varC);

        distA.setMean(1);
        distA.setVariance(0.25);

        distB.setIntercept(1);
        //distB.setCoeffParents(new double[]{1});
        distB.setCoeffForParent(varA, 1);
        distB.setVariance(0.04);

        distC.setIntercept(1);
        //distC.setCoeffParents(new double[]{1});
        distC.setCoeffForParent(varB, 1);
        distC.setVariance(0.25);


        System.out.println(bn.toString());

        double meanPA =  distA.getMean();
        double sdPA =  distA.getSd();

        double b0PB =  distB.getIntercept();
        //double b1PB = distB.getCoeffParents()[0];
        double b1PB = distB.getCoeffForParent(varA);
        double sdPB =  distB.getSd();

        double b0PC =  distC.getIntercept();
        //double b1PC = distC.getCoeffParents()[0];
        double b1PC = distC.getCoeffForParent(varB);
        double sdPC =  distC.getSd();

        VMP vmp = new VMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        vmp.setModel(bn);

        EF_Normal qADist = ((EF_Normal) vmp.getNodes().get(0).getQDist());
        EF_Normal qBDist = ((EF_Normal) vmp.getNodes().get(1).getQDist());
        EF_Normal qCDist = ((EF_Normal) vmp.getNodes().get(2).getQDist());

        double meanQA= qADist.getMomentParameters().get(0);
        double sdQA= Math.sqrt(qADist.getMomentParameters().get(1) - qADist.getMomentParameters().get(0) * qADist.getMomentParameters().get(0));

        double meanQC= qCDist.getMomentParameters().get(0);
        double sdQC= Math.sqrt(qCDist.getMomentParameters().get(1) - qCDist.getMomentParameters().get(0)*qCDist.getMomentParameters().get(0));

        double meanQB= 0.4;

        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varB, 0.4);

        vmp.setEvidence(assignment);
        vmp.runInference();

        Normal postA = vmp.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Normal postC = ((Normal)vmp.getPosterior(varC));
        System.out.println("P(C) = " + postC.toString());

        boolean convergence = false;
        double oldvalue = 0;

        while(!convergence){

            sdQA = Math.sqrt(Math.pow(b1PB*b1PB/(sdPB*sdPB) + 1.0/(sdPA*sdPA),-1));
            meanQA = sdQA*sdQA*(b1PB*meanQB/(sdPB*sdPB) - b0PB*b1PB/(sdPB*sdPB) + meanPA/(sdPA*sdPA));

            sdQC = sdPC;
            meanQC = sdQC*sdQC*(b0PC/(sdPC*sdPC) + b1PC*meanQB/(sdPC*sdPC));

            if (Math.abs(sdQA + meanQA + sdQC + meanQC - oldvalue) < 0.001) {
                convergence = true;
            }
            oldvalue = sdQA + meanQA + + sdQC + meanQC;
        }

        System.out.println("Mean and Sd of A: " + meanQA +", " + sdQA );
        System.out.println("Mean and Sd of C: " + meanQC +", " + sdQC );

        Assert.assertEquals(postA.getMean(),meanQA,0.01);
        Assert.assertEquals(postA.getSd(),sdQA,0.01);
        Assert.assertEquals(postC.getMean(),meanQC,0.01);
        Assert.assertEquals(postC.getSd(),sdQC,0.01);
    }

}
