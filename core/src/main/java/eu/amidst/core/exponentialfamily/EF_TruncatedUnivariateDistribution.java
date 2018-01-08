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

package eu.amidst.core.exponentialfamily;

/**
 * Created by andresmasegosa on 14/12/2017.
 */
public abstract class EF_TruncatedUnivariateDistribution extends EF_UnivariateDistribution{
    //It defines the upper interval of the distribution
    double upperInterval = 1;

    //It defines the lower interval of the distribution
    double lowerInterval = 0;

    /**
     * Gets the upper interval.
     * @return
     */
    public double getUpperInterval() {
        return upperInterval;
    }

    /**
     * Gets the lower interval.
     * @return
     */
    public double getLowerInterval() {
        return lowerInterval;
    }

    /**
     * Set the upper interval of the truncated.
     * @param upperInterval
     */
    public void setUpperInterval(double upperInterval) {
        this.upperInterval = upperInterval;
    }

    /**
     * Set the lower interval interval of the truncated.
     * @param lowerInterval
     */
    public void setLowerInterval(double lowerInterval) {
        this.lowerInterval = lowerInterval;
    }

}
