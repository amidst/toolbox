package eu.amidst.pgm2016MAPflink;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.inference.MAPInferenceRobustNew;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by dario on 3/5/16.
 */
public class MAPRobust_Precision {

    public static void main(String[] args) throws Exception {

        final int sizeBayesianNetwork = 500;

        int seedBayesianNetwork = 98983;
        int seedVariablesChoice = 82125;

        int samplingMethodSize = 100000;
        int startingPoints = 20;
        int numberOfIterations = 1000;


        int nVarsEvidence = sizeBayesianNetwork/10;
        int nVarsInterest = sizeBayesianNetwork/10;;


        long timeStart;
        long timeStop;
        double execTime;
        Assignment mapEstimate;


        /**********************************************
         *    INITIALIZATION
         *********************************************/

        BayesianNetworkGenerator.setSeed(seedBayesianNetwork);

        BayesianNetworkGenerator.setNumberOfGaussianVars(sizeBayesianNetwork/2);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(sizeBayesianNetwork/2,2);
        BayesianNetworkGenerator.setNumberOfLinks((int)(1.4*sizeBayesianNetwork));

        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
        System.out.println(bn.toString());



        /****************************************************************
         *   CHOOSE VARIABLES OF INTEREST AND THOSE TO BE OBSERVED
         ****************************************************************/

        Random variablesChoiceRandom = new Random(seedVariablesChoice);

        List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);
        List<Variable> varsInterest = new ArrayList<>(nVarsInterest);

//        for (int i = 0; i < nVarsInterest; i++) {
//            varsInterest.add(bn.getVariables().getVariableById(i));
//        }
//        for (int i = 0; i < nVarsEvidence; i++) {
//            varsEvidence.add(bn.getVariables().getVariableById(nVarsInterest + i));
//        }

        while(varsEvidence.size()<nVarsEvidence) {
            int varIndex = variablesChoiceRandom.nextInt(bn.getNumberOfVars());
            Variable variable = bn.getVariables().getVariableById(varIndex);
            if (! varsEvidence.contains(variable)) {
                varsEvidence.add(variable);
            }
        }

        while(varsInterest.size()<nVarsInterest) {
            int varIndex = variablesChoiceRandom.nextInt(bn.getNumberOfVars());
            Variable variable = bn.getVariables().getVariableById(varIndex);
            if (! varsInterest.contains(variable) && ! varsEvidence.contains(variable)) {
                varsInterest.add(variable);
            }
        }

        varsEvidence.sort((variable1,variable2) -> (variable1.getVarID() > variable2.getVarID() ? 1 : -1));
        varsInterest.sort((variable1,variable2) -> (variable1.getVarID() > variable2.getVarID() ? 1 : -1));


        System.out.println("\nVARIABLES OF INTEREST:");
        varsInterest.forEach(var -> System.out.println(var.getName()));


        System.out.println("\nVARIABLES IN THE EVIDENCE:");
        varsEvidence.forEach(var -> System.out.println(var.getName()));



        /***********************************************
         *     GENERATE AND INCLUDE THE EVIDENCE
         ************************************************/

        BayesianNetworkSampler bayesianNetworkSampler = new BayesianNetworkSampler(bn);
        bayesianNetworkSampler.setSeed(variablesChoiceRandom.nextInt());
        DataStream<DataInstance> fullSample = bayesianNetworkSampler.sampleToDataStream(1);

        HashMapAssignment evidence = new HashMapAssignment(nVarsEvidence);
        varsEvidence.stream().forEach(variable -> evidence.setValue(variable,fullSample.stream().findFirst().get().getValue(variable)));

        System.out.println("\nEVIDENCE: ");
        System.out.println(evidence.outputString(varsEvidence));



        /***********************************************
         *     INITIALIZE MAP INFERENCE OBJECT
         ************************************************/

        MAPInferenceRobustNew mapInference = new MAPInferenceRobustNew();
        mapInference.setModel(bn);

        mapInference.setSampleSize(samplingMethodSize);
        mapInference.setNumberOfStartingPoints(startingPoints);
        mapInference.setNumberOfIterations(numberOfIterations);
        mapInference.setSeed(362371);

        mapInference.setParallelMode(true);

        mapInference.setMAPVariables(varsInterest);
        mapInference.setEvidence(evidence);


        DataStream<DataInstance> fullSample2 = bayesianNetworkSampler.sampleToDataStream(1);
        HashMapAssignment configuration = new HashMapAssignment(bn.getNumberOfVars());

        bn.getVariables().getListOfVariables().stream().forEach(variable -> configuration.setValue(variable,fullSample2.stream().findFirst().get().getValue(variable)));


        System.out.println();

        mapInference.setSampleSizeEstimatingProbabilities(100);

//        int nVarsMover = 3;
//        Random random = new Random(23326);
//        Assignment config2 = new HashMapAssignment(configuration);
//        config2 = mapInference.fullAssignmentToMAPassignment(config2);
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println("NEW FINAL CONFIG: " + config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));


//         DUMB EXECUTION FOR 'HEATING UP'
        mapInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_GLOBAL);


        /***********************************************
         *        SIMULATED ANNEALING
         ************************************************/


        // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_GLOBAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (SA.Global): " + mapEstimate.outputString(varsInterest));
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();


        // MAP INFERENCE WITH SIMULATED ANNEALING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_LOCAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (SA.Local): " + mapEstimate.outputString(varsInterest));
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();


        /***********************************************
         *        HILL CLIMBING
         ************************************************/

        //  MAP INFERENCE WITH HILL CLIMBING, MOVING ALL VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_GLOBAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (HC.Global): " + mapEstimate.outputString(varsInterest));
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();



        //  MAP INFERENCE WITH HILL CLIMBING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_LOCAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (HC.Local): " + mapEstimate.outputString(varsInterest));
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();


        /************************************************
         *        SAMPLING
         ************************************************/

        // MAP INFERENCE WITH SIMULATION AND PICKING MAX
        mapInference.setNumberOfStartingPoints(samplingMethodSize);
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SAMPLING);

        mapEstimate = mapInference.getEstimate();

        System.out.println("MAP estimate (SAMPLING): " + mapEstimate.outputString(varsInterest));
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;

        mapInference.setSampleSizeEstimatingProbabilities(5000);
        double estimatedProbability = mapInference.estimateLogProbabilityOfPartialAssignment(mapEstimate);
        System.out.println("with PRECISE RE-estimated probability: " + Math.exp(estimatedProbability));
        System.out.println("with PRECISE RE-estimated log-probability: " + estimatedProbability);

        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();






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
//        distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_LOCAL);
//
//        System.out.println(distributedMAPInference.getEstimate().outputString(varsInterest));
//        System.out.println("log-prob of estimate: " + distributedMAPInference.getLogProbabilityOfEstimate());
//
//
//
//        distributedMAPInference.setNumberOfStartingPoints(samplingSize);
//        distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SAMPLING);
//
//        System.out.println(distributedMAPInference.getEstimate().outputString(varsInterest));
//        System.out.println("log-prob of estimate: " + distributedMAPInference.getLogProbabilityOfEstimate());


    }


}
