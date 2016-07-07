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

package eu.amidst.flinklink.core.learning.dynamic;

import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.Main;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.utils.DBNSampler;
import junit.framework.TestCase;
import org.apache.flink.api.common.functions.FlatJoinFunction;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.operators.base.JoinOperatorBase;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 25/9/15.
 */
public class DynamicParallelVBTest extends TestCase {

    public static String NETWORK_NAME = "HuginCajaMarDefaulterPredictor.dbn";
    public static int NSETS = 3;

    public static int SAMPLESIZE = 10000;
    public static int BATCHSIZE = 1000;

    public static void createDBN1(boolean connect) throws Exception {

        DynamicVariables dynamicVariables = new DynamicVariables();
        Variable classVar = dynamicVariables.newMultinomialDynamicVariable("C", 2);

        for (int i = 0; i < 2; i++) {
            dynamicVariables.newMultinomialDynamicVariable("A" + i, 2);
        }
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        for (int i = 0; i < 2; i++) {
            dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(classVar);
            if (connect) dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(dynamicVariables.getVariableByName("A" + i).getInterfaceVariable());

        }

        dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());
        dag.setName("dbn1");
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);
        dbn.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println(dbn.toString());

        DynamicBayesianNetworkWriter.save(dbn, "../networks/simulated/dbn1.dbn");
    }

    public static void createDBN2() throws Exception {

        DynamicVariables dynamicVariables = new DynamicVariables();
        Variable classVar = dynamicVariables.newMultinomialDynamicVariable("C", 2);

        for (int i = 0; i < 2; i++) {
            dynamicVariables.newGaussianDynamicVariable("A" + i);
        }
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        for (int i = 0; i < 2; i++) {
            dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(classVar);
            dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(dynamicVariables.getVariableByName("A" + i).getInterfaceVariable());
        }

        dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());
        dag.setName("dbn2");
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);
        dbn.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println(dbn.toString());

        DynamicBayesianNetworkWriter.save(dbn, "../networks/simulated/dbn2.dbn");
    }

    public static void createDBN3() throws Exception {

        DynamicVariables dynamicVariables = new DynamicVariables();
        Variable classVar = dynamicVariables.newGaussianDynamicVariable("C");

        for (int i = 0; i < 2; i++) {
            dynamicVariables.newGaussianDynamicVariable("A" + i);
        }
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        for (int i = 0; i < 2; i++) {
            dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(classVar);
            dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(dynamicVariables.getVariableByName("A" + i).getInterfaceVariable());
        }

        dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());
        dag.setName("dbn1");
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);
        dbn.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println(dbn.toString());

        DynamicBayesianNetworkWriter.save(dbn, "../networks/simulated/dbn3.dbn");
    }


    public static void createDataSets(String networkName, List<String> hiddenVars, List<String> noisyVars) throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("../networks/simulated/" + networkName + ".dbn");
        dbn.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println(dbn.toString());

        DBNSampler sampler = new DBNSampler(dbn);
        sampler.setNSamples(SAMPLESIZE);
        sampler.setBatchSize(BATCHSIZE);
        sampler.setSeed(1);

        if (hiddenVars!=null) {
            for (String hiddenVar : hiddenVars) {
                sampler.setHiddenVar(dbn.getDynamicVariables().getVariableByName(hiddenVar));
            }
        }
        if (noisyVars!=null){
            for (String noisyVar : noisyVars) {
                sampler.setMARVar(dbn.getDynamicVariables().getVariableByName(noisyVar), 0.1);
            }
        }

        DataFlink<DynamicDataInstance> data0 = sampler.cascadingSample(env,null);


        DataFlinkWriter.writeDataToARFFFolder(data0, "../datasets/simulated/cajaMarSynthetic/data0.arff");
        data0 = DataFlinkLoader.loadDynamicDataFromFolder(env, "../datasets/simulated/cajaMarSynthetic/data0.arff", false);

        List<Long> list = data0.getDataSet().map(d -> d.getSequenceID()).collect();
        if (Main.VERBOSE) System.out.println(list);

        HashSet<Long> noDupSet = new HashSet();
        noDupSet.addAll(list);
        assertEquals(SAMPLESIZE, noDupSet.size());
        if (Main.VERBOSE) System.out.println(noDupSet);


        DataFlink<DynamicDataInstance> dataPrev = data0;
        for (int i = 1; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = sampler.cascadingSample(env,dataPrev);
            DataFlinkWriter.writeDataToARFFFolder(dataNew, "../datasets/simulated/cajaMarSynthetic/data" + i + ".arff");
            dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env, "../datasets/simulated/cajaMarSynthetic/data" + i + ".arff", false);

            dataPrev = dataNew;

            List<Long> listNew = dataNew.getDataSet().map(d -> d.getSequenceID()).collect();
            if (Main.VERBOSE) System.out.println(listNew);

            HashSet<Long> noDupSetNew = new HashSet();
            noDupSetNew.addAll(listNew);
            assertEquals(SAMPLESIZE, noDupSetNew.size());
            if (Main.VERBOSE) System.out.println(noDupSetNew);


            for (Long id : noDupSetNew) {
                assertEquals("ID: " + id, true, noDupSet.contains(id));
            }

        }
    }

    public static void testJoin() throws Exception {

        createDBN1(true);
        createDataSets("dbn1",null,null);

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,
                "../datasets/simulated/cajaMarSynthetic/data0.arff", false);

        List<Long> list = data0.getDataSet().map(d -> d.getSequenceID()).collect();

        HashSet<Long> noDupSet = new HashSet();
        noDupSet.addAll(list);
        assertEquals(SAMPLESIZE, noDupSet.size());


        for (int i = 1; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    "../datasets/simulated/cajaMarSynthetic/data" + i + ".arff", false);

            DataSet<Long> dataJoin = data0.getDataSet().join(dataNew.getDataSet(), JoinOperatorBase.JoinHint.REPARTITION_HASH_FIRST).where(
                    new KeySelector<DynamicDataInstance, Long>() {
                        @Override
                        public Long getKey(DynamicDataInstance value) throws Exception {
                            return value.getSequenceID();
                        }
                    }).equalTo(new KeySelector<DynamicDataInstance, Long>() {
                @Override
                public Long getKey(DynamicDataInstance value) throws Exception {
                    return value.getSequenceID();
                }
            }).with(new JoinFunction<DynamicDataInstance, DynamicDataInstance, Long>() {
                @Override
                public Long join(DynamicDataInstance dynamicDataInstance, DynamicDataInstance dataPosterior) throws Exception {
                    return dynamicDataInstance.getSequenceID();
                }
            });

            List<Long> listNew = dataJoin.map(d -> d).collect();
            //if (Main.VERBOSE) System.out.println(listNew);

            HashSet<Long> noDupSetNew = new HashSet();
            noDupSetNew.addAll(listNew);
            assertEquals(SAMPLESIZE, noDupSetNew.size());
            //if (Main.VERBOSE) System.out.println(noDupSetNew);


            for (Long id : noDupSetNew) {
                assertEquals("ID: " + id, true, noDupSet.contains(id));
            }
        }
    }

    public static void testFlatJoin() throws Exception {
        createDBN1(true);
        createDataSets("dbn1",null,null);

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,
                "../datasets/simulated/cajaMarSynthetic/data0.arff", false);

        List<Long> list = data0.getDataSet().map(d -> d.getSequenceID()).collect();

        HashSet<Long> noDupSet = new HashSet();
        noDupSet.addAll(list);
        assertEquals(SAMPLESIZE, noDupSet.size());


        for (int i = 1; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    "../datasets/simulated/cajaMarSynthetic/data" + i + ".arff", false);

            DataSet<Long> dataJoin = data0.getDataSet().join(dataNew.getDataSet(), JoinOperatorBase.JoinHint.REPARTITION_HASH_FIRST).where(
                    new KeySelector<DynamicDataInstance, Long>() {
                        @Override
                        public Long getKey(DynamicDataInstance value) throws Exception {
                            return value.getSequenceID();
                        }
                    }).equalTo(new KeySelector<DynamicDataInstance, Long>() {
                @Override
                public Long getKey(DynamicDataInstance value) throws Exception {
                    return value.getSequenceID();
                }
            }).with(new FlatJoinFunction<DynamicDataInstance, DynamicDataInstance, Long>() {
                @Override
                public void join(DynamicDataInstance first, DynamicDataInstance second, Collector<Long> out) throws Exception {
                    out.collect(first.getSequenceID());
                }
            });

            List<Long> listNew = dataJoin.map(d -> d).collect();
            //if (Main.VERBOSE) System.out.println(listNew);

            HashSet<Long> noDupSetNew = new HashSet();
            noDupSetNew.addAll(listNew);
            assertEquals(SAMPLESIZE, noDupSetNew.size());
            //if (Main.VERBOSE) System.out.println(noDupSetNew);


            for (Long id : noDupSetNew) {
                assertEquals("ID: " + id, true, noDupSet.contains(id));
            }
        }
    }

    public static void update0Learn(String networkName) throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("networks/simulated/" + NETWORK_NAME);
        dbn.randomInitialization(new Random(0));

        if (Main.VERBOSE) System.out.println(dbn.toString());


        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,
                "../networks/bnaic2015/BCC/cajaMarSynthetic/data0.arff", false);
        dbn.getDynamicVariables().setAttributes(data0.getAttributes());

        DynamicParallelVB learn = new DynamicParallelVB();
        learn.setMaximumGlobalIterations(10);
        learn.setBatchSize(BATCHSIZE);
        learn.setDAG(dbn.getDynamicDAG());
        learn.setOutput(true);
        learn.initLearning();

        if (Main.VERBOSE) System.out.println("--------------- DATA " + 0 + " --------------------------");
        learn.updateModelWithNewTimeSlice(0, data0);

        if (Main.VERBOSE) System.out.println(learn.getLearntDynamicBayesianNetwork().toBayesianNetworkTime0());

        assertEquals(true, dbn.toBayesianNetworkTime0().equalBNs(learn.getLearntDynamicBayesianNetwork().toBayesianNetworkTime0(), 0.1));
    }

    public static void testUpdateN(String networkName, double threshold) throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("../networks/simulated/" + networkName+".dbn");
        dbn.randomInitialization(new Random(0));

        if (Main.VERBOSE) System.out.println(dbn.toString());


        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,
                "../datasets/simulated/cajaMarSynthetic/data0.arff", false);
        dbn.getDynamicVariables().setAttributes(data0.getAttributes());

        DynamicParallelVB learn = new DynamicParallelVB();
        learn.setMaximumGlobalIterations(20);
        learn.setGlobalThreshold(0.0001);
        learn.setLocalThreshold(0.0001);
        learn.setMaximumLocalIterations(200);
        learn.setBatchSize(BATCHSIZE);
        learn.setDAG(dbn.getDynamicDAG());
        learn.setOutput(true);
        learn.initLearning();

        if (Main.VERBOSE) System.out.println("--------------- DATA " + 0 + " --------------------------");
        learn.updateModelWithNewTimeSlice(0, data0);

        for (int i = 1; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    "../datasets/simulated/cajaMarSynthetic/data" + i + ".arff", false);
            learn.updateModelWithNewTimeSlice(i, dataNew);
            assertEquals(SAMPLESIZE, learn.dataPosteriorDataSet.count());
        }

        if (Main.VERBOSE) System.out.println(learn.getLearntDynamicBayesianNetwork());
        if (threshold>0) assertEquals(true, dbn.equalDBNs(learn.getLearntDynamicBayesianNetwork(), threshold));
        //learn.getLearntBayesianNetwork()
    }
/*
    //Fails some times on travis-ci
    public static void testHuginCajaMar() throws Exception {
        String networkName = "HuginCajaMarDefaulterPredictor";
        createDataSets(networkName,null,null);
        testUpdateN(networkName, 0.3);
    }

    //Fails some times on travis-ci
    public static void testDBN1() throws Exception {
        String networkName = "dbn1";
        createDBN1(true);
        createDataSets(networkName,null,null);
        testUpdateN(networkName, 0.1);
    }
*/
    public static void testDBN2() throws Exception {
        String networkName = "dbn2";
        createDBN2();
        createDataSets(networkName,null,null);
        testUpdateN(networkName, 0.3);
    }

    public static void testDBN3() throws Exception {
        String networkName = "dbn3";
        createDBN3();
        createDataSets(networkName,null,null);
        testUpdateN(networkName, 0.3);
    }

    public static void testDBN1Hidden() throws Exception {
        String networkName = "dbn1";
        createDBN1(true);
        createDataSets(networkName, Arrays.asList("C"),null);
        testUpdateN(networkName, 0.0);
    }

    public static void testDBN1Noise() throws Exception {
        String networkName = "dbn1";
        createDBN1(true);
        createDataSets(networkName, null, Arrays.asList("C"));
        testUpdateN(networkName, 0.1);
    }

    public static void testDBN3Hidden() throws Exception {
        String networkName = "dbn3";
        createDBN3();
        createDataSets(networkName, Arrays.asList("A0"),null);
        testUpdateN(networkName, 0.0);
    }
}