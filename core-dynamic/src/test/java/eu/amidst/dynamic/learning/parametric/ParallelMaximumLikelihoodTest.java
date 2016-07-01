/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.dynamic.learning.parametric;


import com.google.common.base.Stopwatch;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import eu.amidst.dynamic.variables.DynamicVariables;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

/**
 * Created by Hanen on 27/01/15.
 */
public class ParallelMaximumLikelihoodTest {

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

        Stopwatch watch = Stopwatch.createStarted();

        ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setWindowsSize(1000);
        parallelMaximumLikelihood.setDynamicDAG(dynamicNB.getDynamicDAG());
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(data);

        DynamicBayesianNetwork bnet = parallelMaximumLikelihood.getLearntDBN();

        System.out.println(watch.stop());
        System.out.println();

        //Check if the probability distributions of each node over both time 0 and T
        for (Variable var : dynamicNB.getDynamicVariables()) {
            System.out.println("\n---------- Variable " + var.getName() + " -----------");
            // time 0
            System.out.println("\nTrue distribution at time 0:\n" + dynamicNB.getConditionalDistributionTime0(var));
            System.out.println("\nLearned distribution at time 0:\n"+ bnet.getConditionalDistributionTime0(var));
            Assert.assertTrue(bnet.getConditionalDistributionTime0(var).equalDist(dynamicNB.getConditionalDistributionTime0(var), 0.05));
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
           Stopwatch watch = Stopwatch.createStarted();

        ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setWindowsSize(1000);
        parallelMaximumLikelihood.setDynamicDAG(dynamicNB.getDynamicDAG());
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(data);

        DynamicBayesianNetwork bnet = parallelMaximumLikelihood.getLearntDBN();

        System.out.println(watch.stop());
        System.out.println();

        //Check if the probability distributions of each node over both time 0 and T
        for (Variable var : dynamicNB.getDynamicVariables()) {
            System.out.println("\n---------- Variable " + var.getName() + " -----------");
            // time T
            System.out.println("\nTrue distribution at time T:\n"+ dynamicNB.getConditionalDistributionTimeT(var));
            System.out.println("\nLearned distribution at time T:\n"+ bnet.getConditionalDistributionTimeT(var));
            Assert.assertTrue(bnet.getConditionalDistributionTimeT(var).equalDist(dynamicNB.getConditionalDistributionTimeT(var), 0.05));
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

        Stopwatch watch = Stopwatch.createStarted();

        ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setWindowsSize(1000);
        parallelMaximumLikelihood.setDynamicDAG(dynamicNB.getDynamicDAG());
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(data);

        DynamicBayesianNetwork bnet = parallelMaximumLikelihood.getLearntDBN();

        System.out.println(watch.stop());
        System.out.println();

        //Check if the probability distributions of each node over both time 0 and T
        for (Variable var : dynamicNB.getDynamicVariables()) {
            System.out.println("\n---------- Variable " + var.getName() + " -----------");
            // time 0
            System.out.println("\nTrue distribution at time 0:\n" + dynamicNB.getConditionalDistributionTime0(var));
            System.out.println("\nLearned distribution at time 0:\n"+ bnet.getConditionalDistributionTime0(var));
            Assert.assertTrue(bnet.getConditionalDistributionTime0(var).equalDist(dynamicNB.getConditionalDistributionTime0(var), 0.5));
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


        Variable classVar = dynamicNB.getDynamicDAG().getDynamicVariables().getVariableByName("ClassVar");

        Variable var1 = dynamicNB.getDynamicDAG().getDynamicVariables().getVariableByName("ContinuousVar1");
        Variable var1_interface = dynamicNB.getDynamicDAG().getDynamicVariables().getInterfaceVariableByName("ContinuousVar1");
        ConditionalLinearGaussian distContVar1Class0 = ((Normal_MultinomialNormalParents)dynamicNB.getConditionalDistributionTimeT(var1)).getNormal_NormalParentsDistribution(0);
        distContVar1Class0.setCoeffForParent(var1_interface, 1.3);
        ConditionalLinearGaussian distContVar1Class1 = ((Normal_MultinomialNormalParents)dynamicNB.
                getConditionalDistributionTimeT(dynamicNB.getDynamicDAG().getDynamicVariables().getVariableByName("ContinuousVar1"))).getNormal_NormalParentsDistribution(1);
        //distContVar1Class1.setCoeffParents(new double[]{-1.3});
        distContVar1Class1.setCoeffForParent(var1_interface, -1.3);

        Variable var2 = dynamicNB.getDynamicDAG().getDynamicVariables().getVariableByName("ContinuousVar2");
        Variable var2_interface = dynamicNB.getDynamicDAG().getDynamicVariables().getInterfaceVariableByName("ContinuousVar2");
        ConditionalLinearGaussian distContVar2Class0 = ((Normal_MultinomialNormalParents)dynamicNB.
                getConditionalDistributionTimeT(var2)).getNormal_NormalParentsDistribution(0);
        //distContVar2Class0.setCoeffParents(new double[]{2.3});
        distContVar2Class0.setCoeffForParent(var2_interface, 2.3);
        ConditionalLinearGaussian distContVar2Class1 = ((Normal_MultinomialNormalParents)dynamicNB.
                getConditionalDistributionTimeT(var2)).getNormal_NormalParentsDistribution(1);
        //distContVar2Class1.setCoeffParents(new double[]{-2.3});
        distContVar2Class1.setCoeffForParent(var2_interface,-2.3);

        System.out.println(dynamicNB.getDynamicDAG().toString());
        System.out.println(dynamicNB.toString());

        //Sampling from the generated Dynamic NB
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
        sampler.setSeed(2);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(10000,10);


        //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
        // and just apply then test parameter learning

        //Parameter Learning
        Stopwatch watch = Stopwatch.createStarted();

        ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setWindowsSize(1000);
        parallelMaximumLikelihood.setDynamicDAG(dynamicNB.getDynamicDAG());
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(data);

        DynamicBayesianNetwork bnet = parallelMaximumLikelihood.getLearntDBN();

        System.out.println(watch.stop());
        System.out.println();

        //Check if the probability distributions of each node over both time 0 and T
        for (Variable var : dynamicNB.getDynamicVariables()) {
            System.out.println("\n---------- Variable " + var.getName() + " -----------");
            // time T
            System.out.println("\nTrue distribution at time T:\n"+ dynamicNB.getConditionalDistributionTimeT(var));
            System.out.println("\nLearned distribution at time T:\n"+ bnet.getConditionalDistributionTimeT(var));
            Assert.assertTrue(bnet.getConditionalDistributionTimeT(var).equalDist(dynamicNB.getConditionalDistributionTimeT(var), 0.5));
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


            //dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            //dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));

            dynamicDAG.getParentSetTimeT(classVar).addParent(dynamicVariables.getInterfaceVariable(classVar));

            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB =  new DynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i));

/*            ConditionalLinearGaussian distVarAClass0 = (ConditionalLinearGaussian)((BaseDistribution_MultinomialParents)dynamicNB.
                    getConditionalDistributionTimeT(varA)).getBaseConditionalDistribution(0);
            distVarAClass0.setIntercept(1.0);
            distVarAClass0.setCoeffParents(new double[]{1.3, -3.1});
            ConditionalLinearGaussian distVarAClass1 = (ConditionalLinearGaussian)((BaseDistribution_MultinomialParents)dynamicNB.
                    getConditionalDistributionTimeT(varA)).getBaseConditionalDistribution(1);
            distVarAClass1.setIntercept(3.2);
            distVarAClass1.setCoeffParents(new double[]{-2.3, 1.1});*/

            System.out.println(dynamicNB.getDynamicDAG().toString());
            System.out.println(dynamicNB.toString());


            //Sampling from the generated Dynamic NB
            DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
            sampler.setSeed(2);

            //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

            DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(1000, 10);


            //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
            // and just apply then test parameter learning

            //Parameter Learning

            Stopwatch watch = Stopwatch.createStarted();
            ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            parallelMaximumLikelihood.setWindowsSize(1000);
            parallelMaximumLikelihood.setDynamicDAG(dynamicNB.getDynamicDAG());
            parallelMaximumLikelihood.initLearning();
            parallelMaximumLikelihood.updateModel(data);

            DynamicBayesianNetwork bnet = parallelMaximumLikelihood.getLearntDBN();

            System.out.println(watch.stop());
            System.out.println();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + bnet.getConditionalDistributionTimeT(var));
                Assert.assertTrue(bnet.getConditionalDistributionTimeT(var).equalDist(dynamicNB.getConditionalDistributionTimeT(var), 0.5));
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



            dynamicDAG.getParentSetTimeT(varA).addParent(dynamicVariables.getInterfaceVariable(varA));
            dynamicDAG.getParentSetTimeT(varB).addParent(dynamicVariables.getInterfaceVariable(varB));


            //The number of states for the class variable is equal to 2
            DynamicBayesianNetwork dynamicNB = new DynamicBayesianNetwork(dynamicDAG);

            dynamicNB.randomInitialization(new Random(i));

            ConditionalLinearGaussian distVarA = dynamicNB.getConditionalDistributionTimeT(varA);
            distVarA.setCoeffForParent(dynamicVariables.getInterfaceVariable(varA),1.3);
            ConditionalLinearGaussian distVarB = dynamicNB.getConditionalDistributionTimeT(varB);
            distVarB.setCoeffForParent(dynamicVariables.getInterfaceVariable(varB),-2.3);


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

            Stopwatch watch = Stopwatch.createStarted();

            //data.stream().forEach(d -> System.out.println(d.getValue(varA) + ", "+ d.getValue(varB)));


            ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            parallelMaximumLikelihood.setWindowsSize(1000);
            parallelMaximumLikelihood.setDynamicDAG(dynamicNB.getDynamicDAG());
            parallelMaximumLikelihood.initLearning();
            parallelMaximumLikelihood.updateModel(data);

            DynamicBayesianNetwork bnet = parallelMaximumLikelihood.getLearntDBN();

            System.out.println(watch.stop());
            System.out.println();

            //Check if the probability distributions of each node over both time 0 and T
            for (Variable var : dynamicNB.getDynamicVariables()) {
                System.out.println("\n---------- Variable " + var.getName() + " -----------");
                // time T
                System.out.println("\nTrue distribution at time T:\n" + dynamicNB.getConditionalDistributionTimeT(var));
                System.out.println("\nLearned distribution at time T:\n" + bnet.getConditionalDistributionTimeT(var));
                Assert.assertTrue(bnet.getConditionalDistributionTimeT(var).equalDist(dynamicNB.getConditionalDistributionTimeT(var), 0.5));
            }
        }
    }

}
