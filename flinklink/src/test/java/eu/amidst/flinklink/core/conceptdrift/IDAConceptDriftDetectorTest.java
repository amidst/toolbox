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

package eu.amidst.flinklink.core.conceptdrift;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.utils.BayesianNetworkSampler;
import junit.framework.TestCase;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 9/12/15.
 */
public class IDAConceptDriftDetectorTest extends TestCase {

    public static int NSETS = 20;
    public static int SAMPLESIZE = 1000;
    public static int BATCHSIZE = 500;

    public static void createDataSets(String networkName, List<String> hiddenVars, List<String> noisyVars) throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        BayesianNetwork dbn = BayesianNetworkLoader.loadFromFile("networks/" + networkName + ".dbn");
        dbn.randomInitialization(new Random(0));
        System.out.println(dbn.toString());

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
            System.out.println("--------------- DATA " + i + " --------------------------");
            if (i%5==0){
                dbn.randomInitialization(new Random(i));
                sampler = new BayesianNetworkSampler(dbn);
                sampler.setBatchSize(BATCHSIZE);
                sampler.setSeed(1);
            }
            DataFlink<DataInstance> data0 = sampler.sampleToDataFlink(SAMPLESIZE);
            DataFlinkWriter.writeDataToARFFFolder(data0, "./datasets/dataFlink/conceptdrift/data" + i + ".arff");
        }
    }


    public static void createBN1(int nVars) throws Exception {

        Variables dynamicVariables = new Variables();
        Variable classVar = dynamicVariables.newMultionomialVariable("C", 2);

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
        System.out.println(dbn.toString());

        BayesianNetworkWriter.saveToFile(dbn, "./networks/dbn1.dbn");
    }


    public static void testUpdateN(String networkName) throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> data0 = DataFlinkLoader.loadDataFromFolder(env,
                "./datasets/dataFlink/conceptdrift/data0.arff", false);

        IDAConceptDriftDetector learn = new IDAConceptDriftDetector();
        learn.setBatchSize(100);
        learn.setClassIndex(0);
        learn.setAttributes(data0.getAttributes());
        learn.setNumberOfGlobalVars(1);
        learn.setTransitionVariance(0.1);
        learn.setSeed(0);

        learn.initLearning();
        double[] output = new double[NSETS];

        System.out.println("--------------- DATA " + 0 + " --------------------------");
        double[] out = learn.updateModelWithNewTimeSlice(data0);
        output[0] = out[0];

        for (int i = 1; i < NSETS; i++) {
            System.out.println("--------------- DATA " + i + " --------------------------");
            DataFlink<DataInstance> dataNew = DataFlinkLoader.loadDataFromFolder(env,
                    "./datasets/dataFlink/conceptdrift/data" + i + ".arff", false);
            out = learn.updateModelWithNewTimeSlice(dataNew);
            output[i] = out[0];

        }

        System.out.println(learn.getLearntDynamicBayesianNetwork());

        for (int i = 0; i < NSETS; i++) {
            System.out.println("E(H_"+i+") =\t" + output[i]);
        }

    }

    public static void test1()  throws Exception {
        String networkName = "dbn1";
        createBN1(2);
        createDataSets(networkName,null,null);
        testUpdateN(networkName);
    }

}