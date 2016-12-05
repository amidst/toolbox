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
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.NaiveBayesClassifier;

import java.io.FileWriter;

/**
 * Created by andresmasegosa on 14/10/15.
 */
public class NBEvaluation {

    public static void main(String[] args) throws Exception{

        String className = "Default";

        String fileModel;
        String fileTest;
        String outputFolder;
        String dataSetName;

        if(args.length == 4) { // Args:  train.arff test.arff outputFolder
            fileModel = args[0];
            fileTest = args[1];
            outputFolder = args[2];
            dataSetName = args[3];
        }
        else {

            System.out.println("Incorrect number of arguments, use: \"NBEvaluation fileModel fileTest outputFolder dataSetName \"");

            String folder = "/Users/dario/Desktop/CAJAMAR_Estaticos/10-11-2016_discretas/";

            fileModel = folder + "NB_model.bn";  //CAJAMAR_DatosNB
            fileTest = folder + "test.arff";
            outputFolder = folder;
            dataSetName = "";
        }

        String fileOutput   =   outputFolder + "NB_" + dataSetName + "_predictions.csv";

        DataStream<DataInstance> test = DataStreamLoader.open(fileTest);
        FileWriter fw = new FileWriter(fileOutput);

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile(fileModel);
        NaiveBayesClassifier naiveBayesClassifier = new NaiveBayesClassifier(test.getAttributes(),bn);

        naiveBayesClassifier.setClassName(className);
        naiveBayesClassifier.setWindowSize(5000);



        System.out.println(naiveBayesClassifier.getModel());

        Attribute seq_id = test.getAttributes().getSeq_id();
        Attribute classAtt  = test.getAttributes().getAttributeByName(className);
        for (DataInstance dataInstance : test) {
            double actualClass = dataInstance.getValue(classAtt);
            dataInstance.setValue(classAtt, Utils.missingValue());
            fw.write((long)dataInstance.getValue(seq_id) + "," + naiveBayesClassifier.predict(dataInstance).getParameters()[1] + "," + (long)actualClass + "\n");
        }

        fw.close();

    }
}
