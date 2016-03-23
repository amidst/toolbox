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


import eu.amidst.core.utils.Serialization;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.io.SerializedInputFormat;
import org.apache.flink.api.common.io.SerializedOutputFormat;
import org.apache.flink.api.common.typeinfo.BasicArrayTypeInfo;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

/**
 * Created by andresmasegosa on 16/9/15.
 */
public class DataSetSerializer {

    public static <T> void serializeDataSet(DataSet<T> dataSet, String pathFile){
        DataSet<byte[]> tmp = dataSet.map(new RichMapFunction<T, byte[]>() {
            @Override
            public byte[] map(T value) throws Exception {
                return Serialization.serializeObject(value);
            }
        });
        tmp.write(new SerializedOutputFormat(), pathFile, FileSystem.WriteMode.OVERWRITE);
    }

    public static <T> DataSet<T> deserializeDataSet(String pathFile){
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        SerializedInputFormat inputFormat = new SerializedInputFormat();
        inputFormat.setFilePath(new Path(pathFile));
        DataSet<byte[]> tmp = env.createInput(inputFormat, BasicArrayTypeInfo.BYTE_ARRAY_TYPE_INFO);

        return tmp.map(new RichMapFunction<byte[], T>() {
            @Override
            public T map(byte[] value) throws Exception {
                return Serialization.deserializeObject(value);
            }
        });
    }

}
