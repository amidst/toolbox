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

package eu.amidst.flinklink.examples.reviewMeeting2015;

import eu.amidst.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.flinklink.core.conceptdrift.IDAConceptDriftDetector;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

/**
 * Created by ana@cs.aau.dk on 18/01/16.
 */
public class ConceptDriftDetector {

    //public int NSETS = 15;


    public static void learnIDAConceptDriftDetector(int NSETS) throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.getConfig().disableSysoutLogging();
        env.setParallelism(Main.PARALLELISM);

      //  DataFlink<DataInstance> data0 = DataFlinkLoader.loadDataFromFolder(env,
      //          "hdfs:///tmp_conceptdrift_data0.arff", false);

          DataFlink<DataInstance> data0 = DataFlinkLoader.open(env,
                  "./datasets/simulated/tmp_conceptdrift_data0.arff", false);

        long start = System.nanoTime();
        IDAConceptDriftDetector learn = new IDAConceptDriftDetector();
        learn.setBatchSize(1000);
        learn.setClassIndex(0);
        learn.setAttributes(data0.getAttributes());
        learn.setNumberOfGlobalVars(1);
        learn.setTransitionVariance(0.1);
        learn.setSeed(0);

        learn.initLearning();

        System.out.println(learn.getGlobalDAG().toString());

        double[] output = new double[NSETS];

        System.out.println("--------------- LEARNING DATA " + 0 + " --------------------------");
        double[] out = learn.updateModelWithNewTimeSlice(data0);
        //System.out.println(learn.getLearntDynamicBayesianNetwork());
        output[0] = out[0];

        for (int i = 1; i < NSETS; i++) {
            System.out.println("--------------- LEARNING DATA " + i + " --------------------------");
            DataFlink<DataInstance> dataNew = DataFlinkLoader.open(env,
                    "./datasets/simulated/tmp_conceptdrift_data"+i+".arff", false);
            out = learn.updateModelWithNewTimeSlice(dataNew);
            //System.out.println(learn.getLearntDynamicBayesianNetwork());
            output[i] = out[0];

        }
        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;

        System.out.println("Running time" + seconds + " seconds");

        //System.out.println(learn.getLearntDynamicBayesianNetwork());

        for (int i = 0; i < NSETS; i++) {
            System.out.println("E(H_"+i+") =\t" + output[i]);
        }

    }

    public static void main(String[] args) throws Exception {

        int NSETS = Integer.parseInt(args[0]);

        learnIDAConceptDriftDetector(NSETS);
    }

}
