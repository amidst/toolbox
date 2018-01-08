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

package electricity;

import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by andresmasegosa on 27/1/17.
 */
public class Utils {

    /**
     * Commands for arff preprocessing
     *  find . -name '*.arff' -print0 | xargs -0 sed -i "" "s/DOWN/0.0/g"
     *  find . -name '*.arff' -print0 | xargs -0 sed -i "" "s/UP/1.0/g"
     *  find . -name '*.arff' -print0 | xargs -0 sed -i "" "s/@attribute class {1.0, 0.0}/@attribute class numeric/g"
     */

    public static void splitByMonth(String[] args) throws IOException {


        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/DriftSets/";
        String arrffName = "ElecOriginal.arff";

        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+arrffName);

        for (int year = 96; year <=98 ; year++) {
            for (int month = 1; month <=12; month++) {
                DataOnMemoryListContainer<DataInstance> dataMonth = new DataOnMemoryListContainer<DataInstance>(dataInstances.getAttributes());
                for (DataInstance dataInstance : dataInstances) {
                    String date = dataInstance.getValue(dataInstances.getAttributes().getAttributeByName("date")) + "";
                    int localMonth = Integer.parseInt(date.substring(2, 4));
                    int localYear = Integer.parseInt(date.substring(0, 2));
                    if ( localMonth== month && localYear == year) {
                        dataMonth.add(dataInstance);
                    }
                }

                if (dataMonth.getNumberOfDataInstances()>0)
                    DataStreamWriter.writeDataToFile(dataMonth, dataPath + "electricityByMonth/data_" + year + "_" + month + ".arff");
            }
        }
    }

    public static void countInstances(String[] args) {

        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/DriftSets/electricityByMonth/";

        String[] strings = new File(dataPath).list();
        Arrays.sort(strings);
        for (String string : strings) {
            if (!string.endsWith(".arff"))
                continue;

            DataOnMemory<DataInstance> batch= DataStreamLoader.loadDataOnMemoryFromFile(dataPath+string);

            System.out.println(batch.getNumberOfDataInstances());
        }


    }
    public static void inputMissingToZero(String[] args) throws IOException {

        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/DriftSets/electricityByMonth/";
        String dataPathOutput = "/Users/andresmasegosa/Dropbox/Amidst/datasets/DriftSets/electricityByMonthToZero/";

        String[] strings = new File(dataPath).list();
        Arrays.sort(strings);
        for (String string : strings) {
            if (!string.endsWith(".arff"))
                continue;

            final DataOnMemory<DataInstance> batchFinal= DataStreamLoader.loadDataOnMemoryFromFile(dataPath+string);

            DataStream<DataInstance> batch = batchFinal.map(d -> {
                for (Attribute attribute : batchFinal.getAttributes()) {
                    if (eu.amidst.core.utils.Utils.isMissingValue(d.getValue(attribute))){
                        d.setValue(attribute,0.0);
                    }
                }
                return d;
            });

            DataStreamWriter.writeDataToFile(batch,dataPathOutput+string);
        }
    }

    public static void main(String[] args) throws IOException {
        Utils.inputMissingToZero(args);
    }
}
