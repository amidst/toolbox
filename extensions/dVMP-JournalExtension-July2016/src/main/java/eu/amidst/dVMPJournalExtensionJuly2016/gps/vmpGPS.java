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

package eu.amidst.dVMPJournalExtensionJuly2016.gps;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.dVMPJournalExtensionJuly2016.DAGsGeneration;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ana@cs.aau.dk on 08/02/16.
 */
public class vmpGPS {

    static Logger logger = LoggerFactory.getLogger(vmpGPS.class);

    public static void main(String[] args) throws Exception {

        String fileName = args[0]+"_train.arff";
        String fileTest = args[0]+"_test.arff";
        int windowSize = Integer.parseInt(args[1]);
        long timeLimit = Long.parseLong(args[2]);
        int nStates = Integer.parseInt(args[3]);
        String model =args[4];

        int globalIter = 100;
        double globalThreshold = 0.0000000001;
        int localIter = 100;
        double localThreshold = 0.0000000001;
        int seed = 0;
        int nParallelDegree = 1;


        //BasicConfigurator.configure();
        //PropertyConfigurator.configure(args[4]);

        // set up the execution environment
        Configuration conf = new Configuration();
        //conf.setInteger("taskmanager.network.numberOfBuffers", 16000);
        conf.setInteger("taskmanager.numberOfTaskSlots",nParallelDegree);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.setParallelism(nParallelDegree);
        env.getConfig().disableSysoutLogging();

        DataStream<DataInstance> dataFlink = DataStreamLoader.open(fileName);


        DAG hiddenNB = null;
        if (model.compareTo("mixture")==0){
            hiddenNB = DAGsGeneration.getGPSMixtureDAG(dataFlink.getAttributes(), nStates);
        }else if (model.compareTo("FA")==0){
            hiddenNB = DAGsGeneration.getGPSFADAG(dataFlink.getAttributes(), nStates);
        }

        long start = System.nanoTime();

        //Parameter Learning
        SVB parallelVB = new SVB();
        parallelVB.setOutput(true);
        parallelVB.getPlateuStructure().getVMP().setThreshold(localThreshold);
        parallelVB.getPlateuStructure().getVMP().setMaxIter(localIter);
        parallelVB.setSeed(seed);

        //Set the window size
        parallelVB.setWindowsSize(windowSize);


        parallelVB.setOutput(true);
        parallelVB.setDAG(hiddenNB);
        parallelVB.initLearning();
        parallelVB.updateModel(dataFlink);
        BayesianNetwork LearnedBnet = parallelVB.getLearntBayesianNetwork();

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= 4; i++) {
            builder.append(args[i]);
            builder.append("_");
        }
        BayesianNetworkWriter.save(LearnedBnet, "./dVMPv1GPS_"+ builder.toString() +".bn");
        System.out.println(LearnedBnet.toString());


        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.println("Total running time:" + seconds+ "seconds.");
        logger.info("Total running time: {} seconds.", seconds);

    }

}
