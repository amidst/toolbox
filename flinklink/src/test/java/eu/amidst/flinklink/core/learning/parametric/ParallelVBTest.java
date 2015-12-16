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
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.learning.parametric.bayesian.DataPosterior;
import eu.amidst.core.learning.parametric.bayesian.DataPosteriorAssignment;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.DAGGenerator;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import junit.framework.TestCase;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
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

        DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "./datasets/tmp.arff", false);

        network.getDAG().getVariables().setAttributes(dataFlink.getAttributes());

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(batchSize);
        VMP vmp = parallelVB.getSVB().getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);


        parallelVB.setDAG(network.getDAG());
        parallelVB.setDataFlink(dataFlink);
        parallelVB.runLearning();
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : network.getVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n" + network.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n" + bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(network.getConditionalDistribution(var), error));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(network, error));
    }


    public static void testMultinomials1() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newMultionomialVariable("A", 2);

        DAG dag = new DAG(variables);

        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(0));
        Multinomial distA = bn.getConditionalDistribution(varA);

        distA.setProbabilities(new double[]{1.0, 0.0});

        System.out.println(bn.toString());

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataStream(100);

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        baseTest(env, data, bn, 10, 0.05);

    }

    public static void testMultinomials2() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newMultionomialVariable("A", 2);
        Variable varB = variables.newMultionomialVariable("B", 2);

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

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        baseTest(env, data, bn, 100, 0.05);

    }

    public static void testGaussian0() throws IOException, ClassNotFoundException {

        String[] bns = {"networks/Normal.bn", "networks/Normal_1NormalParents.bn"};

        for (String bnname : bns) {
            BayesianNetwork bn = BayesianNetworkLoader.loadFromFile(bnname);

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(2);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

            baseTest(env, data, bn, 1000, 0.1);

        }
    }



    public static void testGaussian1() throws IOException, ClassNotFoundException {


        //for (int i = 2; i <3; i++) {
            BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("networks/Normal_MultinomialParents.bn");
            //bn.randomInitialization(new Random(0));

            BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
            sampler.setSeed(2);
            DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

            final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

            baseTest(env, data, bn, 1000, 0.2);

        //}
    }

    public void testingMLParallelAsia() throws IOException, ClassNotFoundException {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");
        asianet.randomInitialization(new Random(0));
        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().outputString());
        System.out.println(asianet.toString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "./datasets/tmp.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(1000);
        VMP vmp = parallelVB.getSVB().getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);


        parallelVB.setDAG(asianet.getDAG());
        parallelVB.setDataFlink(dataFlink);
        parallelVB.runLearning();
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : asianet.getVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n" + asianet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n" + bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.02));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.02));
    }

    public void testingMLParallelAsiaWithUpdate() throws IOException, ClassNotFoundException {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");
        asianet.randomInitialization(new Random(0));
        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().outputString());
        System.out.println(asianet.toString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "./datasets/tmp.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(1000);
        VMP vmp = parallelVB.getSVB().getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);


        parallelVB.setDAG(asianet.getDAG());
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : asianet.getVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n" + asianet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n" + bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.05));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.05));
    }

    public void testingMLParallelAsiaHidden() throws IOException, ClassNotFoundException {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");
        asianet.randomInitialization(new Random(0));
        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().outputString());
        System.out.println(asianet.toString());

        //Sampling from Asia BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);
        sampler.setHiddenVar(asianet.getVariables().getVariableById(6));
        DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "./datasets/tmp.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setGlobalThreshold(0.001);

        parallelVB.setSeed(5);
        parallelVB.setBatchSize(1000);
        VMP vmp = parallelVB.getSVB().getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);


        parallelVB.setDAG(asianet.getDAG());
        parallelVB.setDataFlink(dataFlink);
        parallelVB.runLearning();
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        System.out.println(bnet.toString());
    }

    public void testingMLParallelWasteHidden() throws IOException, ClassNotFoundException {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();


        // load the true WasteIncinerator Bayesian network
        BayesianNetwork wasteIncinerator = BayesianNetworkLoader.loadFromFile("networks/WasteIncinerator.bn");
        wasteIncinerator.randomInitialization(new Random(0));
        System.out.println("\nAsia network \n ");
        //System.out.println(asianet.getDAG().outputString());
        System.out.println(wasteIncinerator.toString());

        //Sampling from WasteIncinerator BN
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(wasteIncinerator);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(1000);
        sampler.setHiddenVar(wasteIncinerator.getVariables().getVariableById(6));
        DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");

        //We load the data
        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "./datasets/tmp.arff", false);


        //ParallelVB is defined
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(100);


        //Defining the finishing condition for global iterations
        parallelVB.setMaximumGlobalIterations(20);
        parallelVB.setGlobalThreshold(0.001);

        //Defining the finishing condition for local iterations
        VMP vmp = parallelVB.getSVB().getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);


        //Setting DAG
        parallelVB.setDAG(wasteIncinerator.getDAG());

        //Setting the distributed data source
        parallelVB.setDataFlink(dataFlink);

        //Run
        parallelVB.runLearning();
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        System.out.println(bnet.toString());
    }

    public void testingMLParallelWaste() throws Exception {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        // load the true Asia Bayesian network
        BayesianNetwork asianet = BayesianNetworkLoader.loadFromFile("networks/WasteIncinerator.bn");

        System.out.println("\nWasteIncinerator network \n ");
        //System.out.println(asianet.getDAG().outputString());
        System.out.println(asianet.toString());

        //Sampling from Asia BN
/*        BayesianNetworkSampler sampler = new BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        //Load the sampled data
        DataStream<DataInstance> data = sampler.sampleToDataStream(10000);

        DataStreamWriter.writeDataToFile(data, "./datasets/tmp.arff");
*/

        eu.amidst.flinklink.core.utils.BayesianNetworkSampler sampler = new eu.amidst.flinklink.core.utils.BayesianNetworkSampler(asianet);
        sampler.setSeed(0);
        DataFlink<DataInstance> data = sampler.sampleToDataFlink(10000);

        DataFlinkWriter.writeDataToARFFFolder(data, "./datasets/tmpfolder.arff");



        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env, "./datasets/tmpfolder.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(1000);
        VMP vmp = parallelVB.getSVB().getPlateuStructure().getVMP();
        vmp.setOutput(true);
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);

        parallelVB.setDAG(asianet.getDAG());
        parallelVB.setDataFlink(dataFlink);
        parallelVB.runLearning();
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        //Check if the probability distributions of each node
        for (Variable var : asianet.getVariables()) {
            System.out.println("\n------ Variable " + var.getName() + " ------");
            System.out.println("\nTrue distribution:\n" + asianet.getConditionalDistribution(var));
            System.out.println("\nLearned distribution:\n" + bnet.getConditionalDistribution(var));
            Assert.assertTrue(bnet.getConditionalDistribution(var).equalDist(asianet.getConditionalDistribution(var), 0.4));
        }

        //Or check directly if the true and learned networks are equals
        Assert.assertTrue(bnet.equalBNs(asianet, 0.4));
    }

    public void testingMLParallelPosteriors() throws Exception {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env,
                "./datasets/dataFlink/test_not_modify/SmallDataSet.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setGlobalThreshold(0.001);

        parallelVB.setSeed(5);
        parallelVB.setBatchSize(5);
        VMP vmp = parallelVB.getSVB().getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);

        DAG dag = DAGGenerator.getHiddenNaiveBayesStructure(dataFlink.getAttributes(), "GlobalHidden", 2);
        System.out.println(dag.toString());
        parallelVB.setDAG(dag);
        parallelVB.setDataFlink(dataFlink);
        parallelVB.runLearning();
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        System.out.println(bnet.toString());

        DataSet<DataPosterior> dataPosteriorDataSet = parallelVB.computePosterior(Arrays.asList(dag.getVariables().getVariableByName("GlobalHidden")));

        dataPosteriorDataSet.print();

        //DataSetSerializer.serializeDataSet(dataPosteriorDataSet, "./datasets/tmp.ser");
        //dataPosteriorDataSet = DataSetSerializer.deserializeDataSet("./datasets/tmp.ser");

        dataPosteriorDataSet.print();
    }

    public void testingMLParallelPosteriorsAssignment() throws Exception {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env,
                "./datasets/dataFlink/test_not_modify/SmallDataSet.arff", false);

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        ParallelVB parallelVB = new ParallelVB();
        parallelVB.setOutput(true);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setGlobalThreshold(0.001);

        parallelVB.setSeed(5);
        parallelVB.setBatchSize(5);
        VMP vmp = parallelVB.getSVB().getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(100);
        vmp.setThreshold(0.0001);

        DAG dag = DAGGenerator.getHiddenNaiveBayesStructure(dataFlink.getAttributes(), "GlobalHidden", 2);
        System.out.println(dag.toString());
        parallelVB.setDAG(dag);
        parallelVB.setDataFlink(dataFlink);
        parallelVB.runLearning();
        BayesianNetwork bnet = parallelVB.getLearntBayesianNetwork();

        System.out.println(bnet.toString());
        List<Variable> list = new ArrayList<>();
        list.add(dag.getVariables().getVariableByName("GlobalHidden"));
        list.add(dag.getVariables().getVariableById(0));

        DataSet<DataPosteriorAssignment> dataPosteriorDataSet = parallelVB.computePosteriorAssignment(list);

        dataPosteriorDataSet.print();


        dataPosteriorDataSet.print();
    }


}