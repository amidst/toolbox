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
import eu.amidst.core.variables.Variables;
import junit.framework.TestCase;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class EF_SparseMultinomialTest extends TestCase {

    public static void test1() {
        Variables variables = new Variables();

        EF_SparseMultinomial dist = new EF_SparseMultinomial(variables.newSparseMultionomialVariable("A", 10));


        if (Main.VERBOSE) System.out.println(dist.getSufficientStatistics(1).output());
        if (Main.VERBOSE) System.out.println(dist.getNaturalParameters().output());
        if (Main.VERBOSE) System.out.println(dist.getMomentParameters().output());


        if (Main.VERBOSE) System.out.println();

        dist.setMomentParameters(dist.getSufficientStatistics(1));
        if (Main.VERBOSE) System.out.println(dist.getMomentParameters().output());

        if (Main.VERBOSE) System.out.println();


        dist.updateNaturalFromMomentParameters();
        if (Main.VERBOSE) System.out.println(dist.getNaturalParameters().output());

        if (Main.VERBOSE) System.out.println();

        dist.updateMomentFromNaturalParameters();
        if (Main.VERBOSE) System.out.println(dist.getMomentParameters().output());
    }

    public static void test2() {
        Variables variables = new Variables();

        EF_SparseMultinomial dist = new EF_SparseMultinomial(variables.newSparseMultionomialVariable("A", 10));

        int size = 10;
        SufficientStatistics sufficientStatistics = dist.getSufficientStatistics(1);
        sufficientStatistics.multiplyBy(size);

        sufficientStatistics.sum(dist.createInitSufficientStatistics());
        if (Main.VERBOSE) System.out.println(sufficientStatistics.output());

        if (Main.VERBOSE) System.out.println();


        dist.setMomentParameters(sufficientStatistics);
        if (Main.VERBOSE) System.out.println(dist.getMomentParameters().output());

        if (Main.VERBOSE) System.out.println();


        dist.updateNaturalFromMomentParameters();
        if (Main.VERBOSE) System.out.println(dist.getNaturalParameters().output());

        if (Main.VERBOSE) System.out.println();

        dist.fixNumericalInstability();
        if (Main.VERBOSE) System.out.println(dist.getNaturalParameters().output());

        if (Main.VERBOSE) System.out.println();

        dist.updateMomentFromNaturalParameters();
        if (Main.VERBOSE) System.out.println(dist.getMomentParameters().output());
        if (Main.VERBOSE) System.out.println((size+0.1)/(size+1));

        assertEquals((size+0.1)/(size+1),dist.getMomentParameters().get(1),0.0000001);


    }


    public static void test3() {
        Variables variables = new Variables();

        EF_SparseMultinomial dist = new EF_SparseMultinomial(variables.newSparseMultionomialVariable("A", 10));
        EF_Multinomial distM = new EF_Multinomial(variables.newMultinomialVariable("AM", 10));

        int size = 10;
        SufficientStatistics sufficientStatistics = dist.getSufficientStatistics(1);
        sufficientStatistics.multiplyBy(size);
        sufficientStatistics.sum(dist.createInitSufficientStatistics());



        dist.setMomentParameters(sufficientStatistics);
        distM.setMomentParameters(sufficientStatistics);


        dist.updateNaturalFromMomentParameters();
        distM.updateNaturalFromMomentParameters();

        dist.fixNumericalInstability();
        distM.fixNumericalInstability();

        dist.updateMomentFromNaturalParameters();
        distM.updateMomentFromNaturalParameters();

        assertEquals(dist.getNaturalParameters().sum(), distM.getNaturalParameters().sum());
        assertEquals(dist.getMomentParameters().sum(), distM.getMomentParameters().sum());

        assertEquals(dist.computeLogBaseMeasure(1.0), distM.computeLogBaseMeasure(1.0));

        assertEquals(dist.computeLogNormalizer(), distM.computeLogNormalizer());

        for (int i = 0; i < 10; i++) {
            assertEquals(dist.computeLogProbabilityOf(i), distM.computeLogProbabilityOf(i));
        }
    }
}