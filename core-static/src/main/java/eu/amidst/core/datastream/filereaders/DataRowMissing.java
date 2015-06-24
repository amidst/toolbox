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

/**
 *
 * This class represents a {@link DataRow} object where all the attributes
 * have assigned a missing value.
 *
 */
public class DataRowMissing implements DataRow{

    /**
     * Return a Double.NaN value representing a missing observation
     * @param att, the Attribute object we want to query
     * @return a Double.NaN value
     */
    @Override
    public double getValue(Attribute att) {
        return Double.NaN;
    }


    /**
     * This method makes no sense in this implementation of a DataRow
     * @param att, the Attribute object we want to assign
     * @param value, the assigned value
     */
    @Override
    public void setValue(Attribute att, double value) {

    }


    /**
     * This method makes no sense in this implementation of a DataRow.
     * @return null
     */
    @Override
    public Attributes getAttributes() {
        return null;
    }
}
