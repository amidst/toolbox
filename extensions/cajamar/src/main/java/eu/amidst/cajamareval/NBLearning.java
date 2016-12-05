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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.NaiveBayesClassifier;

import java.io.File;
import java.io.PrintWriter;

/**
 * Created by andresmasegosa on 14/10/15.
 */
public class NBLearning {

    public static void main(String[] args) throws Exception{

        String className = "Default";

        String fileTrain;
        String outputFolder;
        String dataSetName;

        if(args.length == 3) { // Args:  train.arff  outputFolder dataSetName
            fileTrain = args[0];
            outputFolder = args[1];
            dataSetName = args[2];
        }
        else {

            System.out.println("Incorrect number of arguments, use: \"NBLearning fileTrain outputFolder dataSetName \"");
            String folder = "/Users/dario/Desktop/CAJAMAR_Estaticos/10-11-2016_discretas/";

            fileTrain = folder + "train.arff";  //CAJAMAR_DatosNB
            outputFolder = folder;
            dataSetName = "";
        }

        String modelOutput  =   outputFolder + "NB_" + dataSetName + "_model.bn";
        String modelOutputTxt = outputFolder + "NB_" + dataSetName + "_model.txt";

        DataStream<DataInstance> train = DataStreamLoader.open(fileTrain);


        NaiveBayesClassifier naiveBayesClassifier = new NaiveBayesClassifier(train.getAttributes());

        naiveBayesClassifier.setClassName(className);
        naiveBayesClassifier.setWindowSize(5000);
        naiveBayesClassifier.updateModel(train);

        BayesianNetworkWriter.save(naiveBayesClassifier.getModel(), modelOutput);

        File modelOutputFile = new File(modelOutputTxt);
        PrintWriter modelWriter = new PrintWriter(modelOutputFile, "UTF-8");
        modelWriter.print(naiveBayesClassifier.getModel().toString());
        modelWriter.close();

    }
}
