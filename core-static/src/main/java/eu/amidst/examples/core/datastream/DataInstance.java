/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.examples.core.datastream;

import eu.amidst.examples.core.variables.Assignment;
import eu.amidst.examples.core.variables.Variable;

/**
 * Created by ana@cs.aau.dk on 10/11/14.
 */
public interface DataInstance extends Assignment {

    @Override
    default double getValue(Variable var) {
        return this.getValue(var.getAttribute());
    }

    @Override
    default void setValue(Variable var, double value) {
        this.setValue(var.getAttribute(), value);
    }

    double getValue(Attribute att);

    void setValue(Attribute att, double val);

    default String toString(Attributes atts) {
        StringBuilder builder = new StringBuilder(atts.getList().size()*2);
        builder.append("{");
        atts.getList().stream().limit(atts.getList().size()-1).forEach(att -> builder.append(att.getName()+ " = "+this.getValue(att)+", "));
        builder.append(atts.getList().get(atts.getList().size()-1).getName()+ " = "+ this.getValue(atts.getList().get(atts.getList().size()-1)));
        builder.append("}");
        return builder.toString();
    }

}