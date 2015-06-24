/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.distribution;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.Random;

/**
 * Created by afa on 12/11/14.
 */
public abstract class Distribution implements Serializable {

    private static final long serialVersionUID = -3436599636425587512L;
    /**
     * The variable of the distribution
     */
    protected Variable var;


    public abstract double[] getParameters();

    public abstract int getNumberOfParameters();

    /**
     * Gets the variable of the distribution
     *
     * @return A <code>Variable</code> object.
     */
    public Variable getVariable() {
        return this.var;
    }

    public abstract String label();

    public abstract void randomInitialization(Random random);

    public abstract boolean equalDist(Distribution dist, double threshold);

    public abstract String toString();

    public abstract double getLogProbability(Assignment assignment);
}
