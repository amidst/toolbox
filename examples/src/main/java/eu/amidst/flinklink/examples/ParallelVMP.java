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

package eu.amidst.flinklink.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.learning.parametric.ParallelVB;
import eu.amidst.flinklink.core.utils.BayesianNetworkSampler;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.junit.Assert;

/**
 * Created by Hanen on 08/10/15.
 */
public class ParallelVMP {

    public static void main(String[] args) throws Exception {


        //Set-up Flink session.
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // load the true Asia Bayesian network
        BayesianNetwork originalBnet = BayesianNetworkLoader.loadFromFile(args[0]);
        System.out.println("\n Network \n " + args[0]);



        //originalBnet.randomInitialization(new Random(0));
        //System.out.println(originalBnet.toString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(originalBnet);
        sampler.setSeed(0);

        //Load the sampled data
        int sizeData = Integer.parseInt(args[1]);
        DataFlink<DataInstance> data = sampler.sampleToDataFlink(env,sizeData);

        DataFlinkWriter.writeDataToARFFFolder(data, args[2]);

        //DataFlink<DataInstance> dataStream = DataFlinkLoader.loadData(env, "./tmp.arff");
        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,args[2], false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        long start = System.nanoTime();

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setLocalThreshold(100);
        parallelVB.setLocalThreshold(0.01);
        parallelVB.setGlobalThreshold(0.1);
        parallelVB.setSeed(5);

        //Set the window size
        parallelVB.setBatchSize(Integer.parseInt(args[3]));

        parallelVB.setDAG(originalBnet.getDAG());
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork LearnedBnet = parallelVB.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : originalBnet.getVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ originalBnet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n" + LearnedBnet.getConditionalDistribution(var));
            Assert.assertTrue(originalBnet.getConditionalDistribution(var).equalDist(LearnedBnet.getConditionalDistribution(var), 0.4));
        }

        if (LearnedBnet.equalBNs(originalBnet, 0.02))
            System.out.println("\n The true and learned networks are equals :-) \n ");
        else
            System.out.println("\n The true and learned networks are NOT equals!!! \n ");

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.println("Running time: \n" + seconds + " secs");

    }

    }
