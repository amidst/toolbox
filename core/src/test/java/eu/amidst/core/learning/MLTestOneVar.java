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


import com.google.common.base.Stopwatch;
import eu.amidst.core.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Hanen on 12/01/15.
 */
public class MLTestOneVar {

    @Test
    public void MLTest() throws  IOException, ClassNotFoundException {

        // load the true Asia Bayesian network
        BayesianNetwork net = BayesianNetworkLoader.loadFromFile("../networks/simulated/One.bn");

        if (Main.VERBOSE) System.out.println("\nOne network \n ");
        if (Main.VERBOSE) System.out.println(net.getDAG().toString());
        if (Main.VERBOSE) System.out.println(net.toString());

        //Sampling 5000 instances from Asia BN
        Stopwatch watch = Stopwatch.createStarted();
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(net);
        sampler.setSeed(0);
        if (Main.VERBOSE) System.out.println(watch.stop());

        DataStream<DataInstance> data = sampler.sampleToDataStream(10);
        if (Main.VERBOSE) data.stream().forEach( e ->  System.out.println(e.outputString()));

        //Load the sampled data
        data = sampler.sampleToDataStream(10);
        //Structure learning is excluded from the test, i.e., so we use here the same initial network structure net.getDAG()

        //Parameter Learning
        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setWindowsSize(1000);
        parallelMaximumLikelihood.setParallelMode(true);
        parallelMaximumLikelihood.setLaplace(false);

        parallelMaximumLikelihood.setDAG(net.getDAG());
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(data);
        BayesianNetwork bn = parallelMaximumLikelihood.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bn.toString());


        //Check if the probability distributions of the true and learned networks are equals
        for (Variable var : net.getVariables()) {
            if (Main.VERBOSE) System.out.println("\n------ Variable " + var.getName() + " ------");
            Distribution trueCD = net.getConditionalDistribution(var);
            if (Main.VERBOSE) System.out.println("\nThe true distribution:\n"+ trueCD);

            Distribution learnedCD = bn.getConditionalDistribution(var);
            if (Main.VERBOSE) System.out.println("\nThe learned distribution:\n"+ learnedCD);

        }

    }

}
