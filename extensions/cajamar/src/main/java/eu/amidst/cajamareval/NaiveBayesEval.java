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
import eu.amidst.core.utils.Utils;
import eu.amidst.latentvariablemodels.staticmodels.classifiers.NaiveBayesClassifier;

import java.io.FileWriter;

/**
 * Created by andresmasegosa on 14/10/15.
 */
public class NaiveBayesEval {

    public static void main(String[] args) throws Exception{

        String fileTrain;
        String fileTest;
        String fileOutput;
        String className;

        if(args.length == 4) {
            fileTrain = args[0];
            fileTest = args[1];
            fileOutput = args[2];
            className = args[3];
        }
        else {

            String folder = "/Users/dario/Desktop/CAJAMAR_Estaticos/10-11-2016_reales/";

            fileTrain = folder + "train.arff";  //CAJAMAR_DatosNB
            fileTest = folder + "test.arff";
            fileOutput = folder + "output_NB.txt";
            className = "Default";
        }


        DataStream<DataInstance> train = DataStreamLoader.open(fileTrain);
        DataStream<DataInstance> test = DataStreamLoader.open(fileTest);
        FileWriter fw = new FileWriter(fileOutput);


        NaiveBayesClassifier naiveBayesClassifier = new NaiveBayesClassifier(train.getAttributes());

        naiveBayesClassifier.setClassName(className);
        naiveBayesClassifier.setWindowSize(10000);
        naiveBayesClassifier.updateModel(train);

        BayesianNetworkWriter.save(naiveBayesClassifier.getModel(), fileOutput + "_NB_model.bn");


//        Domain huginNetwork = BNConverterToHugin.convertToHugin(naiveBayesClassifier.getModel());
//        huginNetwork.saveAsNet(fileOutput + "_NB_model.net");

        System.out.println(naiveBayesClassifier.getModel());

        Attribute seq_id = train.getAttributes().getSeq_id();
        Attribute classAtt  = train.getAttributes().getAttributeByName(className);
        for (DataInstance dataInstance : test) {
            double actualClass = dataInstance.getValue(classAtt);
            dataInstance.setValue(classAtt, Utils.missingValue());
            fw.write((long)dataInstance.getValue(seq_id) + "\t" + naiveBayesClassifier.predict(dataInstance).getParameters()[1] + "\t" + (long)actualClass + "\n");
        }

        fw.close();

    }
}
