//package eu.amidst.pgm2016MAPflink;
//
//import eu.amidst.core.inference.MAPInference;
//import eu.amidst.core.models.BayesianNetwork;
//import eu.amidst.core.utils.BayesianNetworkGenerator;
//import eu.amidst.core.variables.HashMapAssignment;
//import eu.amidst.core.variables.Variable;
//import eu.amidst.flinklink.core.inference.DistributedMAPInference;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * Created by dario on 3/5/16.
// */
//public class MAPFlink_PrecisionScalability {
//
//    public static void main(String[] args) throws Exception {
//
//
//        BayesianNetworkGenerator.setSeed(2152364);
//
//        BayesianNetworkGenerator.setNumberOfGaussianVars(100);
//        BayesianNetworkGenerator.setNumberOfMultinomialVars(100,2);
//        BayesianNetworkGenerator.setNumberOfLinks(250);
//
//        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
//
//        int startingPoints=30;
//        int samplingSize=20000;
//
//        int numberOfCores=2;
//
//
//        /***********************************************
//         *        VARIABLES OF INTEREST
//         ************************************************/
//
//        Variable varInterest1 = bn.getVariables().getVariableById(6);
//        Variable varInterest2 = bn.getVariables().getVariableById(50);
//        Variable varInterest3 = bn.getVariables().getVariableById(70);
//
//        List<Variable> varsInterest = new ArrayList<>(3);
//        varsInterest.add(varInterest1);
//        varsInterest.add(varInterest2);
//        varsInterest.add(varInterest3);
//        System.out.println("MAP Variables of Interest: " + Arrays.toString(varsInterest.stream().map(Variable::getName).toArray()));
//        System.out.println();
//
//
//        DistributedMAPInference distributedMAPInference = new DistributedMAPInference();
//
//        distributedMAPInference.setModel(bn);
//        distributedMAPInference.setMAPVariables(varsInterest);
//
//        distributedMAPInference.setSeed(1955237);
//        distributedMAPInference.setNumberOfCores(numberOfCores);
//        distributedMAPInference.setNumberOfStartingPoints(startingPoints);
//        distributedMAPInference.setNumberOfIterations(200);
//
//
//
//
//        /***********************************************
//         *        INCLUDING EVIDENCE
//         ************************************************/
//
//        Variable variable1 = bn.getVariables().getVariableById(10);
//        Variable variable2 = bn.getVariables().getVariableById(20);
//        Variable variable3 = bn.getVariables().getVariableById(110);
//
//        int var1value=0;
//        int var2value=1;
//        int var3value=1;
//
//        System.out.println("Evidence: Variable " + variable1.getName() + " = " + var1value + ", Variable " + variable2.getName() + " = " + var2value + ", " + " and Variable " + variable3.getName() + " = " + var3value);
//        System.out.println();
//
//        HashMapAssignment evidence = new HashMapAssignment(3);
//
//        evidence.setValue(variable1, var1value);
//        evidence.setValue(variable2, var2value);
//        evidence.setValue(variable3, var3value);
//
//        distributedMAPInference.setEvidence(evidence);
//
//        /***********************************************
//         *        RUN INFERENCE
//         ************************************************/
//
//        distributedMAPInference.runInference(MAPInference.SearchAlgorithm.HC_LOCAL);
//
//        System.out.println(distributedMAPInference.getEstimate().outputString(varsInterest));
//        System.out.println("log-prob of estimate: " + distributedMAPInference.getLogProbabilityOfEstimate());
//
//
//
//        distributedMAPInference.setNumberOfStartingPoints(samplingSize);
//        distributedMAPInference.runInference(MAPInference.SearchAlgorithm.SAMPLING);
//
//        System.out.println(distributedMAPInference.getEstimate().outputString(varsInterest));
//        System.out.println("log-prob of estimate: " + distributedMAPInference.getLogProbabilityOfEstimate());
//    }
//
//
//}
