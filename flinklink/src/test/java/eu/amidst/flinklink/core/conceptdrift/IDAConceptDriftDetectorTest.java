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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
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
import eu.amidst.flinklink.core.utils.BayesianNetworkSampler;
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
public class IDAConceptDriftDetectorTest extends TestCase {

    public static int NSETS = 15;
    public static int SAMPLESIZE = 1000;
    public static int BATCHSIZE = 500;

    public static void createDataSets(String networkName, List<String> hiddenVars, List<String> noisyVars) throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);


        BayesianNetwork dbn = BayesianNetworkLoader.loadFromFile("../networks/simulated/" + networkName + ".dbn");
        dbn.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println(dbn.toString());

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(dbn);
        sampler.setBatchSize(BATCHSIZE);
        sampler.setSeed(1);

        if (hiddenVars!=null) {
            for (String hiddenVar : hiddenVars) {
                sampler.setHiddenVar(dbn.getVariables().getVariableByName(hiddenVar));
            }
        }
        if (noisyVars!=null){
            for (String noisyVar : noisyVars) {
                sampler.setMARVar(dbn.getVariables().getVariableByName(noisyVar), 0.1);
            }
        }
        for (int i = 0; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA " + i + " --------------------------");
            if (i%5==0){
                dbn.randomInitialization(new Random((long)((i+10)%2)));
                sampler = new BayesianNetworkSampler(dbn);
                sampler.setBatchSize(BATCHSIZE);
                sampler.setSeed(1);
            }
            DataFlink<DataInstance> data0 = sampler.sampleToDataFlink(env,SAMPLESIZE);
            DataFlinkWriter.writeDataToARFFFolder(data0, "../datasets/simulated/conceptdrift/data" + i + ".arff");
        }
    }


    public static void createBN1(int nVars) throws Exception {

        Variables dynamicVariables = new Variables();
        Variable classVar = dynamicVariables.newMultinomialVariable("C", 2);

        for (int i = 0; i < nVars; i++) {
            dynamicVariables.newGaussianVariable("A" + i);
        }
        DAG dag = new DAG(dynamicVariables);

        for (int i = 0; i < nVars; i++) {
            dag.getParentSet(dynamicVariables.getVariableByName("A" + i)).addParent(classVar);
        }

        dag.setName("dbn1");
        BayesianNetwork dbn = new BayesianNetwork(dag);
        dbn.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println(dbn.toString());

        BayesianNetworkWriter.save(dbn, "../networks/simulated/dbn1.dbn");
    }


    public static void testUpdateN(String networkName) throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlink<DataInstance> data0 = DataFlinkLoader.loadDataFromFolder(env,
                "../datasets/simulated/conceptdrift/data0.arff", false);

        IDAConceptDriftDetector learn = new IDAConceptDriftDetector();
        learn.setBatchSize(1000);
        learn.setClassIndex(0);
        learn.setAttributes(data0.getAttributes());
        learn.setNumberOfGlobalVars(1);
        learn.setTransitionVariance(0.1);
        learn.setSeed(0);

        learn.initLearning();
        double[] output = new double[NSETS];

        double[] out = null;
        for (int i = 0; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DataInstance> dataNew = DataFlinkLoader.loadDataFromFolder(env,
                    "../datasets/simulated/conceptdrift/data" + i + ".arff", false);
            out = learn.updateModelWithNewTimeSlice(dataNew);
            if (Main.VERBOSE) System.out.println(learn.getLearntDynamicBayesianNetwork());
            output[i] = out[0];

        }

        if (Main.VERBOSE) System.out.println(learn.getLearntDynamicBayesianNetwork());

        for (int i = 0; i < NSETS; i++) {
            if (Main.VERBOSE) System.out.println("E(H_"+i+") =\t" + output[i]);
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

        dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());
        dag.setName("dbn1");
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);
        dbn.randomInitialization(new Random(0));
        if (Main.VERBOSE) System.out.println(dbn.toString());

        DynamicBayesianNetworkWriter.save(dbn, "../networks/simulated/dbn1.dbn");
    }


    public static void createDataSetsDBN(String networkName, List<String> hiddenVars, List<String> noisyVars) throws Exception {
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
            dist.getNormal_NormalParentsDistribution(0).setIntercept(1);

            dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
            dist.getNormal_NormalParentsDistribution(1).setIntercept(1);
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
                    dist.getNormal_NormalParentsDistribution(0).setIntercept(-1);

                    dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(1).setIntercept(-1);
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
        createBN1(10);
        createDataSets(networkName,null,null);
        testUpdateN(networkName);
    }

    public static void test2()  throws Exception {
        String networkName = "dbn1";
        createDBN1(10,true);
        createDataSetsDBN(networkName,null,null);
        testUpdateN(networkName);
    }
}