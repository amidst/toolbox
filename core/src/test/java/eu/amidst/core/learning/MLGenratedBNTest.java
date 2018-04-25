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
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Hanen on 28/01/15.
 */
public class MLGenratedBNTest {

    @Test
    public void testingMLGeneratedBN() throws IOException, ClassNotFoundException {

        BayesianNetworkGenerator.loadOptions();

        BayesianNetworkGenerator.setNumberOfGaussianVars(10);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(1, 2);
        BayesianNetworkGenerator.setSeed(0);
        BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(2);
        if (Main.VERBOSE) System.out.println(naiveBayes.toString());

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setSeed(0);

        DataStream<DataInstance> data = sampler.sampleToDataStream(1000000);


        //Parameter Learning
        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setWindowsSize(1000);
        parallelMaximumLikelihood.setParallelMode(true);
        parallelMaximumLikelihood.setLaplace(false);
        parallelMaximumLikelihood.setDAG(naiveBayes.getDAG());
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(data);
        BayesianNetwork bnet = parallelMaximumLikelihood.getLearntBayesianNetwork();

        //Check the probability distributions of each node
        for (Variable var : naiveBayes.getVariables()) {
            if (Main.VERBOSE) System.out.println("\n------ Variable " + var.getName() + " ------");
            if (Main.VERBOSE) System.out.println("\nTrue distribution:\n"+ naiveBayes.getConditionalDistribution(var));
            if (Main.VERBOSE) System.out.println("\nLearned distribution:\n"+ bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(naiveBayes.getConditionalDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(naiveBayes, 0.05));
    }

}
