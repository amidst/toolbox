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
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Hanen on 27/01/15.
 */
public class MLMultinomialsNormalsTest {

        @Test
        public void testingML2() throws  IOException, ClassNotFoundException {

            // load the true WasteIncinerator hugin Bayesian network containing 3 Multinomial and 6 Gaussian variables

            BayesianNetwork trueBN = BayesianNetworkLoader.loadFromFile("../networks/simulated/WasteIncinerator.bn");

            if (Main.VERBOSE) System.out.println("\nWasteIncinerator network \n ");
            if (Main.VERBOSE) System.out.println(trueBN.getDAG().toString());
            if (Main.VERBOSE) System.out.println(trueBN.toString());

            //Sampling from trueBN
            BayesianNetworkSampler sampler = new BayesianNetworkSampler(trueBN);
            sampler.setSeed(0);

            //Load the sampled data
            DataStream<DataInstance> data = sampler.sampleToDataStream(100000);

            //try{
            //    sampler.sampleToAnARFFFile("./dataTests/WasteIncineratorSamples.arff", 10000);
            //} catch (IOException ex){
            //}
            //DataStream data = new StaticDataOnDiskFromFile(new ARFFDataReader(new String("dataTests/WasteIncineratorSamples.arff")));

            //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
            // and just learn then test the parameter learning

            //Parameter Learning
            ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
            parallelMaximumLikelihood.setWindowsSize(1000);
            parallelMaximumLikelihood.setParallelMode(true);
            parallelMaximumLikelihood.setLaplace(false);
            parallelMaximumLikelihood.setDAG(trueBN.getDAG());
            parallelMaximumLikelihood.initLearning();
            parallelMaximumLikelihood.updateModel(data);
            BayesianNetwork bnet = parallelMaximumLikelihood.getLearntBayesianNetwork();

                 //Check if the probability distributions of each node
            for (Variable var : trueBN.getVariables()) {
                if (Main.VERBOSE) System.out.println("\n------ Variable " + var.getName() + " ------");
                if (Main.VERBOSE) System.out.println("\nTrue distribution:\n"+ trueBN.getConditionalDistribution(var));
                if (Main.VERBOSE) System.out.println("\nLearned distribution:\n"+ bnet.getConditionalDistribution(var));
                Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(trueBN.getConditionalDistribution(var), 0.07));
            }

            //Or check directly if the true and learned networks are equals
            Assert.assertTrue(bnet.equalBNs(trueBN, 0.07));
        }
}

