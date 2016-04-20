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

package eu.amidst.ecml2016;

import COM.hugin.HAPI.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.inference.DynamicMAPInference;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.huginlink.converters.BNConverterToHugin;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by dario on 08/03/16.
 */
public class ComparisonToHugin {

    public static void main(String[] args) {

        int seed = 186248462;

        int nTimeSteps=10;



        Random random = new Random(seed);

        HiddenLayerDynamicModel model = new HiddenLayerDynamicModel();



        model.setnStatesClassVar(2);
        model.setnStatesHidden(2);
        model.setnStates(2);

        model.setnHiddenContinuousVars(2);
        model.setnObservableDiscreteVars(2);
        model.setnObservableContinuousVars(2);

        model.generateModel();


        model.printDAG();

        model.setSeed(random.nextInt());
        model.randomInitialization(random);
        model.setProbabilityOfKeepingClass(0.6);

        model.printHiddenLayerModel();


        DynamicBayesianNetwork DBNmodel = model.getModel();
        model.generateEvidence(nTimeSteps);

        List<DynamicAssignment> evidence = model.getEvidenceNoClass();
        evidence.forEach(dynamicAssignment -> System.out.println(dynamicAssignment.outputString(DBNmodel.getDynamicVariables().getListOfDynamicVariables())));
        System.out.println("\n\n");

        DynamicMAPInference dynMAP = new DynamicMAPInference();

        Variable MAPVariable = model.getClassVariable();
        dynMAP.setModel(DBNmodel);
        dynMAP.setMAPvariable(MAPVariable);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);

        dynMAP.setEvidence(evidence);


        BayesianNetwork staticModel = dynMAP.getUnfoldedStaticModel();
        Assignment staticEvidence = dynMAP.getUnfoldedEvidence();


        //System.out.println(staticModel.toString());

        //System.out.println("STATIC VARIABLES");
        //staticModel.getVariables().getListOfVariables().forEach(variable -> System.out.println(variable.getName()));

        //System.out.println("STATIC EVIDENCE:");
        //System.out.println(staticEvidence.outputString(staticModel.getVariables().getListOfVariables())+"\n");



        try {
            Domain huginBN = BNConverterToHugin.convertToHugin(staticModel);
            huginBN.compile();
            System.out.println("HUGIN Domain compiled");

            staticEvidence.getVariables().forEach(variable -> {
                if (variable.isMultinomial()){
                    try {
                        ((DiscreteNode) huginBN.getNodeByName(variable.getName())).selectState((int) staticEvidence.getValue(variable));
                    }
                    catch (ExceptionHugin e) {
                        System.out.println(e.getMessage());
                    }
                }
                else if (variable.isNormal()) {
                    try {
                      ((ContinuousChanceNode)huginBN.getNodeByName(variable.getName())).enterValue(staticEvidence.getValue(variable));
                    }
                    catch (ExceptionHugin e) {
                        System.out.println(e.getMessage());
                    }
                }
                else {
                    throw new IllegalArgumentException("Variable type not allowed.");
                }
            });

            System.out.println("HUGIN Evidence set");

            huginBN.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);

            System.out.println("HUGIN Propagation done");
            NodeList classVarReplications = new NodeList();

            //System.out.println(huginBN.getNodes().toString());
            huginBN.getNodes().stream().filter(node -> {
                try {
                    return node.getName().contains(MAPVariable.getName());
                }
                catch (ExceptionHugin e) {
                    System.out.println(e.getMessage());
                    return false;
                }
            }).forEach(classVarReplications::add);

            System.out.println("HUGIN Prob. evidence: " + huginBN.getLogLikelihood());


            System.out.println("HUGIN MAP Variables:" + classVarReplications.toString());

            huginBN.findMAPConfigurations(classVarReplications, 0.0001);

            System.out.println("HUGIN MAP Sequences:");
            for (int i = 0; i < huginBN.getNumberOfMAPConfigurations() && i<3; i++) {
                System.out.println(Arrays.toString(huginBN.getMAPConfiguration(i)) + " with probability " + huginBN.getProbabilityOfMAPConfiguration(i));
            }
        }
        catch (ExceptionHugin e) {
            System.out.println("\nHUGIN EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        dynMAP.setNumberOfMergedClassVars(3);


        dynMAP.setSampleSize(10000);

        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
        System.out.println("DynMAP (Grouped-IS) Sequence:");
        System.out.println(Arrays.toString(dynMAP.getMAPsequence()));

        dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
        System.out.println("DynMAP (Grouped-VMP) Sequence:");
        System.out.println(Arrays.toString(dynMAP.getMAPsequence()));

        dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.IS);
        System.out.println("DynMAP (Ungrouped-IS) Sequence:");
        System.out.println(Arrays.toString(dynMAP.getMAPsequence()));

        dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.VMP);
        System.out.println("DynMAP (Ungrouped-VMP) Sequence:");
        System.out.println(Arrays.toString(dynMAP.getMAPsequence()));

        System.out.println("ORIGINAL SEQUENCE:\n" + Arrays.toString(model.getClassSequence()));
    }

}
