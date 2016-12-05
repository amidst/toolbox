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

package eu.amidst.dynamic.datastream;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.utils.Utils;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.core.variables.Variable;

/**
 *  The DynamicDataInstance interface represents a dynamic data sample.
 *  It extends the {@link DataInstance} and {@link DynamicAssignment} interfaces.
 *  Hence, it can be also interpreted as a dynamic assignment to a set of {@link Variable} objects.
 *
 * <p> An example of use of the DynamicDataInstance interface can be found in {@see eu.amidst.dynamic.examples.datastream} </p>
 */

public interface DynamicDataInstance extends DataInstance, DynamicAssignment{

    /**
     * Returns the sequence ID of this DynamicDataInstance.
     * @return a {@code long} that represents the sequence ID.
     */
    long getSequenceID();

    /**
     * Returns the time ID of this DynamicDataInstance.
     * @return a {@code long} that represents the time ID.
     */
    long getTimeID();

    /**
     * Returns the value of a given {@link Attribute} in this DynamicDataInstance.
     * @param att an {@link Attribute} object.
     * @param present a {@code boolean} that refers to either the present or past time.
     * @return a {@code double} that represents the value of the attribute.
     */
    double getValue(Attribute att, boolean present);

    /**
     * Sets the value of a given {@link Attribute} in this DynamicDataInstance.
     * @param att an {@link Attribute} object.
     * @param val a {@code double} that represents the value of the attribute.
     * @param present a {@code boolean} that refers to either the present or past time.
     */
    void setValue(Attribute att, double val, boolean present);

    /**
     * {@inheritDoc}
     */
    @Override
    default double getValue(Attribute att){
        return this.getValue(att,true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void setValue(Attribute att, double val){
        this.setValue(att,val,true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default double getValue(Variable var) {
        if (!var.isObservable())
            return Utils.missingValue();

        if (var.isInterfaceVariable()) {
            return this.getValue(var.getAttribute(),false);
        } else {
            return this.getValue(var.getAttribute(),true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void setValue(Variable var, double val) {
        if (!var.isObservable())
            throw new IllegalArgumentException("Changing values of a non observable variable!");

        if (var.isInterfaceVariable()) {
            this.setValue(var.getAttribute(), val, false);
        } else {
            this.setValue(var.getAttribute(), val, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default String outputString(){
        StringBuilder builder = new StringBuilder(this.getAttributes().getFullListOfAttributes().size()*2);
        builder.append("{");
        this.getAttributes().getFullListOfAttributes().stream().forEach(att -> builder.append(att.getName()+ " = "+ att.stringValue(this.getValue(att))+", "));
        builder.append("}");
        return builder.toString();
    }
}
