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

package eu.amidst.dynamic.examples.datastream;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.utils.DataSetGenerator;

/**
 * An example showing how to load an use a DataStream object. For more options refer to class
 * eu.amidst.core.examples.datastream and simply change DataInstance by DynamicDataInstance
 *
 * Created by ana@cs.aau.dk on 02/12/15.
 */
public class DataStreamsExample {
    public static void main(String[] args) throws Exception {

        //Open the data stream using the class DynamicDataStreamLoader
        //DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("datasetsTests/data.arff");

        //Generate the data stream using the class DataSetGenerator
        DataStream<DynamicDataInstance> data = DataSetGenerator.generate(1,10,5,5);

        //Access the attributes defining the data stream
        System.out.println("Attributes defining the data set");
        for (Attribute attribute : data.getAttributes()) {
            System.out.println(attribute.getName());
        }
        Attribute discreteVar0 = data.getAttributes().getAttributeByName("DiscreteVar0");

        //Iterate over dynamic data instances
        System.out.println("1. Iterating over samples using a for loop");
        for (DynamicDataInstance dataInstance : data) {
            System.out.println("SequenceID = "+dataInstance.getSequenceID()+", TimeID = "+dataInstance.getTimeID());
            System.out.println("The value of attribute discreteVar0 for the current data instance is: " +
                    dataInstance.getValue(discreteVar0));
        }

    }
}
