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
import eu.amidst.flinklink.core.learning.parametric.ParallelMaximumLikelihood;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by Hanen on 07/10/15.
 */
public class ParallelMLE {

    public static void main(String[] args) throws Exception {

        // load the true Asia Bayesian network
        BayesianNetwork originalBnet = BayesianNetworkLoader.loadFromFile(args[0]);
        System.out.println("\n Network \n " + args[0]);

        //System.out.println(originalBnet.getDAG().outputString());
        //System.out.println(originalBnet.outputString());

        //Sampling from Asia BN
        //BayesianNetworkSampler sampler = new BayesianNetworkSampler(originalBnet);
        //sampler.setSeed(0);
        //Load the sampled data

        //int sizeData = Integer.parseInt(args[1]);
        //DataStream<DataInstance> data = sampler.sampleToDataStream(sizeData);

        //DataStreamWriter.writeDataToFile(data, "./tmp.arff");

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,"hdfs:///tmp.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        long start = System.nanoTime();

        //Parameter Learning
        ParallelMaximumLikelihood parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setDAG(originalBnet.getDAG());
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(dataFlink);
        BayesianNetwork LearnedBnet = parallelMaximumLikelihood.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : originalBnet.getVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ originalBnet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n" + LearnedBnet.getConditionalDistribution(var));
        }

        if (LearnedBnet.equalBNs(originalBnet, 0.1))
            System.out.println("\n The true and learned networks are equals :-) \n ");
        else
            System.out.println("\n The true and learned networks are NOT equals!!! \n ");


        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.println("Running time: \n" + seconds + " secs");

    }
}
