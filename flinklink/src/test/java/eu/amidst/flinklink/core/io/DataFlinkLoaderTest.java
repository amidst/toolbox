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

package eu.amidst.flinklink.core.io;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.flinklink.Main;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.utils.ConversionToBatches;
import junit.framework.TestCase;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 2/9/15.
 */
public class DataFlinkLoaderTest extends TestCase {

    public static void test1() throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env,
                "../datasets/simulated/test_not_modify/SmallDataSet.arff", false);
        DataSet<DataInstance> data = dataFlink.getDataSet();

        data.print();

        List<DataInstance> instanceList = data.collect();

        assertEquals(16, instanceList.size());
        List<String> names = Arrays.asList("A", "B", "C", "D", "E", "G");
        List<Integer> states = Arrays.asList(2, 3, 2, 2, 2, -1);

        List<Attribute> atts = dataFlink.getAttributes().getListOfNonSpecialAttributes();
        for (int i = 0; i < names.size(); i++) {
            if (Main.VERBOSE) System.out.println(names.get(i));
            assertEquals(atts.get(i).getName(), names.get(i));
            assertEquals(atts.get(i).getNumberOfStates(), states.get(i).intValue());
        }
    }


    public static void test2() throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env,
                "../datasets/simulated/test_not_modify/SmallDataSet.arff", false);
        DataSet<DataOnMemory<DataInstance>> data = dataFlink.getBatchedDataSet(3);

        data.print();

        List<DataOnMemory<DataInstance>> batchList = data.collect();

        int size = 0;
        for (DataOnMemory<DataInstance> dataInstanceDataBatch : batchList) {
            if (Main.VERBOSE) System.out.println("Batch :" + dataInstanceDataBatch.getList().size());
            size += dataInstanceDataBatch.getList().size();
        }
        assertEquals(16, size);

        List<DataInstance> instanceList = batchList.stream().flatMap(batch -> batch.getList().stream()).collect(Collectors.toList());

        assertEquals(16, instanceList.size());
        List<String> names = Arrays.asList("A", "B", "C", "D", "E", "G");
        List<Integer> states = Arrays.asList(2, 3, 2, 2, 2, -1);

        List<Attribute> atts = dataFlink.getAttributes().getListOfNonSpecialAttributes();
        for (int i = 0; i < names.size(); i++) {
            if (Main.VERBOSE) System.out.println(names.get(i));
            assertEquals(atts.get(i).getName(), names.get(i));
            assertEquals(atts.get(i).getNumberOfStates(), states.get(i).intValue());
        }
    }

    public static void test3() throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlinkWriterTest.test1();
        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFolder(env, "../datasets/simulated/tmp_2.arff", false);
        DataSet<DataInstance> data = dataFlink.getDataSet();

        data.print();

        List<DataInstance> instanceList = data.collect();

        assertEquals(16, instanceList.size());
        List<String> names = Arrays.asList("A", "B", "C", "D", "E", "G");
        List<Integer> states = Arrays.asList(2, 3, 2, 2, 2, -1);

        List<Attribute> atts = dataFlink.getAttributes().getListOfNonSpecialAttributes();
        for (int i = 0; i < names.size(); i++) {
            if (Main.VERBOSE) System.out.println(names.get(i));
            assertEquals(atts.get(i).getName(), names.get(i));
            assertEquals(atts.get(i).getNumberOfStates(), states.get(i).intValue());
        }

    }

    public static void test4() throws Exception {

        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlink<DataInstance> dataInstances = DataFlinkLoader.loadDataFromFile(env, "../datasets/text/docword.simulated.arff", false);


        for (int i = 0; i < 3; i++) {
            DataOnMemory<DataInstance> batch = dataInstances.subsample(0, 1, ConversionToBatches::toBatchesBySeqID);

            int docId = -1;
            for (DataInstance dataInstance : batch) {
                if (docId == -1) {
                    docId = (int) dataInstance.getValue(dataInstance.getAttributes().getSeq_id());
                    if (Main.VERBOSE) System.out.println("DOC ID: " + docId);
                }

                assertEquals(docId, (int) dataInstance.getValue(dataInstance.getAttributes().getSeq_id()));
            }
        }

    }
}