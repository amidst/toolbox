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
 * This interface represents a row of the data matrix containing the data. It can also
 * be seen as a specific assignment to the attributes of a data set.
 *
 */
public interface DataRow {

    /**
     * This method return the value assigned to a given Attribute
     * @param att, the Attribute object we want to query
     * @return The assigned value to the given Attribute. Returns a Double.NaN if
     * the attribute is not observed in this assignment.
     */
    double getValue(Attribute att);

    /**
     * This method set the value assigned to an Attribute. If the value is already include,
     * then the value is updated accordingly.
     *
     * @param att, the Attribute object we want to assign
     * @param value, the assigned value
     */
    void setValue(Attribute att, double value);

    /**
     * This method return the set of attributes which have an assigned value
     * stored in this object.
     * @return A valid Attributes object
     */
    Attributes getAttributes();

}


