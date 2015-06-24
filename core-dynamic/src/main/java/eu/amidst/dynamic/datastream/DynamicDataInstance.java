/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.datastream;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.core.variables.Variable;

/**
 * Created by andresmasegosa on 27/01/15.
 */
public interface DynamicDataInstance extends DataInstance, DynamicAssignment{

    int getSequenceID();

    int getTimeID();

    double getValue(Attribute att, boolean present);

    void setValue(Attribute att, double val, boolean present);

    @Override
    default double getValue(Attribute att){
        return this.getValue(att,true);
    }

    @Override
    default void setValue(Attribute att, double val){
        this.setValue(att,val,true);
    }

    @Override
    default double getValue(Variable var) {
        if (var.isInterfaceVariable()) {
            return this.getValue(var.getAttribute(),false);
        } else {
            return this.getValue(var.getAttribute(),true);
        }
    }

    @Override
    default void setValue(Variable var, double val) {
        if (var.isInterfaceVariable()) {
            this.setValue(var.getAttribute(), val, false);
        } else {
            this.setValue(var.getAttribute(), val, true);
        }
    }
}
