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

package eu.amidst.core.variables.stateSpaceTypes;

import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StateSpaceTypeEnum;

/**
 * This class extends the abstract class {@link StateSpaceType}.
 * It defines and handles the state space for continuous variables, i.e., the real state space.
 */
public class RealStateSpace extends StateSpaceType {

    /** Represents the minimum value of the interval. */
    private double minInterval;

    /** Represents the maximum value of the interval. */
    private double maxInterval;

    /**
     * Creates a new RealStateSpace.
     */
    public RealStateSpace() {
        super(StateSpaceTypeEnum.REAL);
        minInterval = Double.NEGATIVE_INFINITY;
        maxInterval = Double.POSITIVE_INFINITY;
    }

    /**
     * Creates a new RealStateSpace given minimum and maximum values of the interval.
     * @param minInterval1 the minimum value of the interval.
     * @param maxInterval1 the maximum value of the interval.
     */
    public RealStateSpace(double minInterval1, double maxInterval1) {
        super(StateSpaceTypeEnum.REAL);
        this.maxInterval=maxInterval1;
        this.minInterval=minInterval1;
    }

    /**
     * Returns the minimum value of the interval.
     * @return the minimum value of the interval.
     */
    public double getMinInterval() {
        return minInterval;
    }

    /**
     * Sets the maximum value of the interval.
     * @param value the minimum value of the interval.
     */
    public void setMaxInterval(double value) {
         maxInterval = value;
    }

    /**
     * Sets the minimum value of the interval.
     * @param value the maximum value of the interval.
     */
    public void setMinInterval(double value) {
        minInterval = value;
    }

    /**
     * Returns the maximum value of the interval.
     * @return the maximum value of the interval.
     */
    public double getMaxInterval() {
        return maxInterval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String stringValue(double value) {
        return value+"";
    }
}
