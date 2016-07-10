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

package eu.amidst.flinklink.examples;

import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import eu.amidst.flinklink.core.utils.DBNSampler;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Created by Hanen on 08/10/15.
 */
public class DynamicDataSets {

    static Logger logger = LoggerFactory.getLogger(DynamicDataSets.class);


    public void generateDynamicDataset(String[] args) throws Exception {
        int nCVars = Integer.parseInt(args[0]);
        int nMVars = Integer.parseInt(args[1]);
        int nSamples = Integer.parseInt(args[2]);
        int windowSize = Integer.parseInt(args[3]);
        int nsets = Integer.parseInt(args[4]);
        int seed = Integer.parseInt(args[5]);

        /*
         * Logging
         */
        //PropertyConfigurator.configure(args[6]);
        BasicConfigurator.configure();

        logger.info("Starting DynmaicDataSets experiments");


        //String fileName = "hdfs:///tmp"+nCVars+"_"+nMVars+"_"+nSamples+"_"+nsets+"_"+seed;
        String fileName = "./datasets/tmp"+nCVars+"_"+nMVars+"_"+nSamples+"_"+nsets+"_"+seed;

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(nCVars);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(nMVars);
        DynamicDAG dynamicDAG = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayesDAG(2,true);
        DynamicBayesianNetwork dbn = new DynamicBayesianNetwork(dynamicDAG);
        dbn.randomInitialization(new Random(seed));

        //Sampling from Asia BN
        DBNSampler sampler = new DBNSampler(dbn);
        sampler.setSeed(seed);
        sampler.setBatchSize(windowSize);
        sampler.setNSamples(nSamples);

        DataFlink<DynamicDataInstance> data0 = sampler.cascadingSample(env,null);

        logger.info("--------------- DATA " + 0 + " --------------------------");
        DataFlinkWriter.writeDataToARFFFolder(data0, fileName+"_iter_"+0+".arff");
        data0 = DataFlinkLoader.loadDynamicDataFromFolder(env, fileName+"_iter_"+0+".arff", false);


        DataFlink<DynamicDataInstance> dataPrev = data0;
        for (int i = 1; i < nsets; i++) {
            logger.info("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = sampler.cascadingSample(env,dataPrev);
            DataFlinkWriter.writeDataToARFFFolder(dataNew, fileName+"_iter_"+i+".arff");
            dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env, fileName+"_iter_"+i+".arff", false);
            dataPrev = dataNew;
        }
    }

    /**
     *
     * ./bin/flink run -m yarn-cluster -yn 8 -ys 4 -yjm 1024 -ytm 9000
     *              -c eu.amidst.flinklink.examples.DynamicDataSets ../flinklink.jar 0 0 1000000 100 3 0
     *
     *
     *
     * @param args command line arguments
     * @throws Exception
     */

    public static void main(String[] args) throws Exception {
        DynamicDataSets dynamicDataSets = new DynamicDataSets();
        dynamicDataSets.generateDynamicDataset(args);
    }



}
