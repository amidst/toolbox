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

package eu.amidst.flinklink.examples.reviewMeeting2015;

import eu.amidst.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.distribution.Normal_MultinomialNormalParents;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.utils.BayesianNetworkSampler;
import eu.amidst.flinklink.core.utils.DBNSampler;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.util.List;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 19/01/16.
 */
public class GenerateData {

    public static int BATCHSIZE = 500;

    public static boolean connectDBN = true;

    public static BayesianNetwork createBN(int nVars) throws Exception {

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
        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(1));

        return bn;
    }

    public static void createDataSets(List<String> hiddenVars, List<String> noisyVars, int numVars, int SAMPLESIZE,
                                      int NSETS) throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.getConfig().disableSysoutLogging();
        env.setParallelism(Main.PARALLELISM);

        BayesianNetwork bn = createBN(numVars);

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setBatchSize(BATCHSIZE);
        sampler.setSeed(1);

        if (hiddenVars!=null) {
            for (String hiddenVar : hiddenVars) {
                sampler.setHiddenVar(bn.getVariables().getVariableByName(hiddenVar));
            }
        }
        if (noisyVars!=null){
            for (String noisyVar : noisyVars) {
                sampler.setMARVar(bn.getVariables().getVariableByName(noisyVar), 0.1);
            }
        }
        for (int i = 0; i < NSETS; i++) {
            System.out.println("--------------- CREATING DATA " + i + " --------------------------");
            if (i%5==0){
                bn.randomInitialization(new Random((long)((i+10)%2)));
                sampler = new BayesianNetworkSampler(bn);
                sampler.setBatchSize(BATCHSIZE);
                sampler.setSeed(1);
            }
            DataFlink<DataInstance> data0 = sampler.sampleToDataFlink(env,SAMPLESIZE);
            DataFlinkWriter.writeDataToARFFFolder(data0, "./datasets/simulated/tmp_conceptdrift_data" + i + ".arff");
        }
    }

    public static DynamicBayesianNetwork createDBN1(int numVars) throws Exception {

        DynamicVariables dynamicVariables = new DynamicVariables();
        Variable classVar = dynamicVariables.newMultinomialDynamicVariable("C", 2);

        for (int i = 0; i < numVars; i++) {
            dynamicVariables.newGaussianDynamicVariable("A" + i);
        }
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        for (int i = 0; i < numVars; i++) {
            dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(classVar);
            if (connectDBN) dag.getParentSetTimeT(dynamicVariables.getVariableByName("A" + i)).addParent(dynamicVariables.getVariableByName("A" + i).getInterfaceVariable());

        }

        //dag.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());
        dag.setName("dbn1");
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dag);
        dbn.randomInitialization(new Random(1));

        return dbn;
    }

    public static void createDataSetsDBN(List<String> hiddenVars, List<String> noisyVars, int numVars, int SAMPLESIZE,
                                         int NSETS) throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.getConfig().disableSysoutLogging();
        env.setParallelism(Main.PARALLELISM);

        DynamicBayesianNetwork dbn = createDBN1(numVars);

        for (Variable variable : dbn.getDynamicVariables()) {
            if (!variable.getName().startsWith("A"))
                continue;


            Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
            dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
            dist.getNormal_NormalParentsDistribution(0).setIntercept(10);

            dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
            dist.getNormal_NormalParentsDistribution(1).setIntercept(10);
        }

        //System.out.println(dbn.toString());

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


        System.out.println("--------------- CREATING DATA 0 --------------------------");
        DataFlinkWriter.writeDataToARFFFolder(data0, "./datasets/simulated/tmp_conceptdrift_data0.arff");
        data0 = DataFlinkLoader.loadDynamicDataFromFolder(env, "./datasets/simulated/tmp_conceptdrift_data0.arff", false);

        List<Long> list = data0.getDataSet().map(d -> d.getSequenceID()).collect();
        //System.out.println(list);


        DataFlink<DynamicDataInstance> dataPrev = data0;
        for (int i = 1; i < NSETS; i++) {
            System.out.println("--------------- CREATING DATA " + i + " --------------------------");
            if (i==4){
                for (Variable variable : dbn.getDynamicVariables()) {
                    if (!variable.getName().startsWith("A"))
                        continue;


                    Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
                    dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(0).setIntercept(0);

                    dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(1).setIntercept(0);
                }
                //System.out.println(dbn);
                sampler.setDBN(dbn);
            }
            if (i==7){
                for (Variable variable : dbn.getDynamicVariables()) {
                    if (!variable.getName().startsWith("A"))
                        continue;


                    Normal_MultinomialNormalParents dist = dbn.getConditionalDistributionTimeT(variable);
                    dist.getNormal_NormalParentsDistribution(0).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(0).setIntercept(-10);

                    dist.getNormal_NormalParentsDistribution(1).setCoeffParents(new double[]{1.0});
                    dist.getNormal_NormalParentsDistribution(1).setIntercept(-10);
                }
                //System.out.println(dbn);
                sampler.setDBN(dbn);
            }
            DataFlink<DynamicDataInstance> dataNew = sampler.cascadingSample(env,dataPrev);//i%4==1);
            DataFlinkWriter.writeDataToARFFFolder(dataNew, "./datasets/simulated/tmp_conceptdrift_data" + i + ".arff");
            dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env, "./datasets/simulated/tmp_conceptdrift_data" + i + ".arff", false);
            dataPrev = dataNew;
        }
    }


    public static void main(String[] args) throws Exception {

        int numVars = Integer.parseInt(args[0]);
        int SAMPLESIZE = Integer.parseInt(args[1]);
        int NSETS = Integer.parseInt(args[2]);
        boolean sampleFromDBN = Boolean.parseBoolean(args[3]);

        if (sampleFromDBN)
            createDataSetsDBN(null, null, numVars, SAMPLESIZE, NSETS);
        else
            createDataSets(null, null, numVars, SAMPLESIZE, NSETS);

    }
}
