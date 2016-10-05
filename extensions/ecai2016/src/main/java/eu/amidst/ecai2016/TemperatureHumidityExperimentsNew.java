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

package eu.amidst.ecai2016;

import COM.hugin.HAPI.*;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.inference.ImportanceSamplingCLG;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.inference.DynamicMAPInference;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.huginlink.converters.BNConverterToHugin;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;

/**
 * Created by dario on 08/03/16.
 */
public class TemperatureHumidityExperimentsNew {

    final static int maxTimeStepsHugin=10;

    public static void main(String[] args) {


        //BasicConfigurator.configure();
        VMP dumb_vmp = new VMP();



        int nTimeSteps;
        int numberOfEvidencesPerModel;
        int nSamplesForIS;
        int seedEvidence;
        double probKeepingClassState;


        if (args.length!=5) {

            System.out.println("\nIncorrect number of parameters (4 are needed: nTimeSteps, nEvidences, nSamplesIS, seed)");
            System.out.println("Using default values for parameters\n\n");

            nTimeSteps=9;
            numberOfEvidencesPerModel = 50;
            nSamplesForIS=10000;
//            seedEvidence=5185680;
            seedEvidence=32946;
            probKeepingClassState = 0.8;
        }
        else {
            nTimeSteps = Integer.parseInt(args[0]);
            numberOfEvidencesPerModel = Integer.parseInt(args[1]);
            nSamplesForIS = Integer.parseInt(args[2]);
            seedEvidence = Integer.parseInt(args[3]);
            probKeepingClassState = Double.parseDouble(args[4]);
        }

        Random randomEvidence= new Random(seedEvidence);

        System.out.println("seedEvidence: " + seedEvidence);
        System.out.println("nSamplesIS: " + nSamplesForIS);


//        int [] sequenceAllZeros = new int[nTimeSteps];
//        int [] sequenceAllOnes = stream(sequenceAllZeros).map(k-> k+1).toArray();

        
        int[] sequence_HuginIterativeAssignment = new int[nTimeSteps];
        int[] sequence_VMP_IterativeAssignment = new int[nTimeSteps];
        int[] sequence_IS_IterativeAssignment = new int[nTimeSteps];

        
        
        

        double[] precision_Hugin = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment = new double[numberOfEvidencesPerModel];


        double[] precision_UngroupedIS = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP = new double[numberOfEvidencesPerModel];






        double[] precision_Hugin_Probability = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment_Probability = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment_Probability = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment_Probability = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedIS_Probability = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS_Probability = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS_Probability = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS_Probability = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP_Probability = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP_Probability = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP_Probability = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP_Probability = new double[numberOfEvidencesPerModel];




        
        double[] precision_Hugin_01 = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment_01 = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment_01 = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment_01 = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedIS_01 = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS_01 = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS_01 = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS_01 = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP_01 = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP_01 = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP_01 = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP_01 = new double[numberOfEvidencesPerModel];


        
        


        double[] precision_Hugin_sub2 = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment_sub2 = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment_sub2 = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment_sub2 = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedIS_sub2 = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS_sub2 = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS_sub2 = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS_sub2 = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP_sub2 = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP_sub2 = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP_sub2 = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP_sub2 = new double[numberOfEvidencesPerModel];

        



        double[] precision_Hugin_sub3 = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment_sub3 = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment_sub3 = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment_sub3 = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedIS_sub3 = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS_sub3 = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS_sub3 = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS_sub3 = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP_sub3 = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP_sub3 = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP_sub3 = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP_sub3 = new double[numberOfEvidencesPerModel];















        double[] precision_Hugin_original = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment_original = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment_original = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment_original = new double[numberOfEvidencesPerModel];


        double[] precision_UngroupedIS_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS_original = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP_original = new double[numberOfEvidencesPerModel];






        double[] precision_Hugin_Probability_original = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment_Probability_original = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment_Probability_original = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment_Probability_original = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedIS_Probability_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS_Probability_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS_Probability_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS_Probability_original = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP_Probability_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP_Probability_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP_Probability_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP_Probability_original = new double[numberOfEvidencesPerModel];





        double[] precision_Hugin_01_original = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment_01_original = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment_01_original = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment_01_original = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedIS_01_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS_01_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS_01_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS_01_original = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP_01_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP_01_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP_01_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP_01_original = new double[numberOfEvidencesPerModel];






        double[] precision_Hugin_sub2_original = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment_sub2_original = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment_sub2_original = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment_sub2_original = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedIS_sub2_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS_sub2_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS_sub2_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS_sub2_original = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP_sub2_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP_sub2_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP_sub2_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP_sub2_original = new double[numberOfEvidencesPerModel];





        double[] precision_Hugin_sub3_original = new double[numberOfEvidencesPerModel];
        double[] precision_HuginIterativeAssignment_sub3_original = new double[numberOfEvidencesPerModel];
        double[] precision_VMP_IterativeAssignment_sub3_original = new double[numberOfEvidencesPerModel];
        double[] precision_IS_IterativeAssignment_sub3_original = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedIS_sub3_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedIS_sub3_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedIS_sub3_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedIS_sub3_original = new double[numberOfEvidencesPerModel];

        double[] precision_UngroupedVMP_sub3_original = new double[numberOfEvidencesPerModel];
        double[] precision_2GroupedVMP_sub3_original = new double[numberOfEvidencesPerModel];
        double[] precision_3GroupedVMP_sub3_original = new double[numberOfEvidencesPerModel];
        double[] precision_4GroupedVMP_sub3_original = new double[numberOfEvidencesPerModel];














        double[] times_Hugin = new double[numberOfEvidencesPerModel];
        double[] times_IterativeIS = new double[numberOfEvidencesPerModel];
        double[] times_IterativeVMP = new double[numberOfEvidencesPerModel];   

        double[] times_UngroupedIS = new double[numberOfEvidencesPerModel];
        double[] times_2GroupedIS = new double[numberOfEvidencesPerModel];
        double[] times_3GroupedIS = new double[numberOfEvidencesPerModel];
        double[] times_4GroupedIS = new double[numberOfEvidencesPerModel];

        double[] times_UngroupedVMP = new double[numberOfEvidencesPerModel];
        double[] times_2GroupedVMP = new double[numberOfEvidencesPerModel];
        double[] times_3GroupedVMP = new double[numberOfEvidencesPerModel];
        double[] times_4GroupedVMP = new double[numberOfEvidencesPerModel];




        TemperatureHumidityDynamicModel model = new TemperatureHumidityDynamicModel();

        model.generateModel();
        model.printDAG();

        long timeStart, timeStop;
        double executionTime;

        int experimentNumber = 0;

        System.out.println("\nDYNAMIC MODEL \n");


        model.setProbabilityOfKeepingClass(probKeepingClassState);


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
                    Domain huginBN2 = BNConverterToHugin.convertToHugin(staticModel);

                    huginBN2.compile();
                    System.out.println("HUGIN Iterative search: Domain compiled");

                    staticEvidence.getVariables().forEach(variable -> {
                        if (variable.isMultinomial()) {
                            try {
                                ((DiscreteNode) huginBN2.getNodeByName(variable.getName())).selectState((int) staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else if (variable.isNormal()) {
                            try {
                                ((ContinuousChanceNode) huginBN2.getNodeByName(variable.getName())).enterValue(staticEvidence.getValue(variable));
                            } catch (ExceptionHugin e) {
                                System.out.println(e.getMessage());
                            }
                        } else {
                            throw new IllegalArgumentException("Variable type not allowed.");
                        }
                    });

                    System.out.println("HUGIN Iterative search: Evidence set");

                    huginBN2.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);

                    System.out.println("HUGIN Iterative search: Propagation done");
                    NodeList variablesToAssign = new NodeList();

                    //System.out.println(huginBN.getNodes().toString());
                    huginBN2.getNodes().stream().filter(node -> {
                        try {
                            return node.getName().contains(MAPVariable.getName());
                        } catch (ExceptionHugin e) {
                            System.out.println(e.getMessage());
                            return false;
                        }
                    }).forEach(node -> {
                        variablesToAssign.add(node);
                    });




                    System.out.println("HUGIN Iterative search: Iterative assignments");

                    while(! variablesToAssign.isEmpty()) {

                        Node nodeMinEntropy = null;
                        Table nodeMinEntropyDistribution = null;
                        double minEntropy = Double.POSITIVE_INFINITY;

//                        System.out.println("HUGIN Iterative MAP assignment");
                        for (Node thisNode : variablesToAssign) {

                            NodeList thisNodeList = new NodeList();
                            thisNodeList.add(thisNode);
                            Table posteriorThisNode = huginBN2.getMarginal(thisNodeList);


                            double p0 = posteriorThisNode.getData()[0];
                            double p1 = posteriorThisNode.getData()[1];
                            double thisEntropy = - (p0 * Math.log(p0) + p1 * Math.log(p1));

                            if (nodeMinEntropy == null || thisEntropy < minEntropy) {
                                nodeMinEntropy = thisNode;
                                nodeMinEntropyDistribution = posteriorThisNode;
                                minEntropy = thisEntropy;
                            }

//                            System.out.println("Marginal of " + thisNode.getName() + ": " + Arrays.toString(posteriorThisNode.getData()) + " with entropy: " + thisEntropy);

                        }

//                        System.out.println("\nNode with min entropy: " + nodeMinEntropy.getName() + " with :" + minEntropy);
//                        System.out.println("with distribution: " + Arrays.toString(nodeMinEntropyDistribution.getData()));
                        double nodeMinEntropy_p0 = nodeMinEntropyDistribution.getData()[0];
                        double nodeMinEntropy_p1 = nodeMinEntropyDistribution.getData()[1];

                        int thisNodeValue = (nodeMinEntropy_p0 > nodeMinEntropy_p1) ? 0 : 1;
                        ((DiscreteNode)huginBN2.getNodeByName(nodeMinEntropy.getName())).selectState(thisNodeValue);

                        String nodeNumberString = nodeMinEntropy.getName().substring(nodeMinEntropy.getName().lastIndexOf("_t")+2);
                        int nodeNumber = Integer.parseInt(nodeNumberString);
                        sequence_HuginIterativeAssignment[nodeNumber] = thisNodeValue;
//                        System.out.println("Assigned value " + thisNodeValue + " to node " + nodeNumber);

                        variablesToAssign.remove(nodeMinEntropy);
                        huginBN2.propagate(Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL);

                    }

//                    System.out.println(Arrays.toString(sequence_HuginIterativeAssignment));


                    System.out.println("HUGIN Iterative search: Complete");
                    huginBN2.delete();



//                System.out.println("HUGIN Prob. evidence: " + huginBN.getLogLikelihood());


//                System.out.println("HUGIN MAP Variables:" + classVarReplications.toString());

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

                    huginBN.getNodes().stream().filter(node -> {
                        try {
                            return node.getName().contains(MAPVariable.getName());
                        } catch (ExceptionHugin e) {
                            System.out.println(e.getMessage());
                            return false;
                        }
                    }).forEach(node -> {
                        classVarReplications.add(node);
                        variablesToAssign.add(node);
                    });

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





            /////////////////////////////////////////////////////////////////////////////////////////
            //   ITERATIVE ASSIGNMENT OF VARIABLE WITH LESS ENTROPY, WITH IS AND VMP
            /////////////////////////////////////////////////////////////////////////////////////////


            if(nTimeSteps<150) {
                System.out.println("IS Iterative MAP assignment");

                timeStart = System.nanoTime();
                List<Variable> MAPvariableToAssign = staticModel.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains(MAPVariable.getName())).collect(Collectors.toList());
                HashMapAssignment evidencePlusMAPVariables = new HashMapAssignment(staticEvidence);

                ImportanceSamplingCLG importanceSamplingCLG = new ImportanceSamplingCLG();
                importanceSamplingCLG.setModel(staticModel);
                importanceSamplingCLG.setVariablesAPosteriori(MAPvariableToAssign);
                importanceSamplingCLG.setSampleSize(nSamplesForIS / 10);

                while (!MAPvariableToAssign.isEmpty()) {


                    importanceSamplingCLG.setEvidence(evidencePlusMAPVariables);
                    importanceSamplingCLG.runInference();

                    Variable varMinEntropy = null;
                    double[] varMinEntropyProbabilities = new double[MAPVariable.getNumberOfStates()];
                    double minEntropy = Double.POSITIVE_INFINITY;

                    for (Variable thisVariable : MAPvariableToAssign) {

                        Multinomial MAPVarPosteriorDistribution = importanceSamplingCLG.getPosterior(thisVariable);

                        double[] MAPVarPosteriorProbabilities = MAPVarPosteriorDistribution.getParameters();


                        double p0 = MAPVarPosteriorProbabilities[0];
                        double p1 = MAPVarPosteriorProbabilities[1];
                        double thisEntropy = -(p0 * Math.log(p0) + p1 * Math.log(p1));

                        if (varMinEntropy == null || thisEntropy < minEntropy) {
                            varMinEntropy = thisVariable;
                            varMinEntropyProbabilities = MAPVarPosteriorProbabilities;
                            minEntropy = thisEntropy;
                        }

//                            System.out.println("Marginal of " + thisNode.getName() + ": " + Arrays.toString(posteriorThisNode.getData()) + " with entropy: " + thisEntropy);

                    }

//                        System.out.println("\nNode with min entropy: " + nodeMinEntropy.getName() + " with :" + minEntropy);
//                        System.out.println("with distribution: " + Arrays.toString(nodeMinEntropyDistribution.getData()));
                    double nodeMinEntropy_p0 = varMinEntropyProbabilities[0];
                    double nodeMinEntropy_p1 = varMinEntropyProbabilities[1];

                    int thisVarValue = (nodeMinEntropy_p0 > nodeMinEntropy_p1) ? 0 : 1;
                    evidencePlusMAPVariables.setValue(varMinEntropy, thisVarValue);

                    String nodeNumberString = varMinEntropy.getName().substring(varMinEntropy.getName().lastIndexOf("_t") + 2);
                    int nodeNumber = Integer.parseInt(nodeNumberString);
                    sequence_IS_IterativeAssignment[nodeNumber] = thisVarValue;
//                        System.out.println("Assigned value " + thisNodeValue + " to node " + nodeNumber);

                    MAPvariableToAssign.remove(varMinEntropy);

                }
                timeStop = System.nanoTime();
                executionTime = (double) (timeStop - timeStart) / 1000000000.0;
                times_IterativeIS[experimentNumber] = executionTime;


                System.out.println("VMP Iterative MAP assignment");
                timeStart = System.nanoTime();

                MAPvariableToAssign = staticModel.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains(MAPVariable.getName())).collect(Collectors.toList());
                evidencePlusMAPVariables = new HashMapAssignment(staticEvidence);

                VMP vmp = new VMP();
                vmp.setModel(staticModel);


                while (!MAPvariableToAssign.isEmpty()) {


                    vmp.setEvidence(evidencePlusMAPVariables);
                    vmp.runInference();

                    Variable varMinEntropy = null;
                    double[] varMinEntropyProbabilities = new double[MAPVariable.getNumberOfStates()];
                    double minEntropy = Double.POSITIVE_INFINITY;

                    for (Variable thisVariable : MAPvariableToAssign) {

                        Multinomial MAPVarPosteriorDistribution = vmp.getPosterior(thisVariable);

                        double[] MAPVarPosteriorProbabilities = MAPVarPosteriorDistribution.getParameters();


                        double p0 = MAPVarPosteriorProbabilities[0];
                        double p1 = MAPVarPosteriorProbabilities[1];
                        double thisEntropy = -(p0 * Math.log(p0) + p1 * Math.log(p1));

                        if (varMinEntropy == null || thisEntropy < minEntropy) {
                            varMinEntropy = thisVariable;
                            varMinEntropyProbabilities = MAPVarPosteriorProbabilities;
                            minEntropy = thisEntropy;
                        }

//                            System.out.println("Marginal of " + thisNode.getName() + ": " + Arrays.toString(posteriorThisNode.getData()) + " with entropy: " + thisEntropy);

                    }

//                        System.out.println("\nNode with min entropy: " + nodeMinEntropy.getName() + " with :" + minEntropy);
//                        System.out.println("with distribution: " + Arrays.toString(nodeMinEntropyDistribution.getData()));
                    double nodeMinEntropy_p0 = varMinEntropyProbabilities[0];
                    double nodeMinEntropy_p1 = varMinEntropyProbabilities[1];

                    int thisVarValue = (nodeMinEntropy_p0 > nodeMinEntropy_p1) ? 0 : 1;
                    evidencePlusMAPVariables.setValue(varMinEntropy, thisVarValue);

                    String nodeNumberString = varMinEntropy.getName().substring(varMinEntropy.getName().lastIndexOf("_t") + 2);
                    int nodeNumber = Integer.parseInt(nodeNumberString);
                    sequence_VMP_IterativeAssignment[nodeNumber] = thisVarValue;
//                        System.out.println("Assigned value " + thisNodeValue + " to node " + nodeNumber);

                    MAPvariableToAssign.remove(varMinEntropy);

                }
                timeStop = System.nanoTime();
                executionTime = (double) (timeStop - timeStart) / 1000000000.0;
                times_IterativeVMP[experimentNumber] = executionTime;
            }




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
                System.out.println();
                System.out.println("HUGIN MAP Sequence:              " + Arrays.toString(sequence_Hugin));
                System.out.println("HUGIN Iterative Sequence:        " + Arrays.toString(sequence_HuginIterativeAssignment));
            }

            System.out.println("IS Iterative Sequence:           " + Arrays.toString(sequence_IS_IterativeAssignment));
            System.out.println("VMP Iterative Sequence:          " + Arrays.toString(sequence_VMP_IterativeAssignment));

//            System.out.println();

            System.out.println("DynMAP (Ungrouped-IS) Sequence:  " + Arrays.toString(sequence_UngroupedIS));
//            System.out.println();

            System.out.println("DynMAP (2Grouped-IS) Sequence:   " + Arrays.toString(sequence_2GroupedIS));
//            System.out.println("       (2Gr-IS) Seq. Submodel 0: " + Arrays.toString(submodel_sequences_2GroupedIS.get(0)));
//            System.out.println("       (2Gr-IS) Seq. Submodel 1: " + Arrays.toString(submodel_sequences_2GroupedIS.get(1)));
//            System.out.println();

            System.out.println("DynMAP (3Grouped-IS) Sequence:   " + Arrays.toString(sequence_3GroupedIS));
//            System.out.println("       (3Gr-IS) Seq. Submodel 0: " + Arrays.toString(submodel_sequences_3GroupedIS.get(0)));
//            System.out.println("       (3Gr-IS) Seq. Submodel 1: " + Arrays.toString(submodel_sequences_3GroupedIS.get(1)));
//            System.out.println("       (3Gr-IS) Seq. Submodel 2: " + Arrays.toString(submodel_sequences_3GroupedIS.get(2)));
//            System.out.println();

            System.out.println("DynMAP (4Grouped-IS) Sequence:   " + Arrays.toString(sequence_4GroupedIS));
//            System.out.println("       (4Gr-IS) Seq. Submodel 0: " + Arrays.toString(submodel_sequences_4GroupedIS.get(0)));
//            System.out.println("       (4Gr-IS) Seq. Submodel 1: " + Arrays.toString(submodel_sequences_4GroupedIS.get(1)));
//            System.out.println("       (4Gr-IS) Seq. Submodel 2: " + Arrays.toString(submodel_sequences_4GroupedIS.get(2)));
//            System.out.println("       (4Gr-IS) Seq. Submodel 3: " + Arrays.toString(submodel_sequences_4GroupedIS.get(3)));
//            System.out.println();

            if (sequence_UngroupedVMP_computed) {
                System.out.println("DynMAP (Ungrouped-VMP) Sequence: " + Arrays.toString(sequence_UngroupedVMP));
//                System.out.println();
            }
            else {
                System.out.println("DynMAP (Ungrouped-VMP) Sequence:  Not obtained\n");
            }

            if (sequence_2GroupedVMP_computed) {
                System.out.println("DynMAP (2Grouped-VMP) Sequence:  " + Arrays.toString(sequence_2GroupedVMP));
//                System.out.println("       (2Gr-VMP) Seq. Submodel 0:" + Arrays.toString(submodel_sequences_2GroupedVMP.get(0)));
//                System.out.println("       (2Gr-VMP) Seq. Submodel 1:" + Arrays.toString(submodel_sequences_2GroupedVMP.get(1)));
//                System.out.println();
            }
            else {
                System.out.println("DynMAP (2Grouped-VMP) Sequence:   Not obtained\n");
            }

            if (sequence_3GroupedVMP_computed) {
                System.out.println("DynMAP (3Grouped-VMP) Sequence:  " + Arrays.toString(sequence_3GroupedVMP));
//                System.out.println("       (3Gr-VMP) Seq. Submodel 0:" + Arrays.toString(submodel_sequences_3GroupedVMP.get(0)));
//                System.out.println("       (3Gr-VMP) Seq. Submodel 1:" + Arrays.toString(submodel_sequences_3GroupedVMP.get(1)));
//                System.out.println("       (3Gr-VMP) Seq. Submodel 2:" + Arrays.toString(submodel_sequences_3GroupedVMP.get(2)));
//                System.out.println();
            }
            else {
                System.out.println("DynMAP (3Grouped-VMP) Sequence:   Not obtained\n");
            }
            if (sequence_4GroupedVMP_computed) {
                System.out.println("DynMAP (4Grouped-VMP) Sequence:  " + Arrays.toString(sequence_4GroupedVMP));
//                System.out.println("       (4Gr-VMP) Seq. Submodel 0:" + Arrays.toString(submodel_sequences_4GroupedVMP.get(0)));
//                System.out.println("       (4Gr-VMP) Seq. Submodel 1:" + Arrays.toString(submodel_sequences_4GroupedVMP.get(1)));
//                System.out.println("       (4Gr-VMP) Seq. Submodel 2:" + Arrays.toString(submodel_sequences_4GroupedVMP.get(2)));
//                System.out.println("       (4Gr-VMP) Seq. Submodel 3:" + Arrays.toString(submodel_sequences_4GroupedVMP.get(3)));
            }
            else {
                System.out.println("DynMAP (4Grouped-VMP) Sequence:   Not obtained\n");
            }
            System.out.println();
            

//            double current_precision_Hugin=0;
//            if (nTimeSteps<=maxTimeStepsHugin) {
//                current_precision_Hugin =
////                    System.out.println("Precision HUGIN: " + current_precision_Hugin);
////                sequence_original = sequence_Hugin;
//            }





            precision_Hugin_original[experimentNumber]=compareIntArrays(sequence_original, sequence_Hugin);;
            precision_HuginIterativeAssignment_original[experimentNumber] = compareIntArrays(sequence_original,sequence_HuginIterativeAssignment);
            precision_IS_IterativeAssignment_original[experimentNumber] = compareIntArrays(sequence_original,sequence_IS_IterativeAssignment);
            precision_VMP_IterativeAssignment_original[experimentNumber] = compareIntArrays(sequence_original,sequence_VMP_IterativeAssignment);

            precision_UngroupedIS_original[experimentNumber]=compareIntArrays(sequence_original,sequence_UngroupedIS);
            precision_2GroupedIS_original[experimentNumber]=compareIntArrays(sequence_original,sequence_2GroupedIS);
            precision_3GroupedIS_original[experimentNumber]=compareIntArrays(sequence_original,sequence_3GroupedIS);
            precision_4GroupedIS_original[experimentNumber]=compareIntArrays(sequence_original,sequence_4GroupedIS);

            precision_UngroupedVMP_original[experimentNumber]=compareIntArrays(sequence_original,sequence_UngroupedVMP);
            precision_2GroupedVMP_original[experimentNumber]=compareIntArrays(sequence_original,sequence_2GroupedVMP);
            precision_3GroupedVMP_original[experimentNumber]=compareIntArrays(sequence_original,sequence_3GroupedVMP);
            precision_4GroupedVMP_original[experimentNumber]=compareIntArrays(sequence_original,sequence_4GroupedVMP);



            System.out.println("PRECISION USING NORMALIZED HAMMING DISTANCE vs ORIGINAL SEQUENCE:");
            if(nTimeSteps<= maxTimeStepsHugin) {
                System.out.println("          HUGIN: " + precision_Hugin_original[experimentNumber]);
                System.out.println("HUGIN iterative: " + precision_HuginIterativeAssignment_original[experimentNumber]);
            }

            System.out.println("   IS Iterative: " + precision_IS_IterativeAssignment_original[experimentNumber]);
            System.out.println("  VMP Iterative: " + precision_VMP_IterativeAssignment_original[experimentNumber]);
            System.out.println();

            System.out.println("   IS Ungrouped: " + precision_UngroupedIS_original[experimentNumber]);
            System.out.println("   IS 2-Grouped: " + precision_2GroupedIS_original[experimentNumber]);
            System.out.println("   IS 3-Grouped: " + precision_3GroupedIS_original[experimentNumber]);
            System.out.println("   IS 4-Grouped: " + precision_4GroupedIS_original[experimentNumber]);
            System.out.println();

            System.out.println("  VMP Ungrouped: " + precision_UngroupedVMP_original[experimentNumber]);
            System.out.println("  VMP 2-Grouped: " + precision_2GroupedVMP_original[experimentNumber]);
            System.out.println("  VMP 3-Grouped: " + precision_3GroupedVMP_original[experimentNumber]);
            System.out.println("  VMP 4-Grouped: " + precision_4GroupedVMP_original[experimentNumber]);
            System.out.println();


            

            precision_Hugin_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_Hugin);
            precision_HuginIterativeAssignment_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_HuginIterativeAssignment);
            precision_IS_IterativeAssignment_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_IS_IterativeAssignment);
            precision_VMP_IterativeAssignment_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_VMP_IterativeAssignment);

            precision_UngroupedIS_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_UngroupedIS);
            precision_2GroupedIS_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_2GroupedIS);
            precision_3GroupedIS_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_3GroupedIS);
            precision_4GroupedIS_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_4GroupedIS);

            precision_UngroupedVMP_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_UngroupedVMP);
            precision_2GroupedVMP_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_2GroupedVMP);
            precision_3GroupedVMP_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_3GroupedVMP);
            precision_4GroupedVMP_01_original[experimentNumber] = compareFullIntArrays(sequence_original,sequence_4GroupedVMP);


            
            
            System.out.println("PRECISION USING 0/1 SCORE vs ORIGINAL SEQUENCE:");
            if(nTimeSteps<= maxTimeStepsHugin) {
                System.out.println("          HUGIN: " + precision_Hugin_01_original[experimentNumber]);
                System.out.println("HUGIN iterative: " + precision_HuginIterativeAssignment_01_original[experimentNumber]);
            }

            System.out.println("   IS Iterative: " + precision_IS_IterativeAssignment_01_original[experimentNumber]);
            System.out.println("  VMP Iterative: " + precision_VMP_IterativeAssignment_01_original[experimentNumber]);
            System.out.println();

            System.out.println("   IS Ungrouped: " + precision_UngroupedIS_01_original[experimentNumber]);
            System.out.println("   IS 2-Grouped: " + precision_2GroupedIS_01_original[experimentNumber]);
            System.out.println("   IS 3-Grouped: " + precision_3GroupedIS_01_original[experimentNumber]);
            System.out.println("   IS 4-Grouped: " + precision_4GroupedIS_01_original[experimentNumber]);
            System.out.println();

            System.out.println("  VMP Ungrouped: " + precision_UngroupedVMP_01_original[experimentNumber]);
            System.out.println("  VMP 2-Grouped: " + precision_2GroupedVMP_01_original[experimentNumber]);
            System.out.println("  VMP 3-Grouped: " + precision_3GroupedVMP_01_original[experimentNumber]);
            System.out.println("  VMP 4-Grouped: " + precision_4GroupedVMP_01_original[experimentNumber]);
            System.out.println();




            precision_Hugin_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_Hugin,2);
            precision_HuginIterativeAssignment_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_HuginIterativeAssignment,2);
            precision_IS_IterativeAssignment_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_IS_IterativeAssignment,2);
            precision_VMP_IterativeAssignment_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_VMP_IterativeAssignment,2);

            precision_UngroupedIS_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_UngroupedIS,2);
            precision_2GroupedIS_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_2GroupedIS,2);
            precision_3GroupedIS_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_3GroupedIS,2);
            precision_4GroupedIS_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_4GroupedIS,2);

            precision_UngroupedVMP_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_UngroupedVMP,2);
            precision_2GroupedVMP_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_2GroupedVMP,2);
            precision_3GroupedVMP_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_3GroupedVMP,2);
            precision_4GroupedVMP_sub2_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_4GroupedVMP,2);



            System.out.println("PRECISION COMPARING SUBSEQUENCES OF LENGTH 2 vs ORIGINAL SEQUENCE:");
            if(nTimeSteps<= maxTimeStepsHugin) {
                System.out.println("          HUGIN: " + precision_Hugin_sub2_original[experimentNumber]);
                System.out.println("HUGIN iterative: " + precision_HuginIterativeAssignment_sub2_original[experimentNumber]);
            }

            System.out.println("   IS Iterative: " + precision_IS_IterativeAssignment_sub2_original[experimentNumber]);
            System.out.println("  VMP Iterative: " + precision_VMP_IterativeAssignment_sub2_original[experimentNumber]);
            System.out.println();

            System.out.println("   IS Ungrouped: " + precision_UngroupedIS_sub2_original[experimentNumber]);
            System.out.println("   IS 2-Grouped: " + precision_2GroupedIS_sub2_original[experimentNumber]);
            System.out.println("   IS 3-Grouped: " + precision_3GroupedIS_sub2_original[experimentNumber]);
            System.out.println("   IS 4-Grouped: " + precision_4GroupedIS_sub2_original[experimentNumber]);
            System.out.println();

            System.out.println("  VMP Ungrouped: " + precision_UngroupedVMP_sub2_original[experimentNumber]);
            System.out.println("  VMP 2-Grouped: " + precision_2GroupedVMP_sub2_original[experimentNumber]);
            System.out.println("  VMP 3-Grouped: " + precision_3GroupedVMP_sub2_original[experimentNumber]);
            System.out.println("  VMP 4-Grouped: " + precision_4GroupedVMP_sub2_original[experimentNumber]);
            System.out.println();



            precision_Hugin_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_Hugin,3);
            precision_HuginIterativeAssignment_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_HuginIterativeAssignment,3);
            precision_IS_IterativeAssignment_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_IS_IterativeAssignment,3);
            precision_VMP_IterativeAssignment_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_VMP_IterativeAssignment,3);

            precision_UngroupedIS_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_UngroupedIS,3);
            precision_2GroupedIS_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_2GroupedIS,3);
            precision_3GroupedIS_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_3GroupedIS,3);
            precision_4GroupedIS_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_4GroupedIS,3);

            precision_UngroupedVMP_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_UngroupedVMP,3);
            precision_2GroupedVMP_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_2GroupedVMP,3);
            precision_3GroupedVMP_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_3GroupedVMP,3);
            precision_4GroupedVMP_sub3_original[experimentNumber] = compareSubsequencesIntArrays(sequence_original,sequence_4GroupedVMP,3);


            System.out.println("PRECISION COMPARING SUBSEQUENCES OF LENGTH 3 vs ORIGINAL SEQUENCE:");
            if(nTimeSteps<= maxTimeStepsHugin) {
                System.out.println("          HUGIN: " + precision_Hugin_sub3_original[experimentNumber]);
                System.out.println("HUGIN iterative: " + precision_HuginIterativeAssignment_sub3_original[experimentNumber]);
            }

            System.out.println("   IS Iterative: " + precision_IS_IterativeAssignment_sub3_original[experimentNumber]);
            System.out.println("  VMP Iterative: " + precision_VMP_IterativeAssignment_sub3_original[experimentNumber]);
            System.out.println();

            System.out.println("   IS Ungrouped: " + precision_UngroupedIS_sub3_original[experimentNumber]);
            System.out.println("   IS 2-Grouped: " + precision_2GroupedIS_sub3_original[experimentNumber]);
            System.out.println("   IS 3-Grouped: " + precision_3GroupedIS_sub3_original[experimentNumber]);
            System.out.println("   IS 4-Grouped: " + precision_4GroupedIS_sub3_original[experimentNumber]);
            System.out.println();

            System.out.println("  VMP Ungrouped: " + precision_UngroupedVMP_sub3_original[experimentNumber]);
            System.out.println("  VMP 2-Grouped: " + precision_2GroupedVMP_sub3_original[experimentNumber]);
            System.out.println("  VMP 3-Grouped: " + precision_3GroupedVMP_sub3_original[experimentNumber]);
            System.out.println("  VMP 4-Grouped: " + precision_4GroupedVMP_sub3_original[experimentNumber]);
            System.out.println();
            
            
//            precision_allZeros[experimentNumber]=current_precision_allZeros;
//            precision_allOnes[experimentNumber]=current_precision_allOnes;




            List<Variable> MAPvariables = staticModel.getVariables().getListOfVariables().stream().filter(variable -> variable.getName().contains(MAPVariable.getName())).collect(Collectors.toList());



            double estimatedProb_sequence_original = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_original);

            double estimatedProb_sequence_HUGIN = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_Hugin);

            double estimatedProb_sequence_HUGIN_IterativeAssignment = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_HuginIterativeAssignment);
            double estimatedProb_sequence_IS_IterativeAssignment = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_IS_IterativeAssignment);
            double estimatedProb_sequence_VMP_IterativeAssignment = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_VMP_IterativeAssignment);


            double estimatedProb_sequence_UngroupedIS = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_UngroupedIS);
            double estimatedProb_sequence_2GroupedIS = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_2GroupedIS);
            double estimatedProb_sequence_3GroupedIS = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_3GroupedIS);
            double estimatedProb_sequence_4GroupedIS = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_4GroupedIS);

            double estimatedProb_sequence_UngroupedVMP = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_UngroupedVMP);
            double estimatedProb_sequence_2GroupedVMP = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_2GroupedVMP);
            double estimatedProb_sequence_3GroupedVMP = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_3GroupedVMP);
            double estimatedProb_sequence_4GroupedVMP = estimateProbabilityOfSequence(staticModel,staticEvidence,MAPvariables,sequence_4GroupedVMP);

            
            System.out.println("ESTIMATED PROBABILITIES OF THE SEQUENCES (WITH IS)");
            System.out.println("      Original: " + estimatedProb_sequence_original);

            if(nTimeSteps<= maxTimeStepsHugin) {
                System.out.println("         HUGIN: " + estimatedProb_sequence_HUGIN);
                System.out.println("HUGIN iterative: " + estimatedProb_sequence_HUGIN_IterativeAssignment);
            }

            System.out.println("  IS Iterative: " + estimatedProb_sequence_IS_IterativeAssignment);
            System.out.println(" VMP Iterative: " + estimatedProb_sequence_VMP_IterativeAssignment);
            System.out.println();

            System.out.println("  IS Ungrouped: " + estimatedProb_sequence_UngroupedIS);
            System.out.println("  IS 2-Grouped: " + estimatedProb_sequence_2GroupedIS);
            System.out.println("  IS 3-Grouped: " + estimatedProb_sequence_3GroupedIS);
            System.out.println("  IS 4-Grouped: " + estimatedProb_sequence_4GroupedIS);
            System.out.println();

            System.out.println(" VMP Ungrouped: " + estimatedProb_sequence_UngroupedVMP);
            System.out.println(" VMP 2-Grouped: " + estimatedProb_sequence_2GroupedVMP);
            System.out.println(" VMP 3-Grouped: " + estimatedProb_sequence_3GroupedVMP);
            System.out.println(" VMP 4-Grouped: " + estimatedProb_sequence_4GroupedVMP);
            System.out.println();


            precision_Hugin_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_HUGIN-estimatedProb_sequence_original);
            precision_HuginIterativeAssignment_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_HUGIN_IterativeAssignment-estimatedProb_sequence_original);


            precision_IS_IterativeAssignment_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_IS_IterativeAssignment-estimatedProb_sequence_original);
            precision_UngroupedIS_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_UngroupedIS-estimatedProb_sequence_original);
            precision_2GroupedIS_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_2GroupedIS-estimatedProb_sequence_original);
            precision_3GroupedIS_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_3GroupedIS-estimatedProb_sequence_original);
            precision_4GroupedIS_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_4GroupedIS-estimatedProb_sequence_original);


            precision_VMP_IterativeAssignment_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_VMP_IterativeAssignment-estimatedProb_sequence_original);
            precision_UngroupedVMP_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_UngroupedVMP-estimatedProb_sequence_original);
            precision_2GroupedVMP_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_2GroupedVMP-estimatedProb_sequence_original);
            precision_3GroupedVMP_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_3GroupedVMP-estimatedProb_sequence_original);
            precision_4GroupedVMP_Probability_original[experimentNumber] = Math.exp(estimatedProb_sequence_4GroupedVMP-estimatedProb_sequence_original);




            if(nTimeSteps<=maxTimeStepsHugin) {



                precision_Hugin_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_HUGIN-estimatedProb_sequence_HUGIN);
                precision_HuginIterativeAssignment_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_HUGIN_IterativeAssignment-estimatedProb_sequence_HUGIN);


                precision_IS_IterativeAssignment_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_IS_IterativeAssignment-estimatedProb_sequence_HUGIN);
                precision_UngroupedIS_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_UngroupedIS-estimatedProb_sequence_HUGIN);
                precision_2GroupedIS_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_2GroupedIS-estimatedProb_sequence_HUGIN);
                precision_3GroupedIS_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_3GroupedIS-estimatedProb_sequence_HUGIN);
                precision_4GroupedIS_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_4GroupedIS-estimatedProb_sequence_HUGIN);


                precision_VMP_IterativeAssignment_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_VMP_IterativeAssignment-estimatedProb_sequence_HUGIN);
                precision_UngroupedVMP_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_UngroupedVMP-estimatedProb_sequence_HUGIN);
                precision_2GroupedVMP_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_2GroupedVMP-estimatedProb_sequence_HUGIN);
                precision_3GroupedVMP_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_3GroupedVMP-estimatedProb_sequence_HUGIN);
                precision_4GroupedVMP_Probability[experimentNumber] = Math.exp(estimatedProb_sequence_4GroupedVMP-estimatedProb_sequence_HUGIN);


                
                
                
                


                precision_Hugin[experimentNumber]=compareIntArrays(sequence_Hugin, sequence_Hugin);;
                precision_HuginIterativeAssignment[experimentNumber] = compareIntArrays(sequence_Hugin,sequence_HuginIterativeAssignment);
                precision_IS_IterativeAssignment[experimentNumber] = compareIntArrays(sequence_Hugin,sequence_IS_IterativeAssignment);
                precision_VMP_IterativeAssignment[experimentNumber] = compareIntArrays(sequence_Hugin,sequence_VMP_IterativeAssignment);

                precision_UngroupedIS[experimentNumber]=compareIntArrays(sequence_Hugin,sequence_UngroupedIS);
                precision_2GroupedIS[experimentNumber]=compareIntArrays(sequence_Hugin,sequence_2GroupedIS);
                precision_3GroupedIS[experimentNumber]=compareIntArrays(sequence_Hugin,sequence_3GroupedIS);
                precision_4GroupedIS[experimentNumber]=compareIntArrays(sequence_Hugin,sequence_4GroupedIS);

                precision_UngroupedVMP[experimentNumber]=compareIntArrays(sequence_Hugin,sequence_UngroupedVMP);
                precision_2GroupedVMP[experimentNumber]=compareIntArrays(sequence_Hugin,sequence_2GroupedVMP);
                precision_3GroupedVMP[experimentNumber]=compareIntArrays(sequence_Hugin,sequence_3GroupedVMP);
                precision_4GroupedVMP[experimentNumber]=compareIntArrays(sequence_Hugin,sequence_4GroupedVMP);



                System.out.println("PRECISION USING NORMALIZED HAMMING DISTANCE vs HUGIN MAP SEQUENCE:");
                if(nTimeSteps<= maxTimeStepsHugin) {
                    System.out.println("          HUGIN: " + precision_Hugin[experimentNumber]);
                    System.out.println("HUGIN iterative: " + precision_HuginIterativeAssignment[experimentNumber]);
                }

                System.out.println("   IS Iterative: " + precision_IS_IterativeAssignment[experimentNumber]);
                System.out.println("  VMP Iterative: " + precision_VMP_IterativeAssignment[experimentNumber]);
                System.out.println();

                System.out.println("   IS Ungrouped: " + precision_UngroupedIS[experimentNumber]);
                System.out.println("   IS 2-Grouped: " + precision_2GroupedIS[experimentNumber]);
                System.out.println("   IS 3-Grouped: " + precision_3GroupedIS[experimentNumber]);
                System.out.println("   IS 4-Grouped: " + precision_4GroupedIS[experimentNumber]);
                System.out.println();

                System.out.println("  VMP Ungrouped: " + precision_UngroupedVMP[experimentNumber]);
                System.out.println("  VMP 2-Grouped: " + precision_2GroupedVMP[experimentNumber]);
                System.out.println("  VMP 3-Grouped: " + precision_3GroupedVMP[experimentNumber]);
                System.out.println("  VMP 4-Grouped: " + precision_4GroupedVMP[experimentNumber]);
                System.out.println();




                precision_Hugin_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_Hugin);
                precision_HuginIterativeAssignment_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_HuginIterativeAssignment);
                precision_IS_IterativeAssignment_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_IS_IterativeAssignment);
                precision_VMP_IterativeAssignment_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_VMP_IterativeAssignment);

                precision_UngroupedIS_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_UngroupedIS);
                precision_2GroupedIS_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_2GroupedIS);
                precision_3GroupedIS_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_3GroupedIS);
                precision_4GroupedIS_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_4GroupedIS);

                precision_UngroupedVMP_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_UngroupedVMP);
                precision_2GroupedVMP_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_2GroupedVMP);
                precision_3GroupedVMP_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_3GroupedVMP);
                precision_4GroupedVMP_01[experimentNumber] = compareFullIntArrays(sequence_Hugin,sequence_4GroupedVMP);




                System.out.println("PRECISION USING 0/1 SCORE vs HUGIN MAP SEQUENCE:");
                if(nTimeSteps<= maxTimeStepsHugin) {
                    System.out.println("          HUGIN: " + precision_Hugin_01[experimentNumber]);
                    System.out.println("HUGIN iterative: " + precision_HuginIterativeAssignment_01[experimentNumber]);
                }

                System.out.println("   IS Iterative: " + precision_IS_IterativeAssignment_01[experimentNumber]);
                System.out.println("  VMP Iterative: " + precision_VMP_IterativeAssignment_01[experimentNumber]);
                System.out.println();

                System.out.println("   IS Ungrouped: " + precision_UngroupedIS_01[experimentNumber]);
                System.out.println("   IS 2-Grouped: " + precision_2GroupedIS_01[experimentNumber]);
                System.out.println("   IS 3-Grouped: " + precision_3GroupedIS_01[experimentNumber]);
                System.out.println("   IS 4-Grouped: " + precision_4GroupedIS_01[experimentNumber]);
                System.out.println();

                System.out.println("  VMP Ungrouped: " + precision_UngroupedVMP_01[experimentNumber]);
                System.out.println("  VMP 2-Grouped: " + precision_2GroupedVMP_01[experimentNumber]);
                System.out.println("  VMP 3-Grouped: " + precision_3GroupedVMP_01[experimentNumber]);
                System.out.println("  VMP 4-Grouped: " + precision_4GroupedVMP_01[experimentNumber]);
                System.out.println();




                precision_Hugin_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_Hugin,2);
                precision_HuginIterativeAssignment_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_HuginIterativeAssignment,2);
                precision_IS_IterativeAssignment_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_IS_IterativeAssignment,2);
                precision_VMP_IterativeAssignment_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_VMP_IterativeAssignment,2);

                precision_UngroupedIS_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_UngroupedIS,2);
                precision_2GroupedIS_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_2GroupedIS,2);
                precision_3GroupedIS_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_3GroupedIS,2);
                precision_4GroupedIS_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_4GroupedIS,2);

                precision_UngroupedVMP_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_UngroupedVMP,2);
                precision_2GroupedVMP_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_2GroupedVMP,2);
                precision_3GroupedVMP_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_3GroupedVMP,2);
                precision_4GroupedVMP_sub2[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_4GroupedVMP,2);



                System.out.println("PRECISION COMPARING SUBSEQUENCES OF LENGTH 2 vs HUGIN MAP SEQUENCE:");
                if(nTimeSteps<= maxTimeStepsHugin) {
                    System.out.println("          HUGIN: " + precision_Hugin_sub2[experimentNumber]);
                    System.out.println("HUGIN iterative: " + precision_HuginIterativeAssignment_sub2[experimentNumber]);
                }

                System.out.println("   IS Iterative: " + precision_IS_IterativeAssignment_sub2[experimentNumber]);
                System.out.println("  VMP Iterative: " + precision_VMP_IterativeAssignment_sub2[experimentNumber]);
                System.out.println();

                System.out.println("   IS Ungrouped: " + precision_UngroupedIS_sub2[experimentNumber]);
                System.out.println("   IS 2-Grouped: " + precision_2GroupedIS_sub2[experimentNumber]);
                System.out.println("   IS 3-Grouped: " + precision_3GroupedIS_sub2[experimentNumber]);
                System.out.println("   IS 4-Grouped: " + precision_4GroupedIS_sub2[experimentNumber]);
                System.out.println();

                System.out.println("  VMP Ungrouped: " + precision_UngroupedVMP_sub2[experimentNumber]);
                System.out.println("  VMP 2-Grouped: " + precision_2GroupedVMP_sub2[experimentNumber]);
                System.out.println("  VMP 3-Grouped: " + precision_3GroupedVMP_sub2[experimentNumber]);
                System.out.println("  VMP 4-Grouped: " + precision_4GroupedVMP_sub2[experimentNumber]);
                System.out.println();



                precision_Hugin_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_Hugin,3);
                precision_HuginIterativeAssignment_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_HuginIterativeAssignment,3);
                precision_IS_IterativeAssignment_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_IS_IterativeAssignment,3);
                precision_VMP_IterativeAssignment_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_VMP_IterativeAssignment,3);

                precision_UngroupedIS_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_UngroupedIS,3);
                precision_2GroupedIS_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_2GroupedIS,3);
                precision_3GroupedIS_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_3GroupedIS,3);
                precision_4GroupedIS_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_4GroupedIS,3);

                precision_UngroupedVMP_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_UngroupedVMP,3);
                precision_2GroupedVMP_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_2GroupedVMP,3);
                precision_3GroupedVMP_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_3GroupedVMP,3);
                precision_4GroupedVMP_sub3[experimentNumber] = compareSubsequencesIntArrays(sequence_Hugin,sequence_4GroupedVMP,3);


                System.out.println("PRECISION COMPARING SUBSEQUENCES OF LENGTH 3 vs HUGIN MAP SEQUENCE:");
                if(nTimeSteps<= maxTimeStepsHugin) {
                    System.out.println("          HUGIN: " + precision_Hugin_sub3[experimentNumber]);
                    System.out.println("HUGIN iterative: " + precision_HuginIterativeAssignment_sub3[experimentNumber]);
                }

                System.out.println("   IS Iterative: " + precision_IS_IterativeAssignment_sub3[experimentNumber]);
                System.out.println("  VMP Iterative: " + precision_VMP_IterativeAssignment_sub3[experimentNumber]);
                System.out.println();

                System.out.println("   IS Ungrouped: " + precision_UngroupedIS_sub3[experimentNumber]);
                System.out.println("   IS 2-Grouped: " + precision_2GroupedIS_sub3[experimentNumber]);
                System.out.println("   IS 3-Grouped: " + precision_3GroupedIS_sub3[experimentNumber]);
                System.out.println("   IS 4-Grouped: " + precision_4GroupedIS_sub3[experimentNumber]);
                System.out.println();

                System.out.println("  VMP Ungrouped: " + precision_UngroupedVMP_sub3[experimentNumber]);
                System.out.println("  VMP 2-Grouped: " + precision_2GroupedVMP_sub3[experimentNumber]);
                System.out.println("  VMP 3-Grouped: " + precision_3GroupedVMP_sub3[experimentNumber]);
                System.out.println("  VMP 4-Grouped: " + precision_4GroupedVMP_sub3[experimentNumber]);
                System.out.println();



            }







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





        System.out.println("\nPRECISIONS - HAMMING DISTANCE vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_original));
        }
        System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_original));
        System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_original));


        System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_original));
        System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_original));
        System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_original));
        System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_original));

        System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_original));
        System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_original));
        System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_original));
        System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_original));


        System.out.println("\nGLOBAL MEAN PRECISIONS - HAMMING DISTANCE vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + stream(precision_Hugin_original).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_original).average().getAsDouble());
        }
        System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_original).average().getAsDouble());
        System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_original).average().getAsDouble());


        System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_original).average().getAsDouble());
        System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_original).average().getAsDouble());
        System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_original).average().getAsDouble());
        System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_original).average().getAsDouble());

        System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_original).average().getAsDouble());
        System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_original).average().getAsDouble());
        System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_original).average().getAsDouble());
        System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_original).average().getAsDouble());




        System.out.println("\nPRECISIONS -  0/1 DISTANCE DISTANCE vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin_01_original));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_01_original));
        }
        System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_01_original));
        System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_01_original));


        System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_01_original));
        System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_01_original));
        System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_01_original));
        System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_01_original));

        System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_01_original));
        System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_01_original));
        System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_01_original));
        System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_01_original));


        System.out.println("\nGLOBAL MEAN PRECISIONS - 0/1 DISTANCE vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + stream(precision_Hugin_01_original).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_01_original).average().getAsDouble());
        }
        System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_01_original).average().getAsDouble());
        System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_01_original).average().getAsDouble());


        System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_01_original).average().getAsDouble());
        System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_01_original).average().getAsDouble());
        System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_01_original).average().getAsDouble());
        System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_01_original).average().getAsDouble());

        System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_01_original).average().getAsDouble());
        System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_01_original).average().getAsDouble());
        System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_01_original).average().getAsDouble());
        System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_01_original).average().getAsDouble());



        System.out.println("\nPRECISIONS -  2-LENGTH SUBSEQ vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin_sub2_original));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_sub2_original));
        }
        System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_sub2_original));
        System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_sub2_original));


        System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_sub2_original));
        System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_sub2_original));
        System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_sub2_original));
        System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_sub2_original));

        System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_sub2_original));
        System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_sub2_original));
        System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_sub2_original));
        System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_sub2_original));


        System.out.println("\nGLOBAL MEAN PRECISIONS - 2-LENGTH SUBSEQ vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + stream(precision_Hugin_sub2_original).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_sub2_original).average().getAsDouble());
        }
        System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_sub2_original).average().getAsDouble());
        System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_sub2_original).average().getAsDouble());


        System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_sub2_original).average().getAsDouble());
        System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_sub2_original).average().getAsDouble());
        System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_sub2_original).average().getAsDouble());
        System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_sub2_original).average().getAsDouble());

        System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_sub2_original).average().getAsDouble());
        System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_sub2_original).average().getAsDouble());
        System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_sub2_original).average().getAsDouble());
        System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_sub2_original).average().getAsDouble());




        System.out.println("\nPRECISIONS -  3-LENGTH SUBSEQ vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin_sub3_original));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_sub3_original));
        }
        System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_sub3_original));
        System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_sub3_original));


        System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_sub3_original));
        System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_sub3_original));
        System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_sub3_original));
        System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_sub3_original));

        System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_sub3_original));
        System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_sub3_original));
        System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_sub3_original));
        System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_sub3_original));


        System.out.println("\nGLOBAL MEAN PRECISIONS - 3-LENGTH SUBSEQ vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + stream(precision_Hugin_sub3_original).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_sub3_original).average().getAsDouble());
        }
        System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_sub3_original).average().getAsDouble());
        System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_sub3_original).average().getAsDouble());


        System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_sub3_original).average().getAsDouble());
        System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_sub3_original).average().getAsDouble());
        System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_sub3_original).average().getAsDouble());
        System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_sub3_original).average().getAsDouble());

        System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_sub3_original).average().getAsDouble());
        System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_sub3_original).average().getAsDouble());
        System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_sub3_original).average().getAsDouble());
        System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_sub3_original).average().getAsDouble());





        System.out.println("\nPRECISIONS -  LIKELIHOOD RATIO vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin_Probability_original));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_Probability_original));
        }
        System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_Probability_original));
        System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_Probability_original));


        System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_Probability_original));
        System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_Probability_original));
        System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_Probability_original));
        System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_Probability_original));

        System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_Probability_original));
        System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_Probability_original));
        System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_Probability_original));
        System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_Probability_original));


        System.out.println("\nGLOBAL MEAN PRECISIONS - LIKELIHOOD RATIO vs ORIGINAL SEQUENCE:");
        if(nTimeSteps<=maxTimeStepsHugin) {
            System.out.println("         HUGIN: " + stream(precision_Hugin_Probability_original).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_Probability_original).average().getAsDouble());
        }
        System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_Probability_original).average().getAsDouble());
        System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_Probability_original).average().getAsDouble());


        System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_Probability_original).average().getAsDouble());
        System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_Probability_original).average().getAsDouble());
        System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_Probability_original).average().getAsDouble());
        System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_Probability_original).average().getAsDouble());

        System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_Probability_original).average().getAsDouble());
        System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_Probability_original).average().getAsDouble());
        System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_Probability_original).average().getAsDouble());
        System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_Probability_original).average().getAsDouble());
        
        
        

        if (nTimeSteps<=maxTimeStepsHugin) {

            System.out.println("\nPRECISIONS - HAMMING DISTANCE vs HUGIN MAP SEQUENCE:");
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment));
            System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment));
            System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment));


            System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS));
            System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS));
            System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS));
            System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS));

            System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP));
            System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP));
            System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP));
            System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP));


            System.out.println("\nGLOBAL MEAN PRECISIONS - HAMMING DISTANCE vs HUGIN MAP SEQUENCE:");

            System.out.println("         HUGIN: " + stream(precision_Hugin).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment).average().getAsDouble());
            System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment).average().getAsDouble());
            System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment).average().getAsDouble());


            System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS).average().getAsDouble());
            System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS).average().getAsDouble());
            System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS).average().getAsDouble());
            System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS).average().getAsDouble());

            System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP).average().getAsDouble());
            System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP).average().getAsDouble());
            System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP).average().getAsDouble());
            System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP).average().getAsDouble());




            System.out.println("\nPRECISIONS -  0/1 DISTANCE DISTANCE vs HUGIN MAP SEQUENCE:");
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin_01));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_01));
            System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_01));
            System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_01));


            System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_01));
            System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_01));
            System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_01));
            System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_01));

            System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_01));
            System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_01));
            System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_01));
            System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_01));


            System.out.println("\nGLOBAL MEAN PRECISIONS - 0/1 DISTANCE vs HUGIN MAP SEQUENCE:");

            System.out.println("         HUGIN: " + stream(precision_Hugin_01).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_01).average().getAsDouble());
            System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_01).average().getAsDouble());
            System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_01).average().getAsDouble());


            System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_01).average().getAsDouble());
            System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_01).average().getAsDouble());
            System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_01).average().getAsDouble());
            System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_01).average().getAsDouble());

            System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_01).average().getAsDouble());
            System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_01).average().getAsDouble());
            System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_01).average().getAsDouble());
            System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_01).average().getAsDouble());



            System.out.println("\nPRECISIONS -  2-LENGTH SUBSEQ vs HUGIN MAP SEQUENCE:");
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin_sub2));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_sub2));
            System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_sub2));
            System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_sub2));


            System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_sub2));
            System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_sub2));
            System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_sub2));
            System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_sub2));

            System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_sub2));
            System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_sub2));
            System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_sub2));
            System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_sub2));


            System.out.println("\nGLOBAL MEAN PRECISIONS - 2-LENGTH SUBSEQ vs HUGIN MAP SEQUENCE:");

            System.out.println("         HUGIN: " + stream(precision_Hugin_sub2).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_sub2).average().getAsDouble());
            System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_sub2).average().getAsDouble());
            System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_sub2).average().getAsDouble());


            System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_sub2).average().getAsDouble());
            System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_sub2).average().getAsDouble());
            System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_sub2).average().getAsDouble());
            System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_sub2).average().getAsDouble());

            System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_sub2).average().getAsDouble());
            System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_sub2).average().getAsDouble());
            System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_sub2).average().getAsDouble());
            System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_sub2).average().getAsDouble());




            System.out.println("\nPRECISIONS -  3-LENGTH SUBSEQ vs HUGIN MAP SEQUENCE:");
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin_sub3));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_sub3));
            System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_sub3));
            System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_sub3));


            System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_sub3));
            System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_sub3));
            System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_sub3));
            System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_sub3));

            System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_sub3));
            System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_sub3));
            System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_sub3));
            System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_sub3));


            System.out.println("\nGLOBAL MEAN PRECISIONS - 3-LENGTH SUBSEQ vs HUGIN MAP SEQUENCE:");

            System.out.println("         HUGIN: " + stream(precision_Hugin_sub3).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_sub3).average().getAsDouble());
            System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_sub3).average().getAsDouble());
            System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_sub3).average().getAsDouble());


            System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_sub3).average().getAsDouble());
            System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_sub3).average().getAsDouble());
            System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_sub3).average().getAsDouble());
            System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_sub3).average().getAsDouble());

            System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_sub3).average().getAsDouble());
            System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_sub3).average().getAsDouble());
            System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_sub3).average().getAsDouble());
            System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_sub3).average().getAsDouble());





            System.out.println("\nPRECISIONS -  LIKELIHOOD RATIO vs HUGIN MAP SEQUENCE:");
            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin_Probability));
            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_Probability));
            System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_Probability));
            System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_Probability));


            System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_Probability));
            System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_Probability));
            System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_Probability));
            System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_Probability));

            System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_Probability));
            System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_Probability));
            System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_Probability));
            System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_Probability));


            System.out.println("\nGLOBAL MEAN PRECISIONS - LIKELIHOOD RATIO vs HUGIN MAP SEQUENCE:");

            System.out.println("         HUGIN: " + stream(precision_Hugin_Probability).average().getAsDouble());
            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_Probability).average().getAsDouble());
            System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_Probability).average().getAsDouble());
            System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_Probability).average().getAsDouble());


            System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_Probability).average().getAsDouble());
            System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_Probability).average().getAsDouble());
            System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_Probability).average().getAsDouble());
            System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_Probability).average().getAsDouble());

            System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_Probability).average().getAsDouble());
            System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_Probability).average().getAsDouble());
            System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_Probability).average().getAsDouble());
            System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_Probability).average().getAsDouble());


        }






//
//        if (nTimeSteps<=maxTimeStepsHugin) {
//            System.out.println("\nPRECISIONS COMPARING THE Probability: ");
//            System.out.println("         HUGIN: " + Arrays.toString(precision_Hugin_Probability));
//            System.out.println("HUGIN iterative:" + Arrays.toString(precision_HuginIterativeAssignment_Probability));
//
//        }
//        else {
//            System.out.println("\nGLOBAL MEAN PRECISIONS COMPARING THE Probability: ");
//            System.out.println("         HUGIN:    Not computed");
//            System.out.println("HUGIN iterative:   Not computed");
//        }
//
//        System.out.println("  IS iterative: " + Arrays.toString(precision_IS_IterativeAssignment_Probability));
//        System.out.println(" VMP iterative: " + Arrays.toString(precision_VMP_IterativeAssignment_Probability));
//
//
//        System.out.println("  IS Ungrouped: " + Arrays.toString(precision_UngroupedIS_Probability));
//        System.out.println("  IS 2-Grouped: " + Arrays.toString(precision_2GroupedIS_Probability));
//        System.out.println("  IS 3-Grouped: " + Arrays.toString(precision_3GroupedIS_Probability));
//        System.out.println("  IS 4-Grouped: " + Arrays.toString(precision_4GroupedIS_Probability));
//
//        System.out.println(" VMP Ungrouped: " + Arrays.toString(precision_UngroupedVMP_Probability));
//        System.out.println(" VMP 2-Grouped: " + Arrays.toString(precision_2GroupedVMP_Probability));
//        System.out.println(" VMP 3-Grouped: " + Arrays.toString(precision_3GroupedVMP_Probability));
//        System.out.println(" VMP 4-Grouped: " + Arrays.toString(precision_4GroupedVMP_Probability));
//
//
//
//
//
//
//
//
//        if (nTimeSteps<=maxTimeStepsHugin) {
//            System.out.println("\nGLOBAL MEAN PRECISIONS COMPARING THE Probability: ");
//            System.out.println("         HUGIN: " + stream(precision_Hugin_Probability).average().getAsDouble() + " (this one compared to the original sequence)");
//            System.out.println("HUGIN iterative:" + stream(precision_HuginIterativeAssignment_Probability).average().getAsDouble());
//
//        }
//        else {
//            System.out.println("\nGLOBAL MEAN PRECISIONS COMPARING THE Probability: ");
//            System.out.println("         HUGIN:    Not computed");
//            System.out.println("HUGIN iterative:   Not computed");
//        }
//
//        System.out.println("  IS iterative: " + stream(precision_IS_IterativeAssignment_Probability).average().getAsDouble());
//        System.out.println(" VMP iterative: " + stream(precision_VMP_IterativeAssignment_Probability).average().getAsDouble());
//
//
//        System.out.println("  IS Ungrouped: " + stream(precision_UngroupedIS_Probability).average().getAsDouble());
//        System.out.println("  IS 2-Grouped: " + stream(precision_2GroupedIS_Probability).average().getAsDouble());
//        System.out.println("  IS 3-Grouped: " + stream(precision_3GroupedIS_Probability).average().getAsDouble());
//        System.out.println("  IS 4-Grouped: " + stream(precision_4GroupedIS_Probability).average().getAsDouble());
//
//        System.out.println(" VMP Ungrouped: " + stream(precision_UngroupedVMP_Probability).average().getAsDouble());
//        System.out.println(" VMP 2-Grouped: " + stream(precision_2GroupedVMP_Probability).average().getAsDouble());
//        System.out.println(" VMP 3-Grouped: " + stream(precision_3GroupedVMP_Probability).average().getAsDouble());
//        System.out.println(" VMP 4-Grouped: " + stream(precision_4GroupedVMP_Probability).average().getAsDouble());
//
//
//















        System.out.println("\n\n EXECUTION TIMES: ");
        System.out.println(Arrays.toString(times_Hugin).replace("[","times_hugin=c(").replace("]",")"));

        System.out.println(Arrays.toString(times_IterativeIS).replace("[","times_iterativeIS=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_IterativeVMP).replace("[","times_iterativeVMP=c(").replace("]",")"));


        System.out.println(Arrays.toString(times_UngroupedIS).replace("[","times_ungroupedIS=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_2GroupedIS).replace("[","times_2groupedIS=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_3GroupedIS).replace("[","times_3groupedIS=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_4GroupedIS).replace("[","times_4groupedIS=c(").replace("]",")"));

        System.out.println(Arrays.toString(times_UngroupedVMP).replace("[","times_ungroupedVMP=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_2GroupedVMP).replace("[","times_2groupedVMP=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_3GroupedVMP).replace("[","times_3groupedVMP=c(").replace("]",")"));
        System.out.println(Arrays.toString(times_4GroupedVMP).replace("[","times_4groupedVMP=c(").replace("]",")"));


        System.out.println("\n\n MEAN EXECUTION TIMES: ");
        System.out.println("         HUGIN: " + stream(times_Hugin).average().getAsDouble());

        System.out.println("  IS Iterative: " + stream(times_IterativeIS).average().getAsDouble());
        System.out.println(" VMP Iterative: " + stream(times_IterativeVMP).average().getAsDouble());

        System.out.println("  IS Ungrouped: " + stream(times_UngroupedIS).average().getAsDouble());
        System.out.println("  IS 2-Grouped: " + stream(times_2GroupedIS).average().getAsDouble());
        System.out.println("  IS 3-Grouped: " + stream(times_3GroupedIS).average().getAsDouble());
        System.out.println("  IS 4-Grouped: " + stream(times_4GroupedIS).average().getAsDouble());

        System.out.println(" VMP Ungrouped: " + stream(times_UngroupedVMP).average().getAsDouble());
        System.out.println(" VMP 2-Grouped: " + stream(times_2GroupedVMP).average().getAsDouble());
        System.out.println(" VMP 3-Grouped: " + stream(times_3GroupedVMP).average().getAsDouble());
        System.out.println(" VMP 4-Grouped: " + stream(times_4GroupedVMP).average().getAsDouble());


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

    private static double compareFullIntArrays(int[] array1, int[] array2) {
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

        return ((double)atomicInteger.get())==((double)array1.length) ? 1 : 0;
    }


    private static double compareSubsequencesIntArrays(int[] array1, int[] array2, int subsequenceLength) {
        if (array1.length!=array2.length) {
            System.out.println("Both arrays must be the same length");
            System.exit(-50);
        }

        if (subsequenceLength<1 || subsequenceLength>array1.length) {
            System.out.println("Incorrect subsequence length");
            System.exit(-40);
        }

        AtomicInteger atomicInteger = new AtomicInteger();
        IntStream.range(0,array1.length+1-subsequenceLength).forEachOrdered(i -> {

            int [] subsequence1 = Arrays.copyOfRange(array1,i,i+subsequenceLength);
            int [] subsequence2 = Arrays.copyOfRange(array2,i,i+subsequenceLength);

            if ( compareFullIntArrays(subsequence1,subsequence2)==1 ) {
                atomicInteger.incrementAndGet();
            }
        });

        return ((double)atomicInteger.get())/((double)array1.length+1-subsequenceLength);
    }

    private static double estimateProbabilityOfSequence(BayesianNetwork model, Assignment evidence, List<Variable> MAPvariables, int[] sequence) {

        return 0;

//        HashMapAssignment evidencePlusSequence = new HashMapAssignment(evidence);
//        for (int i = 0; i < MAPvariables.size(); i++) {
//            evidencePlusSequence.setValue(MAPvariables.get(i),sequence[i]);
//        }

//        VMP vmp1 = new VMP();
//        vmp1.setModel(model);
//        vmp1.setEvidence(evidencePlusSequence);
//        vmp1.runInference();
//        vmp1.setThreshold(0.00001);
//        vmp1.setMaxIter(5000);
//        return vmp1.getLogProbabilityOfEvidence();


//        ImportanceSamplingCLG importanceSamplingRobust = new ImportanceSamplingCLG();
//        importanceSamplingRobust.setModel(model);
//        importanceSamplingRobust.setVariablesAPosteriori(MAPvariables);
//        importanceSamplingRobust.setSampleSize(1000000);
//        importanceSamplingRobust.setEvidence(evidencePlusSequence);
//
//        importanceSamplingRobust.runInference();
//
//        return importanceSamplingRobust.getLogProbabilityOfEvidence();


//        System.out.println(estimatedProbability);
    }
}