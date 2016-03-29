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

package eu.amidst.moalink.converterFromMoaToAmidst;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.filereaders.DataRow;
import weka.core.Instance;

/**
 * This class implements the interface {@link DataRow} and defines a row of the data in WEKA format.
 * <p> For more information about WEKA Attribute-Relation File Format (ARFF),
 * take a look at http://www.cs.waikato.ac.nz/ml/weka/arff.html </p>
 */
public class DataRowWeka implements DataRow {

    /** Represents an {@link weka.core.Instance} object. */
    private Instance dataRow;

    /** Represents a set of {@link Attributes}. */
    private Attributes attributes;

    /**
     * Creates a new DataRowWeka from a given instance and a set of attributes.
     * @param dataRow an {@link weka.core.Instance} object.
     * @param attributes_ a set of {@link Attributes}.
     */
    public DataRowWeka(Instance dataRow, Attributes attributes_){
            this.dataRow = dataRow;
            this.attributes = attributes_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getValue(Attribute att) {
        return dataRow.value(att.getIndex());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Attribute att, double value) {
        dataRow.setValue(att.getIndex(), value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] toArray() {
        return dataRow.toDoubleArray();
    }
}
