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

package eu.amidst.lda.flink;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.StochasticVI;
import eu.amidst.flinklink.core.utils.ConversionToBatches;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.util.Arrays;

/**
 * Created by andresmasegosa on 12/5/16.
 */
public class SVI_LDA {

    public static void main(String[] args) throws Exception {

        String dataPath = "/Users/ana/Dropbox/amidst_postdoc/abstracts/abstracts.all.shuffled_train.arff";
        String dataTest = "/Users/ana/Dropbox/amidst_postdoc/abstracts/abstracts.all.shuffled_test.arff";

        int ntopics = 5;
        int niter = 100;
        double threshold = 0.1;
        int docsPerBatch = 10;
        double learningRate = 0.75;
        int timeLimit = -1;
        int dataSize = 98851; //Number of documents at training
        int ncores = 4;
        boolean amazon_cluster = true;

        if (args.length>1){
            dataPath = args[0];
            dataTest = args[1];
            dataSize = Integer.parseInt(args[2]);
            ntopics = Integer.parseInt(args[3]);
            niter = Integer.parseInt(args[4]);
            threshold = Double.parseDouble(args[5]);
            docsPerBatch = Integer.parseInt(args[6]);
            learningRate = Double.parseDouble(args[7]);
            timeLimit = Integer.parseInt(args[8]);
            ncores = Integer.parseInt(args[9]);
            amazon_cluster = Boolean.parseBoolean(args[10]);
        }

        final ExecutionEnvironment env;

        if(amazon_cluster){
            env = ExecutionEnvironment.getExecutionEnvironment();
            env.getConfig().disableSysoutLogging();
        }else{
            Configuration conf = new Configuration();
            conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
            conf.setInteger("taskmanager.numberOfTaskSlots",ncores);
            env = ExecutionEnvironment.createLocalEnvironment(conf);
            env.setParallelism(ncores);
            env.getConfig().disableSysoutLogging();
        }

        DataFlink<DataInstance> dataInstances = DataFlinkLoader.loadDataFromFile(env, dataPath, false);


        StochasticVI svb = new StochasticVI();
        PlateauLDAFlink plateauLDA = new PlateauLDAFlink(dataInstances.getAttributes(),"word","count");
        plateauLDA.setNTopics(ntopics);
        svb.setPlateuStructure(plateauLDA);

        svb.setOutput(true);
        svb.setMaximumLocalIterations(niter);
        svb.setLocalThreshold(threshold);
        svb.setSeed(5);

        svb.setBatchSize(docsPerBatch);
        svb.setLearningFactor(learningRate);
        svb.setDataSetSize(dataSize);
        svb.setTimiLimit(timeLimit);
        svb.setBatchConverter(ConversionToBatches::toBatchesBySeqID);
        svb.initLearning();
        svb.updateModel(dataInstances);


        DataFlink<DataInstance> instancesTest = DataFlinkLoader.loadDataFromFile(env, dataTest, false);

        double test_log_likelihood = StochasticVI.computeELBO(instancesTest,svb.getSVI().getSVB(),ConversionToBatches::toBatchesBySeqID);

        System.out.println("TEST LOG_LIKE: " + test_log_likelihood);


        if(args.length>0) {
            args[0] = "";
            args[1] = "";
        }
        String pathNetwork = "SVI_"+ Arrays.toString(args)+"_.bn";

        svb.getSVI().getSVB().setDAG(((PlateauLDAFlink)svb.getSVI().getSVB().getPlateuStructure()).getDagLDA());
        System.out.println(svb.getLearntBayesianNetwork().toString());

        BayesianNetworkWriter.save(svb.getLearntBayesianNetwork(),pathNetwork);


    }

}
