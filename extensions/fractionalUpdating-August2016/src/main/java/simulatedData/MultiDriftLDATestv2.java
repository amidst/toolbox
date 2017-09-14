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

package simulatedData;

import eu.amidst.core.exponentialfamily.EF_Dirichlet;
import eu.amidst.core.exponentialfamily.EF_TruncatedExponential;
import eu.amidst.core.exponentialfamily.ParameterVariables;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.lda.core.PlateauLDA;

/**
 * Created by andresmasegosa on 20/1/17.
 */
public class MultiDriftLDATestv2 {

    public static void test1(String[] args) {

        int sampleSizeT = 1;
/*
        double[] countsP0 = {1, 1, 1};
        double[] countsQ0 = {10, 10, 10};
        double[] countsQ1 = {10, 0.00001, 20};
        int nstates = countsP0.length;
*/

        int nstates = 10000;
        double[] countsP0 = new double[nstates];
        double[] countsQ0 = new double[nstates];
        double[] countsQ1 = new double[nstates];

        for (int i = 0; i < nstates; i++) {
            countsP0[i]=PlateauLDA.TOPIC_PRIOR;
        }


        for (int i = 0; i < 100; i++) {
            countsQ0[i]=100+countsP0[i];
        }

        for (int i = 100; i < nstates; i++) {
            countsQ0[i]=countsP0[i];
        }

        for (int i = 0; i < 100; i++) {
            countsQ1[i]=0;
        }

        for (int i = 100; i < 150; i++) {
            countsQ1[i]=100;
        }

        double deltaQ = 0.9999;

        double delta = 0.1;

        for (int i = 0; i < nstates; i++) {
            countsQ0[i] *= sampleSizeT;
            countsQ1[i] *= (sampleSizeT);
        }


        Variables variables = new Variables();
        ParameterVariables parameterVariables = new ParameterVariables(0);
        EF_TruncatedExponential ef_TExpQ = variables.newTruncatedExponential("E").getDistributionType().newEFUnivariateDistribution(delta);


        Variable dirichletParameter = parameterVariables.newDirichletParameter("Local", nstates);
        EF_Dirichlet localP0 = new EF_Dirichlet(dirichletParameter);
        EF_Dirichlet localQ0 = new EF_Dirichlet(dirichletParameter);
        EF_Dirichlet localQ1 = new EF_Dirichlet(dirichletParameter);



        for (int k = 0; k < 10; k++) {


            for (int i = 0; i < nstates; i++) {
                localP0.getNaturalParameters().set(i, countsP0[i]);
                localQ0.getNaturalParameters().set(i, countsQ0[i]);
                localQ1.getNaturalParameters().set(i, deltaQ * countsQ0[i] + (1 - deltaQ) * countsP0[i] + countsQ1[i]);
            }

            localP0.updateMomentFromNaturalParameters();
            localQ0.updateMomentFromNaturalParameters();
            localQ1.updateMomentFromNaturalParameters();

            //double localKLQ1P0 = MultiDriftLDAv2.local_kl(localQ1,localP0);
            //double localKLQ1Q0 = MultiDriftLDAv2.local_kl(localQ1,localQ0);


            double localKLQ1P0 = localQ1.kl(localP0.getNaturalParameters(),localP0.computeLogNormalizer());
            double localKLQ1Q0 = localQ1.kl(localQ0.getNaturalParameters(),localQ0.computeLogNormalizer());

            System.out.print(localKLQ1P0 +"\t" + localKLQ1Q0 + "\t");
            ef_TExpQ.getNaturalParameters().set(0, localKLQ1P0 - localKLQ1Q0 + delta);
            ef_TExpQ.fixNumericalInstability();
            ef_TExpQ.updateMomentFromNaturalParameters();
            System.out.print(localQ0.getNaturalParameters().get(0)+"\t"+localQ1.getNaturalParameters().get(0)+"\t");
            System.out.print(ef_TExpQ.getMomentParameters().get(0) + "\n");
            deltaQ = ef_TExpQ.getMomentParameters().get(0);
            System.out.println(" -----------");
        }
    }

    public static void main(String[] args) {
        MultiDriftLDATestv2.test1(args);
    }
}