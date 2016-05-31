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
import eu.amidst.flinklink.core.inference.DistributedMAPInference;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by dario on 3/5/16.
 */
public class MAPFlink_PrecisionScalability {

    public static void main(String[] args) throws Exception {


        int sizeBayesianNetwork;

        int startingPointsPerCore;
        int numberOfIterations;
        int sampleSizeForEstimatingProbabilities;

        int samplesPerCore;

        if (args.length!=5) {

            sizeBayesianNetwork = 200;

            startingPointsPerCore = 1;
            numberOfIterations = 100;
            sampleSizeForEstimatingProbabilities = 200;

            samplesPerCore = 1500;

        }
        else {
            sizeBayesianNetwork = Integer.parseInt(args[0]);

            startingPointsPerCore = Integer.parseInt(args[1]);
            numberOfIterations = Integer.parseInt(args[2]);
            sampleSizeForEstimatingProbabilities = Integer.parseInt(args[3]);

            samplesPerCore = Integer.parseInt(args[4]);

        }


        int seedBayesianNetwork = 35734;
        int seedVariablesChoice = 1241;
        int seedDistributedMAPInference = 616162;


        int nVarsEvidence = 7*sizeBayesianNetwork/10;
        int nVarsInterest = sizeBayesianNetwork/10;;


        long timeStart;
        long timeStop;
        double execTime;

        final int numberOfNetworks = 5;
        final int numberOfEvidencesPerNetwork = 3;

        System.out.println("MAP FLINK PRECISION SCALABILITY EXPERIMENT");
        System.out.println("Parameters:");
        System.out.println("Bayesian Network Size " + sizeBayesianNetwork + " with seed " + seedBayesianNetwork);
        System.out.println("(half discrete with 2 states and half Gaussians, number of links " + (int)(1.4*sizeBayesianNetwork) + ")");
        System.out.println();
        System.out.println("Seed for choosing variables of interest and evidence: " + seedVariablesChoice);
        System.out.println("Number of variables in the evidence: " + nVarsEvidence);
        System.out.println("Number of variables of interest (MAP variables): " + nVarsInterest);
        System.out.println();
        System.out.println("Seed for the distributed MAP inference object: " + seedDistributedMAPInference);
        System.out.println("Starting points per core for optimization algorithms: " + startingPointsPerCore);
        System.out.println("Number of iterations for each optimization algorithms: " + numberOfIterations);
        System.out.println("Samples per core for the sampling search algorithms: " + samplesPerCore);

        /**********************************************
         *    INITIALIZATION
         *********************************************/


        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        final int maxParallelism = env.getParallelism();

        int log2MaxCores = (int) (Math.log(maxParallelism) / Math.log(2));
        log2MaxCores = 5; // NOT NECESSARILY THE ACTUAL NUMBER OF CORES, 5: 32 CORES
        System.out.println("Amount of cores available: " + maxParallelism);

        System.out.println();
        System.out.println();


        double[][] resultingLogProbabilities = new double[5][numberOfNetworks * numberOfEvidencesPerNetwork * (log2MaxCores+1)];
        int experimentCounter = 0;


        for (int i = 0; i < numberOfNetworks; i++) {

            System.out.println("\n\n BAYESIAN NETWORK NUMBER " + i + "\n\n");
            BayesianNetworkGenerator.setSeed(seedBayesianNetwork + i);

            BayesianNetworkGenerator.setNumberOfGaussianVars(sizeBayesianNetwork / 2);
            BayesianNetworkGenerator.setNumberOfMultinomialVars(sizeBayesianNetwork / 2, 2);
            BayesianNetworkGenerator.setNumberOfLinks((int) (1.4 * sizeBayesianNetwork));

            BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
            System.out.println(bn.toString());

            for (int j = 0; j < numberOfEvidencesPerNetwork; j++) {

                System.out.println("\n\n EVIDENCE NUMBER " + j + "\n\n");

                /****************************************************************
                 *   CHOOSE VARIABLES OF INTEREST AND THOSE TO BE OBSERVED
                 ****************************************************************/

                Random variablesChoiceRandom = new Random(seedVariablesChoice + j);

                List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);
                List<Variable> varsInterest = new ArrayList<>(nVarsInterest);

//        //To choose the first variables as MAP vars and the following ones as evidence
//        for (int i = 0; i < nVarsInterest; i++) {
//            varsInterest.add(bn.getVariables().getVariableById(i));
//        }
//        for (int i = 0; i < nVarsEvidence; i++) {
//            varsEvidence.add(bn.getVariables().getVariableById(nVarsInterest + i));
//        }

                //Randomly chooses variables for the evidence and (different) variables of interest.
                while (varsEvidence.size() < nVarsEvidence) {
                    int varIndex = variablesChoiceRandom.nextInt(bn.getNumberOfVars());
                    Variable variable = bn.getVariables().getVariableById(varIndex);
                    if (!varsEvidence.contains(variable)) {
                        varsEvidence.add(variable);
                    }
                }

                while (varsInterest.size() < nVarsInterest) {
                    int varIndex = variablesChoiceRandom.nextInt(bn.getNumberOfVars());
                    Variable variable = bn.getVariables().getVariableById(varIndex);
                    if (!varsInterest.contains(variable) && !varsEvidence.contains(variable)) {
                        varsInterest.add(variable);
                    }
                }

                varsEvidence.sort((variable1, variable2) -> (variable1.getVarID() > variable2.getVarID() ? 1 : -1));
                varsInterest.sort((variable1, variable2) -> (variable1.getVarID() > variable2.getVarID() ? 1 : -1));


                System.out.println("\nVARIABLES OF INTEREST:");
                System.out.println("Discrete vars: " + varsInterest.stream().filter(Variable::isMultinomial).count());
                System.out.println("Continuous vars: " + varsInterest.stream().filter(Variable::isNormal).count());
//                varsInterest.forEach(var -> System.out.println(var.getName()));


                System.out.println("\nVARIABLES IN THE EVIDENCE:");
                System.out.println("Discrete vars: " + varsEvidence.stream().filter(Variable::isMultinomial).count());
                System.out.println("Continuous vars: " + varsEvidence.stream().filter(Variable::isNormal).count());
//                varsEvidence.forEach(var -> System.out.println(var.getName()));


                /************************************************
                 *     GENERATE AND INCLUDE THE EVIDENCE
                 ************************************************/

                BayesianNetworkSampler bayesianNetworkSampler = new BayesianNetworkSampler(bn);
                bayesianNetworkSampler.setSeed(variablesChoiceRandom.nextInt());
                DataStream<DataInstance> fullSample = bayesianNetworkSampler.sampleToDataStream(1);

                HashMapAssignment evidence = new HashMapAssignment(nVarsEvidence);
                varsEvidence.stream().forEach(variable -> evidence.setValue(variable, fullSample.stream().findFirst().get().getValue(variable)));

                System.out.println("\nEVIDENCE: ");
                System.out.println(evidence.outputString(varsEvidence));


                for (int k = 0; k <= log2MaxCores; k++) {

                    int nCoresToUse = (int)Math.pow(2,k);


                    int nSamplesToUse = nCoresToUse * samplesPerCore;
                    int nStartingPointsToUse = nCoresToUse * startingPointsPerCore;

                    /************************************************
                     *     INITIALIZE MAP INFERENCE OBJECT
                     ************************************************/

                    DistributedMAPInference distributedMAPInference = new DistributedMAPInference();
                    distributedMAPInference.setModel(bn);

                    distributedMAPInference.setMAPVariables(varsInterest);

                    distributedMAPInference.setSampleSize(nSamplesToUse);
                    distributedMAPInference.setNumberOfStartingPoints(nStartingPointsToUse);
                    distributedMAPInference.setNumberOfIterations(numberOfIterations);
                    distributedMAPInference.setSampleSizeEstimatingProbabilities(sampleSizeForEstimatingProbabilities);
                    distributedMAPInference.setSeed(seedDistributedMAPInference);

                    distributedMAPInference.setEvidence(evidence);
                    distributedMAPInference.setNumberOfCores(maxParallelism);

                    System.out.println("DISTRIBUTED MAP INFERENCE USING " + maxParallelism + " CORES, SIMULATING " + nCoresToUse + " CORES");

                    DataStream<DataInstance> fullSample2 = bayesianNetworkSampler.sampleToDataStream(1);
                    HashMapAssignment configuration = new HashMapAssignment(bn.getNumberOfVars());

                    bn.getVariables().getListOfVariables().stream().forEach(variable -> configuration.setValue(variable, fullSample2.stream().findFirst().get().getValue(variable)));


                    System.out.println();


//        int nVarsMover = 3;
//        Random random = new Random(23326);
//        Assignment config2 = new HashMapAssignment(configuration);
//        config2 = distributedMAPInference.fullAssignmentToMAPassignment(config2);
//
//        config2 = (HashMapAssignment)distributedMAPInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println("NEW FINAL CONFIG: " + config2.outputString(varsInterest));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//
//        config2 = (HashMapAssignment)distributedMAPInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)distributedMAPInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)distributedMAPInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)distributedMAPInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)distributedMAPInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)distributedMAPInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(distributedMAPInference.estimateLogProbabilityOfPartialAssignment(config2));


//         DUMB EXECUTION FOR 'HEATING UP'
                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_GLOBAL);


                    /************************************************
                     *        SIMULATED ANNEALING
                     ************************************************/

                    // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME
                    timeStart = System.nanoTime();
                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_GLOBAL);

                    Assignment mapEstimate_SAGlobal = distributedMAPInference.getEstimate();
                    System.out.println("MAP estimate  (SA.Global): " + mapEstimate_SAGlobal.outputString(varsInterest));
//                    System.out.println("with estimated (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                    System.out.println("with estimated (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                    timeStop = System.nanoTime();
                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
                    System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                    System.out.println();


                    // MAP INFERENCE WITH SIMULATED ANNEALING, SOME VARIABLES EACH TIME
                    timeStart = System.nanoTime();
                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_LOCAL);

                    Assignment mapEstimate_SALocal = distributedMAPInference.getEstimate();
                    System.out.println("MAP estimate  (SA.Local): " + mapEstimate_SALocal.outputString(varsInterest));
//                    System.out.println("with estimated (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                    System.out.println("with estimated (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                    timeStop = System.nanoTime();
                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
                    System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                    System.out.println();


                    /************************************************
                     *        HILL CLIMBING
                     ************************************************/

                    //  MAP INFERENCE WITH HILL CLIMBING, MOVING ALL VARIABLES EACH TIME
                    timeStart = System.nanoTime();
                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_GLOBAL);

                    Assignment mapEstimate_HCGlobal = distributedMAPInference.getEstimate();
                    System.out.println("MAP estimate  (HC.Global): " + mapEstimate_HCGlobal.outputString(varsInterest));
//                    System.out.println("with estimated (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                    System.out.println("with estimated (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                    timeStop = System.nanoTime();
                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
                    System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                    System.out.println();


                    //  MAP INFERENCE WITH HILL CLIMBING, SOME VARIABLES EACH TIME
                    timeStart = System.nanoTime();
                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_LOCAL);

                    Assignment mapEstimate_HCLocal = distributedMAPInference.getEstimate();
                    System.out.println("MAP estimate  (HC.Local): " + mapEstimate_HCLocal.outputString(varsInterest));
//                    System.out.println("with estimated (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                    System.out.println("with estimated (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                    timeStop = System.nanoTime();
                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
                    System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                    System.out.println();


                    /************************************************
                     *        SAMPLING
                     ************************************************/

                    // MAP INFERENCE WITH SIMULATION AND PICKING MAX
                    distributedMAPInference.setNumberOfStartingPoints(samplesPerCore);
                    timeStart = System.nanoTime();
                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SAMPLING);

                    Assignment mapEstimate_Sampling = distributedMAPInference.getEstimate();

                    System.out.println("MAP estimate (SAMPLING): " + mapEstimate_Sampling.outputString(varsInterest));
//                    System.out.println("with estimated (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                    System.out.println("with estimated (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                    timeStop = System.nanoTime();
                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
                    System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                    System.out.println();


                    // INDEPENDENT ESTIMATION OF THE LOG-PROBABILITIES OF THE MAP ESTIMATES
                    MAPInferenceRobustNew mapInferenceRobustNew = new MAPInferenceRobustNew();
                    mapInferenceRobustNew.setModel(bn);
                    mapInferenceRobustNew.setMAPVariables(varsInterest);
                    mapInferenceRobustNew.setEvidence(evidence);
                    mapInferenceRobustNew.setSeed(seedBayesianNetwork);


                    int sampleSizePreciseEstimation = 200000;
                    double estimatedLogProbability;

                    System.out.println("SA Global");
                    estimatedLogProbability = preciseEstimationOfLogProbabilities(mapInferenceRobustNew, mapEstimate_SAGlobal, sampleSizePreciseEstimation);
                    System.out.println("Estimated logProbability: " + estimatedLogProbability);
                    resultingLogProbabilities[0][experimentCounter] = estimatedLogProbability;


                    System.out.println("SA Local");
                    estimatedLogProbability = preciseEstimationOfLogProbabilities(mapInferenceRobustNew, mapEstimate_SALocal, sampleSizePreciseEstimation);
                    System.out.println("Estimated logProbability: " + estimatedLogProbability);
                    resultingLogProbabilities[1][experimentCounter] = estimatedLogProbability;


                    System.out.println("HC Global");
                    estimatedLogProbability = preciseEstimationOfLogProbabilities(mapInferenceRobustNew, mapEstimate_HCGlobal, sampleSizePreciseEstimation);
                    System.out.println("Estimated logProbability: " + estimatedLogProbability);
                    resultingLogProbabilities[2][experimentCounter] = estimatedLogProbability;


                    System.out.println("HC Local");
                    estimatedLogProbability = preciseEstimationOfLogProbabilities(mapInferenceRobustNew, mapEstimate_HCLocal, sampleSizePreciseEstimation);
                    System.out.println("Estimated logProbability: " + estimatedLogProbability);
                    resultingLogProbabilities[3][experimentCounter] = estimatedLogProbability;

                    System.out.println("Sampling");
                    estimatedLogProbability = preciseEstimationOfLogProbabilities(mapInferenceRobustNew, mapEstimate_Sampling, sampleSizePreciseEstimation);
                    System.out.println("Estimated logProbability: " + estimatedLogProbability);
                    resultingLogProbabilities[4][experimentCounter] = estimatedLogProbability;

                    experimentCounter++;
                }
            }
        }

        System.out.println("LOG PROBABILITIES WITH BN SIZE " + sizeBayesianNetwork);
        System.out.println("logProbs_SAGlobal = " + Arrays.toString(resultingLogProbabilities[0]).replace("[", "c(").replace("]", ");"));
        System.out.println("logProbs_SALocal = " + Arrays.toString(resultingLogProbabilities[1]).replace("[", "c(").replace("]", ");"));
        System.out.println("logProbs_HCGlobal = " + Arrays.toString(resultingLogProbabilities[2]).replace("[", "c(").replace("]", ");"));
        System.out.println("logProbs_HCLocal = " + Arrays.toString(resultingLogProbabilities[3]).replace("[", "c(").replace("]", ");"));
        System.out.println("logProbs_Sampling = " + Arrays.toString(resultingLogProbabilities[4]).replace("[", "c(").replace("]", ");"));


    }


    private static double preciseEstimationOfLogProbabilities(MAPInferenceRobustNew mapInferenceRobustNew, Assignment mapEstimate, int initialSampleSize) {

        int sampleSizePreciseEstimation = initialSampleSize;
        mapInferenceRobustNew.setSampleSizeEstimatingProbabilities(sampleSizePreciseEstimation);

        double relativeError = 1;
        double estimatedLogProbability1, estimatedLogProbability2, estimatedLogProbability3;

        double meanEstimatedProbability = 0, varianceEstimatedProbability, standardErrorEstimatedProbability;

        while(relativeError>0.03) {

            mapInferenceRobustNew.setSampleSizeEstimatingProbabilities(sampleSizePreciseEstimation);

            estimatedLogProbability1 = mapInferenceRobustNew.estimateLogProbabilityOfPartialAssignment(mapEstimate);
//            System.out.println("SAGlobal PRECISE RE-estimated log-probability: " + estimatedLogProbability1);

            estimatedLogProbability2 = mapInferenceRobustNew.estimateLogProbabilityOfPartialAssignment(mapEstimate);
//            System.out.println("SAGlobal PRECISE RE-estimated log-probability: " + estimatedLogProbability2);

            estimatedLogProbability3 = mapInferenceRobustNew.estimateLogProbabilityOfPartialAssignment(mapEstimate);
//            System.out.println("SAGlobal PRECISE RE-estimated log-probability: " + estimatedLogProbability3);

            System.out.println("Raw probs: " + estimatedLogProbability1 + ", " + estimatedLogProbability2 + ", " + estimatedLogProbability3);

            meanEstimatedProbability = (estimatedLogProbability1 + estimatedLogProbability2 + estimatedLogProbability3) / 3;
            varianceEstimatedProbability = (Math.pow(estimatedLogProbability1-meanEstimatedProbability,2) + Math.pow(estimatedLogProbability2-meanEstimatedProbability,2) + Math.pow(estimatedLogProbability3-meanEstimatedProbability,2)) / 2;


            standardErrorEstimatedProbability = Math.sqrt(varianceEstimatedProbability)/Math.sqrt(3);

            relativeError = standardErrorEstimatedProbability/Math.abs(meanEstimatedProbability);

            System.out.println(meanEstimatedProbability + ", " + varianceEstimatedProbability + ", " + standardErrorEstimatedProbability);
            System.out.println("Relative error with " + sampleSizePreciseEstimation + " samples: " + relativeError);


//            meanEstimatedProbability = ImportanceSamplingRobust.robustSumOfLogarithms(ImportanceSamplingRobust.robustSumOfLogarithms(estimatedLogProbability1,estimatedLogProbability2),estimatedLogProbability3) - Math.log(3);
//            varianceEstimatedProbability = (Math.pow(Math.exp(ImportanceSamplingRobust.robustDifferenceOfLogarithms(estimatedLogProbability1,meanEstimatedProbability)),2) + Math.pow(Math.exp(ImportanceSamplingRobust.robustDifferenceOfLogarithms(estimatedLogProbability2,meanEstimatedProbability)),2)  + Math.pow(Math.exp(ImportanceSamplingRobust.robustDifferenceOfLogarithms(estimatedLogProbability3,meanEstimatedProbability)),2)) / 2;
//            standardErrorEstimatedProbability = Math.sqrt(varianceEstimatedProbability)/Math.sqrt(3);
//
//            relativeError = standardErrorEstimatedProbability/Math.abs(meanEstimatedProbability);
//
//            System.out.println(meanEstimatedProbability + ", " + varianceEstimatedProbability + ", " + standardErrorEstimatedProbability);
//            System.out.println("Relative error with " + sampleSizePreciseEstimation + " samples: " + relativeError);

            sampleSizePreciseEstimation = 4 * sampleSizePreciseEstimation;

        }
        return meanEstimatedProbability;
    }

}
