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

package eu.amidst.cajamareval;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;

/**
 * Created by andresmasegosa on 23/10/15.
 */
public class TestArff {
    public static void main(String[] args) throws Exception {
        String fileTrain = "datasets/datatest.arff";//args[0];

        System.out.println("Processing ARFF File: ");

        DataStream<DataInstance> train = DataStreamLoader.openFromFile(fileTrain);

        System.out.println("Number of attributes:" + train.getAttributes().getNumberOfAttributes());


        long count = train.stream().count();

        System.out.println("Number of Instances: " + count);

        System.out.println("ARFF successfully loaded");
    }
}