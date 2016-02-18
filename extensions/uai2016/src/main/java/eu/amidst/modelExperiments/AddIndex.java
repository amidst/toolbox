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

package eu.amidst.modelExperiments;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by andresmasegosa on 18/2/16.
 */
public class AddIndex {


    public static void main(String[] args) throws Exception {
/*        String pathString="/Users/andresmasegosa/Desktop/cajamardata/ALL-AGGREGATED/train.arff/attributes.txt";
        Path pathFile = Paths.get(pathString);

        FileWriter fw = new FileWriter("/Users/andresmasegosa/Desktop/cajamardata/ALL-AGGREGATED/train.arff/attributes2.txt");
        int[] count=new int[1];
        count[0]=0;
        Files.lines(pathFile).forEach(line ->{
            String[] parts = line.split(" ");
            try {
                fw.write(parts[0]+" "+parts[1]+" "+count[0]+" "+parts[2]+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            count[0]++;
        });

        fw.close();
*/

        //DataStream<DataInstance> data = DataStreamLoader.openFromFile("/Users/andresmasegosa/Desktop/cajamardata/ALL-AGGREGATED/totalWeka-ContinuousReduced.arff");

        //DataStreamWriter.writeDataToFile(data,"/Users/andresmasegosa/Desktop/cajamardata/ALL-AGGREGATED/testWeka.arff");

        DataFlink<DataInstance> data =DataFlinkLoader.loadDataFromFile(ExecutionEnvironment.getExecutionEnvironment(),"/Users/andresmasegosa/Desktop/cajamardata/ALL-AGGREGATED/totalWeka-ContinuousReduced.arff", false);

        DataFlinkWriter.writeDataToARFFFolder(data,"/Users/andresmasegosa/Desktop/cajamardata/ALL-AGGREGATED/totalWeka-ContinuousReducedFolder.arff");
    }

}