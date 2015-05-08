/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public interface DataFileReader extends Iterable<DataRow> {

    void loadFromFile(String path);

    Attributes getAttributes();

    boolean doesItReadThisFileExtension(String fileExtension);

    Stream<DataRow> stream();

    default void restart(){
        //Only needed if iterator is not based on streams.
    }

    default void close(){
        //Only needed if iterator is not based on streams.
    }

    default Stream<DataRow> parallelStream(int batchSize) {
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    default Stream<DataRow> parallelStream(){
        return this.stream().parallel();
    }

    default Iterator<DataRow> iterator() {
        return this.stream().iterator();
    }
}
