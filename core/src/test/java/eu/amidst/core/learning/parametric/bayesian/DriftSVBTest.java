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

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import junit.framework.TestCase;

import java.util.Random;

/**
 * Created by andresmasegosa on 20/4/16.
 */
public class DriftSVBTest extends TestCase {

    public static void test1() throws Exception {


        BayesianNetwork oneNormalVarBN = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal.bn");

        //if (Main.VERBOSE) System.out.println(oneNormalVarBN);
        int batchSize = 100;


        DriftSVB svb = new DriftSVB();
        svb.setWindowsSize(batchSize);
        svb.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(oneNormalVarBN.getDAG());

        svb.initLearning();

        for (int i = 0; i < 10; i++) {

            if (i % 3 == 0) {
                oneNormalVarBN.randomInitialization(new Random(i));
                //if (Main.VERBOSE) System.out.println(oneNormalVarBN);
            }

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(oneNormalVarBN);
            sampler.setSeed(i);

            svb.updateModelWithConceptDrift(sampler.sampleToDataStream(batchSize).toDataOnMemory());

            if (Main.VERBOSE) System.out.println("N Iter: " + i + ", " + svb.getLambdaMomentParameter());


            if (i > 0 && i % 3 == 0) {
                assertEquals(0.0, svb.getLambdaMomentParameter(), 0.1);
            } else if (i > 0 && i % 3 != 0) {
                assertEquals(1.0, svb.getLambdaMomentParameter(), 0.1);
            }
        }
    }

    public static void test2() throws Exception {


        BayesianNetwork oneNormalVarBN = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal.bn");

        if (Main.VERBOSE) System.out.println(oneNormalVarBN);
        int batchSize = 1;


        DriftSVB svb = new DriftSVB();
        svb.setWindowsSize(batchSize);
        svb.setSeed(0);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        svb.setDAG(oneNormalVarBN.getDAG());

        svb.initLearning();

        double pred = 0;

        for (int i = 0; i < 100; i++) {

            if (false) {
                Normal normal = oneNormalVarBN.getConditionalDistribution(oneNormalVarBN.getVariables().getVariableByName("A"));
                normal.setMean(normal.getMean()+5);
                normal.setVariance(normal.getVariance()+0.5);

                //if (Main.VERBOSE) System.out.println(oneNormalVarBN);
            }

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(oneNormalVarBN);
            sampler.setSeed(i);

            DataOnMemory<DataInstance> batch = sampler.sampleToDataStream(batchSize).toDataOnMemory();

            if (i>0)
                pred+=svb.predictedLogLikelihood(batch);

            svb.updateModelWithConceptDrift(batch);
            //svb.updateModel(batch);

            //if (Main.VERBOSE) System.out.println(svb.getLearntBayesianNetwork());

            if (Main.VERBOSE) System.out.println("N Iter: " + i + ", " + svb.getLambdaMomentParameter());


            /*if (i > 0 && i % 3 == 0) {
                assertEquals(0.0, svb.getLambdaMomentParameter(), 0.1);
            } else if (i > 0 && i % 3 != 0) {
                assertEquals(1.0, svb.getLambdaMomentParameter(), 0.1);
            }*/
        }

        if (Main.VERBOSE) System.out.println(svb.getLearntBayesianNetwork());


        if (Main.VERBOSE) System.out.println(pred);

    }


}