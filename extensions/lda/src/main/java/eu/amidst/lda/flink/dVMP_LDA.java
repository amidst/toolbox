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
import eu.amidst.flinklink.core.learning.parametric.dVMP;
import eu.amidst.flinklink.core.utils.ConversionToBatches;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.util.Arrays;

/**
 * Created by andresmasegosa on 12/5/16.
 */
public class dVMP_LDA {

    public static void main(String[] args) throws Exception {

        String dataPath = "/Users/ana/Dropbox/amidst_postdoc/abstracts/abstracts.all.shuffled_train.arff";
        String dataTest = "/Users/ana/Dropbox/amidst_postdoc/abstracts/abstracts.all.shuffled_test.arff";
        int ntopics = 5;
        int niter = 100;
        double threshold = 0.1;
        int docsPerBatch = 10;
        int timeLimit = -1;
        int ncores = 4;
        boolean amazon_cluster = true;


        if (args.length>1){
            dataPath = args[0];
            dataTest = args[1];
            ntopics = Integer.parseInt(args[2]);
            niter = Integer.parseInt(args[3]);
            threshold = Double.parseDouble(args[4]);
            docsPerBatch = Integer.parseInt(args[5]);
            timeLimit = Integer.parseInt(args[6]);
            ncores = Integer.parseInt(args[7]);
            amazon_cluster = Boolean.parseBoolean(args[8]);
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


        dVMP svb = new dVMP();
        PlateauLDAFlink plateauLDA = new PlateauLDAFlink(dataInstances.getAttributes(),"word","count");
        plateauLDA.setNTopics(ntopics);
        svb.setPlateuStructure(plateauLDA);

        svb.setOutput(true);
        svb.setMaximumGlobalIterations(niter);
        svb.setMaximumLocalIterations(niter);
        svb.setLocalThreshold(threshold);
        svb.setGlobalThreshold(threshold);
        svb.setTimeLimit(timeLimit);
        svb.setSeed(5);

        svb.setBatchSize(docsPerBatch);
        svb.setBatchConverter(ConversionToBatches::toBatchesBySeqID);
        svb.setDAG(plateauLDA.getDagLDA());
        svb.initLearning();
        svb.updateModel(dataInstances);


        DataFlink<DataInstance> instancesTest = DataFlinkLoader.loadDataFromFile(env, dataTest, false);

        double test_log_likelihood = StochasticVI.computeELBO(instancesTest,svb.getSVB(),ConversionToBatches::toBatchesBySeqID);

        System.out.println("TEST LOG_LIKE: " + test_log_likelihood);


        if (args.length>0) {
            args[0] = "";
            args[1] = "";
        }
        String pathNetwork = "dVMP_"+ Arrays.toString(args)+"_.bn";

        svb.getSVB().setDAG(((PlateauLDAFlink)svb.getSVB().getPlateuStructure()).getDagLDA());
        System.out.println(svb.getLearntBayesianNetwork().toString());

        BayesianNetworkWriter.save(svb.getLearntBayesianNetwork(),pathNetwork);

    }

}
