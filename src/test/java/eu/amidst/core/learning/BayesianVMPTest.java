package eu.amidst.core.learning;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.DataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.Normal_NormalParents;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class BayesianVMPTest extends TestCase {


    public static void testMultinomials() throws IOException, ClassNotFoundException {
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
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataStream<DataInstance> data = sampler.sampleToDataBase(1000);

        BayesianLearningEngineForBN.setDAG(bn.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learnBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(bn.toString());
        System.out.println(learnBN.toString());
        assertTrue(bn.equalBNs(learnBN,0.05));
    }

    public static void testAsia() throws IOException, ClassNotFoundException{

        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");
        asianet.randomInitialization(new Random(0));
        System.out.println("\nAsia network \n ");

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

        BayesianLearningEngineForBN.setDAG(asianet.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learnAsianet = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(asianet.toString());
        System.out.println(learnAsianet.toString());
        assertTrue(asianet.equalBNs(learnAsianet,0.05));

    }

    public static void testGaussian() throws IOException, ClassNotFoundException{

        BayesianNetwork oneNormalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal.bn");

        Variable varA = oneNormalVarBN.getStaticVariables().getVariableByName("A");
        Normal dist = oneNormalVarBN.getDistribution(varA);

        dist.setMean(-10);
        dist.setSd(5);



        System.out.println("\nOne normal variable network \n ");

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(oneNormalVarBN);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataStream<DataInstance> data = sampler.sampleToDataBase(10000);

        System.out.println(1+data.stream().mapToDouble(d -> Math.pow(d.getValue(varA)-10,2)).sum()/2.0);


        BayesianLearningEngineForBN.setDAG(oneNormalVarBN.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learntOneNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(oneNormalVarBN.toString());
        System.out.println(learntOneNormalVarBN.toString());
        assertTrue(oneNormalVarBN.equalBNs(learntOneNormalVarBN,0.1));

    }

    public static void testGaussian_1Gaussian() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_1NormalParents.bn");

        System.out.println("\nNormal|Normal variable network \n ");

        Normal dist_B = normalVarBN.getDistribution(normalVarBN.getStaticVariables().getVariableByName("B"));

        dist_B.setMean(1);
        dist_B.setSd(2);

        Normal_NormalParents dist = normalVarBN.getDistribution(normalVarBN.getStaticVariables().getVariableByName("A"));

        dist.setCoeffParents(new double[]{2.0});
        dist.setSd(0.5);
        dist.setIntercept(5);

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataStream<DataInstance> data = sampler.sampleToDataBase(1000);

        BayesianNetwork learntNormalVarBN = LearningEngineForBN.learnParameters(normalVarBN.getDAG(), data);

        System.out.println(normalVarBN.toString());
        System.out.println(learntNormalVarBN.toString());
        assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.1));

        BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        System.out.println(BayesianLearningEngineForBN.getLogMarginalProbability());
        learntNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(normalVarBN.toString());
        System.out.println(learntNormalVarBN.toString());
        assertTrue(normalVarBN.equalBNs(learntNormalVarBN,0.1));


    }

    public static void testGaussian_2Gaussian() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("networks/Normal_NormalParents.bn");

        normalVarBN.randomInitialization(new Random(0));
        System.out.println("\nNormal|2Normal variable network \n ");


        BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataStream<DataInstance> data = sampler.sampleToDataBase(100000);

        BayesianLearningEngineForBN.setDAG(normalVarBN.getDAG());
        BayesianLearningEngineForBN.setDataStream(data);
        BayesianLearningEngineForBN.runLearning();

        BayesianNetwork learntNormalVarBN = BayesianLearningEngineForBN.getLearntBayesianNetwork();

        System.out.println(normalVarBN.toString());
        System.out.println(learntNormalVarBN.toString());
        assertTrue(normalVarBN.equalBNs(learntNormalVarBN,0.1));

    }

}
