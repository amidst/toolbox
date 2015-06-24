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
package eu.amidst.core.examples.datastream;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;

/**
 *
 * In this example we show how to use the main features of a DataStream object. More precisely,  we show six different
 * ways of iterating over the data samples of a DataStream object.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class DataStreamsExample {

    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/SmallDataSet.arff");

        //Access to the attributes defining the data set
        System.out.println("Attributes defining the data set");
        for (Attribute attribute : data.getAttributes()) {
            System.out.println(attribute.getName());
        }
        Attribute attA = data.getAttributes().getAttributeByName("A");

        //1. Iterating over samples using a for loop
        System.out.println("1. Iterating over samples using a for loop");
        for (DataInstance dataInstance : data) {
            System.out.println("The value of attribute A for the current data instance is: " + dataInstance.getValue(attA));
        }


        //2. Iterating using streams. We need to restart the data again as a DataStream can only be used once.
        System.out.println("2. Iterating using streams.");
        data.restart();
        data.stream().forEach(dataInstance ->
                        System.out.println("The value of attribute A for the current data instance is: " + dataInstance.getValue(attA))
        );


        //3. Iterating using parallel streams.
        System.out.println("3. Iterating using parallel streams.");
        data.restart();
        data.parallelStream(10).forEach(dataInstance ->
                        System.out.println("The value of attribute A for the current data instance is: " + dataInstance.getValue(attA))
        );

        //4. Iterating over a stream of data batches.
        System.out.println("4. Iterating over a stream of data batches.");
        data.restart();
        data.streamOfBatches(10).forEach(batch -> {
            for (DataInstance dataInstance : batch)
                System.out.println("The value of attribute A for the current data instance is: " + dataInstance.getValue(attA));
        });

        //5. Iterating over a parallel stream of data batches.
        System.out.println("5. Iterating over a parallel stream of data batches.");
        data.restart();
        data.parallelStreamOfBatches(10).forEach(batch -> {
            for (DataInstance dataInstance : batch)
                System.out.println("The value of attribute A for the current data instance is: " + dataInstance.getValue(attA));
        });


        //6. Iterating over data batches using a for loop
        System.out.println("6. Iterating over data batches using a for loop.");
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(10)) {
            for (DataInstance dataInstance : batch)
                System.out.println("The value of attribute A for the current data instance is: " + dataInstance.getValue(attA));
        }
    }

}
