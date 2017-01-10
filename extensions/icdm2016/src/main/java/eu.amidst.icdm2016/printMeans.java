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

package eu.amidst.icdm2016;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;

import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 9/1/17.
 */
public class printMeans {

    //static String path = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/dataWeka";

    static String path="/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWekaResiduals/R1_";

    static int NSETS = 84;

    static double transitionVariance = 10;

//    static String[] varNames = {"VAR01","VAR02","VAR03","VAR04","VAR07","VAR08"};

    static String[] varNames = {"VAR04"};

    static int windowSize = 50000;


    public static void main(String[] args) {

        DataStream<DataInstance> dataMonth0 = DataStreamLoader.openFromFile(path+"00.arff");

        int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};

        for (int i = 0; i < NSETS; i++) {

            int currentMonth = i;

            if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            DataStream<DataInstance> dataMonthi;

            if (currentMonth < 10)
                dataMonthi = DataStreamLoader.openFromFile(path + "0" + currentMonth + ".arff");
            else
                dataMonthi = DataStreamLoader.openFromFile(path + currentMonth + ".arff");

            double[] means = new double[varNames.length];

            int count = 0;
            for (DataInstance dataInstance : dataMonthi) {
                for (int j = 0; j < varNames.length; j++) {
                    means[j]+=dataInstance.getValue(dataInstance.getAttributes().getAttributeByName(varNames[j]));
                }
                count++;
            }
            for (int j = 0; j < varNames.length; j++) {
                System.out.print(means[j]/count+"\t");
            }

            System.out.println();
        }
    }
}
