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
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.huginlink.converters.BNConverterToHugin;
import eu.amidst.huginlink.learning.ParallelTAN;

import java.io.FileWriter;

/**
 * Created by andresmasegosa on 14/10/15.
 */
public class ParallelTANEval {
    public static void main(String[] args) throws Exception {
        String fileTrain = args[0];
        String fileTest = args[1];
        String fileOutput = args[2];


        DataStream<DataInstance> train = DataStreamLoader.openFromFile(fileTrain);
        DataStream<DataInstance> test = DataStreamLoader.openFromFile(fileTest);
        FileWriter fw = new FileWriter(fileOutput);

        ParallelTAN tan = new ParallelTAN();
        tan.setOptions(args);
        tan.setParallelMode(true);
        BayesianNetwork bn = tan.learn(train,10000);

        BayesianNetworkWriter.saveToFile(bn, fileOutput + "_TAN_model.bn");

        Domain huginNetwork = BNConverterToHugin.convertToHugin(bn);
        huginNetwork.saveAsNet(fileOutput + "_TAN_model.net");

        System.out.println(bn);

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

