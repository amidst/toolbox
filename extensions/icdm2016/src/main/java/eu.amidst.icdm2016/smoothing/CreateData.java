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

package eu.amidst.icdm2016.smoothing;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 19/12/16.
 */
public class CreateData {
    static String path = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/dataWeka";
    static String outputFile = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/joinMonthsMinor.arff";
    static int[] peakMonths = {2, 8, 14, 20, 26, 32, 38, 44, 47, 50, 53, 56, 59, 62, 65, 68, 71, 74, 77, 80, 83};
    static         int NSETS = 84;
    static int[] nvars = {2,3,4,5,6,7,13};
    public static void printDataSetSizes(String[] args) {

        for (int i = 0; i < NSETS; i++) {

            int currentMonth = i;

            if (IntStream.of(CreateData.peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            DataStream<DataInstance> dataMonthi;
            if (currentMonth < 10)
                dataMonthi = DataStreamLoader.openFromFile(path + "0" + currentMonth + ".arff");
            else
                dataMonthi = DataStreamLoader.openFromFile(path + currentMonth + ".arff");


            System.out.println(dataMonthi.stream().count());
        }
    }


    public static void main(String[] args) throws IOException {

        Iterator<DataInstance>[] dataMonthi = new Iterator[NSETS];

        for (int i = 0; i < NSETS; i++) {

            int currentMonth = i;

            if (IntStream.of(CreateData.peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            if (currentMonth < 10)
                dataMonthi[i] = DataStreamLoader.openFromFile(path + "0" + currentMonth + ".arff").iterator();
            else
                dataMonthi[i] = DataStreamLoader.openFromFile(path + currentMonth + ".arff").iterator();
        }


        FileWriter fileWriter = new FileWriter(outputFile);

        fileWriter.write("@relation joinData\n");

        for (int i = 0; i < NSETS; i++) {
            int currentMonth = i;

            if (IntStream.of(CreateData.peakMonths).anyMatch(x -> x == currentMonth))
                continue;

            fileWriter.write("@attribute VAR01__"+currentMonth+" real\n" +
                    "@attribute VAR02__"+currentMonth+" real\n" +
                    "@attribute VAR03__"+currentMonth+" real\n" +
                    "@attribute VAR04__"+currentMonth+" real\n" +
                    "@attribute VAR07__"+currentMonth+" real\n" +
                    "@attribute VAR08__"+currentMonth+" real\n" +
                    "@attribute DEFAULTING__"+currentMonth+" {0, 1}\n"
            );

        }

        fileWriter.write("@data\n");

        boolean end = false;

        while(!end) {

            for (int i = 0; i < NSETS; i++) {
                int currentMonth = i;

                if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                    continue;

                if (dataMonthi[i].hasNext()) {
                    double[] array = dataMonthi[i].next().toArray();
                    for (int j = 0; j < nvars.length; j++) {
                        if (i == (NSETS - 2) && j== (nvars.length-1))
                            fileWriter.write((int)array[nvars[j]]+"\n");
                        else if (j== (nvars.length-1))
                            fileWriter.write((int)array[nvars[j]]+",");
                        else
                            fileWriter.write(array[nvars[j]]+",");
                    }
                }
            }


            for (int i = 0; i < NSETS; i++) {
                int currentMonth = i;

                if (IntStream.of(peakMonths).anyMatch(x -> x == currentMonth))
                    continue;

                if (!dataMonthi[i].hasNext()) {
                    end=true;
                }
            }

            fileWriter.flush();

        }

        fileWriter.close();

        System.out.println(DataStreamLoader.openFromFile(outputFile).stream().count());
    }


}
