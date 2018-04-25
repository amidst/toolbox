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

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.filereaders.DataFileReader;
import eu.amidst.core.datastream.filereaders.DataRow;
import eu.amidst.core.datastream.filereaders.DataRowMissing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The DynamicDataStreamFromFile class implements the {@link DataOnMemory} interface and loads a dynamic data on memory from a given file.
 */
public class DynamicDataOnMemoryFromFile implements DataOnMemory<DynamicDataInstance> {

    /** Represents a {@link DataFileReader} object. */
    private DataFileReader reader;

    /** Represents a {@link Iterator} over {@link DataRow}. */
    private Iterator<DataRow> dataRowIterator;

    /** Represents the {@link Attribute} object defining the sequence ID. */
    private Attribute attSequenceID;

    /** Represents the {@link Attribute} object defining the time ID. */
    private Attribute attTimeID;

    /** Represents a {@link NextDynamicDataInstance} object. */
    private NextDynamicDataInstance nextDynamicDataInstance;

    /** Represents an array of {@link DynamicDataInstance} objects. */
    private DynamicDataInstance[] dataInstances;

    /** Represents a pointer initialized to 0. */
    private int pointer = 0;

    /**
     * Creates a new DynamicDataOnMemoryFromFile.
     * @param reader1 a valid {@link DataFileReader} object.
     */
    public DynamicDataOnMemoryFromFile(DataFileReader reader1) {
        this.reader = reader1;
        dataRowIterator = this.reader.iterator();

        List<DynamicDataInstance> dataInstancesList = new ArrayList<>();

        DataRow present;
        DataRow past = new DataRowMissing();

        int timeID = 0;
        int sequenceID = 0;

        if (dataRowIterator.hasNext()) {
            present = this.dataRowIterator.next();
        }
        else {
            throw new UnsupportedOperationException("There are insufficient instances to learn a model.");
        }

        attSequenceID = this.reader.getAttributes().getSeq_id();
        if (attSequenceID!=null)
            sequenceID = (int)present.getValue(attSequenceID);

        attTimeID = this.reader.getAttributes().getTime_id();
        if (attTimeID!=null)
            timeID = (int)present.getValue(attSequenceID);

        nextDynamicDataInstance = new NextDynamicDataInstance(past, present, sequenceID, timeID);

        while (dataRowIterator.hasNext()) {

            /* 0 = false, false, i.e., No sequenceID or TimeID are provided. */
            /* 1 = true,  false, i.e., TimeID is provided */
            /* 2 = false, true,  i.e., SequenceID is provided */
            /* 3 = true,  true,  i.e., Both SequenceID and TimeID are provided. */
            int option = (attTimeID == null) ? 0 : 1 + 2 * ((attSequenceID == null) ? 0 : 1);

            switch (option) {

            /* Not sequenceID nor TimeID are provided*/
                case 0:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance_NoTimeID_NoSeq(dataRowIterator));
                    break;
             /* Only TimeID is provided*/
                case 1:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance_NoSeq(dataRowIterator, attTimeID));
                    break;

             /* Only SequenceID is provided*/
                case 2:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance_NoTimeID(dataRowIterator, attSequenceID));
                    break;

             /* SequenceID and TimeID are provided*/
                case 3:
                    dataInstancesList.add(nextDynamicDataInstance.nextDataInstance(dataRowIterator, attSequenceID, attTimeID));
                    break;

                default:
                    throw new IllegalArgumentException();
            }
        }
        reader.restart();
        this.dataRowIterator=reader.iterator();

        dataInstances = new DynamicDataInstanceImpl[dataInstancesList.size()];
        int counter = 0;
        for (DynamicDataInstance inst : dataInstancesList) {
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
    public DynamicDataInstance getDataInstance(int i) {
        return dataInstances[i];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DynamicDataInstance> getList() {
        return Arrays.asList(this.dataInstances);
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
    public Stream<DynamicDataInstance> stream() {
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