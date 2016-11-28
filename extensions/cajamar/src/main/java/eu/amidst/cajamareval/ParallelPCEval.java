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

package eu.amidst.cajamareval;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Created by andresmasegosa on 14/10/15.
 */
public class ParallelPCEval {
    public static void main(String[] args) throws Exception {

        String fileTrain;
        String fileTest;
        String outputFolder;
        String dataSetName;

        if(args.length >0) { // Args:  train.arff test.arff outputFolder

            fileTrain = args[0];
            fileTest = args[1];
            outputFolder = args[2];
            dataSetName = args[3];
        }
        else {
            System.out.println("Incorrect number of arguments, use: \"ParallelPCEval fileTrain fileTest outputFolder dataSetName \"");

            String folder = "/Users/dario/Desktop/";

            fileTrain = folder + "datosPrueba.arff";  //CAJAMAR_DatosNB
            fileTest = folder + "datosPrueba.arff";
            outputFolder = folder;
            dataSetName = "";

            String[] args2 = new String[]{fileTrain, fileTest, outputFolder, dataSetName, "-numSamplesOnMemory", "10000", "-numCores", "4", "-nameRoot", "VAR7", "-nameTarget", "Default", "-parallelMode"};
            args = args2;
        }

        DataStream<DataInstance> train = DataStreamLoader.open(fileTrain);
        DataStream<DataInstance> test = DataStreamLoader.open(fileTest);

        String fileOutput   =   outputFolder + "PC_" + dataSetName + "_predictions.csv";
        String modelOutput  =   outputFolder + "PC_" + dataSetName + "_model.bn";
        String modelOutputTxt = outputFolder + "PC_" + dataSetName + "_model.txt";


        ParallelPCCajamar modelPC = new ParallelPCCajamar();
        modelPC.setOptions(args);
        //modelPC.setNameTarget("Default");
        modelPC.setParallelMode(true);
        modelPC.setNumCores(Runtime.getRuntime().availableProcessors());
        modelPC.setNumSamplesOnMemory(1000);
        //modelPC.setBatchSize(5000);

        BayesianNetwork bn = modelPC.learn(train,40000);

        System.out.println(bn.toString());
        BayesianNetworkWriter.save(bn, modelOutput);

        File modelOutputFile = new File(modelOutputTxt);
        PrintWriter modelWriter = new PrintWriter(modelOutputFile, "UTF-8");
        modelWriter.print(bn.toString());
        modelWriter.close();

//        Domain huginNetwork = BNConverterToHugin.convertToHugin(bn);
//        huginNetwork.saveAsNet(fileOutput + "_TAN_model.net");

//        System.out.println(bn);


        FileWriter fw = new FileWriter(fileOutput);


        Attribute seq_id = train.getAttributes().getSeq_id();
        Attribute classAtt  = train.getAttributes().getAttributeByName("Default");
        double nNontest = 0;
        for (DataInstance dataInstance : test) {
            double actualClassValue = dataInstance.getValue(classAtt);

            dataInstance.setValue(classAtt, Utils.missingValue());
            double pred = Double.NaN;
            try {
                pred = modelPC.predict(dataInstance)[1];
                //fw.write(dataInstance.getValue(seq_id) +"\t" + pred+"\n");

            } catch (Exception ex) {
                ex.printStackTrace();
                nNontest++;
            }
            finally {
                fw.write((long)dataInstance.getValue(seq_id) + "," + pred + "," + (long)actualClassValue + "\n");
            }
        }

        System.out.println("Non tested clients: " + (int) nNontest);

        fw.close();
    }
}

