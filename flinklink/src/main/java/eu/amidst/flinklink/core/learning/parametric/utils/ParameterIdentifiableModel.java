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

package eu.amidst.flinklink.core.learning.parametric.utils;

import eu.amidst.core.variables.Variable;

import java.io.Serializable;

/**
 * Created by andresmasegosa on 18/1/16.
 */
public class ParameterIdentifiableModel implements IdenitifableModelling, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    @Override
    public int getNumberOfEpochs() {
        return 1;
    }

    @Override
    public boolean isActiveAtEpoch(Variable variable, int epoch) {
        return true;
    }
}
