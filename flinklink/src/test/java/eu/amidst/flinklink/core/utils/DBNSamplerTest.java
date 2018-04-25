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

package eu.amidst.flinklink.core.utils;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.flinklink.Main;
import eu.amidst.flinklink.core.data.DataFlink;
import junit.framework.TestCase;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.junit.Assert;

import java.util.Random;

/**
 * Created by andresmasegosa on 25/9/15.
 */
public class DBNSamplerTest extends TestCase {

    public static void test0() throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("../networks/bnaic2015/BCC/HuginCajaMarDefaulterPredictor.dbn");
        dbn.randomInitialization(new Random(0));

        if (Main.VERBOSE) System.out.println(dbn.toString());

        BayesianNetwork bn = dbn.toBayesianNetworkTime0();
        if (Main.VERBOSE) System.out.println(bn.toString());
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(1);
        sampler.setBatchSize(100);
        DataFlink<DataInstance> data0= sampler.sampleToDataFlink(env,1000);

        double count = 0;
        for (DataInstance datainstance : data0.getDataSet().collect()) {
            if (datainstance.getValue(bn.getVariables().getVariableByName("DEFAULTER"))==1.0
                    && datainstance.getValue(bn.getVariables().getVariableByName("CREDITCARD"))==1.0)
                count++;
        }
        if (Main.VERBOSE) System.out.println(count);

    }


    public static void test1() throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("../networks/bnaic2015/BCC/HuginCajaMarDefaulterPredictor.dbn");
        dbn.randomInitialization(new Random(0));

        if (Main.VERBOSE) System.out.println(dbn.toString());

        DBNSampler sampler = new DBNSampler(dbn);
        sampler.setNSamples(1000);
        sampler.setBatchSize(100);
        sampler.setSeed(1);


        DataFlink<DynamicDataInstance> data0 = sampler.cascadingSample(env,null);

        double count = 0;
        for (DataInstance datainstance : data0.getDataSet().collect()) {
            if (datainstance.getValue(dbn.getDynamicVariables().getVariableByName("DEFAULTER"))==0.0)
                //    && datainstance.getValue(dbn.getDynamicVariables().getVariableByName("CREDITCARD"))==1.0)
                count++;
        }
        if (Main.VERBOSE) System.out.println(count);

    }



    public static void test2() throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);


        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("../networks/bnaic2015/BCC/HuginCajaMarDefaulterPredictor.dbn");
        dbn.randomInitialization(new Random(0));

        if (Main.VERBOSE) System.out.println(dbn.toString());

        DBNSampler sampler = new DBNSampler(dbn);
        sampler.setNSamples(1000);
        sampler.setBatchSize(100);
        sampler.setSeed(1);


        DataFlink<DynamicDataInstance> data0 = sampler.cascadingSample(env,null);
        DataFlink<DynamicDataInstance> data1= sampler.cascadingSample(env,data0);

        double countA = 0;
        for (DataInstance datainstance : data0.getDataSet().collect()) {
            if (datainstance.getValue(dbn.getDynamicVariables().getVariableByName("DEFAULTER"))==0.0)
                //    && datainstance.getValue(dbn.getDynamicVariables().getVariableByName("CREDITCARD"))==1.0)
                countA++;
        }

        double countB = 0;
        for (DataInstance datainstance : data1.getDataSet().collect()) {
            if (datainstance.getValue(dbn.getDynamicVariables().getVariableByName("DEFAULTER"))==0.0)
                //    && datainstance.getValue(dbn.getDynamicVariables().getVariableByName("CREDITCARD"))==1.0)
                countB++;
        }

        if (Main.VERBOSE) System.out.println(countA);
        if (Main.VERBOSE) System.out.println(countB);

    }

    public static void test3() throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("../networks/bnaic2015/BCC/HuginCajaMarDefaulterPredictor.dbn");
        dbn.randomInitialization(new Random(0));
        Multinomial_MultinomialParents dist = dbn.getConditionalDistributionTimeT(dbn.getDynamicVariables().getVariableByName("DEFAULTER"));
        dist.getMultinomial(0).setProbabilities(new double[]{0.99,0.01});
        dist.getMultinomial(1).setProbabilities(new double[]{0.99, 0.01});

        if (Main.VERBOSE) System.out.println(dbn.toString());

        DBNSampler sampler = new DBNSampler(dbn);
        sampler.setNSamples(10);
        sampler.setBatchSize(2);
        sampler.setSeed(1);

        if (Main.VERBOSE) System.out.println("--------------- DATA 0 --------------------------");
        DataFlink<DynamicDataInstance> data0 = sampler.cascadingSample(env,null);
        data0.getDataSet().print();

        Assert.assertEquals(data0.getDataSet().count(), 10);
        Assert.assertEquals(data0.getAttributes().getNumberOfAttributes(), 10);
        Assert.assertNotNull(data0.getAttributes().getSeq_id());
        Assert.assertNotNull(data0.getAttributes().getTime_id());

        DataFlink<DynamicDataInstance> dataPrev = data0;
        for (int i = 1; i < 10; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA "+i+" --------------------------");
            DataFlink<DynamicDataInstance> dataNew = sampler.cascadingSample(env,dataPrev);
            dataNew.getDataSet().print();
            Assert.assertEquals(dataNew.getDataSet().count(), 10);
            Assert.assertEquals(dataNew.getAttributes().getNumberOfAttributes(), 10);
            Assert.assertNotNull(dataNew.getAttributes().getSeq_id());
            Assert.assertNotNull(dataNew.getAttributes().getTime_id());

            dataPrev = dataNew;
        }


    }
}