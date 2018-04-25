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

package eu.amidst.flinklink.core.conceptdrift;

import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
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
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 9/12/15.
 */
public class IDAConceptDriftDetectorDBNTest extends TestCase {

    public static int NSETS = 15;
    public static int SAMPLESIZE = 1000;
    public static int BATCHSIZE = 500;

    public static void createDataSets(String networkName, List<String> hiddenVars, List<String> noisyVars) throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("networks/simulated/" + networkName + ".dbn");
        dbn.randomInitialization(new Random(1));
        if (Main.VERBOSE) System.out.println(dbn.toString());

        DBNSampler sampler = new DBNSampler(dbn);
        sampler.setNSamples(SAMPLESIZE);
        sampler.setBatchSize(BATCHSIZE);
        sampler.setSeed(0);

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


        DataFlinkWriter.writeDataToARFFFolder(data0, "../datasets/simulated/conceptdrift/data0.arff");
        data0 = DataFlinkLoader.loadDynamicDataFromFolder(env, "../datasets/simulated//conceptdrift/data0.arff", false);

        List<Long> list = data0.getDataSet().map(d -> d.getSequenceID()).collect();
        if (Main.VERBOSE) System.out.println(list);

        HashSet<Long> noDupSet = new HashSet();
        noDupSet.addAll(list);
        assertEquals(SAMPLESIZE, noDupSet.size());
        if (Main.VERBOSE) System.out.println(noDupSet);


        DataFlink<DynamicDataInstance> dataPrev = data0;
        for (int i = 1; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = sampler.cascadingSampleConceptDrift(env,dataPrev, i%4==1);
            DataFlinkWriter.writeDataToARFFFolder(dataNew, "../datasets/simulated/conceptdrift/data" + i + ".arff");
            dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env, "../datasets/simulated/conceptdrift/data" + i + ".arff", false);
            dataPrev = dataNew;
        }
    }


    public static void createDBN1(int nvars, boolean connect) throws Exception {

        DynamicVariables dynamicVariables = new DynamicVariables();
        Variable classVar = dynamicVariables.newMultinomialDynamicVariable("C", 2);

        for (int i = 0; i < nvars; i++) {
            dynamicVariables.newGaussianDynamicVariable("A" + i);
        }
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        for (int i = 0; i < nvars; i++) {
            dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(classVar);
            if (connect) dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(dynamicVariables.getVariableByName("A" + i).getInterfaceVariable());

        }

        //dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());

        dag.setName("dbn1");
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);
        dbn.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println(dbn.toString());

        DynamicBayesianNetworkWriter.save(dbn, "../networks/simulated/dbn1.dbn");
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
                "../datasets/simulated/conceptdrift/data0.arff", false);
        dbn.getDynamicVariables().setAttributes(data0.getAttributes());

        IDAConceptDriftDetectorDBN learn = new IDAConceptDriftDetectorDBN();
        learn.setBatchSize(100);
        learn.setClassIndex(0);
        learn.setAttributes(data0.getAttributes());
        learn.setNumberOfGlobalVars(1);
        learn.setTransitionVariance(0.1);
        learn.setSeed(0);

        learn.initLearning();
        double[] output = new double[NSETS];

        if (Main.VERBOSE) System.out.println("--------------- DATA " + 0 + " --------------------------");
        double[] out = learn.updateModelWithNewTimeSlice(0, data0);
        output[0] = out[0];

        for (int i = 1; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    "../datasets/simulated/conceptdrift/data" + i + ".arff", false);
            out = learn.updateModelWithNewTimeSlice(i, dataNew);
            output[i] = out[0];

            if (Main.VERBOSE) System.out.println(learn.getLearntDynamicBayesianNetwork());

        }

        if (Main.VERBOSE) System.out.println(learn.getLearntDynamicBayesianNetwork());

        for (int i = 0; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("E(H_"+i+") =\t" + output[i]);
        }

    }

    public static void createDataSetsDBN2(String networkName, List<String> hiddenVars, List<String> noisyVars) throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DynamicBayesianNetwork dbn = DynamicBayesianNetworkLoader.loadFromFile("../networks/simulated/" + networkName + ".dbn");
        dbn.randomInitialization(new Random(0));

        for (Variable variable : dbn.getDynamicVariables()) {
            if (!variable.getName().startsWith("A"))
                continue;


            Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
            dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
            dist.getNormal_NormalParentsDistribution(0).setIntercept(10);

            dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
            dist.getNormal_NormalParentsDistribution(1).setIntercept(10);
        }

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


        DataFlinkWriter.writeDataToARFFFolder(data0, "../datasets/simulated/conceptdrift/data0.arff");
        data0 = DataFlinkLoader.loadDynamicDataFromFolder(env, "../datasets/simulated/conceptdrift/data0.arff", false);

        List<Long> list = data0.getDataSet().map(d -> d.getSequenceID()).collect();
        if (Main.VERBOSE) System.out.println(list);

        HashSet<Long> noDupSet = new HashSet();
        noDupSet.addAll(list);
        assertEquals(SAMPLESIZE, noDupSet.size());
        if (Main.VERBOSE) System.out.println(noDupSet);


        DataFlink<DynamicDataInstance> dataPrev = data0;
        for (int i = 1; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA " + i + " --------------------------");
            if (i==5){
                for (Variable variable : dbn.getDynamicVariables()) {
                    if (!variable.getName().startsWith("A"))
                        continue;


                    Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
                    dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(0).setIntercept(0);

                    dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(1).setIntercept(0);
                }
                if (Main.VERBOSE) System.out.println(dbn);
                sampler.setDBN(dbn);
            }
            if (i==10){
                for (Variable variable : dbn.getDynamicVariables()) {
                    if (!variable.getName().startsWith("A"))
                        continue;


                    Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
                    dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(0).setIntercept(-10);

                    dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(1).setIntercept(-10);
                }
                if (Main.VERBOSE) System.out.println(dbn);
                sampler.setDBN(dbn);
            }
            DataFlink<DynamicDataInstance> dataNew = sampler.cascadingSample(env,dataPrev);//i%4==1);
            DataFlinkWriter.writeDataToARFFFolder(dataNew, "../datasets/simulated/conceptdrift/data" + i + ".arff");
            dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env, "../datasets/simulated/conceptdrift/data" + i + ".arff", false);
            dataPrev = dataNew;
        }
    }

    public static void test1()  throws Exception {
        String networkName = "dbn1";
        createDBN1(10,true);
        createDataSetsDBN2(networkName,null,null);
        testUpdateN(networkName, 0.1);
    }

}