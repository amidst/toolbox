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
public class MAPFlink_TimeScalabilityCores {

    public static void main(String[] args) throws Exception {

        int sizeBayesianNetwork;

        int startingPoints;
        int numberOfIterations;
        int sampleSizeForEstimatingProbabilities;

        int samplingSize;

        int repetitions;

        int nCoresToUse;

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        if (args.length!=7) {

            sizeBayesianNetwork = 50;

            startingPoints = 100;
            numberOfIterations = 100;
            sampleSizeForEstimatingProbabilities = 100;

            samplingSize = 100000;

            repetitions = 1;

            nCoresToUse = env.getParallelism();

        }
        else {
            sizeBayesianNetwork = Integer.parseInt(args[0]);

            startingPoints = Integer.parseInt(args[1]);
            numberOfIterations = Integer.parseInt(args[2]);
            sampleSizeForEstimatingProbabilities = Integer.parseInt(args[3]);

            samplingSize = Integer.parseInt(args[4]);

            repetitions = Integer.parseInt(args[5]);

            nCoresToUse = Integer.parseInt(args[6]);
        }


        /**********************************************
         *    INITIALIZATION
         *********************************************/

        env.setParallelism(nCoresToUse);


        long timeStart;
        long timeStop;
        double execTime;
        Assignment mapEstimate;


        int seedBayesianNetwork = 732727;
        int seedVariablesChoice = 23571131;
        int seedDistributedMAPInference = 15833;



        int nVarsEvidence = 7 * sizeBayesianNetwork / 10;
        int nVarsInterest = sizeBayesianNetwork / 10;


        System.out.println("MAP FLINK TIME SCALABILITY EXPERIMENT");
        System.out.println("Amount of cores available: " + env.getParallelism());
        System.out.println("Parameters:");
        System.out.println("Bayesian Network size " + sizeBayesianNetwork + " with seed " + seedBayesianNetwork);
        System.out.println("(half discrete with 2 states and half Gaussians, number of links " + (int) (1.4 * sizeBayesianNetwork) + ")");
        System.out.println("Number of repetitions to measure times: " + repetitions);
        System.out.println();
        System.out.println("Seed for choosing variables of interest and evidence: " + seedVariablesChoice);
        System.out.println("Number of variables in the evidence: " + nVarsEvidence);
        System.out.println("Number of variables of interest (MAP variables): " + nVarsInterest);
        System.out.println();
        System.out.println("Seed for the distributed MAP inference object: " + seedDistributedMAPInference);
        System.out.println("Starting points per core for optimization algorithms: " + startingPoints);
        System.out.println("Number of iterations for each optimization algorithms: " + numberOfIterations);
        System.out.println("Number of samples to estimate probabilities in the optimization: " + sampleSizeForEstimatingProbabilities);
        System.out.println("Samples per core for the sampling search algorithms: " + samplingSize);

        double[][] executionTimes = new double[5][repetitions];

        for (int j = 0; j < repetitions; j++) {


            /****************************************************************
             *   CREATE THE BAYESIAN NETWORK
             ****************************************************************/

            BayesianNetworkGenerator.setSeed(seedBayesianNetwork);
            BayesianNetworkGenerator.setNumberOfGaussianVars(sizeBayesianNetwork / 2);
            BayesianNetworkGenerator.setNumberOfMultinomialVars(sizeBayesianNetwork / 2, 2);
            BayesianNetworkGenerator.setNumberOfLinks((int) (1.4 * (double) sizeBayesianNetwork));

            BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();


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


            /************************************************
             *     INITIALIZE MAP INFERENCE OBJECT
             ************************************************/

            DistributedMAPInference distributedMAPInference = new DistributedMAPInference();
            distributedMAPInference.setModel(bn);

            distributedMAPInference.setMAPVariables(varsInterest);

            distributedMAPInference.setSampleSize(samplingSize);
            distributedMAPInference.setNumberOfStartingPoints(startingPoints);
            distributedMAPInference.setNumberOfIterations(numberOfIterations);
            distributedMAPInference.setSampleSizeEstimatingProbabilities(sampleSizeForEstimatingProbabilities);
            distributedMAPInference.setSeed(seedDistributedMAPInference);

            distributedMAPInference.setEvidence(evidence);
            distributedMAPInference.setNumberOfCores(nCoresToUse);

            System.out.println("DISTRIBUTED MAP INFERENCE USING " + env.getParallelism() + " CORES.");

            DataStream<DataInstance> fullSample2 = bayesianNetworkSampler.sampleToDataStream(1);
            HashMapAssignment configuration = new HashMapAssignment(bn.getNumberOfVars());

            bn.getVariables().getListOfVariables().stream().forEach(variable -> configuration.setValue(variable, fullSample2.stream().findFirst().get().getValue(variable)));
            System.out.println();


            distributedMAPInference.setNumberOfCores(nCoresToUse);
            System.out.println("Computing times with " + env.getParallelism() + " cores...");


            /***********************************************
             *        RUN INFERENCE
             ************************************************/

            // DUMB EXECUTION FOR 'HEATING UP'
            distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_GLOBAL);
//            mapEstimate = distributedMAPInference.getEstimate();

            distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SAMPLING);



            /***********************************************
             *        SIMULATED ANNEALING
             ************************************************/


            // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME
            System.out.println("SA.GLOBAL");

            timeStart = System.nanoTime();
            distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_GLOBAL);

            mapEstimate = distributedMAPInference.getEstimate();
            System.out.println("MAP estimate  (SA.Global): " + mapEstimate.outputString(varsInterest));
//            System.out.println("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
            System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            System.out.println("computed in: " + Double.toString(execTime) + " seconds");


            executionTimes[0][j] = execTime;


            // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING SOME VARIABLES EACH TIME
            System.out.println("SA.LOCAL");

            timeStart = System.nanoTime();
            distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_LOCAL);

            mapEstimate = distributedMAPInference.getEstimate();
            System.out.println("MAP estimate  (SA.Local): " + mapEstimate.outputString(varsInterest));
//            System.out.println("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
            System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            System.out.println("computed in: " + Double.toString(execTime) + " seconds");


            executionTimes[1][j] = execTime;



            /***********************************************
             *        HILL CLIMBING
             ************************************************/

            //  MAP INFERENCE WITH HILL CLIMBING, MOVING ALL VARIABLES EACH TIME
            System.out.println("HC.GLOBAL");

            timeStart = System.nanoTime();
            distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_GLOBAL);

            mapEstimate = distributedMAPInference.getEstimate();
            System.out.println("MAP estimate  (HC.Global): " + mapEstimate.outputString(varsInterest));
//            System.out.println("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
            System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());


            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            System.out.println("computed in: " + Double.toString(execTime) + " seconds");

            executionTimes[2][j] = execTime;


            //  MAP INFERENCE WITH HILL CLIMBING, MOVING SOME VARIABLES EACH TIME
            System.out.println("HC.LOCAL");

            timeStart = System.nanoTime();
            distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_LOCAL);

            mapEstimate = distributedMAPInference.getEstimate();
            System.out.println("MAP estimate  (HC.Local): " + mapEstimate.outputString(varsInterest));
//            System.out.println("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
            System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());


            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            System.out.println("computed in: " + Double.toString(execTime) + " seconds");

            executionTimes[3][j] = execTime;



            /***********************************************
             *        SAMPLING
             ************************************************/

            System.out.println("SAMPLING");

            // MAP INFERENCE WITH SIMULATION AND PICKING MAX

            timeStart = System.nanoTime();
            distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SAMPLING);

            mapEstimate = distributedMAPInference.getEstimate();
            System.out.println("MAP estimate (SAMPLING): " + mapEstimate.outputString(varsInterest));
//            System.out.println("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
            System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());


            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            System.out.println("computed in: " + Double.toString(execTime) + " seconds");

            executionTimes[4][j] = execTime;

        }


        System.out.println("EXECUTION TIMES");

        System.out.println("times_SA_Global = c" + Arrays.toString(executionTimes[0]).replace("[", "(").replace("]", ");"));
        System.out.println("times_SA_Local  = c" + Arrays.toString(executionTimes[1]).replace("[", "(").replace("]", ");"));
        System.out.println("times_HC_Global = c" + Arrays.toString(executionTimes[2]).replace("[", "(").replace("]", ");"));
        System.out.println("times_HC_Local  = c" + Arrays.toString(executionTimes[3]).replace("[", "(").replace("]", ");"));
        System.out.println("times_Sampling  = c" + Arrays.toString(executionTimes[4]).replace("[", "(").replace("]", ");"));

        System.out.println("mean_SA_Global = " + Arrays.stream(executionTimes[0]).average().getAsDouble());
        System.out.println("mean_SA_Local  = " + Arrays.stream(executionTimes[1]).average().getAsDouble());
        System.out.println("mean_HC_Global = " + Arrays.stream(executionTimes[2]).average().getAsDouble());
        System.out.println("mean_HC_Local  = " + Arrays.stream(executionTimes[3]).average().getAsDouble());
        System.out.println("mean_Sampling  = " + Arrays.stream(executionTimes[4]).average().getAsDouble());

    }


}
