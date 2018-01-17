/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.core.constraints;

import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;

/**
 * Created by andresmasegosa on 13/01/2018.
 */
public class Constraint implements Serializable {
    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    String paramaterName;
    Variable variable;
    double[] values;
    double value;

    public Constraint(String paramaterName, Variable variable, double[] values) {
        this.paramaterName = paramaterName;
        this.variable = variable;
        this.values = values;
    }

    public Constraint(String paramaterName, Variable variable, double value) {
        this.paramaterName = paramaterName;
        this.variable = variable;
        this.value = value;
    }

    public Variable getVariable() {
        return variable;
    }

    public String getParamaterName() {
        return paramaterName;
    }

    public double getValue() {
        return value;
    }
}
