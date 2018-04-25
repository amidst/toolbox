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

package eu.amidst.winter.Session2;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.utils.DataSetGenerator;
import eu.amidst.dynamic.datastream.DynamicDataInstance;

import java.io.IOException;

/**
 * Created by andresmasegosa on 16/01/2018.
 */
public class Utils {
    public static void main(String[] args) throws IOException {
        DataStream<DataInstance> data =  eu.amidst.core.utils.DataSetGenerator.generate(0,1000,0,10);
        DataStreamWriter.writeDataToFile(data, "./datasets/artificialDataset.arff");

//        DataStream<DynamicDataInstance> dataDynamic =  eu.amidst.dynamic.utils.DataSetGenerator.generate(0,100, 100,0,10);
//        DataStreamWriter.writeDataToFile(dataDynamic, "./datasets/artificialDatasetDynamic.arff");
    }
}
