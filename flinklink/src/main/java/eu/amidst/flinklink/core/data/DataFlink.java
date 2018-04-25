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
package eu.amidst.flinklink.core.data;


import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.flinklink.core.utils.ConversionToBatches;
import eu.amidst.flinklink.core.utils.Function2;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.utils.DataSetUtils;

import java.util.List;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 8/9/15.
 */
public interface DataFlink<T extends DataInstance> {

    String getName();

    Attributes getAttributes();

    DataSet<T> getDataSet();

    default DataSet<DataOnMemory<T>> getBatchedDataSet(int batchSize){
        return ConversionToBatches.toBatches(this,batchSize);
    }

    default DataSet<DataOnMemory<T>> getBatchedDataSet(int batchSize, Function2<DataFlink<T>,Integer,DataSet<DataOnMemory<T>>> batchFunction){
        return batchFunction.apply(this,batchSize);
    }

    default DataFlink<T> filter(FilterFunction<T> filter){
        return new DataFlink<T>() {
            @Override
            public String getName() {
                return DataFlink.this.getName();
            }

            @Override
            public Attributes getAttributes() {
                return DataFlink.this.getAttributes();
            }

            @Override
            public DataSet<T> getDataSet() {
                return DataFlink.this.getDataSet().<T>filter(filter);
            }
        };
    }

    default <R extends DataInstance> DataFlink<R> map (MapFunction<T,R> mapper){
        return new DataFlink<R>() {
            @Override
            public String getName() {
                return DataFlink.this.getName();
            }

            @Override
            public Attributes getAttributes() {
                return DataFlink.this.getAttributes();
            }

            @Override
            public DataSet<R> getDataSet() {
                return DataFlink.this.getDataSet().map(mapper);
            }
        };
    }


    default DataOnMemory<T> subsample(long seed, int samples) {

        try {
            List<T> subsample = DataSetUtils.sampleWithSize(this.getDataSet(), true, samples, seed).collect();
            Attributes atts = this.getAttributes();
            return new DataOnMemory<T>() {
                @Override
                public int getNumberOfDataInstances() {
                    return subsample.size();
                }

                @Override
                public T getDataInstance(int i) {
                    return subsample.get(i);
                }

                @Override
                public List<T> getList() {
                    return subsample;
                }

                @Override
                public Attributes getAttributes() {
                    return atts;
                }

                @Override
                public void close() {

                }

                @Override
                public boolean isRestartable() {
                    return true;
                }

                @Override
                public void restart() {

                }

                @Override
                public Stream<T> stream() {
                    return subsample.stream();
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    default DataOnMemory<T> subsample(long seed, int samples, Function2<DataFlink<T>,Integer,DataSet<DataOnMemory<T>>> batchFunction) {

        try {
            List<DataOnMemory<T>> subsample = DataSetUtils.sampleWithSize(this.getBatchedDataSet(1,batchFunction), true, samples, seed).collect();

            DataOnMemoryListContainer<T> dataInstances = new DataOnMemoryListContainer(this.getAttributes());

            for (DataOnMemory<T> ts : subsample) {
                for (T t : ts) {
                    dataInstances.add(t);
                }
            }
            return dataInstances;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
