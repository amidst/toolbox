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

package eu.amidst.core.io;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import junit.framework.TestCase;

/**
 * Created by andresmasegosa on 14/10/15.
 */
public class DataStreamLoaderTest extends TestCase {

    public static void test1(){

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("./datasets/datatest.arff");
        for (Attribute attribute : data.getAttributes().getFullListOfAttributes()) {
            System.out.println(attribute.getName() +", "+attribute.getIndex());
        }
        assertEquals(data.getAttributes().getNumberOfAttributes(),7);
        assertEquals(data.stream().count(), 16);
    }
}