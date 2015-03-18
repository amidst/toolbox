package eu.amidst.core.learning;


import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.learning.dynamic.MaximumLikelihoodForBN;
import eu.amidst.core.learning.dynamic.MaximumLikelihoodForDBN;
import eu.amidst.core.models.*;
import eu.amidst.core.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.core.utils.DynamicBayesianNetworkSampler;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Created by Hanen on 27/01/15.
 */
public class MLDBNTest {

    @Test
    public void testingMLforDBN1() throws IOException, ClassNotFoundException {

        //Generate a dynamic Naive Bayes with only Multinomial variables
        DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();

        //Set the number of Discrete variables, their number of states, the number of Continuous variables
        dbnGenerator.setNumberOfContinuousVars(0);
        dbnGenerator.setNumberOfDiscreteVars(5);
        dbnGenerator.setNumberOfStates(2);

        //The number of states for the class variable is equal to 2
        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        System.out.println(dynamicNB.getDynamicDAG().toString());
        System.out.println(dynamicNB.toString());

        //Sampling from the generated Dynamic NB
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
        sampler.setSeed(0);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(10000,2);


        //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
        // and just apply then test parameter learning

        //Parameter Learning
        MaximumLikelihoodForDBN.setBatchSize(1000);
        MaximumLikelihoodForDBN.setParallelMode(true);

        Stopwatch watch = Stopwatch.createStarted();

        DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dynamicNB.getDynamicDAG(), data);

        System.out.println(watch.stop());
        System.out.println();

        //Check if the probability distributions of each node over both time 0 and T
        for (Variable var : dynamicNB.getDynamicVariables()) {
            System.out.println("\n---------- Variable " + var.getName() + " -----------");
            // time 0
            System.out.println("\nTrue distribution at time 0:\n" + dynamicNB.getDistributionTime0(var));
            System.out.println("\nLearned distribution at time 0:\n"+ bnet.getDistributionTime0(var));
            assertTrue(bnet.getDistributionTime0(var).equalDist(dynamicNB.getDistributionTime0(var), 0.05));
        }
    }


    @Test
    public void testingMLforDBN2() throws IOException, ClassNotFoundException {

        //Generate a dynamic Naive Bayes with only Multinomial variables
        DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();

        //Set the number of Discrete variables, their number of states, the number of Continuous variables
        dbnGenerator.setNumberOfContinuousVars(0);
        dbnGenerator.setNumberOfDiscreteVars(5);
        dbnGenerator.setNumberOfStates(2);

        //The number of states for the class variable is equal to 2
        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        System.out.println(dynamicNB.getDynamicDAG().toString());
        System.out.println(dynamicNB.toString());

        //Sampling from the generated Dynamic NB
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
        sampler.setSeed(0);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(10,1000);


        //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
        // and just apply then test parameter learning

        //Parameter Learning
        MaximumLikelihoodForDBN.setBatchSize(1000);
        MaximumLikelihoodForDBN.setParallelMode(true);

        Stopwatch watch = Stopwatch.createStarted();

        DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dynamicNB.getDynamicDAG(), data);

        System.out.println(watch.stop());
        System.out.println();

        //Check if the probability distributions of each node over both time 0 and T
        for (Variable var : dynamicNB.getDynamicVariables()) {
            System.out.println("\n---------- Variable " + var.getName() + " -----------");
            // time T
            System.out.println("\nTrue distribution at time T:\n"+ dynamicNB.getDistributionTimeT(var));
            System.out.println("\nLearned distribution at time T:\n"+ bnet.getDistributionTimeT(var));
            assertTrue(bnet.getDistributionTimeT(var).equalDist(dynamicNB.getDistributionTimeT(var), 0.05));
        }
    }


    @Test
    public void testingMLforDBN3() throws IOException, ClassNotFoundException {

        //Generate a dynamic Naive Bayes with only Multinomial variables
        DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();

        //Set the number of Discrete variables, their number of states, the number of Continuous variables
        dbnGenerator.setNumberOfContinuousVars(5);
        dbnGenerator.setNumberOfDiscreteVars(5);
        dbnGenerator.setNumberOfStates(2);

        //The number of states for the class variable is equal to 2
        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        System.out.println(dynamicNB.getDynamicDAG().toString());
        System.out.println(dynamicNB.toString());

        //Sampling from the generated Dynamic NB
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
        sampler.setSeed(0);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(10000,2);


        //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
        // and just apply then test parameter learning

        //Parameter Learning
        MaximumLikelihoodForDBN.setBatchSize(1000);
        MaximumLikelihoodForDBN.setParallelMode(true);

        Stopwatch watch = Stopwatch.createStarted();

        DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dynamicNB.getDynamicDAG(), data);

        System.out.println(watch.stop());
        System.out.println();

        //Check if the probability distributions of each node over both time 0 and T
        for (Variable var : dynamicNB.getDynamicVariables()) {
            System.out.println("\n---------- Variable " + var.getName() + " -----------");
            // time 0
            System.out.println("\nTrue distribution at time 0:\n" + dynamicNB.getDistributionTime0(var));
            System.out.println("\nLearned distribution at time 0:\n"+ bnet.getDistributionTime0(var));
            assertTrue(bnet.getDistributionTime0(var).equalDist(dynamicNB.getDistributionTime0(var), 0.5));
        }
    }
    @Test
    public void testingMLforDBN4() throws IOException, ClassNotFoundException {

        //Generate a dynamic Naive Bayes with only Multinomial variables
        DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();

        //Set the number of Discrete variables, their number of states, the number of Continuous variables
        dbnGenerator.setNumberOfContinuousVars(2);
        dbnGenerator.setNumberOfDiscreteVars(0);
        dbnGenerator.setNumberOfStates(2);

        //The number of states for the class variable is equal to 2
        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(1), 2, true);

        dynamicNB.randomInitialization(new Random(1));

        System.out.println(dynamicNB.getDynamicDAG().toString());
        System.out.println(dynamicNB.toString());

        //Sampling from the generated Dynamic NB
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
        sampler.setSeed(2);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(1000,10);


        //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
        // and just apply then test parameter learning

        //Parameter Learning
        MaximumLikelihoodForDBN.setBatchSize(1000);
        MaximumLikelihoodForDBN.setParallelMode(true);

        Stopwatch watch = Stopwatch.createStarted();

        DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dynamicNB.getDynamicDAG(), data);

        System.out.println(watch.stop());
        System.out.println();

        //Check if the probability distributions of each node over both time 0 and T
        for (Variable var : dynamicNB.getDynamicVariables()) {
            System.out.println("\n---------- Variable " + var.getName() + " -----------");
            // time T
            System.out.println("\nTrue distribution at time T:\n"+ dynamicNB.getDistributionTimeT(var));
            System.out.println("\nLearned distribution at time T:\n"+ bnet.getDistributionTimeT(var));
            assertTrue(bnet.getDistributionTimeT(var).equalDist(dynamicNB.getDistributionTimeT(var), 0.5));
        }
    }


    @Test
    public void testingMLforDBN5() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 10; i++) {


            DynamicVariables dynamicVariables = new DynamicVariables();

            Variable classVar = dynamicVariables.newMultinomialDynamicVariable("Class", 2);
            Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

            dynamicDAG.getParentSetTimeT(varA).addParent(classVar);
            dynamicDAG.getParentSetTimeT(varB).addParent(classVar);
            dynamicDAG.getParentSetTimeT(varC).addParent(classVar);

            dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            //dynamicDAG.getParentSetTimeT(varB).addParent(varC);


            //dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getTemporalClone(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getTemporalClone(varB));

            dynamicDAG.getParentSetTimeT(classVar).addParent(dynamicVariables.getTemporalClone(classVar));

            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i));

            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setSeed(2);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(100, 10);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning
            MaximumLikelihoodForDBN.setBatchSize(1000);
            MaximumLikelihoodForDBN.setParallelMode(true);

            Stopwatch watch = Stopwatch.createStarted();

            DynamicBayesianNetwork bnet = MaximumLikelihoodForDBN.learnDynamic(dynamicNB.getDynamicDAG(), data);

            System.out.println(watch.stop());
            System.out.println();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + bnet.getDistributionTimeT(var));
                assertTrue(bnet.getDistributionTimeT(var).equalDist(dynamicNB.getDistributionTimeT(var), 0.5));
            }
        }
    }


    @Test
    public void testingMLforDBN6() throws IOException, ClassNotFoundException {

        for (int i = 0; i < 10; i++) {


            DynamicVariables dynamicVariables = new DynamicVariables();

            Variable varA = dynamicVariables.newGaussianDynamicVariable("A");
            Variable varB = dynamicVariables.newGaussianDynamicVariable("B");
            Variable varC = dynamicVariables.newGaussianDynamicVariable("C");

            DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

            //dynamicDAG.getParentSetTimeT(varA).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varB).addParent(classVar);
            //dynamicDAG.getParentSetTimeT(varC).addParent(classVar);

            //dynamicDAG.getParentSetTimeT(varA).addParent(varB);
            //dynamicDAG.getParentSetTimeT(varA).addParent(varC);
            //dynamicDAG.getParentSetTimeT(varB).addParent(varC);



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getTemporalClone(varA));
            dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getTemporalClone(varB));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i));


            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setSeed(2);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(100, 10);


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
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + bnet.getDistributionTimeT(var));
                assertTrue(bnet.getDistributionTimeT(var).equalDist(dynamicNB.getDistributionTimeT(var), 0.5));
            }
        }
    }

}
