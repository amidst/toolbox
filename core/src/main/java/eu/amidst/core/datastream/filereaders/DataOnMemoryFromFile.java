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
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class implements the interface {@link DataOnMemory} and produces {@link DataOnMemory} objects from a given file.
 */
public class DataOnMemoryFromFile implements DataOnMemory<DataInstance> {

    /** Represents the {@link DataFileReader} object providing access to the data stored in a file. */
    private DataFileReader reader;

    /**
     * Represents an Array of data instances.
     * <p>The data is assumed here to be relatively small, and thus it would be possible to be read multiple times.
     * In this case, it is possible to store all the data instances in an Array. Otherwise, it might be better to store all
     * the data instances in an ArrayList and avoid therefore the extra pass in the constructor.</p>
     */
    private DataInstanceFromDataRow[] dataInstances;

    /**
     * Creates a new DataOnMemoryFromFile from a given {@link DataFileReader} object.
     * @param reader a {@link DataFileReader} object.
     */
    public DataOnMemoryFromFile(DataFileReader reader) {
        this.reader = reader;

        List<DataInstanceFromDataRow> dataInstancesList = new ArrayList<>();

        for (DataRow row: reader){
            dataInstancesList.add(new DataInstanceFromDataRow(row));
        }
        reader.restart();

        dataInstances = new DataInstanceFromDataRow[dataInstancesList.size()];
        int counter = 0;
        for (DataInstanceFromDataRow inst : dataInstancesList) {
            dataInstances[counter] = inst;
            counter++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfDataInstances() {
        return dataInstances.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataInstance getDataInstance(int i) {
        return dataInstances[i];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataInstance> getList() {
        return Arrays.asList(this.dataInstances);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attributes getAttributes() {
        return this.reader.getAttributes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<DataInstance> stream() {
        return Arrays.stream(this.dataInstances);
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

    }
}