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

import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import junit.framework.TestCase;

/**
 * Created by andresmasegosa on 19/4/16.
 */
public class EF_TruncatedNormalTest extends TestCase {

    public static void test1() {

        Variables variables = new Variables();
        Variable var = variables.newTruncatedNormal("A");

        EF_TruncatedNormal dist = var.getDistributionType().newEFUnivariateDistribution(1);

        /*
         * MEAN=0
         */
        dist.setNaturalWithMeanPrecision(0, 1);
        assertEquals(0.4598622, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.2911251, dist.getMomentParameters().get(1),0.001);


        /*
         * MEAN=10
         */
        dist.setNaturalWithMeanPrecision(10,1);
        assertEquals(0.8915437, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.806299, dist.getMomentParameters().get(1),0.001);




        /*
         * MEAN=100
         */
        dist.setNaturalWithMeanPrecision(100,1);
        assertEquals(0.989901, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.9800061, dist.getMomentParameters().get(1),0.001);

        dist.setNaturalWithMeanPrecision(100,0.01);
        assertEquals(0.5815536, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.4175454, dist.getMomentParameters().get(1),0.001);


        dist.setNaturalWithMeanPrecision(100,100);
        assertEquals(0.999899, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.9997982, dist.getMomentParameters().get(1),0.001);


        dist.setNaturalWithMeanPrecision(100,10000);
        assertEquals(0.999999, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.9999949, dist.getMomentParameters().get(1),0.001);


        /*
         * MEAN=-100
         */
        dist.setNaturalWithMeanPrecision(-100,1);
        assertEquals(0.009998001, dist.getExpectedParameters().get(0),0.000001);

        dist.setNaturalWithMeanPrecision(-100,100);
        assertEquals(0.000099995, dist.getExpectedParameters().get(0),0.000001);

        /*
         * MEAN=10000
         */
        dist.setNaturalWithMeanPrecision(10000,1);
        assertEquals(0.99988, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.799998, dist.getMomentParameters().get(1),0.001);

        dist.setNaturalWithMeanPrecision(10000,0.01);
        assertEquals(0.989999, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.982746, dist.getMomentParameters().get(1),0.001);


        /*
         * MEAN=-10000
         */
        dist.setNaturalWithMeanPrecision(-10000,1);
        assertEquals(0.0000869246, dist.getExpectedParameters().get(0),0.000001);

        dist.setNaturalWithMeanPrecision(-10000,0.01);
        assertEquals(0.0099995, dist.getExpectedParameters().get(0),0.000001);




        /*
         * OTHER TRUNCATION INTERVALS (NOT [0,1])
         */
        dist.setLowerInterval(0.2);
        dist.setUpperInterval(0.8);

        /*
         * MEAN=0
         */
        dist.setNaturalWithMeanPrecision(0, 1);
        assertEquals(0.485201, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.240985, dist.getMomentParameters().get(1),0.1);


        /*
         * MEAN=10
         */
        dist.setNaturalWithMeanPrecision(10,1);
        assertEquals(0.695606, dist.getExpectedParameters().get(0),0.000001);
        assertEquals(0.493807, dist.getMomentParameters().get(1),0.01);
    }

}