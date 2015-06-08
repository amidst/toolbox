/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DynamicDataInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by ana@cs.aau.dk on 12/11/14.
 */
public class DynamicDataOnMemoryFromFile implements DataOnMemory<DynamicDataInstance> {

    private DataFileReader reader;
    private Iterator<DataRow> dataRowIterator;
    private Attribute attSequenceID;
    private Attribute attTimeID;
    private NextDynamicDataInstance nextDynamicDataInstance;
    private DynamicDataInstance[] dataInstances;
    private int pointer = 0;


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

        try {
            attSequenceID = this.reader.getAttributes().getAttributeByName(Attributes.SEQUENCE_ID_ATT_NAME);
            sequenceID = (int)present.getValue(attSequenceID);
        }catch (UnsupportedOperationException e){
            attSequenceID = null;
        }
        try {
            attTimeID = this.reader.getAttributes().getAttributeByName(Attributes.TIME_ID_ATT_NAME);
            timeID = (int)present.getValue(attSequenceID);
        }catch (UnsupportedOperationException e){
            attTimeID = null;
        }

        nextDynamicDataInstance = new NextDynamicDataInstance(past, present, sequenceID, timeID);

        while (dataRowIterator.hasNext()) {

            /* 0 = false, false, i.e., Not sequenceID nor TimeID are provided */
            /* 1 = true,  false, i.e., TimeID is provided */
            /* 2 = false, true,  i.e., SequenceID is provided */
            /* 3 = true,  true,  i.e., SequenceID is provided*/
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

    @Override
    public int getNumberOfDataInstances() {
        return dataInstances.length;
    }

    @Override
    public DynamicDataInstance getDataInstance(int i) {
        return dataInstances[i];
    }

    @Override
    public List<DynamicDataInstance> getList() {
        return Arrays.asList(this.dataInstances);
    }

    @Override
    public Attributes getAttributes() {
        return reader.getAttributes();
    }

    @Override
    public Stream<DynamicDataInstance> stream() {
        return Arrays.stream(this.dataInstances);
    }

    @Override
    public void close() {
        this.reader.close();
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {
    }
}
