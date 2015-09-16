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
package eu.amidst.flinklink.core.converter;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 2/9/15.
 */
public class DataStreamToDataSet {

    public static DataSet<DataInstance> toDataSet(DataStream<DataInstance> data){
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        List<DataInstance> dataCollection = data.stream().collect(Collectors.toList());
        return env.fromCollection(dataCollection);
    }

    static class CollectionTemp implements Collection<DataInstance>, Serializable {

        private static final long serialVersionUID = -3436599636425587512L;

        transient DataStream<DataInstance> data;

        CollectionTemp(DataStream<DataInstance> data){
            this.data=data;
        }
        @Override
        public int size() {
            return 10;
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public Iterator<DataInstance> iterator() {
            return this.data.iterator();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public boolean add(DataInstance dataInstance) {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public boolean addAll(Collection<? extends DataInstance> c) {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Not defined operation.");
        }

        @Override
        public Spliterator<DataInstance> spliterator() {
            return data.spliterator();
        }

        @Override
        public Stream<DataInstance> stream() {
            return data.stream();
        }

        @Override
        public Stream<DataInstance> parallelStream() {
            return data.parallelStream(1000);
        }
    }
}
