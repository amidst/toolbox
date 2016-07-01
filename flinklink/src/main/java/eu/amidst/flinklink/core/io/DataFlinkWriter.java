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
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;
import eu.amidst.flinklink.core.data.DataFlink;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.TextOutputFormat;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.core.fs.FileSystem;

/**
 * Created by andresmasegosa on 23/9/15.
 */
public class DataFlinkWriter {
    public static <T extends DataInstance> void writeDataToARFFFolder(DataFlink<T> data, String path) throws Exception {

        DataSet<T> dataSet = data.getDataSet();

        DataFlinkWriter.writeHeader(dataSet.getExecutionEnvironment(), data, path, false);

        dataSet.writeAsFormattedText(path + "/data/", FileSystem.WriteMode.OVERWRITE, new TextOutputFormat.TextFormatter<T>() {
                    @Override
                    public String format(T value) {
                        return ARFFDataWriter.dataInstanceToARFFString(value);
                    }
                }
        );

        dataSet.getExecutionEnvironment().execute();

    }

    public static <T extends DataInstance> void writeHeader(ExecutionEnvironment env, DataFlink<T> data, String path,
                                                             boolean includeRanges) {

        DataSource<String> name = env.fromElements("@relation " + data.getName());
        name.writeAsText(path + "/name.txt", FileSystem.WriteMode.OVERWRITE);

        DataSource<Attribute> attData = env.fromCollection(data.getAttributes().getFullListOfAttributes());

        attData.writeAsFormattedText(path + "/attributes.txt", FileSystem.WriteMode.OVERWRITE, new TextOutputFormat.TextFormatter<Attribute>() {
            @Override
            public String format(Attribute att) {
                return ARFFDataWriter.attributeToARFFStringWithIndex(att,includeRanges);
            }
        });
    }
}
