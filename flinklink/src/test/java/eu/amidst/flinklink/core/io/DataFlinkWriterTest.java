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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.flinklink.Main;
import eu.amidst.flinklink.core.data.DataFlink;
import junit.framework.TestCase;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

/**
 * Created by andresmasegosa on 23/9/15.
 */
public class DataFlinkWriterTest extends TestCase {


    public static void test1() throws Exception {
        //Set-up Flink session.
        Configuration conf = new Configuration();
        conf.setInteger("taskmanager.network.numberOfBuffers", 12000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.getConfig().disableSysoutLogging();         env.setParallelism(Main.PARALLELISM);

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env,
                "../datasets/simulated/test_not_modify/SmallDataSet.arff", false);

        DataFlinkWriter.writeDataToARFFFolder(dataFlink, "../datasets/simulated/tmp_2.arff");

    }
}