/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;

import java.util.stream.Stream;

/**
 * This class produces {@link DataStream} objects which consumes data which is placed on a file.
 */
public class DataStreamFromFile implements DataStream<DataInstance> {

    /** The DataFileReader object providing access the the data on the text file */
    DataFileReader reader;

    /**
     * A constructor initialized with a DataFileReader object.
     * @param reader1
     */
    public DataStreamFromFile(DataFileReader reader1) {
        this.reader = reader1;
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
    public Stream<DataInstance> stream() {
        return this.reader.stream().map( dataRow -> new DataInstanceImpl(dataRow));
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

}
