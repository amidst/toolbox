package eu.amidst.pgm2016MAPflink;

import eu.amidst.core.inference.MAPInferenceRobustNew;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.inference.DistributedMAPInference;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dario on 3/5/16.
 */
public class MAPFlink_TimeScalability {

    public static void main(String[] args) throws Exception {




        /**********************************************
         *    INITIALIZATION
         *********************************************/

        FileOutputStream fileOutputStream = new FileOutputStream("./MAPFlink_SamplingTimes.txt");
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
        BufferedWriter output =  new BufferedWriter(outputStreamWriter);


        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        final int maxParallelism = env.getParallelism();

        int log2MaxCores = (int)(Math.log(maxParallelism)/Math.log(2));
        output.write("Amount of cores available: " + maxParallelism);
        output.newLine();

        double [][] executionTimes = new double[5][log2MaxCores+1];

        long timeStart;
        long timeStop;
        double execTime;
        Assignment mapEstimate;



        int seedBayesianNetwork = 2152364;
        int numberOfMultinomialVars = 250;
        BayesianNetworkGenerator.setSeed(seedBayesianNetwork);

        BayesianNetworkGenerator.setNumberOfGaussianVars(numberOfMultinomialVars);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(numberOfMultinomialVars,2);
        BayesianNetworkGenerator.setNumberOfLinks((int)(1.4*(double)2*numberOfMultinomialVars));

        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();


        int seedMAPInference = 1955237;


        int startingPoints=64;
        int samplingSize=300000;

        int numberOfIterations=20;

        int repetitions=5;


        output.write("PARAMETERS OF THE EXPERIMENT:");
        output.newLine();
        output.write("seedBayesianNetwork: " + seedBayesianNetwork + ", number of Multinomial/Gaussian vars:" + numberOfMultinomialVars);
        output.newLine();
        output.write("seedMAPInference: " + seedMAPInference + ", number of starting points:" + startingPoints + ", sampling size: " + samplingSize);
        output.newLine();
        output.write("numberOfIterations: " + numberOfIterations + ", repetitions: " + repetitions);
        output.newLine();

        try {
            /***********************************************
             *        VARIABLES OF INTEREST
             ************************************************/

            Variable varInterest1 = bn.getVariables().getVariableById(5);
            Variable varInterest2 = bn.getVariables().getVariableById(10);
            Variable varInterest3 = bn.getVariables().getVariableById(15);
            Variable varInterest4 = bn.getVariables().getVariableById(20);
            Variable varInterest5 = bn.getVariables().getVariableById(25);
            Variable varInterest6 = bn.getVariables().getVariableById(30);

            List<Variable> varsInterest = new ArrayList<>(6);
            varsInterest.add(varInterest1);
            varsInterest.add(varInterest2);
            varsInterest.add(varInterest3);
            varsInterest.add(varInterest4);
            varsInterest.add(varInterest5);
            varsInterest.add(varInterest6);
            output.write("MAP Variables of Interest: " + Arrays.toString(varsInterest.stream().map(Variable::getName).toArray()));
            output.newLine();
            output.newLine();


            DistributedMAPInference distributedMAPInference = new DistributedMAPInference();

            distributedMAPInference.setModel(bn);
            distributedMAPInference.setMAPVariables(varsInterest);

            distributedMAPInference.setSeed(seedMAPInference);
            distributedMAPInference.setNumberOfStartingPoints(startingPoints);
            distributedMAPInference.setNumberOfIterations(numberOfIterations);


            /***********************************************
             *        INCLUDING EVIDENCE
             ************************************************/

            Variable variable1 = bn.getVariables().getVariableById(50);
            Variable variable2 = bn.getVariables().getVariableById(55);
            Variable variable3 = bn.getVariables().getVariableById(60);
            Variable variable4 = bn.getVariables().getVariableById(65);
            Variable variable5 = bn.getVariables().getVariableById(70);
            Variable variable6 = bn.getVariables().getVariableById(75);

            int var1value = 0;
            int var2value = 1;
            int var3value = 1;
            int var4value = 0;
            int var5value = 0;
            int var6value = 1;

            output.write("Evidence: Variable " + variable1.getName() + " = " + var1value + ", Variable " + variable2.getName() + " = " + var2value + ", " + " and Variable " + variable3.getName() + " = " + var3value);
            output.newLine();
            output.write("          Variable " + variable4.getName() + " = " + var4value + ", Variable " + variable5.getName() + " = " + var5value + ", " + " and Variable " + variable6.getName() + " = " + var6value);
            output.newLine();

            output.newLine();

            HashMapAssignment evidence = new HashMapAssignment(3);

            evidence.setValue(variable1, var1value);
            evidence.setValue(variable2, var2value);
            evidence.setValue(variable3, var3value);
            evidence.setValue(variable4, var4value);
            evidence.setValue(variable5, var5value);
            evidence.setValue(variable6, var6value);

            distributedMAPInference.setEvidence(evidence);


            for (int i = 0; i <= log2MaxCores; i++) {

                int currentNumberOfCores = (int) Math.pow(2, i);

                distributedMAPInference.setNumberOfCores(currentNumberOfCores);


                System.out.println("Computing times with " + currentNumberOfCores + " cores...");

                output.write("Computing times with " + currentNumberOfCores + " cores...");
                output.newLine();


                /***********************************************
                 *        RUN INFERENCE
                 ************************************************/

                // DUMB EXECUTION FOR TRAINING
                distributedMAPInference.setNumberOfStartingPoints(startingPoints);

                distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_GLOBAL);
                mapEstimate = distributedMAPInference.getEstimate();

                double[] timesRepetitions = new double[repetitions];

//
//                /***********************************************
//                 *        SIMULATED ANNEALING
//                 ************************************************/
//
//
//                // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME
//                System.out.println("SA.GLOBAL");
//                for (int j = 0; j < repetitions; j++) {
//
//                    timeStart = System.nanoTime();
//                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_GLOBAL);
//
//                    mapEstimate = distributedMAPInference.getEstimate();
//                    output.write("MAP estimate  (SA.Global): " + mapEstimate.outputString(varsInterest));
//                    output.newLine();
//                    output.write("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
//                    output.newLine();
//                    output.write("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());
//                    output.newLine();
//
//                    timeStop = System.nanoTime();
//                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
//                    output.write("computed in: " + Double.toString(execTime) + " seconds");
//                    //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
//                    output.newLine();
//                    output.newLine();
//
//                    timesRepetitions[j] = execTime;
//                }
//
//                executionTimes[0][i] = Arrays.stream(timesRepetitions).average().getAsDouble();
//                output.write("SA.Global Average Time: " + executionTimes[0][i]);
//                output.newLine();
//                output.newLine();
//                timesRepetitions = new double[repetitions];
//
//
//                // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING SOME VARIABLES EACH TIME
//                System.out.println("SA.LOCAL");
//                for (int j = 0; j < repetitions; j++) {
//                    timeStart = System.nanoTime();
//                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SA_LOCAL);
//
//                    mapEstimate = distributedMAPInference.getEstimate();
//                    output.write("MAP estimate  (SA.Local): " + mapEstimate.outputString(varsInterest));
//                    output.newLine();
//                    output.write("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
//                    output.newLine();
//                    output.write("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());
//                    output.newLine();
//                    timeStop = System.nanoTime();
//                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
//                    output.write("computed in: " + Double.toString(execTime) + " seconds");
//                    //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
//                    output.newLine();
//                    output.newLine();
//
//                    timesRepetitions[j] = execTime;
//                }
//
//                executionTimes[1][i] = Arrays.stream(timesRepetitions).average().getAsDouble();
//                output.write("SA.Local Average Time: " + executionTimes[1][i]);
//                output.newLine();
//                output.newLine();
//                timesRepetitions = new double[repetitions];
//
//
//                /***********************************************
//                 *        HILL CLIMBING
//                 ************************************************/
//
//                //  MAP INFERENCE WITH HILL CLIMBING, MOVING ALL VARIABLES EACH TIME
//                System.out.println("HC.GLOBAL");
//                for (int j = 0; j < repetitions; j++) {
//
//                    timeStart = System.nanoTime();
//                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_GLOBAL);
//
//                    mapEstimate = distributedMAPInference.getEstimate();
//                    output.write("MAP estimate  (HC.Global): " + mapEstimate.outputString(varsInterest));
//                    output.newLine();
//                    output.write("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
//                    output.newLine();
//                    output.write("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());
//                    output.newLine();
//
//                    timeStop = System.nanoTime();
//                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
//                    output.write("computed in: " + Double.toString(execTime) + " seconds");
//                    output.newLine();
//                    output.newLine();
//
//                    timesRepetitions[j] = execTime;
//                }
//
//                executionTimes[2][i] = Arrays.stream(timesRepetitions).average().getAsDouble();
//                output.write("HC.Global Average Time: " + executionTimes[2][i]);
//                output.newLine();
//                output.newLine();
//                timesRepetitions = new double[repetitions];
//
//
//                //  MAP INFERENCE WITH HILL CLIMBING, MOVING SOME VARIABLES EACH TIME
//                System.out.println("HC.LOCAL");
//                for (int j = 0; j < repetitions; j++) {
//
//                    timeStart = System.nanoTime();
//                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.HC_LOCAL);
//
//                    mapEstimate = distributedMAPInference.getEstimate();
//                    output.write("MAP estimate  (HC.Local): " + mapEstimate.outputString(varsInterest));
//                    output.newLine();
//                    output.write("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
//                    output.newLine();
//                    output.write("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());
//                    output.newLine();
//
//                    timeStop = System.nanoTime();
//                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
//                    output.write("computed in: " + Double.toString(execTime) + " seconds");
//                    output.newLine();
//                    output.newLine();
//
//
//                    timesRepetitions[j] = execTime;
//                }
//
//                executionTimes[3][i] = Arrays.stream(timesRepetitions).average().getAsDouble();
//                output.write("HC.Local Average Time: " + executionTimes[3][i]);
//                output.newLine();
//                output.newLine();
//                timesRepetitions = new double[repetitions];

                /***********************************************
                 *        SAMPLING
                 ************************************************/

                System.out.println("SAMPLING");

                // MAP INFERENCE WITH SIMULATION AND PICKING MAX
                for (int j = 0; j < repetitions; j++) {

                    System.out.println("repetition " + j);
                    distributedMAPInference.setNumberOfStartingPoints(samplingSize);
                    timeStart = System.nanoTime();
                    distributedMAPInference.runInference(MAPInferenceRobustNew.SearchAlgorithm.SAMPLING);

                    mapEstimate = distributedMAPInference.getEstimate();
                    output.write("MAP estimate (SAMPLING): " + mapEstimate.outputString(varsInterest));
                    output.newLine();
                    output.write("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                    output.newLine();
                    output.write("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());
                    output.newLine();

                    timeStop = System.nanoTime();
                    execTime = (double) (timeStop - timeStart) / 1000000000.0;
                    output.write("computed in: " + Double.toString(execTime) + " seconds");
                    output.newLine();
                    output.newLine();

                    timesRepetitions[j] = execTime;
                }
                executionTimes[4][i] = Arrays.stream(timesRepetitions).average().getAsDouble();
                output.write("SAMPLING Average Time: " + executionTimes[4][i]);
                output.newLine();
                output.newLine();

            }

            output.write("EXECUTION TIMES");
            output.newLine();
            output.write("times_SA_Global = c" + Arrays.toString(executionTimes[0]).replace("[", "(").replace("]", ");"));
            output.newLine();
            output.write("times_SA_Local  = c" + Arrays.toString(executionTimes[1]).replace("[", "(").replace("]", ");"));
            output.newLine();
            output.write("times_HC_Global = c" + Arrays.toString(executionTimes[2]).replace("[", "(").replace("]", ");"));
            output.newLine();
            output.write("times_HC_Local  = c" + Arrays.toString(executionTimes[3]).replace("[", "(").replace("]", ");"));
            output.newLine();
            output.write("times_Sampling  = c" + Arrays.toString(executionTimes[4]).replace("[", "(").replace("]", ");"));
            output.newLine();

            output.close();
            outputStreamWriter.close();
            fileOutputStream.close();
        }
        catch (Exception e) {
            output.close();
            outputStreamWriter.close();
            fileOutputStream.close();

            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


}
