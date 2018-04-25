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

package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * This interface defines a data file reader.
 */
public interface DataFileReader extends Iterable<DataRow> {

    /**
     * Loads this DataFileReader from a given file.
     * @param path the path of the file from which the DataFileReader will be loaded.
     */
    void loadFromFile(String path);

    /**
     * Returns the set of {@link Attributes} in this DataFileReader.
     * @return a valid {@link Attributes} object.
     */
    Attributes getAttributes();

    /**
     * Tests if this DataFileReader could read the given filename.
     * @param fileName the filename.
     * @return true if the filename could be read.
     */
    boolean doesItReadThisFile(String fileName);

    /**
     * Returns a Stream of {@link DataRow} objects to be processed sequentially.
     * @return a valid Java stream of {@link DataRow} objects.
     */
    Stream<DataRow> stream();

    /**
     * Restarts this DataFileReader.
     * This method is only needed if the iterator is not based on streams.
     */
    default void restart(){

    }

    /**
     * Closes this DataFileReader.
     * This method is only needed if the iterator is not based on streams.
     */
    default void close(){

    }

    /**
     * Returns a fixed batch Stream of {@link DataRow} objects to be processed in parallel.
     * @param batchSize the batch size.
     * @return a fixed batch stream to be processed in parallel.
     * @see eu.amidst.core.utils.FixedBatchParallelSpliteratorWrapper
     */
    default Stream<DataRow> parallelStream(int batchSize) {
        return FixedBatchParallelSpliteratorWrapper.toFixedBatchStream(this.stream(), batchSize);
    }

    /**
     * Returns a Stream of {@link DataRow} objects to be processed in parallel.
     * @return a stream to be processed in parallel.
     */
    default Stream<DataRow> parallelStream(){
        return this.stream().parallel();
    }

    /**
     * Returns an {@link Iterator} over the stream of {@link DataRow} objects.
     * @return an {@link Iterator} over the stream of {@link DataRow} objects.
     */
    default Iterator<DataRow> iterator() {
        return this.stream().iterator();
    }
}
