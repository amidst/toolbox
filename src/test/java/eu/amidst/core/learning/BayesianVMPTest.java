package eu.amidst.core.learning;

import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.*;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class BayesianVMPTest extends TestCase {

    public static void testMultinomials1() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varA = variables.newMultionomialVariable("A", 2);

        DAG dag = new DAG(variables);


        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial distA = bn.getDistribution(varA);

        distA.setProbabilities(new double[]{0.6, 0.4});

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataBase(1000);


        BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(bn.getDAG(), data);

        System.out.println(learntNormalVarBN.toString());

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(bn.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(bn.toString());
        System.out.println(learnBN.toString());
        assertTrue(bn.equalBNs(learnBN,0.05));
    }

    public static void testMultinomials2() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newMultionomialVariable("B", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);

        distA.setProbabilities(new double[]{0.6, 0.4});
        distB.getMultinomial(0).setProbabilities(new double[]{0.75, 0.25});
        distB.getMultinomial(1).setProbabilities(new double[]{0.25, 0.75});

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataBase(1000);


        System.out.println(LearningEngineForBN.learnParameters(bn.getDAG(), data).toString());

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(bn.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(bn.toString());
        System.out.println(learnBN.toString());
        assertTrue(bn.equalBNs(learnBN,0.05));
    }

    public static void testMultinomials3() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newMultionomialVariable("B", 2);
        Variable varC = variables.newMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varA);


        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        bn.randomInitialization(new Random(6));

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        sampler.setHiddenVar(varA);
        DataStream<DataInstance> data = sampler.sampleToDataBase(20000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(bn.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(bn.toString());
        System.out.println(learnBN.toString());
        assertTrue(bn.equalBNs(learnBN,0.1));
    }

    public static void testMultinomials4() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newMultionomialVariable("B", 2);
        Variable varC = variables.newMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varB);
        dag.getParentSet(varB).addParent(varA);



        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        bn.randomInitialization(new Random(5));

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(5);
        sampler.setHiddenVar(varB);
        DataStream<DataInstance> data = sampler.sampleToDataBase(50000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(bn.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(bn.toString());
        System.out.println(learnBN.toString());
        assertTrue(bn.equalBNs(learnBN, 0.1));

    }


    public static void testMultinomials5() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varA = variables.newMultionomialVariable("A", 5);
        Variable varB = variables.newMultionomialVariable("B", 5);
        Variable varC = variables.newMultionomialVariable("C", 5);

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varB);
        dag.getParentSet(varB).addParent(varA);



        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        bn.randomInitialization(new Random(5));

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(5);
        sampler.setHiddenVar(varB);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(bn.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(bn.toString());
        System.out.println(learnBN.toString());
        //assertTrue(bn.equalBNs(learnBN, 0.1));

    }

    public static void testMultinomial6() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varB = variables.newMultionomialVariable("B",4);

        DAG dag = new DAG(variables);


        for (int i = 0; i < 10; i++) {


            BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);
            bn.randomInitialization(new Random(0));


            bn.randomInitialization(new Random(0));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i+299);
            sampler.setHiddenVar(varB);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(bn.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();

            BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(bn.toString());
            System.out.println(learnBN.toString());
            //assertTrue(bn.equalBNs(learnBN, 0.5));
        }
    }

    public static void testAsia() throws IOException, ClassNotFoundException{

        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");
        asianet.randomInitialization(new Random(0));
        System.out.println("\nAsia network \n ");

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(asianet.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        System.out.println(BayesianLearningEngineForBN.getLogMarginalProbability());

        BayesianNetwork learnAsianet = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(asianet.toString());
        System.out.println(learnAsianet.toString());
        assertTrue(asianet.equalBNs(learnAsianet,0.05));

    }

    public static void testAsia2() throws IOException, ClassNotFoundException{

        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");
        System.out.println(asianet.toString());
        asianet.randomInitialization(new Random(0));
        System.out.println("\nAsia network \n ");

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setHiddenVar(asianet.getStaticVariables().getVariableByName("E"));
        sampler.setHiddenVar(asianet.getStaticVariables().getVariableByName("L"));

        //sampler.setMARVar(asianet.getStaticVariables().getVariableByName("E"),0.5);
        //sampler.setMARVar(asianet.getStaticVariables().getVariableByName("L"),0.5);

        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setWindowsSize(100);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(asianet.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        System.out.println(BayesianLearningEngineForBN.getLogMarginalProbability());

        BayesianNetwork learnAsianet = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        //System.out.println(asianet.toString());
        //System.out.println(learnAsianet.toString());
        //assertTrue(asianet.equalBNs(learnAsianet,0.05));

    }

    public static void testGaussian0() throws IOException, ClassNotFoundException{

        BayesianNetwork oneNormalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal.bn");

        for (int i = 0; i < 10; i++) {

            Variable varA = oneNormalVarBN.getStaticVariables().getVariableByName("A");
            Normal dist = oneNormalVarBN.getDistribution(varA);

            dist.setMean(2000);
            dist.setVariance(30);

            oneNormalVarBN.randomInitialization(new Random(i));

            System.out.println("\nOne normal variable network \n ");

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(oneNormalVarBN);
            sampler.setSeed(0);

            DataStream<DataInstance> data = sampler.sampleToDataBase(1000);

            System.out.println(LearningEngineForBN.learnParameters(oneNormalVarBN.getDAG(), data).toString());

            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setWindowsSize(100);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setOutput(true);
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(oneNormalVarBN.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();

            System.out.println(BayesianLearningEngineForBN.getLogMarginalProbability());

            BayesianNetwork learntOneNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(oneNormalVarBN.toString());
            System.out.println(learntOneNormalVarBN.toString());
            assertTrue(oneNormalVarBN.equalBNs(learntOneNormalVarBN, 0.1));
        }
    }

    public static void testWasteIncinerator() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/WasteIncinerator.bn");

        //normalVarBN.randomInitialization(new Random(0));
        System.out.println("\nNormal|2Normal variable network \n ");


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setSeed(1);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

        BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);

        System.out.println(normalVarBN.toString());
        System.out.println(learntNormalVarBN.toString());
        assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.2));

        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        System.out.println(BayesianLearningEngineForBN.getLogMarginalProbability());

        learntNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(normalVarBN.toString());
        System.out.println(learntNormalVarBN.toString());
        assertTrue(normalVarBN.equalBNs(learntNormalVarBN,0.2));


    }

    public static void testGaussian1() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        for (int i = 0; i < 10; i++) {

            System.out.println("\nNormal|Normal variable network \n ");

            normalVarBN.randomInitialization(new Random(i));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(2);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

            BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.1));

            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setParallelMode(false);
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setOutput(false);
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();



            System.out.println(BayesianLearningEngineForBN.getLogMarginalProbability());
            learntNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.1));
        }

    }

    public static void testGaussian2() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_NormalParents.bn");

        int cont=0;
        for (int i = 0; i < 10; i++) {

            normalVarBN.randomInitialization(new Random(i));
            System.out.println("\nNormal|2Normal variable network \n ");


            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(0);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

            BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.1));


            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();

            learntNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.1));
        }
    }

    public static void testGaussian3() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_MultinomialParents.bn");

        for (int i = 0; i < 10; i++) {


            normalVarBN.randomInitialization(new Random(i));
            System.out.println("\nNormal|2Normal variable network \n ");

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(0);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

            BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.2));

            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();

            learntNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.2));
        }
    }

    public static void testGaussian4() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_MultinomialNormalParents.bn");
        int contA=0;
        int contB=0;

        for (int i = 1; i < 2; i++) {

            normalVarBN.randomInitialization(new Random(i));
            System.out.println("\nNormal|2Normal variable network \n ");


            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(0);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

            BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.3));
            if (normalVarBN.equalBNs(learntNormalVarBN, 0.3)) contA++;

            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();

            learntNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.3));
            if (normalVarBN.equalBNs(learntNormalVarBN, 0.3)) contB++;
        }
        System.out.println(contA);
        System.out.println(contB);

    }



    public static void testGaussian5() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varA);


        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);
        bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        sampler.setHiddenVar(varA);
        DataStream<DataInstance> data = sampler.sampleToDataBase(50);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setWindowsSize(5); //Set to 2 and an exception is raised. Numerical instability.
        svb.setSeed(1);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(bn.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(bn.toString());
        System.out.println(learnBN.toString());
        //assertTrue(bn.equalBNs(learnBN,0.1));
    }

    public static void testGaussian6() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        for (int i = 0; i < 1; i++) {


            BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

            bn.randomInitialization(new Random(i));

            System.out.println(bn.toString());
            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i);
            sampler.setMARVar(varB, 0.9);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(bn.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();

            System.out.println("Data Prob: " + BayesianLearningEngineForBN.getLogMarginalProbability());

            BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(bn.toString());
            System.out.println(learnBN.toString());
            assertTrue(bn.equalBNs(learnBN, 0.2));
        }
    }

    public static void testGaussian7() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varB = variables.newGaussianVariable("B");

        DAG dag = new DAG(variables);


        for (int i = 0; i < 10; i++) {


            BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);
            bn.randomInitialization(new Random(0));


            bn.randomInitialization(new Random(0));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i);
            sampler.setHiddenVar(varB);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10);


            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setWindowsSize(10); //Set to 2 and an exception is raised. Numerical instability.
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(bn.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();

            System.out.println("Data Prob: " + BayesianLearningEngineForBN.getLogMarginalProbability());


            BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(bn.toString());
            System.out.println(learnBN.toString());
            //assertTrue(bn.equalBNs(learnBN, 0.5));
        }
    }

    public static void testGaussian8() throws IOException, ClassNotFoundException {
        StaticVariables variables = new StaticVariables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        for (int i = 1; i < 2; i++) {


            BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);
            bn.randomInitialization(new Random(0));

            bn.randomInitialization(new Random(i));

            //System.out.println(bn.toString());
            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i+299);
            sampler.setHiddenVar(varB);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setWindowsSize(10);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(bn.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();

            System.out.println("Data Prob: " + BayesianLearningEngineForBN.getLogMarginalProbability());


            BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(bn.toString());
            System.out.println(learnBN.toString());
            ConditionalLinearGaussian distCP = bn.getDistribution(varC);
            ConditionalLinearGaussian distCQ = learnBN.getDistribution(varC);

            assertEquals(distCP.getSd(), distCQ.getSd(), 0.05);
        }
    }

    public static void testCompareBatchSizes() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        for (int i = 0; i < 1; i++) {

            System.out.println("\nNormal|Normal variable network \n ");

            normalVarBN.randomInitialization(new Random(i));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(i);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

            Attribute attVarA = data.getAttributes().getAttributeByName("A");
            Attribute attVarB = data.getAttributes().getAttributeByName("B");

            Variable varA = normalVarBN.getStaticVariables().getVariableByName("A");
            Variable varB = normalVarBN.getStaticVariables().getVariableByName("B");


            BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);
            String beta0fromML = Double.toString(((ConditionalLinearGaussian)learntNormalVarBN.
                    getConditionalDistribution(varA)).getIntercept());
            String beta1fromML = Double.toString(((ConditionalLinearGaussian)learntNormalVarBN.
                    getConditionalDistribution(varA)).getCoeffParents()[0]);

            /**
             * Incremental sample mean
             */
            String sampleMeanB = "";
            double incrementalMeanB = 0;
            double index = 1;
            for(DataInstance dataInstance: data){
                incrementalMeanB = incrementalMeanB + (dataInstance.getValue(attVarB) - incrementalMeanB)/index;
                sampleMeanB += incrementalMeanB+", ";
                index++;
            }

            /**
             * Streaming Variational Bayes for batches of 1 sample
             */
            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);
            BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);


            String varA_Beta0output = "Variable A beta0 (CLG)\n"+beta0fromML+ "\n",
                    varA_Beta1output = "Variable A beta1 (CLG)\n"+beta1fromML+ "\n",
                    varBoutput = "Variable B mean (univ. normal)\n"+sampleMeanB + "\n";
            int[] windowsSizes = {1,10,100, 1000};
            for (int j = 0; j < windowsSizes.length; j++) {
                svb.setWindowsSize(windowsSizes[j]);
                svb.initLearning();
                String svbBeta0A = "", svbBeta1A = "",svbMeanB = "";
                Iterator<DataOnMemory<DataInstance>> batchIterator = data.streamOfBatches(windowsSizes[j]).iterator();
                while(batchIterator.hasNext()){
                    DataOnMemory<DataInstance> batch = batchIterator.next();
                    svb.updateModel(batch);
                    ConditionalLinearGaussian distAsample = svb.getLearntBayesianNetwork().
                            getConditionalDistribution(varA);
                    double beta0A = distAsample.getIntercept();
                    double beta1A = distAsample.getCoeffParents()[0];
                    svbBeta0A += beta0A+", ";
                    svbBeta1A += beta1A+", ";
                    Normal distBsample = (Normal)((BaseDistribution_MultinomialParents)svb.getLearntBayesianNetwork().
                            getConditionalDistribution(varB)).getBaseDistribution(0);
                    svbMeanB += distBsample.getMean()+", ";
                }
                varA_Beta0output += svbBeta0A +"\n";
                varA_Beta1output += svbBeta1A +"\n";
                varBoutput += svbMeanB +"\n";
            }

            System.out.println(varA_Beta0output);
            System.out.println(varA_Beta1output);
            System.out.println(varBoutput);
        }

    }

    public static void testCompareBatchSizesParallelMode() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        for (int i = 0; i < 1; i++) {

            System.out.println("\nNormal|Normal variable network \n ");

            normalVarBN.randomInitialization(new Random(i));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(i);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

            Attribute attVarA = data.getAttributes().getAttributeByName("A");
            Attribute attVarB = data.getAttributes().getAttributeByName("B");

            Variable varA = normalVarBN.getStaticVariables().getVariableByName("A");
            Variable varB = normalVarBN.getStaticVariables().getVariableByName("B");


            BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);
            String beta0fromML = Double.toString(((ConditionalLinearGaussian)learntNormalVarBN.
                    getConditionalDistribution(varA)).getIntercept());
            String beta1fromML = Double.toString(((ConditionalLinearGaussian)learntNormalVarBN.
                    getConditionalDistribution(varA)).getCoeffParents()[0]);

            /**
             * Incremental sample mean
             */
            String sampleMeanB = "";
            double incrementalMeanB = 0;
            double index = 1;
            for(DataInstance dataInstance: data){
                incrementalMeanB = incrementalMeanB + (dataInstance.getValue(attVarB) - incrementalMeanB)/index;
                sampleMeanB += incrementalMeanB+", ";
                index++;
            }

            /**
             * Streaming Variational Bayes for batches of different sizes
             */
            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);
            BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);


            int[] windowsSizes = {1,10,100, 1000};
            svb.runLearningOnParallelForDifferentBatchWindows(windowsSizes, beta0fromML, beta1fromML, sampleMeanB);
        }

    }

    public static void testLogProbOfEvidenceForDiffBatches_WasteIncinerator() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/WasteIncinerator.bn");

        //normalVarBN.randomInitialization(new Random(0));
        System.out.println("\nWaste Incinerator - \n ");

        for (int j = 0; j < 10; j++) {

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            //sampler.setHiddenVar(null);
            sampler.setSeed(j);
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setSeed(j);
            VMP vmp = svb.getPlateuVMP().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);

            System.out.println("Window Size \t logProg(D) \t Time");
            int[] windowsSizes = {1, 50, 100, 1000, 5000, 10000};
            for (int i = 0; i < windowsSizes.length; i++) {
                svb.setWindowsSize(windowsSizes[i]);
                svb.initLearning();
                Stopwatch watch = Stopwatch.createStarted();
                double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
                System.out.println(windowsSizes[i] + "\t" + logProbOfEv_Batch1 + "\t" + watch.stop());
            }
        }
    }

    public static void testLogProbOfEvidenceForDiffBatches_WasteIncineratorWithLatentVars() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/WasteIncinerator.bn");

        //normalVarBN.randomInitialization(new Random(0));
        System.out.println("\nWaste Incinerator + latent vars - \n ");


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("Mout"));
        sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("D"));
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);

        System.out.println("Window Size \t logProg(D) \t Time");
        int[] windowsSizes = {1, 50, 100, 1000, 5000, 10000};
        for (int i = 0; i < windowsSizes.length; i++) {
            svb.setWindowsSize(windowsSizes[i]);
            svb.initLearning();
            Stopwatch watch = Stopwatch.createStarted();
            double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
            System.out.println(windowsSizes[i] + "\t" + logProbOfEv_Batch1 + "\t" + watch.stop());
            }
    }

    public static void testLogProbOfEvidenceForDiffBatches_Normal1Normal() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        normalVarBN.randomInitialization(new Random(0));
        System.out.println("\nNormal_1NormalParent - \n ");


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);

        System.out.println("Window Size \t logProg(D) \t Time");
        int[] windowsSizes = {1, 50, 100, 1000, 5000, 10000};
        for (int i = 0; i < windowsSizes.length; i++) {
            svb.setWindowsSize(windowsSizes[i]);
            svb.initLearning();
            Stopwatch watch = Stopwatch.createStarted();
            double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
            System.out.println(windowsSizes[i] + "\t" + logProbOfEv_Batch1 + "\t" + watch.stop());
        }
    }

    public static void testLogProbOfEvidenceForDiffBatches_Normal1Normal_LatentVariable() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        normalVarBN.randomInitialization(new Random(0));
        System.out.println("\nNormal_1NormalParent - \n ");


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("A"));
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);

        System.out.println("Window Size \t logProg(D) \t Time");
        int[] windowsSizes = {1, 50, 100, 1000, 5000, 10000};
        for (int i = 0; i < windowsSizes.length; i++) {
            svb.setWindowsSize(windowsSizes[i]);
            svb.initLearning();
            Stopwatch watch = Stopwatch.createStarted();
            double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
            System.out.println(windowsSizes[i] + "\t" + logProbOfEv_Batch1 + "\t" + watch.stop());
        }
    }

    public static void testLogProbOfEvidenceForDiffBatches_Asia() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        System.out.println("\nAsia - \n ");


        normalVarBN.randomInitialization(new Random(0));
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);

        System.out.println("Window Size \t logProg(D) \t Time");
        int[] windowsSizes = {1, 50, 100, 1000, 5000, 10000};
        for (int i = 0; i < windowsSizes.length; i++) {
            svb.setWindowsSize(windowsSizes[i]);
            svb.initLearning();
            Stopwatch watch = Stopwatch.createStarted();
            double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
            System.out.println(windowsSizes[i] + "\t" + logProbOfEv_Batch1 + "\t" + watch.stop());
        }
    }

    public static void testLogProbOfEvidenceForDiffBatches_AsiaLatentVars() throws IOException, ClassNotFoundException {
        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        System.out.println("\nAsia - \n ");


        normalVarBN.randomInitialization(new Random(0));
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        //sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("D"));
        sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("E"));
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);


        StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
        svb.setSeed(0);
        VMP vmp = svb.getPlateuVMP().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

        BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);

        System.out.println("Window Size \t logProg(D) \t Time");
        int[] windowsSizes = {1, 50, 100, 1000, 5000, 10000};
        for (int i = 0; i < windowsSizes.length; i++) {
            svb.setWindowsSize(windowsSizes[i]);
            svb.initLearning();
            Stopwatch watch = Stopwatch.createStarted();
            double logProbOfEv_Batch1 = data.streamOfBatches(windowsSizes[i]).sequential().mapToDouble(svb::updateModel).sum();
            System.out.println(windowsSizes[i] + "\t" + logProbOfEv_Batch1 + "\t" + watch.stop());
        }
    }



    public static void testGaussian1_play() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        for (int i = 0; i < 1; i++) {

            System.out.println("\nNormal|Normal variable network \n ");

            normalVarBN.randomInitialization(new Random(i));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(2);
            //sampler.setHiddenVar(normalVarBN.getStaticVariables().getVariableByName("B"));
            DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

            BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.1));

            StreamingVariationalBayesVMP svb = new StreamingVariationalBayesVMP();
            svb.setWindowsSize(1);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuVMP().getVMP();

            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForBN.setBayesianLearningAlgorithmForBN(svb);

            BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
            BayesianLearningEngineForBN.setDataStream(data);
            BayesianLearningEngineForBN.runLearning();



            System.out.println(BayesianLearningEngineForBN.getLogMarginalProbability());
            BayesianNetwork learntNormalVarBNVMP = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBNVMP.toString());
            //assertTrue(normalVarBN.equalBNs(learntNormalVarBNVMP, 0.1));

            System.out.println(BayesianLearningEngineForBN.getLogMarginalProbability());
            learntNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

            System.out.println(normalVarBN.toString());
            System.out.println(learntNormalVarBN.toString());
            assertFalse(normalVarBN.equalBNs(learntNormalVarBN, 0.1));
        }

    }


}
