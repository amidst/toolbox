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
import eu.amidst.huginlink.learning.ParallelTAN;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Created by andresmasegosa on 14/10/15.
 */
public class ParallelTANEval {
    public static void main(String[] args) throws Exception {

        String fileTrain;
        String fileTest;
        String outputFolder;
        String dataSetName;

//        if(args.length == 4) { // Args:  train.arff test.arff outputFolder

            fileTrain = args[0];
            fileTest = args[1];
            outputFolder = args[2];
            dataSetName = args[3];
//        }
//        else {
//            System.out.println("Incorrect number of arguments, use: \"ParallelTANEval fileTrain fileTest outputFolder dataSetName \"");
//
//            String folder = "/Users/dario/Desktop/CAJAMAR_ultimos/20131231/";
//
//            fileTrain = folder + "train.arff";  //CAJAMAR_DatosNB
//            fileTest = folder + "test.arff";
//            outputFolder = folder;
//            dataSetName = "";
//        }

        DataStream<DataInstance> train = DataStreamLoader.open(fileTrain);
        DataStream<DataInstance> test = DataStreamLoader.open(fileTest);

        String fileOutput   =   outputFolder + "TAN_" + dataSetName + "_predictions.csv";
        String modelOutput  =   outputFolder + "TAN_" + dataSetName + "_model.bn";
        String modelOutputTxt = outputFolder + "TAN_" + dataSetName + "_model.txt";


        ParallelTAN tan = new ParallelTAN();
        tan.setOptions(args);
        //tan.setNameTarget("Default");
        tan.setParallelMode(true);
        BayesianNetwork bn = tan.learn(train,10000);

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
        Attribute classAtt  = train.getAttributes().getAttributeByName(tan.getNameTarget());
        double nNontest = 0;
        for (DataInstance dataInstance : test) {
            dataInstance.setValue(classAtt, Utils.missingValue());
            try {
                double pred = tan.predict(dataInstance)[1];
                fw.write(dataInstance.getValue(seq_id) +"\t" + pred+"\n");
            } catch (Exception ex) {
                ex.printStackTrace();
                nNontest++;
            }
        }

        System.out.println("Non tested clients: "+nNontest);

        fw.close();
    }
}

