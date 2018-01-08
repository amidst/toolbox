/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.learning;

import eu.amidst.core.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class SVBTest extends TestCase {

    public static void testMultinomials1() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newMultinomialVariable("A", 2);

        DAG dag = new DAG(variables);


        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);

        distA.setProbabilities(new double[]{0.6, 0.4});

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataStream(1000);


        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setWindowsSize(1000);
        parallelMaximumLikelihood.setParallelMode(true);
        parallelMaximumLikelihood.setLaplace(false);

        parallelMaximumLikelihood.setDAG(bn.getDAG());
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(data);
        BayesianNetwork learntNormalVarBN = parallelMaximumLikelihood.getLearntBayesianNetwork();


        if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());

        SVB svb = new SVB();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);
        

        svb.setDAG(bn.getDAG());
        svb.setDataStream(data);
        svb.runLearning();

        BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bn.toString());
        if (Main.VERBOSE) System.out.println(learnBN.toString());
        assertTrue(bn.equalBNs(learnBN, 0.05));
    }

    public static void testMultinomials2() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newMultinomialVariable("A", 2);
        Variable varB = variables.newMultinomialVariable("B", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getConditionalDistribution(varB);

        distA.setProbabilities(new double[]{0.6, 0.4});
        distB.getMultinomial(0).setProbabilities(new double[]{0.75, 0.25});
        distB.getMultinomial(1).setProbabilities(new double[]{0.25, 0.75});

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataStream(1000);


        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setWindowsSize(1000);
        parallelMaximumLikelihood.setParallelMode(true);
        parallelMaximumLikelihood.setLaplace(false);

        parallelMaximumLikelihood.setDAG(bn.getDAG());
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(data);
        BayesianNetwork learntNormalVarBN = parallelMaximumLikelihood.getLearntBayesianNetwork();


        SVB svb = new SVB();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(bn.getDAG());
        svb.setDataStream(data);
        svb.runLearning();

        BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bn.toString());
        if (Main.VERBOSE) System.out.println(learnBN.toString());
        assertTrue(bn.equalBNs(learnBN, 0.05));
    }

     public static void testMultinomials5() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newMultinomialVariable("A", 5);
        Variable varB = variables.newMultinomialVariable("B", 5);
        Variable varC = variables.newMultinomialVariable("C", 5);

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varB);
        dag.getParentSet(varB).addParent(varA);



        BayesianNetwork bn = new BayesianNetwork(dag);

        bn.randomInitialization(new Random(5));

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(5);
        sampler.setHiddenVar(varB);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


        SVB svb = new SVB();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(bn.getDAG());
        svb.setDataStream(data);
        svb.runLearning();

        BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bn.toString());
        if (Main.VERBOSE) System.out.println(learnBN.toString());
        //assertTrue(bn.equalBNs(learnBN, 0.1));

    }

    public static void testMultinomial6() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varB = variables.newMultinomialVariable("B",4);

        DAG dag = new DAG(variables);


        for (int i = 0; i < 10; i++) {


            BayesianNetwork bn = new BayesianNetwork(dag);
            bn.randomInitialization(new Random(0));


            bn.randomInitialization(new Random(0));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i+299);
            sampler.setHiddenVar(varB);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


            SVB svb = new SVB();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(bn.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(bn.toString());
            if (Main.VERBOSE) System.out.println(learnBN.toString());
            //assertTrue(bn.equalBNs(learnBN, 0.5));
        }
    }

    public static void testAsia() throws IOException, ClassNotFoundException{

        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("../networks/dataWeka/asia.bn");
        asianet.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println("\nAsia network \n ");

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        SVB svb = new SVB();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(asianet.getDAG());
        svb.setDataStream(data);
        svb.runLearning();

        if (Main.VERBOSE) System.out.println(svb.getLogMarginalProbability());

        BayesianNetwork learnAsianet = svb.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(asianet.toString());
        if (Main.VERBOSE) System.out.println(learnAsianet.toString());
        assertTrue(asianet.equalBNs(learnAsianet, 0.05));

    }

    public static void testAsia2() throws IOException, ClassNotFoundException{

        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("../networks/dataWeka/asia.bn");
        if (Main.VERBOSE) System.out.println(asianet.toString());
        asianet.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println("\nAsia network \n ");

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setHiddenVar(asianet.getVariables().getVariableByName("E"));
        sampler.setHiddenVar(asianet.getVariables().getVariableByName("L"));

        //sampler.setMARVar(asianet.getVariables().getVariableByName("E"),0.5);
        //sampler.setMARVar(asianet.getVariables().getVariableByName("L"),0.5);

        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        SVB svb = new SVB();
        svb.setWindowsSize(100);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(asianet.getDAG());
        svb.setDataStream(data);
        svb.runLearning();

        if (Main.VERBOSE) System.out.println(svb.getLogMarginalProbability());

        BayesianNetwork learnAsianet = svb.getLearntBayesianNetwork();

        //if (Main.VERBOSE) System.out.println(asianet.outputString());
        //if (Main.VERBOSE) System.out.println(learnAsianet.outputString());
        //assertTrue(asianet.equalBNs(learnAsianet,0.05));

    }

    public static void testGaussian0() throws IOException, ClassNotFoundException{

        BayesianNetwork oneNormalVarBN = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal.bn");

        for (int i = 0; i < 1; i++) {

            Variable varA = oneNormalVarBN.getVariables().getVariableByName("A");
            Normal dist = oneNormalVarBN.getConditionalDistribution(varA);

            dist.setMean(2000);
            dist.setVariance(30);

            oneNormalVarBN.randomInitialization(new Random(i));

            if (Main.VERBOSE) System.out.println("\nOne normal variable network \n ");

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(oneNormalVarBN);
            sampler.setSeed(0);

            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            parallelMaximumLikelihood.setWindowsSize(1000);
            parallelMaximumLikelihood.setParallelMode(true);
            parallelMaximumLikelihood.setLaplace(false);

            parallelMaximumLikelihood.setDAG(oneNormalVarBN.getDAG());
            parallelMaximumLikelihood.initLearning();
            parallelMaximumLikelihood.updateModel(data);
            BayesianNetwork learntNormalVarBN = parallelMaximumLikelihood.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());

            SVB svb = new SVB();
            svb.setWindowsSize(1);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setOutput(false);
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(oneNormalVarBN.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            if (Main.VERBOSE) System.out.println(svb.getLogMarginalProbability());

            BayesianNetwork learntOneNormalVarBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(oneNormalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntOneNormalVarBN.toString());
            assertTrue(oneNormalVarBN.equalBNs(learntOneNormalVarBN, 0.1));
        }
    }

    public static void testWasteIncinerator() throws IOException, ClassNotFoundException{

        String[] bns = {"../networks/simulated/Normal.bn",
                "../networks/simulated/Normal_1NormalParents.bn",
                "../networks/simulated/Normal_NormalParents.bn",
                "../networks/simulated/Normal_MultinomialParents.bn",
                "../networks/simulated/WasteIncinerator.bn"
        };


        for (int i = 0; i < bns.length; i++) {
            if (Main.VERBOSE) System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            if (Main.VERBOSE) System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + bns[i] + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            if (Main.VERBOSE) System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile(bns[i]);
            //normalVarBN.randomInitialization(new Random(0));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(1);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


            ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            parallelMaximumLikelihood.setWindowsSize(1000);
            parallelMaximumLikelihood.setParallelMode(true);
            parallelMaximumLikelihood.setLaplace(false);

            parallelMaximumLikelihood.setDAG(normalVarBN.getDAG());
            parallelMaximumLikelihood.initLearning();
            parallelMaximumLikelihood.updateModel(data);
            BayesianNetwork learntNormalVarBN = parallelMaximumLikelihood.getLearntBayesianNetwork();


            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.2));

            SVB svb = new SVB();
            svb.setWindowsSize(1000);
            svb.setSeed(5);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.001);

            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            if (Main.VERBOSE) System.out.println(svb.getLogMarginalProbability());

            learntNormalVarBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.2));
        }
    }

    public static void testGaussian1() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal_1NormalParents.bn");

        for (int i = 0; i < 1; i++) {

            if (Main.VERBOSE) System.out.println("\nNormal|Normal variable network \n ");

            normalVarBN.randomInitialization(new Random(i));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(2);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            parallelMaximumLikelihood.setWindowsSize(1000);
            parallelMaximumLikelihood.setParallelMode(true);
            parallelMaximumLikelihood.setLaplace(false);

            parallelMaximumLikelihood.setDAG(normalVarBN.getDAG());
            parallelMaximumLikelihood.initLearning();
            parallelMaximumLikelihood.updateModel(data);
            BayesianNetwork learntNormalVarBN = parallelMaximumLikelihood.getLearntBayesianNetwork();


            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.2));

            SVB svb = new SVB();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setOutput(false);
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);
            svb.runLearning();



            if (Main.VERBOSE) System.out.println(svb.getLogMarginalProbability());
            learntNormalVarBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.3));
        }

    }

    public static void testGaussian2() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal_NormalParents.bn");

        int cont=0;
        for (int i = 0; i < 10; i++) {

            normalVarBN.randomInitialization(new Random(i));
            if (Main.VERBOSE) System.out.println("\nNormal|2Normal variable network \n ");


            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(0);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            parallelMaximumLikelihood.setWindowsSize(1000);
            parallelMaximumLikelihood.setParallelMode(true);
            parallelMaximumLikelihood.setLaplace(false);

            parallelMaximumLikelihood.setDAG(normalVarBN.getDAG());
            parallelMaximumLikelihood.initLearning();
            parallelMaximumLikelihood.updateModel(data);
            BayesianNetwork learntNormalVarBN = parallelMaximumLikelihood.getLearntBayesianNetwork();


            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.2));


            SVB svb = new SVB();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.000001);

            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            learntNormalVarBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.25));
        }
    }

    public static void testGaussian3() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal_MultinomialParents.bn");

        for (int i = 0; i < 10; i++) {


            normalVarBN.randomInitialization(new Random(i));
            if (Main.VERBOSE) System.out.println("\nNormal|2Normal variable network \n ");

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(0);
            DataStream<DataInstance> data = sampler.sampleToDataStream(20000);

            ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            parallelMaximumLikelihood.setWindowsSize(1000);
            parallelMaximumLikelihood.setParallelMode(true);
            parallelMaximumLikelihood.setLaplace(false);

            parallelMaximumLikelihood.setDAG(normalVarBN.getDAG());
            parallelMaximumLikelihood.initLearning();
            parallelMaximumLikelihood.updateModel(data);
            BayesianNetwork learntNormalVarBN = parallelMaximumLikelihood.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.6));

            SVB svb = new SVB();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            learntNormalVarBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.6));
        }
    }

    public static void testGaussian4() throws IOException, ClassNotFoundException{

        BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal_MultinomialNormalParents.bn");
        int contA=0;
        int contB=0;

        for (int i = 1; i < 2; i++) {

            normalVarBN.randomInitialization(new Random(i));
            if (Main.VERBOSE) System.out.println("\nNormal|2Normal variable network \n ");


            BayesianNetworkSampler sampler = new BayesianNetworkSampler(normalVarBN);
            sampler.setSeed(0);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            parallelMaximumLikelihood.setWindowsSize(1000);
            parallelMaximumLikelihood.setParallelMode(true);
            parallelMaximumLikelihood.setLaplace(false);

            parallelMaximumLikelihood.setDAG(normalVarBN.getDAG());
            parallelMaximumLikelihood.initLearning();
            parallelMaximumLikelihood.updateModel(data);
            BayesianNetwork learntNormalVarBN = parallelMaximumLikelihood.getLearntBayesianNetwork();


            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.3));
            if (normalVarBN.equalBNs(learntNormalVarBN, 0.3)) contA++;

            SVB svb = new SVB();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(normalVarBN.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            learntNormalVarBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.5));
            if (normalVarBN.equalBNs(learntNormalVarBN, 0.5)) contB++;
        }
        if (Main.VERBOSE) System.out.println(contA);
        if (Main.VERBOSE) System.out.println(contB);

    }

    public static void testGaussian5() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varA);


        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(0));

        if (Main.VERBOSE) System.out.println(bn.toString());
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        sampler.setHiddenVar(varA);
        DataStream<DataInstance> data = sampler.sampleToDataStream(50);


        SVB svb = new SVB();
        svb.setWindowsSize(5); //Set to 2 and an exception is raised. Numerical instability.
        svb.setSeed(1);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.001);

        svb.setDAG(bn.getDAG());
        svb.setDataStream(data);
        svb.runLearning();

        BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bn.toString());
        if (Main.VERBOSE) System.out.println(learnBN.toString());
        //assertTrue(bn.equalBNs(learnBN,0.1));
    }

    public static void testGaussian6() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        for (int i = 0; i < 1; i++) {


            BayesianNetwork bn = new BayesianNetwork(dag);

            bn.randomInitialization(new Random(i));

            if (Main.VERBOSE) System.out.println(bn.toString());
            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i);
            sampler.setMARVar(varB, 0.8);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


            SVB svb = new SVB();
            svb.setWindowsSize(1000);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(bn.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            if (Main.VERBOSE) System.out.println("Data Prob: " + svb.getLogMarginalProbability());

            BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(bn.toString());
            if (Main.VERBOSE) System.out.println(learnBN.toString());
            assertTrue(bn.equalBNs(learnBN, 0.3));
        }
    }

    public static void testGaussian7() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varB = variables.newGaussianVariable("B");

        DAG dag = new DAG(variables);


        for (int i = 0; i < 10; i++) {


            BayesianNetwork bn = new BayesianNetwork(dag);
            bn.randomInitialization(new Random(0));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i);
            sampler.setHiddenVar(varB);
            DataStream<DataInstance> data = sampler.sampleToDataStream(1000);


            SVB svb = new SVB();
            svb.setWindowsSize(1000); //Set to 2 and an exception is raised. Numerical instability.
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.01);

            svb.setDAG(bn.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            if (Main.VERBOSE) System.out.println("Data Prob: " + svb.getLogMarginalProbability());


            BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(bn.toString());
            if (Main.VERBOSE) System.out.println(learnBN.toString());
            //assertTrue(bn.equalBNs(learnBN, 0.5));
        }
    }

    public static void testGaussian8() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        for (int i = 1; i < 2; i++) {


            BayesianNetwork bn = new BayesianNetwork(dag);
            bn.randomInitialization(new Random(0));

            bn.randomInitialization(new Random(i));

            //if (Main.VERBOSE) System.out.println(bn.outputString());
            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i);
            sampler.setMARVar(varB,0.7);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


            SVB svb = new SVB();
            svb.setWindowsSize(10);
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.001);

            svb.setDAG(bn.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            if (Main.VERBOSE) System.out.println("Data Prob: " + svb.getLogMarginalProbability());


            BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(bn.toString());
            if (Main.VERBOSE) System.out.println(learnBN.toString());
            ConditionalLinearGaussian distCP = bn.getConditionalDistribution(varC);
            ConditionalLinearGaussian distCQ = learnBN.getConditionalDistribution(varC);

            assertEquals(distCP.getSd(), distCQ.getSd(), 0.1);
        }
    }



    public static void testGaussian9() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varB = variables.newMultinomialVariable("B",2);
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);
        dag.getParentSet(varC).addParent(varB);


        for (int i = 0; i < 10; i++) {


            BayesianNetwork bn = new BayesianNetwork(dag);
            bn.randomInitialization(new Random(0));


            bn.randomInitialization(new Random(0));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i);
            sampler.setHiddenVar(varB);
            DataStream<DataInstance> data = sampler.sampleToDataStream(1000);


            SVB svb = new SVB();
            svb.setWindowsSize(1000); //Set to 2 and an exception is raised. Numerical instability.
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(bn.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            if (Main.VERBOSE) System.out.println("Data Prob: " + svb.getLogMarginalProbability());


            BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(bn.toString());
            if (Main.VERBOSE) System.out.println(learnBN.toString());
            //assertTrue(bn.equalBNs(learnBN, 0.5));
        }
    }

    public static void testGaussian10() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varB = variables.newMultinomialVariable("B",2);
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);
        dag.getParentSet(varC).addParent(varB);


        for (int i = 0; i < 10; i++) {


            BayesianNetwork bn = new BayesianNetwork(dag);
            bn.randomInitialization(new Random(i));



            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


            SVB svb = new SVB();
            svb.setWindowsSize(1000); //Set to 2 and an exception is raised. Numerical instability.
            //svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.0001);

            svb.setDAG(bn.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            if (Main.VERBOSE) System.out.println("Data Prob: " + svb.getLogMarginalProbability());


            BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(bn.toString());
            if (Main.VERBOSE) System.out.println(learnBN.toString());
            assertTrue(bn.equalBNs(learnBN, 0.5));
        }
    }

    public static void testGaussian11() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varB = variables.newGaussianVariable("B");

        DAG dag = new DAG(variables);


        for (int i = 0; i < 10; i++) {


            BayesianNetwork bn = new BayesianNetwork(dag);
            bn.randomInitialization(new Random(0));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(i);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


            SVB svb = new SVB();
            svb.setWindowsSize(1000); //Set to 2 and an exception is raised. Numerical instability.
            svb.setSeed(i);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.01);

            svb.setDAG(bn.getDAG());
            svb.setDataStream(data);
            svb.runLearning();

            if (Main.VERBOSE) System.out.println("Data Prob: " + svb.getLogMarginalProbability());


            BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(bn.toString());
            if (Main.VERBOSE) System.out.println(learnBN.toString());
            assertTrue(bn.equalBNs(learnBN, 0.2));
        }
    }

}
