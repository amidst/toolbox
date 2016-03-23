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

import COM.hugin.HAPI.Domain;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.utils.Utils;
import eu.amidst.huginlink.converters.BNConverterToHugin;

import java.io.FileWriter;

/**
 * Created by andresmasegosa on 14/10/15.
 */
public class NaiveBayesEval {

    public static void main(String[] args) throws Exception{

        /*

        String fileTrain = args[0];
        String fileTest = args[1];
        String fileOutput = args[2];
        String className = args[3];


        DataStream<DataInstance> train = DataStreamLoader.openFromFile(fileTrain);
        DataStream<DataInstance> test = DataStreamLoader.openFromFile(fileTest);
        FileWriter fw = new FileWriter(fileOutput);


        NaiveBayesClassifier naiveBayesClassifier = new NaiveBayesClassifier();

        naiveBayesClassifier.setParallelMode(true);

        naiveBayesClassifier.setClassName(className);

        naiveBayesClassifier.learn(train, 10000);

        BayesianNetworkWriter.saveToFile(naiveBayesClassifier.getBNModel(), fileOutput + "_NB_model.bn");

        Domain huginNetwork = BNConverterToHugin.convertToHugin(naiveBayesClassifier.getBNModel());
        huginNetwork.saveAsNet(fileOutput + "_NB_model.net");

        System.out.println(naiveBayesClassifier.getBNModel());

        Attribute seq_id = train.getAttributes().getSeq_id();
        Attribute classAtt  = train.getAttributes().getAttributeByName(className);
        for (DataInstance dataInstance : test) {
            dataInstance.setValue(classAtt, Utils.missingValue());
            fw.write(dataInstance.getValue(seq_id) + "\t" + naiveBayesClassifier.predict(dataInstance)[1] + "\n");
        }

        fw.close();



    */
    }
}
