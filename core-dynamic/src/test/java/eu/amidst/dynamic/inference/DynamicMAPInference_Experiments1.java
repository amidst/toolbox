package eu.amidst.dynamic.inference;

import eu.amidst.core.inference.MAPInference;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicToStaticBNConverter;
import eu.amidst.dynamic.variables.DynamicAssignment;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by dario on 11/11/15.
 */
public class DynamicMAPInference_Experiments1 {

    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        /*
         * LOADS THE DYNAMIC NETWORK AND PRINTS IT
         */
        int nContVars = 10;
        int nDiscreteVars=40;

        //int numberOfLinks=(int)((nContVars+nDiscreteVars)*1.5);
        int numberOfLinks=(nContVars+nDiscreteVars);

        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(nContVars);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(nDiscreteVars);

        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetworkGenerator.setNumberOfLinks(numberOfLinks);

        //DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, true);
        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetworkGenerator.generateDynamicTAN(new Random(0), 2, true);

        //System.out.println(dynamicBayesianNetwork.getDynamicDAG().toString());
        //System.out.println(dynamicBayesianNetwork.toString());
        /*
         *  INITIALIZE THE DYNAMIC MAP OBJECT
         */

        int nTimeSteps=Integer.parseInt(arguments[0]);

        Variable mapVariable = dynamicBayesianNetwork.getDynamicVariables().getVariableByName("ClassVar");

        eu.amidst.dynamic.inference.DynamicMAPInference dynMAP = new eu.amidst.dynamic.inference.DynamicMAPInference();
        dynMAP.setModel(dynamicBayesianNetwork);
        dynMAP.setMAPvariable(mapVariable);
//        dynMAP.setNumberOfTimeSteps(2);
//        dynMAP.computeDynamicMAPEvenModel();
//        dynMAP.computeDynamicMAPOddModel();



        //dynMAP.getStaticEvenModel().getVariables().forEach(var-> System.out.println(var.getName()));
        //System.out.println(dynMAP.getStaticEvenModel().getDAG().toString());
        //System.out.println(dynMAP.getStaticEvenModel().toString());
        //System.out.println(dynMAP.getStaticOddModel().toString());



        System.out.println("Dynamic MAP in a network of " + dynamicBayesianNetwork.getNumberOfDynamicVars() + " variables and " + numberOfLinks + " links");
        /*
         * GENERATE AN EVIDENCE FOR T=0,...,nTimeSteps-1
         */
        List<Variable> varsDynamicModel = dynamicBayesianNetwork.getDynamicVariables().getListOfDynamicVariables();

//        System.out.println("DYNAMIC VARIABLES:");
//        varsDynamicModel.forEach(var -> System.out.println("Var ID " + var.getVarID() + ": " + var.getName()));
//        System.out.println();

        Random random = new Random();

//        int[] repetitionsTimeSteps = new int[]{20, 40, 60, 80, 100};
//
//        for (int m = 0; m < repetitionsTimeSteps.length; m++) {
//
//            nTimeSteps = repetitionsTimeSteps[m];

            int nRepetitionsExperiments=5;

            double [] executionTimes = new double[nRepetitionsExperiments];
            double [] executionTimesStatic = new double[nRepetitionsExperiments];
            double timeStart, timeStop, execTime;

            for (int n = 0; n < nRepetitionsExperiments; n++) {




                DynamicToStaticBNConverter converter = new DynamicToStaticBNConverter();
                converter.setNumberOfTimeSteps(nTimeSteps);
                converter.setDynamicBayesianNetwork(dynamicBayesianNetwork);
                BayesianNetwork staticBN = converter.convertDBNtoBN();

                Variables staticVariables = staticBN.getVariables();
                //System.out.println(staticBN.toString());

                List<Variable> mapVarReplications = staticVariables.getListOfVariables().stream().filter(var -> var.getName().contains(mapVariable.getName())).collect(Collectors.toList());




                int nVarsEvidence = 10;

                List<DynamicAssignment> evidence = DynamicMAPInference_Experiments2.generateRandomEvidence(dynamicBayesianNetwork, mapVariable, nTimeSteps, nVarsEvidence, random);
                Assignment staticEvidence = DynamicMAPInference_Experiments2.dynamicToStaticEvidence(evidence, staticVariables);


                dynMAP = new eu.amidst.dynamic.inference.DynamicMAPInference();
                dynMAP.setModel(dynamicBayesianNetwork);
                dynMAP.setMAPvariable(mapVariable);
                dynMAP.setNumberOfTimeSteps(nTimeSteps);


//                List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);
//                double varEvidenceValue;
//                List<DynamicAssignment> evidence = new ArrayList<>(nTimeSteps);
//
//                for (int i = 0; i < nVarsEvidence; i++) {
//                    int indexVarEvidence = random.nextInt(dynamicBayesianNetwork.getNumberOfDynamicVars());
//                    Variable varEvidence = varsDynamicModel.get(indexVarEvidence);
//
//                    if (varEvidence.equals(mapVariable)) {
//                        continue;
//                    }
//                    varsEvidence.add(varEvidence);
//                }
//
//                for (int t = 0; t < nTimeSteps; t++) {
//                    HashMapDynamicAssignment dynAssignment = new HashMapDynamicAssignment(varsEvidence.size());
//
//                    for (int i = 0; i < varsEvidence.size(); i++) {
//
//                        dynAssignment.setSequenceID(12302253);
//                        dynAssignment.setTimeID(t);
//                        Variable varEvidence = varsEvidence.get(i);
//
//                        if (varEvidence.isMultinomial()) {
//                            varEvidenceValue = random.nextInt(varEvidence.getNumberOfStates());
//                        } else {
//                            varEvidenceValue = -5 + 10 * random.nextDouble();
//                        }
//                        dynAssignment.setValue(varEvidence, varEvidenceValue);
//                    }
//                    evidence.add(dynAssignment);
//                }


                //        System.out.println("EVIDENCE:");
                //        evidence.forEach(evid -> {
                //            System.out.println("Evidence at time " + evid.getTimeID());
                //            evid.getVariables().forEach(variable -> System.out.println(variable.getName() + ": " + Integer.toString((int) evid.getValue(variable))));
                //            System.out.println();
                //        });

            /*
             *  SET THE EVIDENCE AND MAKE INFERENCE
             */

//            timeStart = System.nanoTime();
//
//            dynMAP.setEvidence(evidence);
//            dynMAP.runInference();
//
//            timeStop = System.nanoTime();
//            execTime = (double) (timeStop - timeStart) / 1000000000.0;
//            executionTimes[n] = execTime;





            MAPInference mapInference = new MAPInference();

            mapInference.setModel(staticBN);
            mapInference.setParallelMode(true);
            mapInference.setSeed(random.nextInt());
            mapInference.setSampleSize(60);
            mapInference.setNumberOfIterations(150);
            mapInference.setMAPVariables(mapVarReplications);



            timeStart = System.nanoTime();

            mapInference.setEvidence(staticEvidence);
            mapInference.runInference();

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            executionTimesStatic[n] = execTime;







            /*
             *  SHOW RESULTS
             */
            Assignment MAPestimate = dynMAP.getMAPestimate();
            double MAPestimateProbability = dynMAP.getMAPestimateProbability();


                //        System.out.println("MAP sequence over " + mapVariable.getName() + ":");
                //        List<Variable> MAPvarReplications = MAPestimate.getVariables().stream().sorted((var1,var2) -> (var1.getVarID()>var2.getVarID()? 1 : -1)).collect(Collectors.toList());
                //
                //        StringBuilder sequence = new StringBuilder();
                //        MAPvarReplications.stream().forEachOrdered(var -> sequence.append( Integer.toString((int)MAPestimate.getValue(var)) + ", "));
                //        //System.out.println(MAPestimate.outputString(MAPvarReplications));
                //        System.out.println(sequence.toString());
                //        System.out.println("with probability prop. to: " + MAPestimateProbability);

        }

        double meanTime = Arrays.stream(executionTimes).summaryStatistics().getAverage();
        double meanTimeStatic = Arrays.stream(executionTimesStatic).summaryStatistics().getAverage();

        System.out.println("With " + nTimeSteps + " replications");
//        System.out.println("Times Dynamic MAP");
//        System.out.println(Arrays.toString(executionTimes));
//        System.out.println("Mean time: " + meanTime);
        System.out.println();
        System.out.println("Times Static MAP");
        System.out.println(Arrays.toString(executionTimesStatic));
        System.out.println("Mean time: " + meanTimeStatic);
        System.out.println();
    }

}
