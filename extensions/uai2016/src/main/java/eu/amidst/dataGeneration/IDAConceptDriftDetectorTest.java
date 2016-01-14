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

package eu.amidst.dataGeneration;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.flinklink.core.conceptdrift.IDAConceptDriftDetector;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by andresmasegosa on 9/12/15.
 */
public class IDAConceptDriftDetectorTest{

    public static int NMONTHS = 84;

    public static void generateIDAData() throws Exception{
        GenerateCajaMarData generateData = new GenerateCajaMarData();
        generateData.setSeed(0);
        generateData.setIncludeSocioEconomicVars(false);
        generateData.setBatchSize(1000);
        generateData.setRscriptsPath("./extensions/uai2016/doc-experiments/dataGenerationForFlink");
        generateData.setNumFiles(3);
        generateData.setNumSamplesPerFile(1000);
        generateData.setOutputFullPath("~/core/datasets/IDAlikeDataCD/" +
                "withIndex");
        generateData.setPrintINDEX(true);
        generateData.setAddConceptDrift(true);//At points 35 and 60
        generateData.generateData();
    }

    public static void testUpdateN_IDAData(String dataPath) throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        //generateIDAData();

        DataFlink<DataInstance> data0 = DataFlinkLoader.loadDataFromFolder(env,
                dataPath+"/MONTH1.arff", true);

        IDAConceptDriftDetector learn = new IDAConceptDriftDetector();
        learn.setBatchSize(100);
        learn.setClassIndex(data0.getAttributes().getNumberOfAttributes()-1);
        learn.setAttributes(data0.getAttributes());
        learn.setNumberOfGlobalVars(1);
        learn.setTransitionVariance(0.1);
        learn.setSeed(0);

        learn.initLearning();
        double[] output = new double[NMONTHS];

        System.out.println("--------------- MONTH " + 1 + " --------------------------");
        double[] out = learn.updateModelWithNewTimeSlice(data0);
        output[0] = out[0];

        for (int i = 1; i < NMONTHS; i++) {
            System.out.println("--------------- MONTH " + (i+1) + " --------------------------");
            DataFlink<DataInstance> dataNew = DataFlinkLoader.loadDataFromFolder(env,
                    dataPath+"/MONTH" + (i+1) + ".arff", true);
            out = learn.updateModelWithNewTimeSlice(dataNew);
            output[i] = out[0];

        }

        System.out.println(learn.getLearntDynamicBayesianNetwork());

        for (int i = 0; i < NMONTHS; i++) {
            System.out.println("E(H_"+i+") =\t" + output[i]);
        }

    }

    public static void main(String[] args) throws Exception{
        //generateIDAlikeDAta();
        String dataPath = args[0];
        testUpdateN_IDAData(dataPath);
    }

}