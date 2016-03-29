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

package eu.amidst.modelExperiments;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.dataGeneration.AddMissingValues;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.dVMP;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.amidst.modelExperiments.DAGsGeneration.getUAIMultiLocalGlobalDAG;

/**
 * Created by ana@cs.aau.dk on 08/02/16.
 */
public class MixtureModelDistributedVMPAmazon {

    static Logger logger = LoggerFactory.getLogger(MixtureModelDistributedVMP.class);

    public static void main(String[] args) throws Exception {

        //args= new String[]{" " +
        //        "/Users/andresmasegosa/Desktop/cajamardata/ALL-AGGREGATED/totalWeka-ContinuousReducedFolder.arff",
        //        "./datasets/dataFlink/data.arff",
        //        "550", "1000", "0.00001", "100", "1", "200", "0", "1", "2"};

        //String fileName = "hdfs:///tmp_uai100K.arff";
        //String fileName = "./datasets/dataFlink/uai1K.arff";
        String fileName = args[0];

        int windowSize = Integer.parseInt(args[1]);
        int globalIter = Integer.parseInt(args[2]);
        double globalThreshold = Double.parseDouble(args[3]);
        int localIter = Integer.parseInt(args[4]);
        double localThreshold = Double.parseDouble(args[5]);
        long timeLimit = Long.parseLong(args[6]);
        int seed = Integer.parseInt(args[7]);
        int nStates = 2;
        boolean addMissingValues = Boolean.parseBoolean(args[8]);

        //BasicConfigurator.configure();
        //PropertyConfigurator.configure(args[4]);

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> dataFlink;
        /*
         * Add Missing Values
         */
        if(addMissingValues){
            AddMissingValues.addMissingValuesToFile(fileName,fileName.replace(".arff","withMissing.arff"));
            dataFlink = DataFlinkLoader.loadDataFromFolder(env,fileName.replace(".arff","withMissing.arff"), false);
        }
        else {
            dataFlink = DataFlinkLoader.loadDataFromFolder(env, fileName, false);
        }

        //DAG hiddenNB = getIDALocalGlobalDAG(dataFlink.getAttributes());
        DAG hiddenNB = getUAIMultiLocalGlobalDAG(dataFlink.getAttributes(), nStates);

        long start = System.nanoTime();

        //Parameter Learning
        dVMP parallelVB = new dVMP();
        parallelVB.setOutput(true);
        parallelVB.setGlobalThreshold(globalThreshold);
        parallelVB.setMaximumGlobalIterations(globalIter);
        parallelVB.setLocalThreshold(localThreshold);
        parallelVB.setMaximumLocalIterations(localIter);
        parallelVB.setTimeLimit(timeLimit);
        parallelVB.setSeed(seed);

        //Set the window size
        parallelVB.setBatchSize(windowSize);

        parallelVB.setOutput(true);
        parallelVB.setDAG(hiddenNB);
        parallelVB.setDataFlink(dataFlink);
        parallelVB.runLearning();
        BayesianNetwork LearnedBnet = parallelVB.getLearntBayesianNetwork();

        System.out.println(LearnedBnet.toString());

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.println("Total running time:" + seconds+ "seconds.");
        logger.info("Total running time: {} seconds.", seconds);

    }

}
