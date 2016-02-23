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

package eu.amidst.modelExperiments;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.StochasticVI;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.amidst.modelExperiments.DAGsGeneration.getUAIMultiLocalGlobalDAG;

/**
 * Created by ana@cs.aau.dk on 08/02/16.
 */
public class MixtureModelDistributedSVI {

    static Logger logger = LoggerFactory.getLogger(MixtureModelDistributedSVI.class);

    public static void main(String[] args) throws Exception {

        //String fileName = "hdfs:///tmp_uai100K.arff";
        //String fileName = "./datasets/dataFlink/uai1K.arff";
        //args= new String[]{" " +
        //        "/Users/andresmasegosa/Desktop/cajamardata/ALL-AGGREGATED/totalWeka-ContinuousReducedFolder.arff",
        //        "1000", "100", "1", "1000", "0", "55000", "0.75", "1", "2"};

        String fileName = args[0];

        int windowSize = Integer.parseInt(args[1]);
        int localIter = Integer.parseInt(args[2]);
        double localThreshold = Double.parseDouble(args[3]);
        long timeLimit = Long.parseLong(args[4]);
        int seed = Integer.parseInt(args[5]);
        int dataSetSize = Integer.parseInt(args[6]);
        double learningRate = Double.parseDouble(args[7]);
        int nStates = Integer.parseInt(args[8]);
        int nParallelDegree = Integer.parseInt(args[9]);

        //BasicConfigurator.configure();
        //PropertyConfigurator.configure(args[4]);

        // set up the execution environment
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 16000);
        conf.setInteger("taskmanager.numberOfTaskSlots",nParallelDegree);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);

        env.setParallelism(nParallelDegree);


        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env,fileName, false);

        DAG hiddenNB = getUAIMultiLocalGlobalDAG(dataFlink.getAttributes(), nStates);
        long start = System.nanoTime();

        //Parameter Learning
        StochasticVI stochasticVI = new StochasticVI();
        stochasticVI.setLocalThreshold(localThreshold);
        stochasticVI.setMaximumLocalIterations(localIter);
        stochasticVI.setSeed(seed);
        //Set the window size
        stochasticVI.setBatchSize(windowSize);

        stochasticVI.setLearningFactor(learningRate);
        stochasticVI.setDataSetSize(dataSetSize);
        stochasticVI.setTimiLimit(timeLimit);


        //List<Variable> hiddenVars = new ArrayList<>();
        //hiddenVars.add(hiddenNB.getVariables().getVariableByName("GlobalHidden"));
        //stochasticVI.setPlateuStructure(new PlateuStructure(hiddenVars));
        //GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars, 0, 1);
        //gaussianHiddenTransitionMethod.setFading(1.0);
        //stochasticVI.setTransitionMethod(gaussianHiddenTransitionMethod);

        stochasticVI.setOutput(true);
        stochasticVI.setDAG(hiddenNB);
        stochasticVI.setDataFlink(dataFlink);
        stochasticVI.runLearning();
        BayesianNetwork LearnedBnet = stochasticVI.getLearntBayesianNetwork();
        System.out.println(LearnedBnet.toString());

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        logger.info("Total running time: {} seconds.", seconds);

    }

}
