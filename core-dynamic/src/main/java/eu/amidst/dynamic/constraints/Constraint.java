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

package eu.amidst.dynamic.constraints;

import eu.amidst.core.variables.Variable;

import java.io.Serializable;

/**
 * Created by andresmasegosa on 13/01/2018.
 */
public class Constraint extends eu.amidst.core.constraints.Constraint implements Serializable {
    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    boolean time0 = false;

    public Constraint(String paramaterName, Variable variable, double[] values) {
        super(paramaterName, variable, values);
    }

    public Constraint(String paramaterName, Variable variable, double value) {
        super(paramaterName, variable, value);
    }

    public Constraint(String paramaterName, Variable variable, double[] values, boolean time0) {
        super(paramaterName, variable, values);
        this.time0 = time0;
    }

    public Constraint(String paramaterName, Variable variable, double value, boolean time0) {
        super(paramaterName, variable, value);
        this.time0 = time0;
    }

    public boolean isTime0() {
        return time0;
    }
}
