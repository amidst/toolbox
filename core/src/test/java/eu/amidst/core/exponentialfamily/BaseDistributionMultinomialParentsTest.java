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

package eu.amidst.core.exponentialfamily;

import eu.amidst.core.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Hanen on 06/02/15.
 */

public class BaseDistributionMultinomialParentsTest {

    // TODO: Consider smoothing method!!!!

    @Test
    public void testingProbabilities_MultinomialMultinomialParents() throws IOException, ClassNotFoundException  {

        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("../networks/dataWeka/asia.bn");

        //Here we had to modify the CPT of the variable E because no smoothing is considered yet
        //Multinomial_MultinomialParents distE = testnet.getConditionalDistribution(testnet.getVariables().getVariableByName("E"));
        //distE.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});
        //distE.getMultinomial(1).setProbabilities(new double[]{0.9, 0.1});
        //distE.getMultinomial(2).setProbabilities(new double[]{0.9, 0.1});
        //distE.getMultinomial(3).setProbabilities(new double[]{0.1, 0.9});

        if (Main.VERBOSE) System.out.println(testnet.toString());

        if (Main.VERBOSE) System.out.println("\nMultinomial_MultinomialParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);

        for(DataInstance e: data) {
            double ef_logProb = 0, logProb = 0;
                for (EF_ConditionalDistribution ef_dist : ef_testnet.getDistributionList()) {
                    ef_logProb += ef_dist.computeLogProbabilityOf(e);
                }

            logProb = testnet.getLogProbabiltyOf(e);

            //if (Main.VERBOSE) System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            Assert.assertEquals(logProb, ef_logProb, 0.0001);

        }
    }


    @Test
    public void testingProbabilities_NormalMultinomialParents() throws IOException, ClassNotFoundException  {

        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal_MultinomialParents.bn");

        if (Main.VERBOSE) System.out.println(testnet.toString());
        if (Main.VERBOSE) System.out.println("\nNormal_MultinomialParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);

        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }
            logProb = testnet.getLogProbabiltyOf(e);
            //if (Main.VERBOSE) System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            Assert.assertEquals(logProb, ef_logProb, 0.0001);

        }
    }

    @Test
    public void testingProbabilities_NormalMultinomialNormalParents() throws IOException, ClassNotFoundException  {

        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal_MultinomialNormalParents.bn");

        if (Main.VERBOSE) System.out.println(testnet.toString());

        if (Main.VERBOSE) System.out.println("\nNormal_MultinomialNormalParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);


        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);

        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }

            logProb = testnet.getLogProbabiltyOf(e);

            //if (Main.VERBOSE) System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            Assert.assertEquals(logProb, ef_logProb, 0.0001);

        }
    }

}