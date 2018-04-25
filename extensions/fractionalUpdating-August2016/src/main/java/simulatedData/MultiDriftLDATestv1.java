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
import eu.amidst.lda.core.MultiDriftLDAv1;
import eu.amidst.lda.core.PlateauLDA;

/**
 * Created by andresmasegosa on 20/1/17.
 */
public class MultiDriftLDATestv1 {

    public static void test1(String[] args) {

        int sampleSizeT = 1;
/*
        double[] countsP0 = {1, 1, 1};
        double[] countsQ0 = {10, 10, 10};
        double[] countsQ1 = {10, 0.00001, 20};
        int nstates = countsP0.length;
*/

        int nstates = 100;
        double[] countsP0 = new double[nstates];
        double[] countsQ0 = new double[nstates];
        double[] countsQ1 = new double[nstates];

        for (int i = 0; i < nstates; i++) {
            countsP0[i]= PlateauLDA.TOPIC_PRIOR;
        }


        for (int i = 0; i < 10; i++) {
            countsQ0[i]=100+countsP0[i];
        }

        for (int i = 10; i < nstates; i++) {
            countsQ0[i]=countsP0[i];
        }

        for (int i = 10; i < 20; i++) {
            countsQ1[i]=50;
        }


        double delta = 0.1;

        for (int i = 0; i < nstates; i++) {
            countsQ0[i] *= sampleSizeT;
            countsQ1[i] *= (sampleSizeT);
        }


        Variables variables = new Variables();
        ParameterVariables parameterVariables = new ParameterVariables(0);
        EF_TruncatedExponential[] ef_TExpQ = new EF_TruncatedExponential[nstates];

        for (int i = 0; i < ef_TExpQ.length; i++) {
            ef_TExpQ[i] = variables.newTruncatedExponential("E" + i).getDistributionType().newEFUnivariateDistribution(delta);
        }


        Variable dirichletParameter = parameterVariables.newDirichletParameter("Local", nstates);
        EF_Dirichlet localP0 = new EF_Dirichlet(dirichletParameter);
        EF_Dirichlet localQ0 = new EF_Dirichlet(dirichletParameter);
        EF_Dirichlet localQ1 = new EF_Dirichlet(dirichletParameter);

        double[] deltas = new double[nstates];
        for (int i = 0; i < nstates; i++) {
            deltas[i] = 1.0;
        }

        for (int k = 0; k < 5; k++) {


            for (int i = 0; i < nstates; i++) {
                localP0.getNaturalParameters().set(i, countsP0[i]);
                localQ0.getNaturalParameters().set(i, countsQ0[i]);
                localQ1.getNaturalParameters().set(i, deltas[i] * countsQ0[i] + (1 - deltas[i]) * countsP0[i] + countsQ1[i]);
            }

            localP0.updateMomentFromNaturalParameters();
            localQ0.updateMomentFromNaturalParameters();
            localQ1.updateMomentFromNaturalParameters();

            double[] localKLQ1P0 = MultiDriftLDAv1.computeLocalKLDirichletBinary(localQ1, localP0);
            double[] localKLQ1Q0 = MultiDriftLDAv1.computeLocalKLDirichletBinary(localQ1, localQ0);

            for (int i = 0; i < nstates; i++) {
                System.out.print(localKLQ1P0[i] + "\t" + localKLQ1Q0[i] + "\t");
                ef_TExpQ[i].getNaturalParameters().set(0, localKLQ1P0[i] - localKLQ1Q0[i] + delta);
                ef_TExpQ[i].fixNumericalInstability();
                ef_TExpQ[i].updateMomentFromNaturalParameters();
                System.out.print(localQ1.getNaturalParameters().get(i) + "\t");
                System.out.print(ef_TExpQ[i].getMomentParameters().get(0) + "\n");
                deltas[i] = ef_TExpQ[i].getMomentParameters().get(0);
            }
            System.out.println("-----------");
        }
    }

    public static void test0(String[] args) {

        int sampleSizeT = 10000;
        double[] countsP0 = {1, 1, 1};
        double[] countsQ0 = {10, 10, 10};
        double[] countsQ1 = {10, 0.00001, 20};
        int nstates = countsP0.length;

/*        int nstates = 1000;
        double[] countsP0 = new double[nstates];
        double[] countsQ0 = new double[nstates];
        double[] countsQ1 = new double[nstates];

        for (int i = 0; i < nstates; i++) {
            countsP0[i]=countsQ0[i]=countsQ1[i]=1;
        }
        countsQ1[0]=1.1;
*/

        double delta = 0.1;

        for (int i = 0; i < nstates; i++) {
            countsQ0[i] *= sampleSizeT;
            countsQ1[i] *= (sampleSizeT);
        }


        Variables variables = new Variables();
        ParameterVariables parameterVariables = new ParameterVariables(0);
        EF_TruncatedExponential[] ef_TExpQ = new EF_TruncatedExponential[nstates];

        for (int i = 0; i < ef_TExpQ.length; i++) {
            ef_TExpQ[i] = variables.newTruncatedExponential("E" + i).getDistributionType().newEFUnivariateDistribution(delta);
        }


        Variable dirichletParameter = parameterVariables.newDirichletParameter("Local", nstates);
        EF_Dirichlet localP0 = new EF_Dirichlet(dirichletParameter);
        EF_Dirichlet localQ0 = new EF_Dirichlet(dirichletParameter);
        EF_Dirichlet localQ1 = new EF_Dirichlet(dirichletParameter);

        for (int i = 0; i < nstates; i++) {
            localP0.getNaturalParameters().set(i, countsP0[i]);
            localQ0.getNaturalParameters().set(i, countsQ0[i]);
            localQ1.getNaturalParameters().set(i, countsQ1[i]);
        }

        localP0.updateMomentFromNaturalParameters();
        localQ0.updateMomentFromNaturalParameters();
        localQ1.updateMomentFromNaturalParameters();

        double[] localKLQ1P0 = MultiDriftLDAv1.computeLocalKLDirichletBinary(localQ1, localP0);
        double[] localKLQ1Q0 = MultiDriftLDAv1.computeLocalKLDirichletBinary(localQ1, localQ0);

        for (int i = 0; i < nstates; i++) {
            System.out.print(localKLQ1P0[i] + "\t" + localKLQ1Q0[i] + "\t");
            ef_TExpQ[i].getNaturalParameters().set(0, localKLQ1P0[i] - localKLQ1Q0[i] + delta);
            ef_TExpQ[i].fixNumericalInstability();
            ef_TExpQ[i].updateMomentFromNaturalParameters();
            System.out.print(ef_TExpQ[i].getMomentParameters().get(0) + "\n");
        }
    }

    public static void main(String[] args) {
        MultiDriftLDATestv1.test1(args);
    }
}