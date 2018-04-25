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

/**
 * This interface defines a row of the data matrix.
 * It represents a specific assignment to all the attributes in the data.
 */
public interface DataRow {

    /**
     * Returns the value assigned to a given {@link Attribute} in this DataRow.
     * @param att an {@link Attribute} object.
     * @return the assigned value to the given {@link Attribute}.
     * If the attribute is not observed, then returns a Double.NaN value.
     */
    double getValue(Attribute att);

    /**
     * Sets the value of an {@link Attribute} in this DataRow.
     * If the value is already included in the data, then the value will be updated accordingly.
     * @param att an {@link Attribute} object.
     * @param value a double value to be assigned to the given {@link Attribute}.
     */
    void setValue(Attribute att, double value);

    /**
     * Returns the set of {@link Attributes} that have observed values in this DataRow.
     * @return a valid {@link Attributes} object.
     */
    Attributes getAttributes();

    /**
     * Returns all the values of this DataRow as an array of doubles.
     * @return an array of doubles.
     */
    double[] toArray();

}


