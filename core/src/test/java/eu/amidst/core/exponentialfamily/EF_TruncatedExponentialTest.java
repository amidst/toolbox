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

import eu.amidst.core.Main;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import junit.framework.TestCase;

/**
 * Created by andresmasegosa on 19/4/16.
 */
public class EF_TruncatedExponentialTest extends TestCase {

    public static void test1() {

        Variables variables = new Variables();
        Variable var = variables.newTruncatedExponential("A");
        EF_TruncatedExponential dist = var.getDistributionType().newEFUnivariateDistribution(-1);

        assertEquals(dist.computeLogProbabilityOf(1),-0.5413249,0.001);
        assertEquals(dist.getNaturalParameters().get(0),-1.0);
        assertEquals(dist.getMomentParameters().get(0),0.4166668,0.01);
        assertEquals(dist.getMomentParameters().get(0),dist.getExpectedParameters().get(0),0.0);


        dist.getNaturalParameters().set(0,100);
        dist.updateMomentFromNaturalParameters();
        if (Main.VERBOSE) System.out.println(dist.getExpectedParameters().get(0));

    }

}