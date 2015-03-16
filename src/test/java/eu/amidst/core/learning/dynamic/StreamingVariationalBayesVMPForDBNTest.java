package eu.amidst.core.learning.dynamic;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.inference.VMP;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.core.utils.DynamicBayesianNetworkSampler;
import junit.framework.TestCase;

import java.util.Random;

public class StreamingVariationalBayesVMPForDBNTest extends TestCase {


    public static void test1(){

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(1);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbn);
        sampler.setSeed(0);
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(1,5000);

        StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
        svb.setWindowsSize(2);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

        BayesianLearningEngineForDBN.setDynamicDAG(dbn.getDynamicDAG());
        BayesianLearningEngineForDBN.setDataStream(dataStream);
        BayesianLearningEngineForDBN.runLearning();

        DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

        for (ConditionalDistribution dist : learnDBN.getDistributionsTimeT()) {
            System.out.println("Real one:");
            System.out.println(dbn.getConditionalDistributionTimeT(dist.getVariable()).toString());
            System.out.println("Learnt one:");
            System.out.println(dist.toString());
            assertTrue(dist.equalDist(dbn.getConditionalDistributionTimeT(dist.getVariable()), 0.05));
        }

    }

    public static void test2(){

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbn);
        sampler.setSeed(0);
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(1000,1);

        StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
        svb.setWindowsSize(1);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

        BayesianLearningEngineForDBN.setDynamicDAG(dbn.getDynamicDAG());
        BayesianLearningEngineForDBN.setDataStream(dataStream);
        BayesianLearningEngineForDBN.runLearning();

        DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

        for (ConditionalDistribution dist : learnDBN.getDistributionsTime0()) {
            System.out.println("Real one:");
            System.out.println(dbn.getConditionalDistributionTime0(dist.getVariable()).toString());
            System.out.println("Learnt one:");
            System.out.println(dist.toString());
            assertTrue(dist.equalDist(dbn.getConditionalDistributionTime0(dist.getVariable()), 0.05));
        }

    }

    public static void test3(){

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbn);
        sampler.setSeed(0);
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(1,5000);

        StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
        svb.setWindowsSize(1);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

        BayesianLearningEngineForDBN.setDynamicDAG(dbn.getDynamicDAG());
        BayesianLearningEngineForDBN.setDataStream(dataStream);
        BayesianLearningEngineForDBN.runLearning();

        DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

        for (ConditionalDistribution dist : learnDBN.getDistributionsTimeT()) {
            System.out.println("Real one:");
            System.out.println(dbn.getConditionalDistributionTimeT(dist.getVariable()).toString());
            System.out.println("Learnt one:");
            System.out.println(dist.toString());
            assertTrue(dist.equalDist(dbn.getConditionalDistributionTimeT(dist.getVariable()), 0.5));
        }

    }

    public static void test4(){

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbn);
        sampler.setSeed(0);
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(1000,1);

        StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
        svb.setWindowsSize(1);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

        BayesianLearningEngineForDBN.setDynamicDAG(dbn.getDynamicDAG());
        BayesianLearningEngineForDBN.setDataStream(dataStream);
        BayesianLearningEngineForDBN.runLearning();

        DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

        for (ConditionalDistribution dist : learnDBN.getDistributionsTime0()) {
            System.out.println("Real one:");
            System.out.println(dbn.getConditionalDistributionTime0(dist.getVariable()).toString());
            System.out.println("Learnt one:");
            System.out.println(dist.toString());
            assertTrue(dist.equalDist(dbn.getConditionalDistributionTime0(dist.getVariable()), 0.5));
        }

    }
}