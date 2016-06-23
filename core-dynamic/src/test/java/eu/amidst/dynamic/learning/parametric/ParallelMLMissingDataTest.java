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
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

/**
 * Created by andresmasegosa on 20/5/16.
 */
public class ParallelMLMissingDataTest extends TestCase {

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
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(0),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(1),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(2),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(3),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(4),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(5),0.1);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(10000,2);


        //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
        // and just apply then test parameter learning

        //Parameter Learning

        Stopwatch watch = Stopwatch.createStarted();

        ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMLMissingData();
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
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(0),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(1),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(2),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(3),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(4),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(5),0.1);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(10,1000);


        //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
        // and just apply then test parameter learning

        //Parameter Learning
        Stopwatch watch = Stopwatch.createStarted();

        ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMLMissingData();
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
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(0),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(1),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(2),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(3),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(4),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(5),0.1);
        sampler.setMARVar(dynamicNB.getDynamicVariables().getVariableById(6),0.1);

        sampler.setSeed(0);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(10000,2);


        //Structure learning is excluded from the test, i.e., we use directly the initial Dynamic Naive Bayes network structure
        // and just apply then test parameter learning

        //Parameter Learning

        Stopwatch watch = Stopwatch.createStarted();

        ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMLMissingData();
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

}