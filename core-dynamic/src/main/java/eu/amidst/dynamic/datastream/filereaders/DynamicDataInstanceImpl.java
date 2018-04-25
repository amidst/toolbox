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
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.datastream.filereaders.DataRow;

import java.io.Serializable;

/**
 * The DynamicDataInstanceImpl class implements the {@link DynamicDataInstance} interface.
 */
class DynamicDataInstanceImpl implements DynamicDataInstance, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    /** Represents a {@link DataRow} object of the present time. */
    private DataRow dataRowPresent;

    /** Represents a {@link DataRow} object of the past time. */
    private DataRow dataRowPast;

    /** Represents the sequence ID. */
    private int sequenceID;

    /** Represents the time ID. */
    private int timeID;

    /**
     * Creates a new DynamicDataInstance object.
     * @param dataRowPast1 a {@link DataRow} object of the past time.
     * @param dataRowPresent1 a {@link DataRow} object of the present time.
     * @param sequenceID1 the sequence ID
     * @param timeID1 the time ID.
     */
     public DynamicDataInstanceImpl(DataRow dataRowPast1, DataRow dataRowPresent1, int sequenceID1, int timeID1){
        dataRowPresent = dataRowPresent1;
        dataRowPast =  dataRowPast1;
        this.sequenceID = sequenceID1;
        this.timeID = timeID1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getValue(Attribute att, boolean present) {
        if (present){
            return dataRowPresent.getValue(att);
        }else {
            return dataRowPast.getValue(att);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Attribute att, double value, boolean present) {
        if (present){
            dataRowPresent.setValue(att, value);
        }else {
            dataRowPast.setValue(att, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attributes getAttributes() {
        return dataRowPresent.getAttributes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getValue(Attribute att) {
        return this.dataRowPresent.getValue(att);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Attribute att, double val) {
        this.dataRowPresent.setValue(att,val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] toArray() {
        return this.dataRowPresent.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSequenceID() {
        return sequenceID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeID() {
        return timeID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        return this.outputString();
    }

}
