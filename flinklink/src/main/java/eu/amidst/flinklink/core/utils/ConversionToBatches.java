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

package eu.amidst.flinklink.core.utils;


import eu.amidst.core.datastream.*;
import eu.amidst.core.utils.Serialization;
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


    public static <T> DataSet<Batch<T>> toBatches(DataSet<T> data, int batchSize){

        Configuration config = new Configuration();
        config.setInteger(BATCH_SIZE, batchSize);

        return data.mapPartition(new BatchMAP<T>()).withParameters(config);
    }

    public static <T extends DataInstance> DataSet<DataOnMemory<T>> toBatches(DataFlink<T> data, int batchSize){

        try{
            Configuration config = new Configuration();
            config.setInteger(BATCH_SIZE, batchSize);
            config.setBytes(ATTRIBUTES, Serialization.serializeObject(data.getAttributes()));

            return data.getDataSet().mapPartition(new DataBatch<T>()).withParameters(config);
        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }

    public static <T extends DataInstance> DataSet<DataOnMemory<T>> toBatchesBySeqID(DataFlink<T> data, int batchSize){

        try{
            Configuration config = new Configuration();
            config.setInteger(BATCH_SIZE, batchSize);
            config.setBytes(ATTRIBUTES, Serialization.serializeObject(data.getAttributes()));

            return data.getDataSet().mapPartition(new DataBatchBySeqID<T>()).withParameters(config);
        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }


    static class BatchMAP<T> extends RichMapPartitionFunction<T, Batch<T>> {


        int batchSize=1000;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            batchSize = parameters.getInteger(BATCH_SIZE, 1000);
        }

        @Override
        public void mapPartition(Iterable<T> values, Collector<Batch<T>> out) throws Exception {

            int index = this.getRuntimeContext().getIndexOfThisSubtask()*100000;

            int batchCount = 0;
            int count = 0;
            List<T> batch = new ArrayList<>();
            for (T value : values) {
                if (count < batchSize){
                    batch.add(value);
                    count++;
                }else {
                    out.collect(new Batch<T>(batchCount+index,batch));
                    batch = new ArrayList<>();
                    batch.add(value);
                    count = 1;
                    batchCount++;
                }
            }
            if (batch.size()>0)
                out.collect(new Batch<T>(batchCount+index,batch));
        }
    }

    static class DataBatch<T extends DataInstance> extends RichMapPartitionFunction<T, DataOnMemory<T>> {


        int batchSize;
        Attributes attributes;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            batchSize = parameters.getInteger(BATCH_SIZE, 1000);
            attributes = Serialization.deserializeObject(parameters.getBytes(ATTRIBUTES, null));
        }

        @Override
        public void mapPartition(Iterable<T> values, Collector<DataOnMemory<T>> out) throws Exception {

            int index = this.getRuntimeContext().getIndexOfThisSubtask()*100000;

            int batchCount = 0;
            int count = 0;
            DataOnMemoryListContainer<T> batch = new DataOnMemoryListContainer<T>(this.attributes);
            for (T value : values) {
                if (count < batchSize){
                    batch.add(value);
                    count++;
                 }else {
                    batch.setId(batchCount+index);
                    out.collect(batch);
                    batch = new DataOnMemoryListContainer<T>(this.attributes);
                    batch.add(value);
                    count = 1;
                    batchCount++;
                }
            }

            if (batch.getNumberOfDataInstances()>0) {
                batch.setId(batchCount+index);
                out.collect(batch);
            }
        }
    }


    static class DataBatchBySeqID<T extends DataInstance> extends RichMapPartitionFunction<T, DataOnMemory<T>> {


        int nDocsPerBatch;
        Attributes attributes;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            nDocsPerBatch = parameters.getInteger(BATCH_SIZE, 100);
            attributes = Serialization.deserializeObject(parameters.getBytes(ATTRIBUTES, null));
        }

        @Override
        public void mapPartition(Iterable<T> values, Collector<DataOnMemory<T>> out) throws Exception {

            int index = this.getRuntimeContext().getIndexOfThisSubtask()*100000;

            int batchCount = 0;
            DataOnMemoryListContainer<T> batch = new DataOnMemoryListContainer<T>(this.attributes);
            Attribute seqID = this.attributes.getSeq_id();
            int currentDocID = -1;
            int nDocs = 0;

            for (T value : values) {

                if (currentDocID==-1)
                    currentDocID= (int) value.getValue(seqID);

                if (currentDocID!=(int) value.getValue(seqID)){
                    nDocs++;
                    currentDocID= (int) value.getValue(seqID);
                }

                if (nDocs<nDocsPerBatch){
                    batch.add(value);
                }else {
                    batch.setId(batchCount+index);
                    out.collect(batch);
                    batch = new DataOnMemoryListContainer<T>(this.attributes);
                    batch.add(value);
                    batchCount++;
                    nDocs=0;
                }
            }

            if (batch.getNumberOfDataInstances()>0) {
                batch.setId(batchCount+index);
                out.collect(batch);
            }
        }
    }

}
