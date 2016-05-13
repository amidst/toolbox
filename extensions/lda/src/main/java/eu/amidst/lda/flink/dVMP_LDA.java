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
import eu.amidst.flinklink.core.learning.parametric.dVMP;
import eu.amidst.flinklink.core.utils.ConversionToBatches;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by andresmasegosa on 12/5/16.
 */
public class dVMP_LDA {

    public static void main(String[] args) throws Exception {

        String dataPath = "hdfs:///docword.kos.arff";
        int ntopics = 5;
        int niter = 100;
        double threshold = 0.1;
        int docsPerBatch = 10;

        dataPath = args[0];

        if (args.length>1){
            ntopics = Integer.parseInt(args[1]);
            niter = Integer.parseInt(args[2]);
            threshold = Double.parseDouble(args[3]);
            docsPerBatch = Integer.parseInt(args[4]);
        }

        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        //env.setParallelism(1);
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
        svb.setSeed(5);

        svb.setBatchSize(docsPerBatch);
        svb.setDataFlink(dataInstances);
        svb.setBatchConverter(ConversionToBatches::toBatchesBySeqID);
        svb.runLearning();

    }

}
