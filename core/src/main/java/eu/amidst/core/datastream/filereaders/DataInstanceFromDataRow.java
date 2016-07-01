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

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;

import java.io.Serializable;

/**
 * This class implements the {@link DataInstance} interface and handles the operations related to the data instances.
 */
public class DataInstanceFromDataRow implements DataInstance, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    /** Represents a {@link DataRow} object. */
    private DataRow dataRow;

    /**
     * Creates a new DataInstanceImpl from a given {@link DataRow} object.
     * @param dataRow1 a {@link DataRow} object.
     */
    public DataInstanceFromDataRow(DataRow dataRow1){
        dataRow=dataRow1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attributes getAttributes() {
        return dataRow.getAttributes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getValue(Attribute att) {
        return dataRow.getValue(att);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Attribute att, double value) {
        this.dataRow.setValue(att, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] toArray() {
        return this.dataRow.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        return this.outputString();
    }
}
