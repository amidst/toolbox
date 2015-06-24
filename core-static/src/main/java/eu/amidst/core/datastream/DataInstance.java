/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream;

import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.Set;

/**
 *  Considering that a {@link DataStream} is composed  by a collection of {@link DataInstance} objects.
 *  Then, the {@link DataInstance} interface represents a data sample. It should basically be seen
 *  as a specific assignment to the {@link Attribute} objects defining the DataStream object.<p>
 *
 *  To simplify the use of this class across the toolbox, it also inherits from
 *  {@link Assignment} and can also be interpreted as an assignment to some {@link Variable} objects. This
 *  variable objects have associated an Attribute object.<p>
 *
 *  So a DataInstance object can be queried either using an{@link Attribute} object or a {@link Variable} object.
 *
 */
public interface DataInstance extends Assignment {

    /**
     * {@inheritDoc}
     */
    @Override
    default double getValue(Variable var) {
        if (var.getAttribute()==null)
            return Utils.missingValue();
        else
            return this.getValue(var.getAttribute());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void setValue(Variable var, double value) {
        if (var.getAttribute()!=null)
            this.setValue(var.getAttribute(), value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Set<Variable> getVariables(){
        return null;
    }


    /**
     * This method return the set of attributes which have an assigned value
     * stored in this object.
     * @return A valid Attributes object
     */
    Attributes getAttributes();

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
     * @param val, the assigned value
     */
    void setValue(Attribute att, double val);
}