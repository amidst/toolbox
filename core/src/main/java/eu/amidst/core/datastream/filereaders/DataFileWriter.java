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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;

import java.io.IOException;

/**
 * This interface defines a data file writer.
 */
public interface DataFileWriter {

    /**
     * Returns the filename extension.
     * @return the filename extension.
     */
    String getFileExtension();

    /**
     * Saves a {@link DataStream} in a given file.
     * @param dataStream the {@link DataStream} to be written in the file.
     * @param file a String that represents the name of the file where the data will be written.
     * @throws IOException in case of an error when writing to file
     */
    void writeToFile(DataStream<? extends DataInstance> dataStream, String file) throws IOException;

}
