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


import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.learning.parametric.bayesian.utils.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.utils.DataPosteriorAssignment;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuIIDReplication;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.Main;
import eu.amidst.flinklink.core.conceptdrift.IDAConceptDriftDetector;
import eu.amidst.flinklink.core.conceptdrift.IDAConceptDriftDetectorTest;
import eu.amidst.flinklink.core.conceptdrift.IdentifiableIDAModel;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import junit.framework.TestCase;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 2/9/15.
 */
public class ParallelVBTest extends TestCase {


    public static void baseTest(ExecutionEnvironment env, DataStream<DataInstance> data, BayesianNetwork network, int batchSize, double error) throws IOException, ClassNotFoundException {

        DataStreamWriter.writeDataToFile(data, "../datasets/simulated/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "../datasets/simulated/tmp.arff", false);

        network.getDAG().getVariables().setAttributes(dataFlink.getAttributes());

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(batchSize);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.05);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);


        parallelVB.setDAG(network.getDAG());
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

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
        Multinomial distA = bn.getConditionalDistribution(varA);

        distA.setProbabilities(new double[]{1.0, 0.0});

        if (Main.VERBOSE) System.out.println(bn.toString());

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataStream(100);

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        baseTest(env, data, bn, 10, 0.05);

    }

    public static void testMultinomials2() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newMultinomialVariable("A", 2);
        Variable varB = variables.newMultinomialVariable("B", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = new BayesianNetwork(dag);

        Multinomial distA = bn.getConditionalDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getConditionalDistribution(varB);

        distA.setProbabilities(new double[]{0.6, 0.4});
        distB.getMultinomial(0).setProbabilities(new double[]{0.75, 0.25});
        distB.getMultinomial(1).setProbabilities(new double[]{0.25, 0.75});

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataStream(1000);

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        baseTest(env, data, bn, 100, 0.05);

    }

    public static void testGaussian0() throws IOException, ClassNotFoundException {

        String[] bns = {"../networks/simulated/Normal.bn", "../networks/simulated/Normal_1NormalParents.bn"};

        for (String bnname : bns) {
            BayesianNetwork bn = BayesianNetworkLoader.loadFromFile(bnname);

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(2);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            //Set-up Flink session.
            Configuration conf = new Configuration();
            conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
            final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                    env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

            baseTest(env, data, bn, 1000, 0.1);

        }
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

            baseTest(env, data, bn, 1000, 0.2);

        //}
    }

    public void testingMLParallelAsia() throws IOException, ClassNotFoundException {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("../networks/dataWeka/asia.bn");
        asianet.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println("\nAsia network \n ");
        //if (Main.VERBOSE) System.out.println(asianet.getDAG().outputString());
        if (Main.VERBOSE) System.out.println(asianet.toString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        DataStreamWriter.writeDataToFile(data, "../datasets/simulated/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "../datasets/simulated/tmp.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(1000);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.05);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);


        parallelVB.setDAG(asianet.getDAG());
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : asianet.getVariables()) {
            if (Main.VERBOSE) System.out.println("\n------ Variable " + var.getName() + " ------");
            if (Main.VERBOSE) System.out.println("\nTrue distribution:\n" + asianet.getConditionalDistribution(var));
            if (Main.VERBOSE) System.out.println("\nLearned distribution:\n" + bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.02));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.02));
    }

    public void testingMLParallelAsiaWithUpdate() throws IOException, ClassNotFoundException {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("../networks/dataWeka/asia.bn");
        asianet.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println("\nAsia network \n ");
        //if (Main.VERBOSE) System.out.println(asianet.getDAG().outputString());
        if (Main.VERBOSE) System.out.println(asianet.toString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        DataStreamWriter.writeDataToFile(data, "../datasets/simulated/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "../datasets/simulated/tmp.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(1000);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.05);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);


        parallelVB.setDAG(asianet.getDAG());
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : asianet.getVariables()) {
            if (Main.VERBOSE) System.out.println("\n------ Variable " + var.getName() + " ------");
            if (Main.VERBOSE) System.out.println("\nTrue distribution:\n" + asianet.getConditionalDistribution(var));
            if (Main.VERBOSE) System.out.println("\nLearned distribution:\n" + bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.05));
    }

    public void testingMLParallelAsiaHidden() throws IOException, ClassNotFoundException {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("../networks/dataWeka/asia.bn");
        asianet.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println("\nAsia network \n ");
        //if (Main.VERBOSE) System.out.println(asianet.getDAG().outputString());
        if (Main.VERBOSE) System.out.println(asianet.toString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);
        sampler.setHiddenVar(asianet.getVariables().getVariableById(7));
        DataStreamWriter.writeDataToFile(data, "../datasets/simulated/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "../datasets/simulated/tmp.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        long start = System.nanoTime();

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(100);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.05);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);


        parallelVB.setDAG(asianet.getDAG());
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bnet.toString());

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        if (Main.VERBOSE) System.out.println("Running time: \n" + seconds + " secs");

    }


    public void testingMLParallelRandomBNHidden() throws IOException, ClassNotFoundException {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        BayesianNetworkGenerator.setSeed(0);
        BayesianNetworkGenerator.setNumberOfGaussianVars(10);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(10, 2);
        BayesianNetwork asianet  = BayesianNetworkGenerator.generateBayesianNetwork();

        if (Main.VERBOSE) System.out.println("\nAsia network \n ");
        //if (Main.VERBOSE) System.out.println(asianet.getDAG().outputString());
        //if (Main.VERBOSE) System.out.println(asianet.toString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(5000);

        DataStreamWriter.writeDataToFile(data, "../datasets/simulated/tmp.arff");


        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "../datasets/simulated/tmp.arff", false);


        DAG hiddenNB = getHiddenNaiveBayesStructure(dataFlink.getAttributes());




        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        long start = System.nanoTime();

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(100);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.05);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);

        parallelVB.setDAG(hiddenNB);
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bnet.toString());

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        if (Main.VERBOSE) System.out.println("Running time: \n" + seconds + " secs");

    }





    public void testingMLParallelWasteHidden() throws IOException, ClassNotFoundException {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);


        // load the true WasteIncinerator Bayesian network
        BayesianNetwork wasteIncinerator = BayesianNetworkLoader.loadFromFile("../networks/simulated/WasteIncinerator.bn");
        wasteIncinerator.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println("\nAsia network \n ");
        //if (Main.VERBOSE) System.out.println(asianet.getDAG().outputString());
        if (Main.VERBOSE) System.out.println(wasteIncinerator.toString());

        //Sampling from WasteIncinerator BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(wasteIncinerator);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(1000);
        sampler.setHiddenVar(wasteIncinerator.getVariables().getVariableById(6));
        DataStreamWriter.writeDataToFile(data, "../datasets/simulated/tmp.arff");

        //We load the data
        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "../datasets/simulated/tmp.arff", false);


        //ParallelVB is defined
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(100);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.001);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);

        //Setting DAG
        parallelVB.setDAG(wasteIncinerator.getDAG());

        //Setting the distributed data source
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);

        //Run
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bnet.toString());
    }

    public void testingMLParallelWaste() throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("../networks/simulated/WasteIncinerator.bn");
        asianet.randomInitialization(new Random(0));

        if (Main.VERBOSE) System.out.println("\nWasteIncinerator network \n ");
        //if (Main.VERBOSE) System.out.println(asianet.getDAG().outputString());
        if (Main.VERBOSE) System.out.println(asianet.toString());

        //Sampling from Asia BN
/*        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");
*/

        eu.amidst.flinklink.core.utils.BayesianNetworkSampler sampler = new eu.amidst.flinklink.core.utils.BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        DataFlink<DataInstance> data = sampler.sampleToDataFlink(env,10000);

        DataFlinkWriter.writeDataToARFFFolder(data, "../datasets/simulated/tmpfolder.arff");



        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env, "../datasets/simulated/tmpfolder.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(1000);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.05);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);


        parallelVB.setDAG(asianet.getDAG());
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : asianet.getVariables()) {
            if (Main.VERBOSE) System.out.println("\n------ Variable " + var.getName() + " ------");
            if (Main.VERBOSE) System.out.println("\nTrue distribution:\n" + asianet.getConditionalDistribution(var));
            if (Main.VERBOSE) System.out.println("\nLearned distribution:\n" + bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.4));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.4));
    }

    public void testingMLParallelPosteriors() throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,
                "../datasets/simulated/test_not_modify/MONTH1.arff", true);
        //DataFlink<DataInstance> dataStream = DataFlinkLoader.loadDataFromFile(env,
        //        "./datasets/dataStream/test_not_modify/SmallDataSet.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(100);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.05);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);

        DAG dag = DAGGenerator.getHiddenNaiveBayesStructure(dataFlink.getAttributes(), "GlobalHidden", 2);
        if (Main.VERBOSE) System.out.println(dag.toString());
        parallelVB.setDAG(dag);
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bnet.toString());

        DataSet<DataPosterior> dataPosteriorDataSet = parallelVB.computePosterior(dataFlink,Arrays.asList(dag.getVariables().getVariableByName("GlobalHidden")));

        dataPosteriorDataSet.print();

        //DataSetSerializer.serializeDataSet(dataPosteriorDataSet, "./datasets/tmp.ser");
        //dataPosteriorDataSet = DataSetSerializer.deserializeDataSet("./datasets/tmp.ser");

        dataPosteriorDataSet.print();
    }

    public void testingMLParallelPosteriorsAssignment() throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,
                "../datasets/simulated/test_not_modify/MONTH1.arff", true);

        //DataFlink<DataInstance> dataStream = DataFlinkLoader.loadDataFromFile(env,
        //        "./datasets/dataStream/test_not_modify/SmallDataSet.arff", false);


        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(100);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.05);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);

        DAG dag = DAGGenerator.getHiddenNaiveBayesStructure(dataFlink.getAttributes(), "GlobalHidden", 2);
        if (Main.VERBOSE) System.out.println(dag.toString());
        parallelVB.setDAG(dag);
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bnet.toString());
        List<Variable> list = new ArrayList<>();
        list.add(dag.getVariables().getVariableByName("GlobalHidden"));
        list.add(dag.getVariables().getVariableById(0));

        DataSet<DataPosteriorAssignment> dataPosteriorDataSet = parallelVB.computePosteriorAssignment(dataFlink,list);

        dataPosteriorDataSet.print();


        dataPosteriorDataSet.print();
    }

    public void testParallelVBExtended() throws Exception {

        int nCVars = 50;//Integer.parseInt(args[0]);
        int nMVars = 50;//Integer.parseInt(args[1]);
        int nSamples = 1000;//Integer.parseInt(args[2]);
        int windowSize = 100;//Integer.parseInt(args[3]);
        int globalIter = 10;//Integer.parseInt(args[4]);
        int localIter = 100;//Integer.parseInt(args[5]);
        int seed = 0;//Integer.parseInt(args[6]);

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);


        /*
         * Logging
         */
        //BasicConfigurator.configure();
        //PropertyConfigurator.configure(args[7]);

        //String fileName = "hdfs:///tmp"+nCVars+"_"+nMVars+"_"+nSamples+"_"+windowSize+"_"+globalIter+"_"+localIter+".arff";
        String fileName = "../datasets/tmp"+nCVars+"_"+nMVars+"_"+nSamples+"_"+windowSize+"_"+globalIter+"_"+localIter+".arff";

        // Randomly generate the data stream using {@link BayesianNetworkGenerator} and {@link BayesianNetworkSampler}.
        BayesianNetworkGenerator.setSeed(seed);
        BayesianNetworkGenerator.setNumberOfGaussianVars(nCVars);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(nMVars, 2);
        BayesianNetwork originalBnet  = BayesianNetworkGenerator.generateBayesianNetwork();

        //Sampling from Asia BN
        eu.amidst.flinklink.core.utils.BayesianNetworkSampler sampler = new eu.amidst.flinklink.core.utils.BayesianNetworkSampler(originalBnet);
        sampler.setSeed(seed);

        //Load the sampled data
        DataFlink<DataInstance> data = sampler.sampleToDataFlink(env,nSamples);

        DataFlinkWriter.writeDataToARFFFolder(data,fileName);

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,fileName, false);

        DAG hiddenNB = getHiddenNaiveBayesStructure(dataFlink.getAttributes());


        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        long start = System.nanoTime();

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setGlobalThreshold(0.1);
        parallelVB.setMaximumGlobalIterations(globalIter);
        parallelVB.setLocalThreshold(0.1);
        parallelVB.setMaximumLocalIterations(localIter);
        parallelVB.setSeed(5);

        //Set the window size
        parallelVB.setBatchSize(windowSize);


        parallelVB.setDAG(hiddenNB);
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork LearnedBnet = parallelVB.getLearntBayesianNetwork();
        if (Main.VERBOSE) System.out.println(LearnedBnet.toString());

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        //logger.info("Global ELBO: {}", parallelVB.getLogMarginalProbability());

    }


    /**
     * Creates a {@link DAG} object with a naive Bayes structure from a given {@link DataStream}.
     * The main variable is defined as a latent binary variable which is set as a parent of all the observed variables.
     * @return a {@link DAG} object.
     */
    public static DAG getHiddenNaiveBayesStructure(Attributes attributes) {

        // Create a Variables object from the attributes of the input data stream.
        Variables modelHeader = new Variables(attributes);

        // Define the global latent binary variable.
        Variable globalHiddenVar = modelHeader.newMultinomialVariable("GlobalHidden", 2);

        // Define the global Gaussian latent binary variable.
        Variable globalHiddenGaussian = modelHeader.newGaussianVariable("globalHiddenGaussian");

        // Define the class variable.
        Variable classVar = modelHeader.getVariableById(0);

        // Create a DAG object with the defined model header.
        DAG dag = new DAG(modelHeader);

        // Define the structure of the DAG, i.e., set the links between the variables.
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> w.getMainVar() != globalHiddenGaussian)
                .filter(w -> w.getMainVar().isMultinomial())
                .forEach(w -> w.addParent(globalHiddenVar));

        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> w.getMainVar() != globalHiddenGaussian)
                .filter(w -> w.getMainVar().isNormal())
                .forEach(w -> w.addParent(globalHiddenGaussian));

        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(classVar));

        // Return the DAG.
        return dag;
    }


    public static void testGaussianCompareSVBvsParralelVB() throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);
        env.setParallelism(1);


        BayesianNetwork network = BayesianNetworkLoader.loadFromFile("../networks/simulated/Normal_1NormalParents.bn");

        eu.amidst.flinklink.core.utils.BayesianNetworkSampler sampler = new eu.amidst.flinklink.core.utils.BayesianNetworkSampler(network);
        sampler.setSeed(2);
        DataFlinkWriter.writeDataToARFFFolder(sampler.sampleToDataFlink(env,10000),"../networks/simulated/simulated/tmpfolder.arff");





        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env, "../networks/simulated/simulated/tmpfolder.arff", false);

        network.getDAG().getVariables().setAttributes(dataFlink.getAttributes());

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(false);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(5000);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.001);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);


        parallelVB.setDAG(network.getDAG());
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();



        DataStream<DataInstance> data = DataStreamLoader.open("../datasets/simulated/tmp.arff");

        SVB svb = new SVB();
        svb.setWindowsSize(10000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.001);

        svb.setDAG(network.getDAG());
        svb.setDataStream(data);
        svb.runLearning();

        if (Main.VERBOSE) System.out.println(network.toString());
        if (Main.VERBOSE) System.out.println(bnet.toString());
        if (Main.VERBOSE) System.out.println(parallelVB.getLogMarginalProbability());

        if (Main.VERBOSE) System.out.println(svb.getLearntBayesianNetwork().toString());
        if (Main.VERBOSE) System.out.println(svb.getLogMarginalProbability());

    }


    public static void testGaussianCompareSVBvsParralelVB2() throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);
        env.setParallelism(1);

        int SAMPLES = 1000;

        IDAConceptDriftDetectorTest.createBN1(3);
        BayesianNetwork network = BayesianNetworkLoader.loadFromFile("../networks/simulated/dbn1.dbn");
        network.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println(network.toString());

        //String dataset= "./datasets/dataStream/conceptdrift/data0.arff";
        String dataset= "../datasets/simulated/tmp.arff";
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(network);
        sampler.setSeed(1);
        DataStreamWriter.writeDataToFile(sampler.sampleToDataStream(SAMPLES),dataset);

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, dataset, false);

        IDAConceptDriftDetector learn = new IDAConceptDriftDetector();
        learn.setBatchSize(10);
        learn.setClassIndex(0);
        learn.setAttributes(dataFlink.getAttributes());
        learn.setNumberOfGlobalVars(1);
        learn.setTransitionVariance(0.1);
        learn.setSeed(0);

        learn.initLearning();

        DAG dag = learn.getGlobalDAG();

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(false);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setSeed(0);
        parallelVB.setBatchSize(100);
        parallelVB.setLocalThreshold(0.01);
        parallelVB.setGlobalThreshold(0.01);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setMaximumGlobalIterations(100);

        List<Variable> hiddenVars = Arrays.asList(dag.getVariables().getVariableByName("GlobalHidden_0"));
        parallelVB.setPlateuStructure(new PlateuIIDReplication(hiddenVars));
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars, 0, 0.1);
        gaussianHiddenTransitionMethod.setFading(1.0);
        parallelVB.setTransitionMethod(gaussianHiddenTransitionMethod);

        parallelVB.setIdenitifableModelling(new IdentifiableIDAModel());
        parallelVB.setDAG(dag);
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();



        DataStream<DataInstance> data = DataStreamLoader.open(dataset);

        SVB svb = new SVB();
        svb.setWindowsSize(SAMPLES);
        svb.setSeed(0);
        svb.setPlateuStructure(parallelVB.getSVB().getPlateuStructure());
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.01);

        svb.setDAG(dag);
        svb.setDataStream(data);
        svb.runLearning();

        if (Main.VERBOSE) System.out.println(network.toString());
        if (Main.VERBOSE) System.out.println(bnet.toString());
        if (Main.VERBOSE) System.out.println(parallelVB.getLogMarginalProbability());

        if (Main.VERBOSE) System.out.println(svb.getLearntBayesianNetwork().toString());
        if (Main.VERBOSE) System.out.println(svb.getLogMarginalProbability());

    }

}