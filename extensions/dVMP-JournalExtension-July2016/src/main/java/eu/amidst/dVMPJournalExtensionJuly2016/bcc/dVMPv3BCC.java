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

package eu.amidst.dVMPJournalExtensionJuly2016.bcc;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.dVMPJournalExtensionJuly2016.DAGsGeneration;
import eu.amidst.dVMPJournalExtensionJuly2016.dVMPv3;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.StochasticVI;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ana@cs.aau.dk on 08/02/16.
 */
public class dVMPv3BCC {

    static Logger logger = LoggerFactory.getLogger(dVMPv3BCC.class);

    public static void main(String[] args) throws Exception {

        String fileName = args[0]+"_train.arff";
        String fileTest = args[0]+"_test.arff";
        int windowSize = Integer.parseInt(args[1]);
        long timeLimit = Long.parseLong(args[2]);
        int nStates = Integer.parseInt(args[3]);
        String model =args[4];

        int globalIter = 100;
        double globalThreshold = 0.0000000001;
        int localIter = 50;
        double localThreshold = 0.001;
        int seed = 0;
        int nParallelDegree = 32;


        //BasicConfigurator.configure();
        //PropertyConfigurator.configure(args[4]);

        // set up the execution environment
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 16000);
        conf.setInteger("taskmanager.numberOfTaskSlots",nParallelDegree);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.setParallelism(nParallelDegree);
        env.getConfig().disableSysoutLogging();

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env, fileName, false);

        DAG hiddenNB = null;
        if (model.compareTo("mixture")==0){
            hiddenNB = DAGsGeneration.getBCCMixtureDAG(dataFlink.getAttributes(), nStates);
        }else if (model.compareTo("FA")==0){
            hiddenNB = DAGsGeneration.getBCCFADAG(dataFlink.getAttributes(), nStates);
        }else if (model.compareTo("LR")==0) {
            hiddenNB = DAGsGeneration.getBCCLRDAG(dataFlink.getAttributes());
        }

        long start = System.nanoTime();

        //Parameter Learning
        dVMPv3 parallelVB = new dVMPv3();
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
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork LearnedBnet = parallelVB.getLearntBayesianNetwork();

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            builder.append(args[i]);
            builder.append("_");
        }
        BayesianNetworkWriter.save(LearnedBnet, "./dVMPv2BCC_"+ builder.toString() +".bn");
        System.out.println(LearnedBnet.toString());

        /// TEST

        DataFlink<DataInstance>  dataTest = DataFlinkLoader.loadDataFromFolder(env,fileTest, false);

        double elboTest = StochasticVI.computeELBO(dataTest,parallelVB.getSVB());

        System.out.println("Test Marginal-Loglikelihood:" + elboTest);


        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.println("Total running time:" + seconds+ "seconds.");
        logger.info("Total running time: {} seconds.", seconds);

    }

}
