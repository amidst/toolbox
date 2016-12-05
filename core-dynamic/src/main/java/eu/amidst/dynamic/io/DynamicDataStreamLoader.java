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

package eu.amidst.dynamic.io;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.filereaders.DataFileReader;
import eu.amidst.dynamic.datastream.filereaders.DynamicDataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;

/**
 * This class allows to load a Dynamic Data Stream from disk.
 */
public class DynamicDataStreamLoader {

    /** Represents the data file reader. */
    private static DataFileReader dataFileReader = new ARFFDataReader();

    /**
     * Loads a {@link DataStream} of {@link DynamicDataInstance} from a given file.
     * @param path the path of the file from which the dynamic data stream will be loaded.
     * @return a {@link DataStream} of {@link DynamicDataInstance}.
     *
     */
    @Deprecated
    public static DataStream<DynamicDataInstance> loadFromFile(String path){
        return open(path);
    }

    public static DataStream<DynamicDataInstance> open(String path){
        dataFileReader.loadFromFile(path);
        return new DynamicDataStreamFromFile(dataFileReader);
    }


}
