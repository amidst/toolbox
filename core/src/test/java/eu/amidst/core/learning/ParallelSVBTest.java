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
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.learning.parametric.bayesian.ParallelSVB;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 04/03/15.
 */
public class ParallelSVBTest extends TestCase {

    public static void testAsia1Core() throws IOException, ClassNotFoundException{

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
        vmp.setThreshold(0.1);

        ParallelSVB parallelSVB = new ParallelSVB();
        parallelSVB.setNCores(1);
        parallelSVB.setSVBEngine(svb);

        parallelSVB.setDAG(asianet.getDAG());
        parallelSVB.setDataStream(data);
        parallelSVB.runLearning();

        if (Main.VERBOSE) System.out.println(parallelSVB.getLogMarginalProbability());

        BayesianNetwork learnAsianet = parallelSVB.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(asianet.toString());
        if (Main.VERBOSE) System.out.println(learnAsianet.toString());
        assertTrue(asianet.equalBNs(learnAsianet, 0.05));

    }
    public static void testAsiaNcore() throws IOException, ClassNotFoundException{

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
        vmp.setThreshold(0.1);

        ParallelSVB parallelSVB = new ParallelSVB();
        parallelSVB.setSVBEngine(svb);

        parallelSVB.setDAG(asianet.getDAG());
        parallelSVB.setDataStream(data);
        parallelSVB.runLearning();

        if (Main.VERBOSE) System.out.println(parallelSVB.getLogMarginalProbability());

        BayesianNetwork learnAsianet = parallelSVB.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(asianet.toString());
        if (Main.VERBOSE) System.out.println(learnAsianet.toString());
        assertTrue(asianet.equalBNs(learnAsianet, 0.05));

    }


    public static void testAsiaNcore2() throws IOException, ClassNotFoundException{

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
        vmp.setThreshold(0.1);

        ParallelSVB parallelSVB = new ParallelSVB();
        parallelSVB.setSVBEngine(svb);

        parallelSVB.setDAG(asianet.getDAG());

        parallelSVB.initLearning();

        parallelSVB.updateModel(data);

        if (Main.VERBOSE) System.out.println(parallelSVB.getLogMarginalProbability());

        BayesianNetwork learnAsianet = parallelSVB.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(asianet.toString());
        if (Main.VERBOSE) System.out.println(learnAsianet.toString());
        assertTrue(asianet.equalBNs(learnAsianet, 0.05));

    }

    public static void testAsiaNcoreHidden() throws IOException, ClassNotFoundException{

        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("../networks/dataWeka/asia.bn");
        asianet.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println("\nAsia network \n ");

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        sampler.setHiddenVar(asianet.getVariables().getVariableById(6));
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        SVB svb = new SVB();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.1);

        ParallelSVB parallelSVB = new ParallelSVB();
        parallelSVB.setOutput(true);
        parallelSVB.setSVBEngine(svb);

        parallelSVB.setDAG(asianet.getDAG());
        parallelSVB.setDataStream(data);
        parallelSVB.runLearning();

        if (Main.VERBOSE) System.out.println(parallelSVB.getLogMarginalProbability());

        BayesianNetwork learnAsianet = parallelSVB.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(asianet.toString());
        if (Main.VERBOSE) System.out.println(learnAsianet.toString());
        //assertTrue(asianet.equalBNs(learnAsianet, 0.05));

    }

    public static void testWasteIncinerator1Core() throws IOException, ClassNotFoundException{

        String[] bns = {"../networks/simulated/Normal.bn",
                "../networks/simulated/Normal_1NormalParents.bn",
                "../networks/simulated/Normal_NormalParents.bn",
                "../networks/simulated/Normal_MultinomialParents.bn",
                "../networks/simulated/WasteIncinerator.bn"
        };


        for (int i = 0; i < bns.length; i++) {
            if (Main.VERBOSE) System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            if (Main.VERBOSE) System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"+bns[i]+"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
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
            vmp.setThreshold(0.01);

            ParallelSVB parallelSVB = new ParallelSVB();
            parallelSVB.setNCores(1);
            parallelSVB.setSVBEngine(svb);

            parallelSVB.setDAG(normalVarBN.getDAG());
            parallelSVB.setDataStream(data);
            parallelSVB.runLearning();

            if (Main.VERBOSE) System.out.println(parallelSVB.getLogMarginalProbability());

            learntNormalVarBN = parallelSVB.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.3));

        }
    }

    public static void testWasteIncineratorNCore() throws IOException, ClassNotFoundException{

        String[] bns = {"../networks/simulated/Normal.bn",
        "../networks/simulated/Normal_1NormalParents.bn",
        "../networks/simulated/Normal_NormalParents.bn",
        "../networks/simulated/Normal_MultinomialParents.bn",
        "../networks/simulated/WasteIncinerator.bn"
        };


        for (int i = 0; i < 3; i++) {
            if (Main.VERBOSE) System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            if (Main.VERBOSE) System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"+bns[i]+"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            if (Main.VERBOSE) System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            BayesianNetwork normalVarBN = BayesianNetworkLoader.loadFromFile(bns[i]);
            //normalVarBN.randomInitialization(new Random(0));

            if (Main.VERBOSE) System.out.println("\n Waste Incinerator \n ");


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
            svb.setSeed(0);
            VMP vmp = svb.getPlateuStructure().getVMP();
            vmp.setTestELBO(true);
            vmp.setMaxIter(1000);
            vmp.setThreshold(0.001);

            ParallelSVB parallelSVB = new ParallelSVB();
            parallelSVB.setOutput(true);
            parallelSVB.setSVBEngine(svb);

            parallelSVB.setDAG(normalVarBN.getDAG());
            parallelSVB.setDataStream(data);
            parallelSVB.runLearning();

            if (Main.VERBOSE) System.out.println(parallelSVB.getLogMarginalProbability());

            learntNormalVarBN = parallelSVB.getLearntBayesianNetwork();

            if (Main.VERBOSE) System.out.println(normalVarBN.toString());
            if (Main.VERBOSE) System.out.println(learntNormalVarBN.toString());
            assertTrue(normalVarBN.equalBNs(learntNormalVarBN, 0.22));
        }
    }

}
