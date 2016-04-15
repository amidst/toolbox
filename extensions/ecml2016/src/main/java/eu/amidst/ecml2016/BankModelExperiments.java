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
import eu.amidst.core.inference.messagepassing.VMP;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by dario on 08/03/16.
 */
public class BankModelExperiments {

    final static int maxTimeStepsHugin=10;

    public static void main(String[] args) {


        //BasicConfigurator.configure();
        VMP dumb_vmp = new VMP();



        int nTimeSteps;
        int numberOfEvidencesPerModel;
        int nSamplesForIS;
        int seedEvidence;


        if (args.length!=4) {

            System.out.println("\nIncorrect number of parameters (4 are needed: nTimeSteps, nEvidences, nSamplesIS, seed)");
            System.out.println("Using default values for parameters\n\n");

            nTimeSteps=50;
            numberOfEvidencesPerModel = 30;
            nSamplesForIS=20000;
            seedEvidence=2795;
        }
        else {
            nTimeSteps = Integer.parseInt(args[0]);
            numberOfEvidencesPerModel = Integer.parseInt(args[1]);
            nSamplesForIS = Integer.parseInt(args[2]);
            seedEvidence = Integer.parseInt(args[3]);
        }

        Random randomEvidence= new Random(seedEvidence);

        System.out.println("seedEvidence: " + seedEvidence);
        System.out.println("nSamplesIS: " + nSamplesForIS);


        int [] sequenceAllZeros = new int[nTimeSteps];
        int [] sequenceAllOnes = Arrays.stream(sequenceAllZeros).map(k-> k+1).toArray();

        double[] precision_UngroupedIS = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP = new double[numberOfEvidencesPerModel];

        double[] precision_allZeros = new double[numberOfEvidencesPerModel];
        double[] precision_allOnes = new double[numberOfEvidencesPerModel];

        double[] precision_Hugin = new double[numberOfEvidencesPerModel];


        double[] times_UngroupedIS = new double[numberOfEvidencesPerModel];
        double[] times_2GroupedIS = new double[numberOfEvidencesPerModel];
        double[] times_3GroupedIS = new double[numberOfEvidencesPerModel];
        double[] times_4GroupedIS = new double[numberOfEvidencesPerModel];

        double[] times_UngroupedVMP = new double[numberOfEvidencesPerModel];
        double[] times_2GroupedVMP = new double[numberOfEvidencesPerModel];
        double[] times_3GroupedVMP = new double[numberOfEvidencesPerModel];
        double[] times_4GroupedVMP = new double[numberOfEvidencesPerModel];

        double[] times_Hugin = new double[numberOfEvidencesPerModel];


        BankSimulatedDynamicModel3 model = new BankSimulatedDynamicModel3();

        model.generateModel();
        model.printDAG();

        long timeStart, timeStop;
        double executionTime;

        int experimentNumber = 0;

        System.out.println("\nDYNAMIC MODEL \n");


        //double probKeepingClassState = 0.80;
        //model.setProbabilityOfKeepingClass(probKeepingClassState);


        DynamicBayesianNetwork DBNmodel = model.getModel();
        System.out.println(DBNmodel.toString());


        for (int j = 0; j < numberOfEvidencesPerModel; j++) {

            System.out.println("\nEVIDENCE NUMBER "+ j);
            System.out.println("(only the LocalIncomes and LocalExpenses values are given to the inference methods as evidence)\n");
            model.setSeed(randomEvidence.nextInt());
            model.generateEvidence(nTimeSteps);

            List<DynamicAssignment> evidence = model.getEvidenceNoClass();
//                IntStream.range(0,evidence.size()).forEachOrdered(k -> {
//                    if (k%2==0) {
//                        evidence.get(k).getVariables().forEach(variable -> evidence.get(k).setValue(variable, Utils.missingValue()));
//                    }
//                });
            List<DynamicAssignment> fullEvidence = model.getFullEvidence();
            fullEvidence.forEach(dynamicAssignment -> System.out.println(dynamicAssignment.outputString(DBNmodel.getDynamicVariables().getListOfDynamicVariables())));
            System.out.println("\n");



            DynamicMAPInference dynMAP = new DynamicMAPInference();

            Variable MAPVariable = model.getClassVariable();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            dynMAP.setEvidence(evidence);

            BayesianNetwork staticModel = dynMAP.getUnfoldedStaticModel();
            Assignment staticEvidence = dynMAP.getUnfoldedEvidence();

            int[] sequence_Hugin = new int[nTimeSteps];
            if (nTimeSteps<=maxTimeStepsHugin) {
                try {

                    timeStart = System.nanoTime();

                    Domain huginBN = BNConverterToHugin.convertToHugin(staticModel);
                    huginBN.compile();
                    System.out.println("HUGIN Domain compiled");

                    staticEvidence.getVariables().forEach(variable -> {
                        if (variable.isMultinomial()) {
                            try {
                                ((DiscreteNode) huginBN.getNodeByName(variable.getName())).selectState((int) staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else if (variable.isNormal()) {
                            try {
                                ((ContinuousChanceNode) huginBN.getNodeByName(variable.getName())).enterValue(staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else {
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
                        } catch (ExceptionHugin e) {
                            System.out.println(e.getMessage());
                            return false;
                        }
                    }).forEach(classVarReplications::add);

//                System.out.println("HUGIN Prob. evidence: " + huginBN.getLogLikelihood());


//                System.out.println("HUGIN MAP Variables:" + classVarReplications.toString());

                    huginBN.findMAPConfigurations(classVarReplications, 0.05);
                    System.out.println("HUGIN MAP configuration found");
        //                System.out.println("HUGIN MAP Sequences:");
        //                for (int i = 0; i < huginBN.getNumberOfMAPConfigurations() && i < 3; i++) {
        //                    System.out.println(Arrays.toString(huginBN.getMAPConfiguration(i)) + " with probability " + huginBN.getProbabilityOfMAPConfiguration(i));
        //                }
                    sequence_Hugin = huginBN.getMAPConfiguration(0);

                    timeStop = System.nanoTime();
                    executionTime = (double) (timeStop - timeStart) / 1000000000.0;
                    times_Hugin[experimentNumber]=executionTime;


                } catch (ExceptionHugin e) {
                    System.out.println("\nHUGIN EXCEPTION:");
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("\n\n");

            int [] sequence_original = model.getClassSequence();


            //System.out.println("ORIGINAL SEQUENCE:               " + Arrays.toString(sequence_original));


//                if (nTimeSteps<=maxTimeStepsHugin) {
//                    System.out.println("HUGIN MAP Sequence:              " + Arrays.toString(sequence_Hugin));
//                }



            /////////////////////////////////////////////////
            // UNGROUPED VARIABLES WITH I.S.
            /////////////////////////////////////////////////

            final BayesianNetwork unfoldedStaticModel1 = dynMAP.getUnfoldedStaticModel();

//                try {
//                    BNConverterToHugin.convertToHugin(unfoldedStaticModel1).saveAsNet(outputDirectory + "ungroupedModel.net");
//                }
//                catch (ExceptionHugin e) {
//                    System.out.println(e.toString());
//                }
            //unfoldedStaticModel1.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("ClassVar")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel1.getConditionalDistribution(variable).toString()));

            timeStart = System.nanoTime();

            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);


            dynMAP.setSampleSize(nSamplesForIS);
            dynMAP.setEvidence(evidence);

            dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.IS);
            int [] sequence_UngroupedIS = dynMAP.getMAPsequence();

            timeStop = System.nanoTime();
            executionTime = (double) (timeStop - timeStart) / 1000000000.0;
            times_UngroupedIS[experimentNumber]=executionTime;

//                System.out.println("Ungrouped IS finished");
//                System.out.println("\n\n");


            /////////////////////////////////////////////////
            // UNGROUPED VARIABLES WITH VMP
            /////////////////////////////////////////////////

            final BayesianNetwork unfoldedStaticModel2 = dynMAP.getUnfoldedStaticModel();
            //unfoldedStaticModel2.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("ClassVar")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel2.getConditionalDistribution(variable).toString()));


            timeStart = System.nanoTime();
            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);
            dynMAP.setEvidence(evidence);

            int [] sequence_UngroupedVMP;
            boolean sequence_UngroupedVMP_computed = false;

            try {
                dynMAP.runInferenceUngroupedMAPVariable(DynamicMAPInference.SearchAlgorithm.VMP);
                sequence_UngroupedVMP_computed = true;
                sequence_UngroupedVMP = dynMAP.getMAPsequence();
            }
            catch (IllegalStateException e) {
                sequence_UngroupedVMP = new int[nTimeSteps];
                System.out.println(e.getMessage());
            }

            timeStop = System.nanoTime();
            executionTime = (double) (timeStop - timeStart) / 1000000000.0;
            times_UngroupedVMP[experimentNumber]=executionTime;


//                System.out.println("Ungrouped VMP finished");
//                System.out.println("\n\n");

            /////////////////////////////////////////////////
            // 2-GROUPED VARIABLES WITH I.S.
            /////////////////////////////////////////////////



            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            dynMAP.setSampleSize(nSamplesForIS);


            dynMAP.setNumberOfMergedClassVars(2);
            dynMAP.computeMergedClassVarModels();

            dynMAP.setEvidence(evidence);

            //final BayesianNetwork unfoldedStaticModel3 = dynMAP.getMergedClassVarModels().get(0);
            //unfoldedStaticModel3.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel3.getConditionalDistribution(variable).toString()));
            //final BayesianNetwork unfoldedStaticModel4 = dynMAP.getMergedClassVarModels().get(1);
            //unfoldedStaticModel4.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel4.getConditionalDistribution(variable).toString()));

            //System.out.println(unfoldedStaticModel3.toString());
            //System.out.println(unfoldedStaticModel4.toString());

//                try {
//                    BNConverterToHugin.convertToHugin(unfoldedStaticModel3).saveAsNet(outputDirectory + "2groupedModel_0.net");
//                    BNConverterToHugin.convertToHugin(unfoldedStaticModel4).saveAsNet(outputDirectory + "2groupedModel_1.net");
//                }
//                catch (ExceptionHugin e) {
//                    System.out.println("Error converting to HUGIN networks");
//                    System.out.println(e.toString());
//                }

            dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
            int [] sequence_2GroupedIS = dynMAP.getMAPsequence();

            timeStop = System.nanoTime();
            executionTime = (double) (timeStop - timeStart) / 1000000000.0;
            times_2GroupedIS[experimentNumber]=executionTime;

            List<int[]> submodel_sequences_2GroupedIS = dynMAP.getBestSequencesForEachSubmodel();

//                System.out.println("2-grouped IS finished");
//                System.out.println("\n\n");

            /////////////////////////////////////////////////
            // 2-GROUPED VARIABLES WITH VMP
            /////////////////////////////////////////////////

            timeStart = System.nanoTime();

            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            dynMAP.setNumberOfMergedClassVars(2);
            dynMAP.computeMergedClassVarModels();

            dynMAP.setEvidence(evidence);

            //final BayesianNetwork unfoldedStaticModel5 = dynMAP.getMergedClassVarModels().get(0);
            //unfoldedStaticModel5.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel5.getConditionalDistribution(variable).toString()));
            //final BayesianNetwork unfoldedStaticModel6 = dynMAP.getMergedClassVarModels().get(1);
            //unfoldedStaticModel6.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains("GROUPED")).forEachOrdered(variable -> System.out.println(unfoldedStaticModel6.getConditionalDistribution(variable).toString()));

            int [] sequence_2GroupedVMP;
            boolean sequence_2GroupedVMP_computed=false;
            try {
                dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
                sequence_2GroupedVMP = dynMAP.getMAPsequence();
                sequence_2GroupedVMP_computed=true;
            }
            catch (IllegalStateException e) {
                sequence_2GroupedVMP = new int[nTimeSteps];
                System.out.println(e.getMessage());
            }

            timeStop = System.nanoTime();
            executionTime = (double) (timeStop - timeStart) / 1000000000.0;
            times_2GroupedVMP[experimentNumber]=executionTime;

            List<int[]> submodel_sequences_2GroupedVMP = dynMAP.getBestSequencesForEachSubmodel();

//                System.out.println("2-grouped VMP finished");
//                System.out.println("\n\n");


            /////////////////////////////////////////////////
            // 3-GROUPED VARIABLES WITH I.S.
            /////////////////////////////////////////////////

            timeStart = System.nanoTime();

            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            dynMAP.setSampleSize(nSamplesForIS);


            dynMAP.setNumberOfMergedClassVars(3);
            dynMAP.computeMergedClassVarModels();

            dynMAP.setEvidence(evidence);

            dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
            int [] sequence_3GroupedIS = dynMAP.getMAPsequence();

            timeStop = System.nanoTime();
            executionTime = (double) (timeStop - timeStart) / 1000000000.0;
            times_3GroupedIS[experimentNumber]=executionTime;

            List<int[]> submodel_sequences_3GroupedIS = dynMAP.getBestSequencesForEachSubmodel();

//                System.out.println("3-grouped IS finished");
//                System.out.println("\n\n");


            /////////////////////////////////////////////////
            // 3-GROUPED VARIABLES WITH VMP
            /////////////////////////////////////////////////

            timeStart = System.nanoTime();

            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            dynMAP.setNumberOfMergedClassVars(3);
            dynMAP.computeMergedClassVarModels();

            dynMAP.setEvidence(evidence);

            int [] sequence_3GroupedVMP;
            boolean sequence_3GroupedVMP_computed=false;
            try {
                dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
                sequence_3GroupedVMP_computed=true;
                sequence_3GroupedVMP = dynMAP.getMAPsequence();
            }
            catch (IllegalStateException e) {
                sequence_3GroupedVMP = new int[nTimeSteps];
                System.out.println(e.getMessage());
            }

            timeStop = System.nanoTime();
            executionTime = (double) (timeStop - timeStart) / 1000000000.0;
            times_3GroupedVMP[experimentNumber]=executionTime;

            List<int[]> submodel_sequences_3GroupedVMP = dynMAP.getBestSequencesForEachSubmodel();


//                System.out.println("3-grouped VMP finished");
//                System.out.println("\n\n");

            /////////////////////////////////////////////////
            // 4-GROUPED VARIABLES WITH I.S.
            /////////////////////////////////////////////////

            timeStart = System.nanoTime();

            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            dynMAP.setSampleSize(nSamplesForIS);


            dynMAP.setNumberOfMergedClassVars(4);
            dynMAP.computeMergedClassVarModels();

            dynMAP.setEvidence(evidence);

            dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.IS);
            int [] sequence_4GroupedIS = dynMAP.getMAPsequence();

            timeStop = System.nanoTime();
            executionTime = (double) (timeStop - timeStart) / 1000000000.0;
            times_4GroupedIS[experimentNumber]=executionTime;

            List<int[]> submodel_sequences_4GroupedIS = dynMAP.getBestSequencesForEachSubmodel();

//                System.out.println("4-grouped IS finished");
//                System.out.println("\n\n");



            /////////////////////////////////////////////////
            // 4-GROUPED VARIABLES WITH VMP
            /////////////////////////////////////////////////

            timeStart = System.nanoTime();


            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(DBNmodel);
            dynMAP.setMAPvariable(MAPVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);

            dynMAP.setNumberOfMergedClassVars(4);
            dynMAP.computeMergedClassVarModels();

            dynMAP.setEvidence(evidence);

            int [] sequence_4GroupedVMP;
            boolean sequence_4GroupedVMP_computed=false;
            try {
                dynMAP.runInference(DynamicMAPInference.SearchAlgorithm.VMP);
                sequence_4GroupedVMP = dynMAP.getMAPsequence();
                sequence_4GroupedVMP_computed = true;
            }
            catch (IllegalStateException e) {
                sequence_4GroupedVMP = new int[nTimeSteps];
                System.out.println(e.getMessage());
            }

            timeStop = System.nanoTime();
            executionTime = (double) (timeStop - timeStart) / 1000000000.0;
            times_4GroupedVMP[experimentNumber]=executionTime;

            List<int[]> submodel_sequences_4GroupedVMP = dynMAP.getBestSequencesForEachSubmodel();

//                System.out.println("4-grouped VMP finished\n\n");
//                System.out.println("\n\n");



            System.out.println("ORIGINAL SEQUENCE:               " + Arrays.toString(sequence_original));

            if (nTimeSteps<=maxTimeStepsHugin) {
                System.out.println("HUGIN MAP Sequence:              " + Arrays.toString(sequence_Hugin));
            }

            System.out.println("DynMAP (Ungrouped-IS) Sequence:  " + Arrays.toString(sequence_UngroupedIS));
            System.out.println();

            System.out.println("DynMAP (2Grouped-IS) Sequence:   " + Arrays.toString(sequence_2GroupedIS));
            System.out.println("       (2Gr-IS) Seq. Submodel 0: " + Arrays.toString(submodel_sequences_2GroupedIS.get(0)));
            System.out.println("       (2Gr-IS) Seq. Submodel 1: " + Arrays.toString(submodel_sequences_2GroupedIS.get(1)));
            System.out.println();

            System.out.println("DynMAP (3Grouped-IS) Sequence:   " + Arrays.toString(sequence_3GroupedIS));
            System.out.println("       (3Gr-IS) Seq. Submodel 0: " + Arrays.toString(submodel_sequences_3GroupedIS.get(0)));
            System.out.println("       (3Gr-IS) Seq. Submodel 1: " + Arrays.toString(submodel_sequences_3GroupedIS.get(1)));
            System.out.println("       (3Gr-IS) Seq. Submodel 2: " + Arrays.toString(submodel_sequences_3GroupedIS.get(2)));
            System.out.println();

            System.out.println("DynMAP (4Grouped-IS) Sequence:   " + Arrays.toString(sequence_4GroupedIS));
            System.out.println("       (4Gr-IS) Seq. Submodel 0: " + Arrays.toString(submodel_sequences_4GroupedIS.get(0)));
            System.out.println("       (4Gr-IS) Seq. Submodel 1: " + Arrays.toString(submodel_sequences_4GroupedIS.get(1)));
            System.out.println("       (4Gr-IS) Seq. Submodel 2: " + Arrays.toString(submodel_sequences_4GroupedIS.get(2)));
            System.out.println("       (4Gr-IS) Seq. Submodel 3: " + Arrays.toString(submodel_sequences_4GroupedIS.get(3)));
            System.out.println();

            if (sequence_UngroupedVMP_computed) {
                System.out.println("DynMAP (Ungrouped-VMP) Sequence: " + Arrays.toString(sequence_UngroupedVMP));
                System.out.println();
            }
            else {
                System.out.println("DynMAP (Ungrouped-VMP) Sequence:  Not obtained\n");
            }

            if (sequence_2GroupedVMP_computed) {
                System.out.println("DynMAP (2Grouped-VMP) Sequence:  " + Arrays.toString(sequence_2GroupedVMP));
                System.out.println("       (2Gr-VMP) Seq. Submodel 0:" + Arrays.toString(submodel_sequences_2GroupedVMP.get(0)));
                System.out.println("       (2Gr-VMP) Seq. Submodel 1:" + Arrays.toString(submodel_sequences_2GroupedVMP.get(1)));
                System.out.println();
            }
            else {
                System.out.println("DynMAP (2Grouped-VMP) Sequence:   Not obtained\n");
            }

            if (sequence_3GroupedVMP_computed) {
                System.out.println("DynMAP (3Grouped-VMP) Sequence:  " + Arrays.toString(sequence_3GroupedVMP));
                System.out.println("       (3Gr-VMP) Seq. Submodel 0:" + Arrays.toString(submodel_sequences_3GroupedVMP.get(0)));
                System.out.println("       (3Gr-VMP) Seq. Submodel 1:" + Arrays.toString(submodel_sequences_3GroupedVMP.get(1)));
                System.out.println("       (3Gr-VMP) Seq. Submodel 2:" + Arrays.toString(submodel_sequences_3GroupedVMP.get(2)));
                System.out.println();
            }
            else {
                System.out.println("DynMAP (3Grouped-VMP) Sequence:   Not obtained\n");
            }
            if (sequence_4GroupedVMP_computed) {
                System.out.println("DynMAP (4Grouped-VMP) Sequence:  " + Arrays.toString(sequence_4GroupedVMP));
                System.out.println("       (4Gr-VMP) Seq. Submodel 0:" + Arrays.toString(submodel_sequences_4GroupedVMP.get(0)));
                System.out.println("       (4Gr-VMP) Seq. Submodel 1:" + Arrays.toString(submodel_sequences_4GroupedVMP.get(1)));
                System.out.println("       (4Gr-VMP) Seq. Submodel 2:" + Arrays.toString(submodel_sequences_4GroupedVMP.get(2)));
                System.out.println("       (4Gr-VMP) Seq. Submodel 3:" + Arrays.toString(submodel_sequences_4GroupedVMP.get(3)));
            }
            else {
                System.out.println("DynMAP (4Grouped-VMP) Sequence:   Not obtained\n");
            }


            double current_precision_Hugin=0;
            if (nTimeSteps<=maxTimeStepsHugin) {
                current_precision_Hugin = compareIntArrays(sequence_original, sequence_Hugin);
//                    System.out.println("Precision HUGIN: " + current_precision_Hugin);
                sequence_original = sequence_Hugin;
            }

            double current_precision_UngroupedIS=compareIntArrays(sequence_original,sequence_UngroupedIS);
            double current_precision_2GroupedIS=compareIntArrays(sequence_original,sequence_2GroupedIS);
            double current_precision_3GroupedIS=compareIntArrays(sequence_original,sequence_3GroupedIS);
            double current_precision_4GroupedIS=compareIntArrays(sequence_original,sequence_4GroupedIS);




//                System.out.println("Precision Ungrouped-IS: " + current_precision_UngroupedIS);
//                System.out.println("Precision 2Grouped-IS: " + current_precision_2GroupedIS);
//                System.out.println("Precision 3Grouped-IS: " + current_precision_3GroupedIS);
//                System.out.println("Precision 4Grouped-IS: " + current_precision_4GroupedIS);

            double current_precision_UngroupedVMP=compareIntArrays(sequence_original,sequence_UngroupedVMP);
            double current_precision_2GroupedVMP=compareIntArrays(sequence_original,sequence_2GroupedVMP);
            double current_precision_3GroupedVMP=compareIntArrays(sequence_original,sequence_3GroupedVMP);
            double current_precision_4GroupedVMP=compareIntArrays(sequence_original,sequence_4GroupedVMP);
//                System.out.println("Precision Ungrouped-VMP: " + current_precision_UngroupedVMP);
//                System.out.println("Precision 2Grouped-VMP: " + current_precision_2GroupedVMP);
//                System.out.println("Precision 3Grouped-VMP: " + current_precision_3GroupedVMP);
//                System.out.println("Precision 4Grouped-VMP: " + current_precision_4GroupedVMP);


            double current_precision_allZeros=compareIntArrays(sequence_original,sequenceAllZeros);
            double current_precision_allOnes=compareIntArrays(sequence_original,sequenceAllOnes);

            precision_UngroupedIS[experimentNumber]=current_precision_UngroupedIS;
            precision_2GroupedIS[experimentNumber]=current_precision_2GroupedIS;
            precision_3GroupedIS[experimentNumber]=current_precision_3GroupedIS;
            precision_4GroupedIS[experimentNumber]=current_precision_4GroupedIS;

            precision_UngroupedVMP[experimentNumber]=current_precision_UngroupedVMP;
            precision_2GroupedVMP[experimentNumber]=current_precision_2GroupedVMP;
            precision_3GroupedVMP[experimentNumber]=current_precision_3GroupedVMP;
            precision_4GroupedVMP[experimentNumber]=current_precision_4GroupedVMP;

            precision_Hugin[experimentNumber]=current_precision_Hugin;

            precision_allZeros[experimentNumber]=current_precision_allZeros;
            precision_allOnes[experimentNumber]=current_precision_allOnes;

            experimentNumber++;

//                if(i==1 && j==2) {
//                    System.exit(-60);
//                }
        }

//        double[] current_model_precision_Hugin = Arrays.copyOfRange(precision_Hugin,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//        double[] current_model_precision_UngroupedIS = Arrays.copyOfRange(precision_UngroupedIS,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//        double[] current_model_precision_2GroupedIS = Arrays.copyOfRange(precision_2GroupedIS,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//        double[] current_model_precision_3GroupedIS = Arrays.copyOfRange(precision_3GroupedIS,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//        double[] current_model_precision_4GroupedIS = Arrays.copyOfRange(precision_4GroupedIS,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//        double[] current_model_precision_UngroupedVMP = Arrays.copyOfRange(precision_UngroupedVMP,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//        double[] current_model_precision_2GroupedVMP = Arrays.copyOfRange(precision_2GroupedVMP,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//        double[] current_model_precision_3GroupedVMP = Arrays.copyOfRange(precision_3GroupedVMP,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//        double[] current_model_precision_4GroupedVMP = Arrays.copyOfRange(precision_4GroupedVMP,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//
//        double[] current_model_precision_allZeros = Arrays.copyOfRange(precision_allZeros,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);
//        double[] current_model_precision_allOnes = Arrays.copyOfRange(precision_allOnes,i*numberOfEvidencesPerModel,(i+1)*numberOfEvidencesPerModel);


//            if (nTimeSteps<=maxTimeStepsHugin) {
//                System.out.println("\nMEAN PRECISIONS FOR THIS MODEL: (compared to HUGIN MAP sequence)");
//                System.out.println("         HUGIN: " + Arrays.stream(current_model_precision_Hugin).average().getAsDouble() + " (this one compared to the original sequence)");
//            }
//            else {
//                System.out.println("\nMEAN PRECISIONS FOR THIS MODEL: ");
//                System.out.println("         HUGIN:    Not computed");
//            }
//
//
//            //System.out.println("  Random Guess: " + Arrays.stream(current_model_precision_random).average().getAsDouble());
//
//            System.out.println("  IS Ungrouped: " + Arrays.stream(current_model_precision_UngroupedIS).average().getAsDouble());
//            System.out.println("  IS 2-Grouped: " + Arrays.stream(current_model_precision_2GroupedIS).average().getAsDouble());
//            System.out.println("  IS 3-Grouped: " + Arrays.stream(current_model_precision_3GroupedIS).average().getAsDouble());
//            System.out.println("  IS 4-Grouped: " + Arrays.stream(current_model_precision_4GroupedIS).average().getAsDouble());
//
//            System.out.println(" VMP Ungrouped: " + Arrays.stream(current_model_precision_UngroupedVMP).average().getAsDouble());
//            System.out.println(" VMP 2-Grouped: " + Arrays.stream(current_model_precision_2GroupedVMP).average().getAsDouble());
//            System.out.println(" VMP 3-Grouped: " + Arrays.stream(current_model_precision_3GroupedVMP).average().getAsDouble());
//            System.out.println(" VMP 4-Grouped: " + Arrays.stream(current_model_precision_4GroupedVMP).average().getAsDouble());
//            System.out.println(" All-Zeros seq: " + Arrays.stream(current_model_precision_allZeros).average().getAsDouble());
//            System.out.println(" All-Ones seq:  " + Arrays.stream(current_model_precision_allOnes).average().getAsDouble());

        System.out.println("\n\n");




        if (nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("\nGLOBAL MEAN PRECISIONS: (compared to HUGIN MAP sequence)");
            System.out.println("         HUGIN: " + Arrays.stream(precision_Hugin).average().getAsDouble() + " (this one compared to the original sequence)");

        }
        else {
            System.out.println("\nGLOBAL MEAN PRECISIONS:");
            System.out.println("         HUGIN:    Not computed");
        }

        System.out.println("  IS Ungrouped: " + Arrays.stream(precision_UngroupedIS).average().getAsDouble());
        System.out.println("  IS 2-Grouped: " + Arrays.stream(precision_2GroupedIS).average().getAsDouble());
        System.out.println("  IS 3-Grouped: " + Arrays.stream(precision_3GroupedIS).average().getAsDouble());
        System.out.println("  IS 4-Grouped: " + Arrays.stream(precision_4GroupedIS).average().getAsDouble());

        System.out.println(" VMP Ungrouped: " + Arrays.stream(precision_UngroupedVMP).average().getAsDouble());
        System.out.println(" VMP 2-Grouped: " + Arrays.stream(precision_2GroupedVMP).average().getAsDouble());
        System.out.println(" VMP 3-Grouped: " + Arrays.stream(precision_3GroupedVMP).average().getAsDouble());
        System.out.println(" VMP 4-Grouped: " + Arrays.stream(precision_4GroupedVMP).average().getAsDouble());

        System.out.println(" All-Zeros seq: " + Arrays.stream(precision_allZeros).average().getAsDouble());
        System.out.println(" All-Ones seq:  " + Arrays.stream(precision_allOnes).average().getAsDouble());




        System.out.println("\n\n EXECUTION TIMES: ");
        System.out.println(Arrays.toString(times_Hugin).replace("[","times_hugin=c(").replace("]",")"));

        System.out.println(Arrays.toString(times_UngroupedIS).replace("[","times_ungroupedIS=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_2GroupedIS).replace("[","times_2groupedIS=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_3GroupedIS).replace("[","times_3groupedIS=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_4GroupedIS).replace("[","times_4groupedIS=c(").replace("]",")"));

        System.out.println(Arrays.toString(times_UngroupedVMP).replace("[","times_ungroupedVMP=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_2GroupedVMP).replace("[","times_2groupedVMP=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_3GroupedVMP).replace("[","times_3groupedVMP=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_4GroupedVMP).replace("[","times_4groupedVMP=c(").replace("]",")"));


        System.out.println("\n\n MEAN EXECUTION TIMES: ");
        System.out.println("         HUGIN: " + Arrays.stream(times_Hugin).average().getAsDouble());

        System.out.println("  IS Ungrouped: " + Arrays.stream(times_UngroupedIS).average().getAsDouble());
        System.out.println("  IS 2-Grouped: " + Arrays.stream(times_2GroupedIS).average().getAsDouble());
        System.out.println("  IS 3-Grouped: " + Arrays.stream(times_3GroupedIS).average().getAsDouble());
        System.out.println("  IS 4-Grouped: " + Arrays.stream(times_4GroupedIS).average().getAsDouble());

        System.out.println(" VMP Ungrouped: " + Arrays.stream(times_UngroupedVMP).average().getAsDouble());
        System.out.println(" VMP 2-Grouped: " + Arrays.stream(times_2GroupedVMP).average().getAsDouble());
        System.out.println(" VMP 3-Grouped: " + Arrays.stream(times_3GroupedVMP).average().getAsDouble());
        System.out.println(" VMP 4-Grouped: " + Arrays.stream(times_4GroupedVMP).average().getAsDouble());


    }

    private static double compareIntArrays(int[] array1, int[] array2) {
        if (array1.length!=array2.length) {
            System.out.println("Both arrays must be the same length");
            System.exit(-50);
        }

        AtomicInteger atomicInteger = new AtomicInteger();
        IntStream.range(0,array1.length).forEachOrdered(i -> {
            if (array1[i]==array2[i]) {
                atomicInteger.incrementAndGet();
            }
        });

        return ((double)atomicInteger.get())/((double)array1.length);
    }
}
