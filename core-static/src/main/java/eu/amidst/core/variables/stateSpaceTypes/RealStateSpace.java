/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.variables.stateSpaceTypes;

import eu.amidst.core.variables.StateSpaceType;
import eu.amidst.core.variables.StateSpaceTypeEnum;

/**
 * Created by andresmasegosa on 25/11/14.
 */
public class RealStateSpace extends StateSpaceType {

    private double minInterval;
    private double maxInterval;


    public RealStateSpace() {
        super(StateSpaceTypeEnum.REAL);
        minInterval = Double.NEGATIVE_INFINITY;
        maxInterval = Double.POSITIVE_INFINITY;
    }

    public RealStateSpace(double minInterval1, double maxInterval1) {
        super(StateSpaceTypeEnum.REAL);
        this.maxInterval=maxInterval1;
        this.minInterval=minInterval1;
    }

    public double getMinInterval() {
        return minInterval;
    }

    public double getMaxInterval() {
        return maxInterval;
    }

}
