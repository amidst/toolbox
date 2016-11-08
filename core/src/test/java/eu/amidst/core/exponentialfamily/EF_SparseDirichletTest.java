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
import junit.framework.TestCase;
import org.apache.commons.math3.special.Gamma;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class EF_SparseDirichletTest extends TestCase {

    public static void test() {
        ParameterVariables variables = new ParameterVariables(0);

        EF_SparseDirichlet dist = new EF_SparseDirichlet(variables.newSparseDirichletParameter("A", 10));


        if (Main.VERBOSE) System.out.println(dist.getNaturalParameters().output());
        if (Main.VERBOSE) System.out.println(dist.getMomentParameters().output());


        if (Main.VERBOSE) System.out.println();

        dist.getNaturalParameters().sumConstant(1.0);
        if (Main.VERBOSE) System.out.println(dist.getNaturalParameters().output());

        if (Main.VERBOSE) System.out.println();

        dist.updateMomentFromNaturalParameters();
        if (Main.VERBOSE) System.out.println(dist.getMomentParameters().output());

        assertEquals(Gamma.digamma(2.0) - Gamma.digamma(10 + 10), dist.getMomentParameters().get(0));

    }


    public static void test3() {
        ParameterVariables variables = new ParameterVariables(0);

        EF_SparseDirichlet dist = new EF_SparseDirichlet(variables.newSparseDirichletParameter("A", 10));
        EF_Dirichlet distM = new EF_Dirichlet(variables.newDirichletParameter("AM", 10));

        dist.getNaturalParameters().sumConstant(1.0);
        distM.getNaturalParameters().sumConstant(1.0);

        dist.updateMomentFromNaturalParameters();
        distM.updateMomentFromNaturalParameters();


        assertEquals(dist.getNaturalParameters().sum(), distM.getNaturalParameters().sum(),0.00001);
        assertEquals(dist.getMomentParameters().sum(), distM.getMomentParameters().sum(),0.00001);


        assertEquals(dist.computeLogBaseMeasure(1.0), distM.computeLogBaseMeasure(1.0));

        assertEquals(dist.computeLogNormalizer(), distM.computeLogNormalizer());

        for (int i = 0; i < 10; i++) {
            assertEquals(dist.computeLogProbabilityOf(i/10 +0.05), distM.computeLogProbabilityOf(i/10 +0.05));
        }
    }
}