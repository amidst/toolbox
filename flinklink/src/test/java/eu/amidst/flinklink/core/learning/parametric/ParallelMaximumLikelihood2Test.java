package eu.amidst.flinklink.core.learning.parametric;


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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import junit.framework.TestCase;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.junit.Assert;

import java.io.IOException;

/**
 * Created by andresmasegosa on 2/9/15.
 */
public class ParallelMaximumLikelihood2Test extends TestCase {

    public void testingMLParallelAsia() throws IOException, ClassNotFoundException {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().outputString());
        //System.out.println(asianet.outputString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        DataStreamWriter.writeDataToFile(data,"./datasets/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env ,"./datasets/tmp.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelMaximumLikelihood2 parallelMaximumLikelihood = new ParallelMaximumLikelihood2();
        parallelMaximumLikelihood.setDAG(asianet.getDAG());
        parallelMaximumLikelihood.setDataFlink(dataFlink);
        parallelMaximumLikelihood.runLearning();
        BayesianNetwork bnet = parallelMaximumLikelihood.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : asianet.getVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ asianet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.05));
    }

    public void testingMLParallelWaste() throws IOException, ClassNotFoundException {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/WasteIncinerator.bn");

        System.out.println("\nWasteIncinerator network \n ");
        //System.out.println(asianet.getDAG().outputString());
        //System.out.println(asianet.outputString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        DataStreamWriter.writeDataToFile(data,"./datasets/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "./datasets/tmp.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelMaximumLikelihood2 parallelMaximumLikelihood = new ParallelMaximumLikelihood2();
        parallelMaximumLikelihood.setDAG(asianet.getDAG());
        parallelMaximumLikelihood.setDataFlink(dataFlink);
        parallelMaximumLikelihood.runLearning();
        BayesianNetwork bnet = parallelMaximumLikelihood.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : asianet.getVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n"+ asianet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n"+ bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.2));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.2));
    }
}