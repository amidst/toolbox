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

package eu.amidst.flinklink.core.utils;


import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.flinklink.core.data.DataFlink;
import org.apache.flink.api.common.functions.RichMapPartitionFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andresmasegosa on 15/9/15.
 */
public class ConversionToBatches {

    public static String ATTRIBUTES = "ATTRIBUTES";

    public static String BATCH_SIZE = "BATCH_SIZE";


    public static <T> DataSet<List<T>> toBatches(DataSet<T> data, int batchSize){

        Configuration config = new Configuration();
        config.setInteger(BATCH_SIZE, batchSize);

        return data.mapPartition(new BatchMAP<T>()).withParameters(config);
    }

    public static <T extends DataInstance> DataSet<DataOnMemory<T>> toBatches(DataFlink<T> data, int batchSize){

        try{
            Configuration config = new Configuration();
            config.setInteger(BATCH_SIZE, batchSize);
            config.setBytes(ATTRIBUTES, Serialization.serializeObject(data.getAttributes()));

            return data.getDataSet().mapPartition(new DataBatch()).withParameters(config);
        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }



    static class BatchMAP<T> extends RichMapPartitionFunction<T, List<T>> {


        int batchSize=1000;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            batchSize = parameters.getInteger(BATCH_SIZE, 1000);
        }

        @Override
        public void mapPartition(Iterable<T> values, Collector<List<T>> out) throws Exception {
            int count = 0;
            List<T> batch = new ArrayList<>();
            for (T value : values) {
                if (count < batchSize){
                    batch.add(value);
                }else {
                    out.collect(batch);
                    batch = new ArrayList<>();
                    batch.add(value);
                }

            }
        }
    }

    static class DataBatch<T extends DataInstance> extends RichMapPartitionFunction<T, DataOnMemory<T>> {


        int batchSize=1000;
        Attributes attributes;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            batchSize = parameters.getInteger(BATCH_SIZE, 1000);
            attributes = Serialization.deserializeObject(parameters.getBytes(ATTRIBUTES, null));
        }

        @Override
        public void mapPartition(Iterable<T> values, Collector<DataOnMemory<T>> out) throws Exception {
            int count = 0;
            DataOnMemoryListContainer<T> batch = new DataOnMemoryListContainer<T>(this.attributes);
            for (T value : values) {
                if (count < batchSize){
                    batch.add(value);
                    count++;
                 }else {
                    out.collect(batch);
                    batch = new DataOnMemoryListContainer<T>(this.attributes);
                    batch.add(value);
                    count = 1;
                }
            }

            if (batch.getNumberOfDataInstances()>0)
                out.collect(batch);
        }
    }
}