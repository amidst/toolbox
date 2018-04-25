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

package eu.amidst.winter;

import eu.amidst.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.dVMP;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.junit.Assert;

import java.io.IOException;

/**
 * Created by andresmasegosa on 14/01/2018.
 */
public class Tmp {

    public static void baseTest(ExecutionEnvironment env, DataStream<DataInstance> data, BayesianNetwork network, int batchSize, double error) throws IOException, ClassNotFoundException {

        DataStreamWriter.writeDataToFile(data, "./datasets/simulated/tmp.arff");

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "./datasets/simulated/tmp.arff", false);

        network.getDAG().getVariables().setAttributes(dataFlink.getAttributes());

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        //Parameter Learning
        dVMP parallelVB = new dVMP();
        parallelVB.setOutput(true);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setSeed(5);
        parallelVB.setBatchSize(batchSize);
        parallelVB.setLocalThreshold(0.001);
        parallelVB.setGlobalThreshold(0.01);
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

    public static void main(String[] args) throws Exception {

        String[] bns = {"./networks/simulated/Normal.bn", "./networks/simulated/Normal_1NormalParents.bn",
                "./networks/simulated/Normal_NormalParents.bn"};

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
}
