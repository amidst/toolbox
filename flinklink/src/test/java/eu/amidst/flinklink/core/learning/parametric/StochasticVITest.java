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

package eu.amidst.flinklink.core.learning.parametric;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.Main;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import junit.framework.TestCase;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.junit.Assert;

import java.io.IOException;
import java.util.Random;

/**
 * Created by andresmasegosa on 15/2/16.
 */
public class StochasticVITest extends TestCase {


    public static void baseTest(ExecutionEnvironment env, DataStream<DataInstance> data, BayesianNetwork network, int dataSetSize, int batchSize, double error) throws IOException, ClassNotFoundException {

        DataStreamWriter.writeDataToFile(data, "../datasets/simulated/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "../datasets/simulated/tmp.arff", false);

        network.getDAG().getVariables().setAttributes(dataFlink.getAttributes());

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        StochasticVI stochasticVI = new StochasticVI();
        stochasticVI.setLearningFactor(0.7);
        stochasticVI.setDataSetSize(dataSetSize);
        stochasticVI.setOutput(true);
        stochasticVI.setSeed(5);
        stochasticVI.setBatchSize(batchSize);
        stochasticVI.setLocalThreshold(0.001);
        stochasticVI.setMaximumLocalIterations(100);
        stochasticVI.setTimiLimit(20);

        stochasticVI.setDAG(network.getDAG());
        stochasticVI.initLearning();
        stochasticVI.updateModel(dataFlink);
        BayesianNetwork bnet = stochasticVI.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : network.getVariables()) {
            if (Main.VERBOSE) System.out.println("\n------ Variable " + var.getName() + " ------");
            if (Main.VERBOSE) System.out.println("\nTrue distribution:\n" + network.getConditionalDistribution(var));
            if (Main.VERBOSE) System.out.println("\nLearned distribution:\n" + bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(network.getConditionalDistribution(var), error));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(network, error));
    }

    public static void testMultinomials1() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newMultinomialVariable("A", 2);

        DAG dag = new DAG(variables);

        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(0));
        //Multinomial distA = bn.getConditionalDistribution(varA);

        //distA.setProbabilities(new double[]{1.0, 0.0});

        if (Main.VERBOSE) System.out.println(bn.toString());

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataStream(100);

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        baseTest(env, data, bn, 100, 10, 0.05);

    }

    public static void testGaussian1() throws IOException, ClassNotFoundException {


        //for (int i = 2; i <3; i++) {
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal_MultinomialParents.bn");
        //bn.randomInitialization(new Random(0));

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        baseTest(env, data, bn, 10000, 1000, 0.2);

        //}
    }

}