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

package eu.amidst.core.io;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.DataFileWriter;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataWriter;

import java.io.IOException;

/**
 * This class allows to save a {@link DataStream} in a file.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#iodatastreamsexample"> http://amidst.github.io/toolbox/CodeExamples.html#iodatastreamsexample </a>  </p>
 *
 */
public final class DataStreamWriter {

    /** Represents the data file writer. */
    private static DataFileWriter dataFileWriter = new ARFFDataWriter();

    /**
     * Sets the data file writer.
     * @param dataFileWriter_ an {@link DataFileWriter} object.
     */
    public static void setDataFileWriter(DataFileWriter dataFileWriter_) {
        dataFileWriter = dataFileWriter_;
    }

    /**
     * Saves a {@link DataStream} in a file.
     * @param data the {@link DataStream} to ba saved.
     * @param path the path of the file where the data stream will be saved.
     * @throws IOException in case of an error while writing to file.
     */
    public static void writeDataToFile(DataStream<? extends DataInstance> data, String path) throws IOException {
        dataFileWriter.writeToFile(data, path);
    }

}
