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
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.utils.ReservoirSampling;

import java.io.IOException;

/**
 * Created by andresmasegosa on 27/5/16.
 */
public class Subsample {
    static String path="/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/dataWeka";
    static String outputPath="/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWekaSubsampled/dataWeka";

    public static void main(String[] args) throws IOException {
        int NSETS = 84;
        int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};
        int windowSize = 100;
        double[] meanHiddenVars;


        for (int i = 0; i < NSETS; i++) {

            System.out.println();
            System.out.println();

            int currentMonth = i;

            //if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
            //    continue;

            DataStream<DataInstance> dataMonth = DataStreamLoader.openFromFile(path + currentMonth + ".arff");

            System.out.print(i+"\t"+dataMonth.stream().count());

            DataOnMemory<DataInstance> newData =ReservoirSampling.samplingNumberOfSamples(5000,dataMonth);

            DataStreamWriter.writeDataToFile(newData, outputPath + currentMonth + ".arff");

        }

    }
}
