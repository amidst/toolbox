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

package eu.amidst.lda.core;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 29/4/16.
 */
public class BatchSpliteratorByIDTest extends TestCase {

    public static void test1() {

        DataStream<DataInstance> dataInstances = DataStreamLoader.open("../../datasets/simulated/simulatedText.arff");

        List<DataOnMemory<DataInstance>> listA =
                BatchSpliteratorByID.streamOverDocuments(dataInstances,2).collect(Collectors.toList());

        assertEquals(2,listA.size());
        assertEquals(6,listA.get(0).getNumberOfDataInstances());
        assertEquals(3,listA.get(1).getNumberOfDataInstances());


        List<DataOnMemory<DataInstance>> list = BatchSpliteratorByID.streamOverDocuments(dataInstances,1).collect(Collectors.toList());

        assertEquals(4,list.size());


        assertEquals(3,list.get(0).getNumberOfDataInstances());
        assertEquals(3,list.get(1).getNumberOfDataInstances());
        assertEquals(2,list.get(2).getNumberOfDataInstances());
        assertEquals(1,list.get(3).getNumberOfDataInstances());


        list = BatchSpliteratorByID.streamOverDocuments(dataInstances,3).collect(Collectors.toList());

        assertEquals(2,list.size());


        assertEquals(8,list.get(0).getNumberOfDataInstances());
        assertEquals(1,list.get(1).getNumberOfDataInstances());

    }

}