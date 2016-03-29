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

package eu.amidst.dynamic.datastream.filereaders;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.filereaders.DataFileReader;

import java.util.stream.Stream;

/**
 * The DynamicDataStreamFromFile class implements the {@link DataStream} interface and loads a dynamic data stream from a given file.
 */
public class DynamicDataStreamFromFile implements DataStream<DynamicDataInstance> {

    /** Represents a {@link DataFileReader} object. */
    private DataFileReader reader;

    /**
     * Creates a new DynamicDataStreamFromFile object.
     * @param reader1 a valid {@link DataFileReader} object.
     */
    public DynamicDataStreamFromFile(DataFileReader reader1){
        this.reader=reader1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        this.reader.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRestartable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart() {
        this.reader.restart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<DynamicDataInstance> stream() {
        return DynamicDataInstanceSpliterator.toDynamicDataInstanceStream(reader);
    }
}
