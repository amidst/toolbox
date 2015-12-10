package eu.amidst.dynamic.learning;

import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.DynamicModelFactory;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.learning.dynamic.BayesianLearningEngineForDBN;
import eu.amidst.dynamic.learning.dynamic.MaximumLikelihoodForDBN;
import eu.amidst.dynamic.learning.dynamic.StreamingVariationalBayesVMPForDBN;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import eu.amidst.dynamic.variables.DynamicVariables;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

public class SVBForDBNTest extends TestCase {


    public static void test0(){

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbn);
        sampler.setSeed(0);
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(10,100);

        //dataStream.stream().forEach(d -> System.out.println(d.getValue(dbn.getDynamicVariables().getVariable("ClassVar"))));


        DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dbn.getDynamicDAG(), dataStream);

        System.out.println(bnet);

        StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
        svb.setWindowsSize(10);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDynamicDAG(dbn.getDynamicDAG());
        svb.setDataStream(dataStream);

        //svb.initLearning();
        //System.out.println(svb.getPlateuVMPDBN().toStringParemetersTimeT());
        //System.out.println(svb.getPlateuVMPDBN().toStringTemporalClones());
        //System.out.println(svb.getPlateuVMPDBN().toStringTimeT());

        svb.runLearning();


        DynamicBayesianNetwork learnDBN = svb.getLearntDBN();

        for (ConditionalDistribution dist : learnDBN.getConditionalDistributionsTimeT()) {
            System.out.println("Real one:");
            System.out.println(dbn.getConditionalDistributionTimeT(dist.getVariable()).toString());
            System.out.println("Learnt one:");
            System.out.println(dist.toString());
            assertTrue(dist.equalDist(dbn.getConditionalDistributionTimeT(dist.getVariable()), 0.05));
        }

    }
    public static void test1(){

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbn);
        sampler.setSeed(0);
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(1,5000);

        StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
        svb.setWindowsSize(1);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

        BayesianLearningEngineForDBN.setDynamicDAG(dbn.getDynamicDAG());
        BayesianLearningEngineForDBN.setDataStream(dataStream);
        BayesianLearningEngineForDBN.runLearning();

        DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

        for (ConditionalDistribution dist : learnDBN.getConditionalDistributionsTimeT()) {
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
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

        BayesianLearningEngineForDBN.setDynamicDAG(dbn.getDynamicDAG());
        BayesianLearningEngineForDBN.setDataStream(dataStream);
        BayesianLearningEngineForDBN.runLearning();

        DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

        for (ConditionalDistribution dist : learnDBN.getConditionalDistributionsTime0()) {
            System.out.println("Real one:");
            System.out.println(dbn.getConditionalDistributionTime0(dist.getVariable()).toString());
            System.out.println("Learnt one:");
            System.out.println(dist.toString());
            assertTrue(dist.equalDist(dbn.getConditionalDistributionTime0(dist.getVariable()), 0.05));
        }

    }

    public static void test3(){

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        DynamicVariables dynamicVariables = dbn.getDynamicVariables();

        Variable var1 = dbn.getDynamicDAG().getDynamicVariables().getVariableByName("ContinuousVar1");
        ConditionalLinearGaussian distContVar1Class0 = (ConditionalLinearGaussian)((Normal_MultinomialNormalParents)dbn.
                getConditionalDistributionTimeT(var1)).getNormal_NormalParentsDistribution(0);
        distContVar1Class0.setIntercept(1.0);
        distContVar1Class0.setCoeffForParent(dynamicVariables.getInterfaceVariable(var1), 0.5);
        ConditionalLinearGaussian distContVar1Class1 = (ConditionalLinearGaussian)((Normal_MultinomialNormalParents)dbn.
                getConditionalDistributionTimeT(dbn.getDynamicDAG().getDynamicVariables().getVariableByName("ContinuousVar1"))).getNormal_NormalParentsDistribution(1);
        distContVar1Class1.setIntercept(-1.0);
        distContVar1Class1.setCoeffForParent(dynamicVariables.getInterfaceVariable(var1), -0.3);

        Variable var2 = dbn.getDynamicDAG().getDynamicVariables().getVariableByName("ContinuousVar2");
        ConditionalLinearGaussian distContVar2Class0 = (ConditionalLinearGaussian)((Normal_MultinomialNormalParents)dbn.
                getConditionalDistributionTimeT(var2)).getNormal_NormalParentsDistribution(0);
        distContVar2Class0.setIntercept(2.1);
        distContVar2Class0.setCoeffForParent(dynamicVariables.getInterfaceVariable(var2), 0.3);
        ConditionalLinearGaussian distContVar2Class1 = (ConditionalLinearGaussian)((Normal_MultinomialNormalParents)dbn.
                getConditionalDistributionTimeT(var2)).getNormal_NormalParentsDistribution(1);
        distContVar2Class1.setIntercept(-2.1);
        distContVar2Class1.setCoeffForParent(dynamicVariables.getInterfaceVariable(var2), -0.3);

        System.out.println("--- Initial DBN ---");
        System.out.println(dbn.toString());

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbn);
        sampler.setSeed(10);
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(100,500);

        DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dbn.getDynamicDAG(), dataStream);


        System.out.println("--- Learnt DBN ---");
        System.out.println(bnet.toString());

        //dataStream.stream().forEach(d -> System.out.println(d.getValue(dbn.getDynamicVariables().getVariable("ContinuousVar1")) + ", "+ d.getValue(dbn.getDynamicVariables().getVariable("ContinuousVar2"))));

        StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
        svb.setWindowsSize(500);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
        vmp.setOutput(true);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.001);
        BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

        BayesianLearningEngineForDBN.setDynamicDAG(dbn.getDynamicDAG());
        BayesianLearningEngineForDBN.setDataStream(dataStream);
        BayesianLearningEngineForDBN.runLearning();

        DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

        for (ConditionalDistribution dist : learnDBN.getConditionalDistributionsTimeT()) {
            System.out.println("Real one:");
            System.out.println(dbn.getConditionalDistributionTimeT(dist.getVariable()).toString());
            System.out.println("Learnt one:");
            System.out.println(dist.toString());
            //assertTrue(dist.equalDist(dbn.getConditionalDistributionTimeT(dist.getVariable()), 0.5));
        }

    }

    public static void test4(){

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbn);
        sampler.setSeed(0);
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(10000,1);

        StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
        svb.setWindowsSize(1);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

        BayesianLearningEngineForDBN.setDynamicDAG(dbn.getDynamicDAG());
        BayesianLearningEngineForDBN.setDataStream(dataStream);
        BayesianLearningEngineForDBN.runLearning();

        DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

        for (ConditionalDistribution dist : learnDBN.getConditionalDistributionsTime0()) {
            System.out.println("Real one:");
            System.out.println(dbn.getConditionalDistributionTime0(dist.getVariable()).toString());
            System.out.println("Learnt one:");
            System.out.println(dist.toString());
            assertTrue(dist.equalDist(dbn.getConditionalDistributionTime0(dist.getVariable()), 0.5));
        }

    }

    @Test
    public void testGaussianChain() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 10; i++) {


            DynamicVariables dynamicVariables = DynamicModelFactory.newDynamicVariables();

            //Variable varA = dynamicVariables.newMultinomialDynamicVariable("A",2);
            //Variable varB = dynamicVariables.newMultinomialDynamicVariable("B",2);
            //Variable varC = dynamicVariables.newMultinomialDynamicVariable("C",2);

            Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            //Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = DynamicModelFactory.newDynamicDAG(dynamicVariables);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            dynamicDAG.getParentSetTimeT(varB).addParent(varA);
            //dynamicDAG.getParentSetTimeT(varC).addParent(varC);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            //dynamicDAG.getParentSetTimeT(varB).addParent(varC);



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));
            //dynamicDAG.getParentSetTimeT(varC).addParent(dynamicVariables.getInterfaceVariable(varC));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicModelFactory.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i+10));


            ConditionalLinearGaussian distA = dynamicNB.getConditionalDistributionTimeT(varA);
            //distA.setIntercept(0.4);
            distA.setCoeffForParent(dynamicVariables.getInterfaceVariable(varA), 0.4);

            ConditionalLinearGaussian distB = dynamicNB.getConditionalDistributionTimeT(varB);
            //distB.setIntercept(0.4);
            distB.setCoeffForParent(varA, 0.6);

            //ConditionalLinearGaussian distA = dynamicNB.getConditionalDistributionTimeT(varA);
            //distA.setIntercept(0.0);
            //distA.setCoeffParents(new double[]{1.0});

            //ConditionalLinearGaussian distB = dynamicNB.getConditionalDistributionTimeT(varA);
            //distB.setIntercept(0.0);
            //distB.setCoeffParents(new double[]{1.0});

            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setSeed(0);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(2, 10000);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning
            MaximumLikelihoodForDBN.setBatchSize(100);
            MaximumLikelihoodForDBN.setParallelMode(true);

            Stopwatch watch = Stopwatch.createStarted();

            //data.stream().forEach(d -> System.out.println(d.getValue(varA) + ", "+ d.getValue(varB)));
            //data.stream().forEach(d -> System.out.println(d.getValue(varA)));


            DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dynamicNB.getDynamicDAG(), data);

            System.out.println(watch.stop());
            System.out.println();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + bnet.getConditionalDistributionTimeT(var));
                assertTrue(bnet.getConditionalDistributionTimeT(var).equalDist(dynamicNB.getConditionalDistributionTimeT(var), 0.3));
            }

            System.out.println();

            StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
            svb.setWindowsSize(500);
            svb.setSeed(5);
            VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
            vmp.setOutput(true);
            vmp.setTestELBO(true);
            vmp.setMaxIter(10000);
            vmp.setThreshold(0.001);
            BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

            BayesianLearningEngineForDBN.setDynamicDAG(dynamicNB.getDynamicDAG());
            BayesianLearningEngineForDBN.setDataStream(data);
            BayesianLearningEngineForDBN.runLearning();

            //System.out.println(svb.getPlateuVMPDBN().toStringTimeT());

            DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + learnDBN.getConditionalDistributionTimeT(var));
                assertTrue(dynamicNB.getConditionalDistributionTimeT(var).equalDist(learnDBN.getConditionalDistributionTimeT(var), 0.3));
            }
            System.out.println();
            System.out.println();

        }
    }


    @Test
    public void testGaussianChain2() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 1; i++) {


            DynamicVariables dynamicVariables = DynamicModelFactory.newDynamicVariables();

            //Variable varA = dynamicVariables.newMultinomialDynamicVariable("A",2);
            //Variable varB = dynamicVariables.newMultinomialDynamicVariable("B",2);
            Variable varC = dynamicVariables.newMultinomialDynamicVariable("C",2);

            Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            //Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            //Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = DynamicModelFactory.newDynamicDAG(dynamicVariables);

            dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            //dynamicDAG.getParentSetTimeT(varB).addParent(varA);
            //dynamicDAG.getParentSetTimeT(varC).addParent(varC);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            //dynamicDAG.getParentSetTimeT(varB).addParent(varC);



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));
            dynamicDAG.getParentSetTimeT(varC).addParent(dynamicVariables.getInterfaceVariable(varC));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicModelFactory.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i+10));

            //ConditionalLinearGaussian distA = dynamicNB.getConditionalDistributionTimeT(varA);
            //distA.setIntercept(0.0);
            //distA.setCoeffParents(new double[]{1.0});



            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setSeed(0);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(2, 1000);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning
            MaximumLikelihoodForDBN.setBatchSize(100);
            MaximumLikelihoodForDBN.setParallelMode(true);

            Stopwatch watch = Stopwatch.createStarted();

            //data.stream().forEach(d -> System.out.println(d.getValue(varA) + ", "+ d.getValue(varB)));
            //data.stream().forEach(d -> System.out.println(d.getValue(varA)));


            DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dynamicNB.getDynamicDAG(), data);

            System.out.println(watch.stop());
            System.out.println();

            boolean skip = false;
            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + bnet.getConditionalDistributionTimeT(var));
                if (!(bnet.getConditionalDistributionTimeT(var).equalDist(dynamicNB.getConditionalDistributionTimeT(var), 0.2))) {
                    skip=true;
                    break;
                }
            }

            if (skip)
                continue;

            System.out.println();

            StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
            svb.setWindowsSize(500);
            svb.setSeed(5);
            VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
            vmp.setOutput(true);
            vmp.setTestELBO(true);
            vmp.setMaxIter(10000);
            vmp.setThreshold(0.001);
            BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

            BayesianLearningEngineForDBN.setDynamicDAG(dynamicNB.getDynamicDAG());
            BayesianLearningEngineForDBN.setDataStream(data);
            BayesianLearningEngineForDBN.runLearning();

            //System.out.println(svb.getPlateuVMPDBN().toStringTimeT());

            DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + learnDBN.getConditionalDistributionTimeT(var));
                assertTrue(dynamicNB.getConditionalDistributionTimeT(var).equalDist(learnDBN.getConditionalDistributionTimeT(var), 0.2));
            }
            System.out.println();
            System.out.println();

        }
    }


    @Test
    public void testGaussianChain3() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 1; i++) {


            DynamicVariables dynamicVariables = DynamicModelFactory.newDynamicVariables();

            //Variable varA = dynamicVariables.newMultinomialDynamicVariable("A",2);
            //Variable varB = dynamicVariables.newMultinomialDynamicVariable("B",2);
            //Variable varC = dynamicVariables.newMultinomialDynamicVariable("C",2);

            Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            //Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = DynamicModelFactory.newDynamicDAG(dynamicVariables);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            dynamicDAG.getParentSetTimeT(varB).addParent(varA);
            //dynamicDAG.getParentSetTimeT(varC).addParent(varC);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            //dynamicDAG.getParentSetTimeT(varB).addParent(varC);



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));
            //dynamicDAG.getParentSetTimeT(varC).addParent(dynamicVariables.getInterfaceVariable(varC));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicModelFactory.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i + 10));

            ConditionalLinearGaussian distA = dynamicNB.getConditionalDistributionTimeT(varA);
            distA.setIntercept(0.4);
            distA.setCoeffForParent(dynamicVariables.getInterfaceVariable(varA), 0.7);

            ConditionalLinearGaussian distB = dynamicNB.getConditionalDistributionTimeT(varB);
            distB.setIntercept(0.3);
            distB.setCoeffForParent(varA, 0.5);
            distB.setVariance(0.1);

            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            //sampler.setHiddenVar(varA);
            sampler.setMARVar(varA,0.6);
            sampler.setSeed(0);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(20, 5000);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning
            MaximumLikelihoodForDBN.setBatchSize(100);
            MaximumLikelihoodForDBN.setParallelMode(true);

            Stopwatch watch = Stopwatch.createStarted();

            //data.stream().forEach(d -> System.out.println(d.getValue(varA) + ", "+ d.getValue(varB)));
            //data.stream().forEach(d -> System.out.println(d.getValue(varA)));



            System.out.println();

            StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
            svb.setWindowsSize(500);
            svb.setSeed(5);
            VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
            vmp.setOutput(true);
            vmp.setTestELBO(true);
            vmp.setMaxIter(10000);
            vmp.setThreshold(0.001);
            BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

            BayesianLearningEngineForDBN.setDynamicDAG(dynamicNB.getDynamicDAG());
            BayesianLearningEngineForDBN.setDataStream(data);
            BayesianLearningEngineForDBN.runLearning();

            //System.out.println(svb.getPlateuVMPDBN().toStringTimeT());

            DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + learnDBN.getConditionalDistributionTimeT(var));
                assertTrue(dynamicNB.getConditionalDistributionTimeT(var).equalDist(learnDBN.getConditionalDistributionTimeT(var), 0.1));
            }
            System.out.println();
            System.out.println();

        }
    }

    @Test
    public void testGaussianChain4() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 1; i++) {


            DynamicVariables dynamicVariables = DynamicModelFactory.newDynamicVariables();

            //Variable varA = dynamicVariables.newMultinomialDynamicVariable("A",2);
            //Variable varB = dynamicVariables.newMultinomialDynamicVariable("B",2);
            //Variable varC = dynamicVariables.newMultinomialDynamicVariable("C",2);

            Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            //Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = DynamicModelFactory.newDynamicDAG(dynamicVariables);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            dynamicDAG.getParentSetTimeT(varB).addParent(varA);
            //dynamicDAG.getParentSetTimeT(varC).addParent(varC);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            //dynamicDAG.getParentSetTimeT(varB).addParent(varC);



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));
            //dynamicDAG.getParentSetTimeT(varC).addParent(dynamicVariables.getInterfaceVariable(varC));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicModelFactory.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i + 10));

            ConditionalLinearGaussian distA = dynamicNB.getConditionalDistributionTimeT(varA);
            distA.setIntercept(0.0);
            distA.setCoeffForParent(dynamicVariables.getInterfaceVariable(varA), 1);
            distA.setVariance(0.000001);


            ConditionalLinearGaussian distB = dynamicNB.getConditionalDistributionTimeT(varB);
            distB.setIntercept(0.0);
            distB.setCoeffForParent(varA, 1);
            distB.setVariance(0.000001);

            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setHiddenVar(varA);
            //sampler.setMARVar(varA,0.4);
            sampler.setSeed(0);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(1, 10000);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning
            MaximumLikelihoodForDBN.setBatchSize(100);
            MaximumLikelihoodForDBN.setParallelMode(true);

            Stopwatch watch = Stopwatch.createStarted();

            //data.stream().forEach(d -> System.out.println(d.getValue(varA) + ", "+ d.getValue(varB)));
            //data.stream().forEach(d -> System.out.println(d.getValue(varA)));



            System.out.println();

            StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
            svb.setWindowsSize(1000);
            svb.setSeed(0);
            VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
            vmp.setOutput(true);
            vmp.setTestELBO(true);
            vmp.setMaxIter(10000);
            vmp.setThreshold(0.001);
            BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

            BayesianLearningEngineForDBN.setDynamicDAG(dynamicNB.getDynamicDAG());
            BayesianLearningEngineForDBN.setDataStream(data);
            BayesianLearningEngineForDBN.runLearning();

            System.out.println(svb.getLogMarginalProbability());

            //System.out.println(svb.getPlateuVMPDBN().toStringTimeT());

            DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + learnDBN.getConditionalDistributionTimeT(var));
                //assertTrue(dynamicNB.getConditionalDistributionTimeT(var).equalDist(learnDBN.getConditionalDistributionTimeT(var), 0.2));
            }
            System.out.println();
            System.out.println();

        }
    }



    @Test
    public void testMultinomialChain() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 10; i++) {


            DynamicVariables dynamicVariables = DynamicModelFactory.newDynamicVariables();

            Variable varA = dynamicVariables.newMultinomialDynamicVariable("A",2);
            //Variable varB = dynamicVariables.newMultinomialDynamicVariable("B",2);
            //Variable varC = dynamicVariables.newMultinomialDynamicVariable("C",2);

            //Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            //Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            //Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = DynamicModelFactory.newDynamicDAG(dynamicVariables);

            //dynamicDAG.getParentSetTimeT(varA).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varB).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varC).addParent(classVar);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            //dynamicDAG.getParentSetTimeT(varB).addParent(varC);



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicModelFactory.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i));

            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setSeed(0);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(1, 10000);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning
            MaximumLikelihoodForDBN.setBatchSize(1000);
            MaximumLikelihoodForDBN.setParallelMode(true);

            Stopwatch watch = Stopwatch.createStarted();

            //data.stream().forEach(d -> System.out.println(d.getValue(varA) + ", "+ d.getValue(varB)));


            DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dynamicNB.getDynamicDAG(), data);

            System.out.println(watch.stop());
            System.out.println();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + bnet.getConditionalDistributionTimeT(var));
                assertTrue(bnet.getConditionalDistributionTimeT(var).equalDist(dynamicNB.getConditionalDistributionTimeT(var), 0.05));
            }


            StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
            svb.setWindowsSize(500);
            svb.setSeed(5);
            VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

            BayesianLearningEngineForDBN.setDynamicDAG(dynamicNB.getDynamicDAG());
            BayesianLearningEngineForDBN.setDataStream(data);
            BayesianLearningEngineForDBN.runLearning();

            //System.out.println(svb.getPlateuVMPDBN().toStringTimeT());

            DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + learnDBN.getConditionalDistributionTimeT(var));
                assertTrue(dynamicNB.getConditionalDistributionTimeT(var).equalDist(learnDBN.getConditionalDistributionTimeT(var), 0.05));
            }

        }
    }


    @Test
    public void testMultinomialChain2() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 10; i++) {


            DynamicVariables dynamicVariables = DynamicModelFactory.newDynamicVariables();

            Variable varA = dynamicVariables.newMultinomialDynamicVariable("A",2);
            Variable varB = dynamicVariables.newMultinomialDynamicVariable("B",2);
            //Variable varC = dynamicVariables.newMultinomialDynamicVariable("C",2);

            //Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            //Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            //Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = DynamicModelFactory.newDynamicDAG(dynamicVariables);

            //dynamicDAG.getParentSetTimeT(varA).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varB).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varC).addParent(classVar);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            dynamicDAG.getParentSetTimeT(varB).addParent(varA);



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicModelFactory.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i));

            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setSeed(0);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(1, 10000);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning
            MaximumLikelihoodForDBN.setBatchSize(1000);
            MaximumLikelihoodForDBN.setParallelMode(true);

            Stopwatch watch = Stopwatch.createStarted();

            //data.stream().forEach(d -> System.out.println(d.getValue(varA) + ", "+ d.getValue(varB)));


            DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dynamicNB.getDynamicDAG(), data);

            System.out.println(watch.stop());
            System.out.println();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + bnet.getConditionalDistributionTimeT(var));
                assertTrue(bnet.getConditionalDistributionTimeT(var).equalDist(dynamicNB.getConditionalDistributionTimeT(var), 0.05));
            }


            StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
            svb.setWindowsSize(500);
            svb.setSeed(5);
            VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

            BayesianLearningEngineForDBN.setDynamicDAG(dynamicNB.getDynamicDAG());
            BayesianLearningEngineForDBN.setDataStream(data);
            BayesianLearningEngineForDBN.runLearning();

            //System.out.println(svb.getPlateuVMPDBN().toStringTimeT());

            DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + learnDBN.getConditionalDistributionTimeT(var));
                assertTrue(dynamicNB.getConditionalDistributionTimeT(var).equalDist(learnDBN.getConditionalDistributionTimeT(var), 0.05));
            }

        }
    }


    @Test
    public void testMultinomialChain3() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 5; i++) {


            DynamicVariables dynamicVariables = DynamicModelFactory.newDynamicVariables();

            Variable varA = dynamicVariables.newMultinomialDynamicVariable("A",2);
            Variable varB = dynamicVariables.newMultinomialDynamicVariable("B",2);
            //Variable varC = dynamicVariables.newMultinomialDynamicVariable("C",2);

            //Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            //Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            //Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = DynamicModelFactory.newDynamicDAG(dynamicVariables);

            //dynamicDAG.getParentSetTimeT(varA).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varB).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varC).addParent(classVar);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            dynamicDAG.getParentSetTimeT(varB).addParent(varA);



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicModelFactory.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i));

            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setMARVar(varA, 0.4);
            sampler.setSeed(0);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(10, 10000);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning
            MaximumLikelihoodForDBN.setBatchSize(1000);
            MaximumLikelihoodForDBN.setParallelMode(true);


            //data.stream().forEach(d -> System.out.println(d.getValue(varA) + ", "+ d.getValue(varB)));



            StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
            svb.setWindowsSize(1000);
            svb.setSeed(5);
            VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
            vmp.setOutput(true);
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

            BayesianLearningEngineForDBN.setDynamicDAG(dynamicNB.getDynamicDAG());
            BayesianLearningEngineForDBN.setDataStream(data);
            BayesianLearningEngineForDBN.runLearning();

            //System.out.println(svb.getPlateuVMPDBN().toStringTimeT());

            DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + learnDBN.getConditionalDistributionTimeT(var));
                assertTrue(dynamicNB.getConditionalDistributionTimeT(var).equalDist(learnDBN.getConditionalDistributionTimeT(var), 0.1));
            }

        }
    }


    @Test
    public void testMultinomialChain4() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 1; i++) {


            DynamicVariables dynamicVariables = DynamicModelFactory.newDynamicVariables();

            Variable varA = dynamicVariables.newMultinomialDynamicVariable("A",2);
            Variable varB = dynamicVariables.newMultinomialDynamicVariable("B",2);
            //Variable varC = dynamicVariables.newMultinomialDynamicVariable("C",2);

            //Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            //Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            //Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = DynamicModelFactory.newDynamicDAG(dynamicVariables);

            //dynamicDAG.getParentSetTimeT(varA).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varB).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varC).addParent(classVar);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            dynamicDAG.getParentSetTimeT(varB).addParent(varA);



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicModelFactory.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i));

            Multinomial_MultinomialParents distB = dynamicNB.getConditionalDistributionTimeT(varB);
            distB.getMultinomial(0).setProbabilities(new double[]{0.99, 0.01});
            distB.getMultinomial(1).setProbabilities(new double[]{0.01, 0.99});

            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setMARVar(varA, 0.9);
            sampler.setSeed(0);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(1, 10000);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning
            MaximumLikelihoodForDBN.setBatchSize(1000);
            MaximumLikelihoodForDBN.setParallelMode(true);


            //data.stream().forEach(d -> System.out.println(d.getValue(varA) + ", "+ d.getValue(varB)));



            StreamingVariationalBayesVMPForDBN svb = new StreamingVariationalBayesVMPForDBN();
            svb.setWindowsSize(500);
            svb.setSeed(5);
            VMP vmp = svb.getPlateuVMPDBN().getVMPTimeT();
            vmp.setOutput(true);
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);
            BayesianLearningEngineForDBN.setBayesianLearningAlgorithmForDBN(svb);

            BayesianLearningEngineForDBN.setDynamicDAG(dynamicNB.getDynamicDAG());
            BayesianLearningEngineForDBN.setDataStream(data);
            BayesianLearningEngineForDBN.runLearning();

            //System.out.println(svb.getPlateuVMPDBN().toStringTimeT());

            DynamicBayesianNetwork learnDBN = BayesianLearningEngineForDBN.getLearntDBN();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + learnDBN.getConditionalDistributionTimeT(var));
                assertTrue(dynamicNB.getConditionalDistributionTimeT(var).equalDist(learnDBN.getConditionalDistributionTimeT(var), 0.05));
            }

        }
    }
}