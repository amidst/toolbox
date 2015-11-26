package eu.amidst.dynamic.inference;

import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.MAPInference;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicToStaticBNConverter;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dario on 11/11/15.
 */
public class DynamicMAPInference_Experiments2 {

    public static List<DynamicAssignment> generateRandomEvidence(DynamicBayesianNetwork dynamicBayesianNetwork, Variable mapVariable, int nTimeSteps, int nVarsEvidence, Random random) {

        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

        if (nVarsEvidence > varsDynamicModel.size()-1) {
            System.out.println("Too many variables to be observe");
            return null;
        }
        List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);
        double varEvidenceValue;

        int currentVarsEvidence=0;
        while (currentVarsEvidence < nVarsEvidence) {
            int indexVarEvidence = random.nextInt(dynamicBayesianNetwork.getNumberOfDynamicVars());
            Variable varEvidence = varsDynamicModel.get(indexVarEvidence);

            if (varEvidence.equals(mapVariable) || varsEvidence.contains(varEvidence)) {
                continue;
            }
            varsEvidence.add(varEvidence);
            currentVarsEvidence++;
        }

        List<DynamicAssignment> evidence = new ArrayList<>(nTimeSteps);

        for (int t = 0; t < nTimeSteps; t++) {
            HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());

            for (int i = 0; i < varsEvidence.size(); i++) {

                dynAssignment.setSequenceID(12302253);
                dynAssignment.setTimeID(t);
                Variable varEvidence = varsEvidence.get(i);

                if (varEvidence.isMultinomial()) {
                    varEvidenceValue = random.nextInt(varEvidence.getNumberOfStates());
                } else {
                    varEvidenceValue = -5 + 10 * random.nextDouble();
                }
                dynAssignment.setValue(varEvidence, varEvidenceValue);
            }
            evidence.add(dynAssignment);
        }
        return evidence;
    }

    public static Assignment dynamicToStaticEvidence(List<DynamicAssignment> dynamicEvidence, Variables staticVariables) {

        Assignment staticEvidence = new HashMapAssignment(staticVariables.getNumberOfVars());

        dynamicEvidence.stream().forEach(dynamicAssignment -> {
            int time = (int) dynamicAssignment.getTimeID();
            Set<Variable> dynAssigVariables = dynamicAssignment.getVariables();
            for (Variable dynVariable : dynAssigVariables) {
                Variable staticVariable = staticVariables.getVariableByName(dynVariable.getName() + "_t" + Integer.toString(time));
                double varValue = dynamicAssignment.getValue(dynVariable);
                staticEvidence.setValue(staticVariable, varValue);
            }
        });
        return staticEvidence;
    }

    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        /*
         * LOADS THE DYNAMIC NETWORK AND PRINTS IT
         */
        int nContVars = 3;          // 5+15,  3+7
        int nDiscreteVars = 7;

        //int numberOfLinks=(int)((nContVars+nDiscreteVars)*1.5);
        int numberOfLinks=(nContVars+nDiscreteVars);

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(nContVars);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(nDiscreteVars);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetworkGenerator.setNumberOfLinks(numberOfLinks);

        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);
        //DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicTAN(new Random(0), 2, true);
        //System.out.println(dynamicBayesianNetwork.getDynamicDAG().toString());
        //System.out.println(dynamicBayesianNetwork.toString());


        /*
         *  INITIALIZE THE DYNAMIC MAP OBJECT
         */

        int nTimeSteps=10;

        Variable mapVariable = dynamicBayesianNetwork.getDynamicVariables().getVariableByName("ClassVar");

        DynamicMAPInference dynMAP = new DynamicMAPInference();
        dynMAP.setModel(dynamicBayesianNetwork);
        dynMAP.setNumberOfTimeSteps(nTimeSteps);
        dynMAP.setMAPvariable(mapVariable);
//        dynMAP.computeDynamicMAPEvenModel();
//        dynMAP.computeDynamicMAPOddModel();



        //dynMAP.getStaticEvenModel().getVariables().forEach(var-> System.out.println(var.getName()));
        //System.out.println(dynMAP.getStaticEvenModel().getDAG().toString());
        //System.out.println(dynMAP.getStaticEvenModel().toString());
        //System.out.println(dynMAP.getStaticOddModel().toString());

        Random random = new Random(36523);


        System.out.println("Dynamic MAP in a network of " + dynamicBayesianNetwork.getNumberOfDynamicVars() + " variables and " + numberOfLinks + " links");
        System.out.println();
//        System.out.println(dynamicBayesianNetwork.toString());
//        System.out.println();

        DynamicToStaticBNConverter converter = new DynamicToStaticBNConverter();
        converter.setNumberOfTimeSteps(nTimeSteps);
        converter.setDynamicBayesianNetwork(dynamicBayesianNetwork);
        BayesianNetwork staticBN = converter.convertDBNtoBN();

        Variables staticVariables = staticBN.getVariables();
        //System.out.println(staticBN.toString());

        List<Variable> mapVarReplications = staticVariables.getListOfVariables().stream().filter(var -> var.getName().contains(mapVariable.getName())).collect(Collectors.toList());



        MAPInference mapInference = new MAPInference();

        mapInference.setModel(staticBN);
        mapInference.setParallelMode(true);
        mapInference.setSeed(random.nextInt());
        mapInference.setSampleSize(60);
        mapInference.setNumberOfIterations(150);
        mapInference.setMAPVariables(mapVarReplications);


        /*
         * GENERATE AN EVIDENCE FOR T=0,...,nTimeSteps-1
         */
        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

//        System.out.println("DYNAMIC VARIABLES:");
//        varsDynamicModel.forEach(var -> System.out.println("Var ID " + var.getVarID() + ": " + var.getName()));
//        System.out.println();



        int repetitions = 20;
        double[] probDynMap = new double[repetitions];
        double[] probStaticMap = new double[repetitions];

        for (int k = 0; k < repetitions; k++) {

            System.out.println("Repetition" + k);
            dynamicBayesianNetwork.randomInitialization(random);

            dynMAP = new DynamicMAPInference();
            dynMAP.setModel(dynamicBayesianNetwork);
            dynMAP.setMAPvariable(mapVariable);
            dynMAP.setNumberOfTimeSteps(nTimeSteps);


            int nVarsEvidence = 2;

            List<DynamicAssignment> evidence = generateRandomEvidence(dynamicBayesianNetwork, mapVariable, nTimeSteps, nVarsEvidence, random);

            //        System.out.println("EVIDENCE:");
            //        evidence.forEach(evid -> {
            //            System.out.println("Evidence at time " + evid.getTimeID());
            //            evid.getVariables().forEach(variable -> System.out.println(variable.getName() + ": " + Integer.toString((int) evid.getValue(variable))));
            //            System.out.println();
            //        });


            Assignment staticEvidence = dynamicToStaticEvidence(evidence, staticVariables);


            /*
             *  SET THE EVIDENCE AND MAKE INFERENCE
             */
            long timeStart, timeStop;
            double execTime;


            timeStart = System.nanoTime();

            dynMAP.setEvidence(evidence);
            dynMAP.runInference();

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;

            List<Variable> dynMAPstaticMAPVariables = dynMAP.getReplicatedMAPVariables();

            System.out.println("Dynamic MAP. Execution time: " + execTime + ", MAP sequence:");
            System.out.println(dynMAP.getMAPestimate().outputString(dynMAPstaticMAPVariables));
            System.out.println();

            timeStart = System.nanoTime();

            mapInference.setEvidence(staticEvidence);
            mapInference.runInference();

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            System.out.println("Static MAP. Execution time: " + execTime + ", MAP sequence:");
            System.out.println(mapInference.getEstimate().outputString(mapVarReplications));
            System.out.println();


            Assignment MAPsequence1 = dynMAP.getMAPestimate();
            Assignment MAPsequence2 = mapInference.getEstimate();

            Assignment extendedEvidence1 = new HashMapAssignment(staticEvidence);
            Assignment extendedEvidence2 = new HashMapAssignment(staticEvidence);

            MAPsequence1.getVariables().stream().forEach(var -> extendedEvidence1.setValue(staticVariables.getVariableByName(var.getName()), MAPsequence1.getValue(var)));
            MAPsequence2.getVariables().stream().forEach(var -> extendedEvidence2.setValue(staticVariables.getVariableByName(var.getName()), MAPsequence2.getValue(var)));

            //extendedEvidence1.getVariables().stream().filter(var -> !staticBN.getVariables().getListOfVariables().contains(var)).forEach(var -> System.out.println(var.getName()));
            //extendedEvidence2.getVariables().stream().filter(var -> !staticBN.getVariables().getListOfVariables().contains(var)).forEach(var -> System.out.println(var.getName()));


            ImportanceSampling importanceSampling = new ImportanceSampling();

            importanceSampling.setModel(staticBN);
            importanceSampling.setKeepDataOnMemory(false);
            importanceSampling.setParallelMode(true);
            importanceSampling.setSeed(random.nextInt());
            importanceSampling.setSampleSize(1000000);

            System.out.println("Estimating probabilities with Importance Sampling");

            importanceSampling.setEvidence(extendedEvidence1);
            importanceSampling.runInference();

            probDynMap[k] = importanceSampling.getLogProbabilityOfEvidence();
            System.out.println("Prob DynMAP: " + Double.toString(probDynMap[k]));


            importanceSampling.setEvidence(extendedEvidence2);
            importanceSampling.runInference();

            probStaticMap[k] = importanceSampling.getLogProbabilityOfEvidence();
            System.out.println("Prob Static MAP: " + Double.toString(probStaticMap[k]));

        }

        System.out.println(Arrays.toString(probDynMap));
        System.out.println(Arrays.toString(probStaticMap));
//            /*
//             *  SHOW RESULTS
//             */
//                Assignment MAPestimate = dynMAP.getMAPestimate();
//                double MAPestimateProbability = dynMAP.getMAPestimateProbability();
//
//
//                //        System.out.println("MAP sequence over " + mapVariable.getName() + ":");
//                //        List<Variable> MAPvarReplications = MAPestimate.getVariables().stream().sorted((var1,var2) -> (var1.getVarID()>var2.getVarID()? 1 : -1)).collect(Collectors.toList());
//                //
//                //        StringBuilder sequence = new StringBuilder();
//                //        MAPvarReplications.stream().forEachOrdered(var -> sequence.append( Integer.toString((int)MAPestimate.getValue(var)) + ", "));
//                //        //System.out.println(MAPestimate.outputString(MAPvarReplications));
//                //        System.out.println(sequence.toString());
//                //        System.out.println("with probability prop. to: " + MAPestimateProbability);
//
//            }
//
//            double meanTime = Arrays.stream(executionTimes).summaryStatistics().getAverage();
//            System.out.println("With " + nTimeSteps + " replications");
//            System.out.println(Arrays.toString(executionTimes));
//            System.out.println("Mean time: " + meanTime);
//            System.out.println();


    }
}
