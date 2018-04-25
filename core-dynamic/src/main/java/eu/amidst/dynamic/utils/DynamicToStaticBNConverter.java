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

package eu.amidst.dynamic.utils;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.VariableBuilder;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * This class converts a {@link DynamicBayesianNetwork} to a static {@link BayesianNetwork}.
 */
public class DynamicToStaticBNConverter {

    /**
     * Converts a given {@link DynamicBayesianNetwork} to a static {@link BayesianNetwork}
     * @param dbn a {@link DynamicBayesianNetwork} object.
     * @param nTimeSteps an {@code int} that represents the number of time steps.
     * @return  a {@link BayesianNetwork} object.
     */
    public static BayesianNetwork convertDBNtoBN(DynamicBayesianNetwork dbn, int nTimeSteps) {

        if (dbn==null)
            return null;

        DynamicVariables dynamicVariables = dbn.getDynamicVariables();
        Variables variables = new Variables();
        DynamicDAG dynamicDAG = dbn.getDynamicDAG();

        /*
         * 1st STEP: ADD REPLICATED VARIABLES.
         * REPLICATIONS OF THE REST OF VARIABLES (EACH ONE REPEATED 'nTimeSteps' TIMES).
         */
        dynamicVariables.getListOfDynamicVariables().stream()
                .forEach(dynVar ->
                                IntStream.range(0, nTimeSteps).forEach(i -> {
                                    VariableBuilder aux = dynVar.getVariableBuilder();
                                    aux.setName(dynVar.getName() + "_t" + Integer.toString(i));
                                    variables.newVariable(aux);
                                })
                );
        DAG dag = new DAG(variables);

        /*
         * 2nd STEP: ADD ARCS BETWEEN VARIABLES, I.E. DEFINE THE STATIC DAG.
         */
        for (int i = 0; i < nTimeSteps; i++) {
            for (int j = 0; j < dynamicVariables.getNumberOfVars(); j++) {

                Variable dynVar = dynamicVariables.getVariableById(j);
                Variable staticVar = variables.getVariableByName(dynVar.getName() + "_t" + Integer.toString(i));

                if (i==0) { // t=0
                    dynamicDAG.getParentSetTime0(dynVar).getParents().stream().forEach(parentaux2 -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentaux2.getName() + "_t0")));
                } else {
                    final int final_i=i;
                    dynamicDAG.getParentSetTimeT(dynVar).getParents().stream().filter(parentVar -> parentVar.isInterfaceVariable()).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentVar.getName().replace("_Interface", "_t" + Integer.toString(final_i - 1)))));
                    dynamicDAG.getParentSetTimeT(dynVar).getParents().stream().filter(parentVar -> !parentVar.isInterfaceVariable()).forEach(parentVar -> dag.getParentSet(staticVar).addParent(variables.getVariableByName(parentVar.getName() + "_t" + Integer.toString(final_i))));
                }
            }
        }

        BayesianNetwork bn = new BayesianNetwork(dag);

        /*
         * 3rd STEP: ADD CONDITIONAL DISTRIBUTIONS, I.E. DEFINE THE STATIC BN.
         */
        for (int i = 0; i < nTimeSteps; i++) {
            for (int j = 0; j < dynamicVariables.getNumberOfVars(); j++) {

                List<Variable> parentList = new ArrayList<>();
                ConditionalDistribution cdist;
                Variable staticVar;

                if(i==0) {
                    staticVar = variables.getVariableByName(dynamicVariables.getVariableById(j).getName() + "_t0");

                    cdist = Serialization.deepCopy(dbn.getConditionalDistributionsTime0().get(j));
                    cdist.getConditioningVariables().stream().forEachOrdered(cdvar -> parentList.add(variables.getVariableByName(cdvar.getName() + "_t0")));
                }
                else {

                    final int final_i = i;
                    staticVar = variables.getVariableByName(dynamicVariables.getVariableById(j).getName() + "_t" + Integer.toString(final_i));

                    cdist = Serialization.deepCopy(dbn.getConditionalDistributionsTimeT().get(j));
                    cdist.getConditioningVariables().stream().forEachOrdered(cdvar -> {
                        parentList.add(((cdvar.isInterfaceVariable() || (cdvar.getName().contains("_t"))) ?
                                variables.getVariableByName(cdvar.getName().replace("_Interface", "_t" + Integer.toString(final_i - 1))) :
                                variables.getVariableByName(cdvar.getName() + "_t" + Integer.toString(final_i))));
                    });
                }
                cdist.setConditioningVariables(parentList);
                cdist.setVar(staticVar);
                bn.setConditionalDistribution(staticVar, cdist);
            }
        }
        return bn;
    }

    public static void main(String[] args) {

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetworkGenerator.setNumberOfLinks(5);

        DynamicBayesianNetwork dynamicNaiveBayes = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);

        System.out.println("ORIGINAL DYNAMIC DAG:");

        System.out.println(dynamicNaiveBayes.getDynamicDAG().toString());
        //System.out.println(dynamicNaiveBayes.toString());
        System.out.println();
        //dynamicNaiveBayes.getDynamicVariables().getListOfDynamicVariables().forEach(var -> System.out.println(var.getName()));
        //dynamicNaiveBayes.getDynamicVariables().getListOfDynamicVariables().forEach(var -> System.out.println(var.getName()));

        BayesianNetwork bn = DynamicToStaticBNConverter.convertDBNtoBN(dynamicNaiveBayes,4);
        System.out.println("NEW STATIC DAG:");
        System.out.println();
        System.out.println(bn.getDAG().toString());

        System.out.println();
        System.out.println("ORIGINAL DYNAMIC BN:");
        System.out.println(dynamicNaiveBayes.toString());

        System.out.println("STATIC BN:");
        System.out.println(bn.toString());
    }
}
