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
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.StochasticVI;
import eu.amidst.flinklink.core.utils.ConversionToBatches;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by andresmasegosa on 12/5/16.
 */
public class SVI_LDA {

    public static void main(String[] args) throws Exception {

        String dataPath = "hdfs:///docword.kos.arff";
        int ntopics = 5;
        int niter = 100;
        double threshold = 0.1;
        int docsPerBatch = 10;
        double learningRate = 0.75;
        int timeLimit = 2000;
        int dataSize;

        dataPath = args[0];
        dataSize = Integer.parseInt(args[1]);

        if (args.length>1){
            ntopics = Integer.parseInt(args[2]);
            niter = Integer.parseInt(args[3]);
            threshold = Double.parseDouble(args[4]);
            docsPerBatch = Integer.parseInt(args[5]);
            learningRate = Double.parseDouble(args[6]);
            timeLimit = Integer.parseInt(args[7]);
        }

        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        //env.setParallelism(1);
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
        svb.setDataFlink(dataInstances);
        svb.setBatchConverter(ConversionToBatches::toBatchesBySeqID);
        svb.runLearning();

    }

}
