package eu.amidst.pgm2016MAPflink;

import eu.amidst.core.inference.MAPInferenceRobust;
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


        BayesianNetworkGenerator.setSeed(2152364);

        BayesianNetworkGenerator.setNumberOfGaussianVars(100);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(100,2);
        BayesianNetworkGenerator.setNumberOfLinks(250);

        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();

        int startingPoints=5;
        int samplingSize=5000;

        int numberOfCores=2;
        int numerOfIterations=20;

        int repetitions=3;



        /***********************************************
         *        VARIABLES OF INTEREST
         ************************************************/

        Variable varInterest1 = bn.getVariables().getVariableById(6);
        Variable varInterest2 = bn.getVariables().getVariableById(50);
        Variable varInterest3 = bn.getVariables().getVariableById(70);

        List<Variable> varsInterest = new ArrayList<>(3);
        varsInterest.add(varInterest1);
        varsInterest.add(varInterest2);
        varsInterest.add(varInterest3);
        System.out.println("MAP Variables of Interest: " + Arrays.toString(varsInterest.stream().map(Variable::getName).toArray()));
        System.out.println();


        DistributedMAPInference distributedMAPInference = new DistributedMAPInference();

        distributedMAPInference.setModel(bn);
        distributedMAPInference.setMAPVariables(varsInterest);

        distributedMAPInference.setSeed(1955237);
        distributedMAPInference.setNumberOfCores(numberOfCores);
        distributedMAPInference.setNumberOfStartingPoints(startingPoints);
        distributedMAPInference.setNumberOfIterations(numerOfIterations);




        /***********************************************
         *        INCLUDING EVIDENCE
         ************************************************/

        Variable variable1 = bn.getVariables().getVariableById(10);
        Variable variable2 = bn.getVariables().getVariableById(20);
        Variable variable3 = bn.getVariables().getVariableById(110);

        int var1value=0;
        int var2value=1;
        int var3value=1;

        System.out.println("Evidence: Variable " + variable1.getName() + " = " + var1value + ", Variable " + variable2.getName() + " = " + var2value + ", " + " and Variable " + variable3.getName() + " = " + var3value);
        System.out.println();

        HashMapAssignment evidence = new HashMapAssignment(3);

        evidence.setValue(variable1, var1value);
        evidence.setValue(variable2, var2value);
        evidence.setValue(variable3, var3value);

        distributedMAPInference.setEvidence(evidence);



        /**********************************************
         *    INITIALIZATION
         *********************************************/

        FileOutputStream fileOutputStream = new FileOutputStream("./MAPFlink_Times.txt");
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



        for (int i = 0; i <= log2MaxCores; i++) {

            int currentNumberOfCores = (int)Math.pow(2,i);

            distributedMAPInference.setNumberOfCores(currentNumberOfCores);



            output.write("Computing times with " + currentNumberOfCores + " cores.");
            output.newLine();



            /***********************************************
             *        RUN INFERENCE
             ************************************************/

            // DUMB EXECUTION FOR TRAINING
            distributedMAPInference.setNumberOfStartingPoints(startingPoints);

            distributedMAPInference.runInference(MAPInferenceRobust.SearchAlgorithm.SA_GLOBAL);
            mapEstimate = distributedMAPInference.getEstimate();


            /***********************************************
             *        SIMULATED ANNEALING
             ************************************************/


            // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME

            double [] timesRepetitions = new double[repetitions];
            for (int j = 0; j < repetitions; j++) {

                timeStart = System.nanoTime();
                distributedMAPInference.runInference(MAPInferenceRobust.SearchAlgorithm.SA_GLOBAL);

                mapEstimate = distributedMAPInference.getEstimate();
                System.out.println("MAP estimate  (SA.All): " + mapEstimate.outputString(varsInterest));
                System.out.println("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                timeStop = System.nanoTime();
                execTime = (double) (timeStop - timeStart) / 1000000000.0;
                System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
                System.out.println();

                timesRepetitions[j] = execTime;
            }

            executionTimes[0][i] = Arrays.stream(timesRepetitions).average().getAsDouble();
            timesRepetitions = new double[repetitions];


            // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING SOME VARIABLES EACH TIME

            for (int j = 0; j < repetitions; j++) {
                timeStart = System.nanoTime();
                distributedMAPInference.runInference(MAPInferenceRobust.SearchAlgorithm.SA_LOCAL);

                mapEstimate = distributedMAPInference.getEstimate();
                System.out.println("MAP estimate  (SA.Some): " + mapEstimate.outputString(varsInterest));
                System.out.println("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                timeStop = System.nanoTime();
                execTime = (double) (timeStop - timeStart) / 1000000000.0;
                System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
                System.out.println();

                timesRepetitions[j] = execTime;
            }

            executionTimes[1][i] = Arrays.stream(timesRepetitions).average().getAsDouble();
            timesRepetitions = new double[repetitions];




            /***********************************************
             *        HILL CLIMBING
             ************************************************/

            //  MAP INFERENCE WITH HILL CLIMBING, MOVING ALL VARIABLES EACH TIME

            for (int j = 0; j < repetitions; j++) {

                timeStart = System.nanoTime();
                distributedMAPInference.runInference(MAPInferenceRobust.SearchAlgorithm.HC_GLOBAL);

                mapEstimate = distributedMAPInference.getEstimate();
                System.out.println("MAP estimate  (HC.All): " + mapEstimate.outputString(varsInterest));
                System.out.println("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                timeStop = System.nanoTime();
                execTime = (double) (timeStop - timeStart) / 1000000000.0;
                System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                System.out.println();

                timesRepetitions[j] = execTime;
            }

            executionTimes[2][i] = Arrays.stream(timesRepetitions).average().getAsDouble();
            timesRepetitions = new double[repetitions];



            //  MAP INFERENCE WITH HILL CLIMBING, MOVING SOME VARIABLES EACH TIME

            for (int j = 0; j < repetitions; j++) {

                timeStart = System.nanoTime();
                distributedMAPInference.runInference(MAPInferenceRobust.SearchAlgorithm.HC_LOCAL);

                mapEstimate = distributedMAPInference.getEstimate();
                System.out.println("MAP estimate  (HC.Some): " + mapEstimate.outputString(varsInterest));
                System.out.println("with (unnormalized) probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                timeStop = System.nanoTime();
                execTime = (double) (timeStop - timeStart) / 1000000000.0;
                System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                System.out.println();

                timesRepetitions[j] = execTime;
            }

            executionTimes[3][i] = Arrays.stream(timesRepetitions).average().getAsDouble();
            timesRepetitions = new double[repetitions];

            /***********************************************
             *        SAMPLING
             ************************************************/

            // MAP INFERENCE WITH SIMULATION AND PICKING MAX
            for (int j = 0; j < repetitions; j++) {

                distributedMAPInference.setNumberOfStartingPoints(samplingSize);
                timeStart = System.nanoTime();
                distributedMAPInference.runInference(MAPInferenceRobust.SearchAlgorithm.SAMPLING);

                mapEstimate = distributedMAPInference.getEstimate();
                System.out.println("MAP estimate (SAMPLING): " + mapEstimate.outputString(varsInterest));
                System.out.println("with probability: " + Math.exp(distributedMAPInference.getLogProbabilityOfEstimate()));
                System.out.println("with (unnormalized) log-probability: " + distributedMAPInference.getLogProbabilityOfEstimate());

                timeStop = System.nanoTime();
                execTime = (double) (timeStop - timeStart) / 1000000000.0;
                System.out.println("computed in: " + Double.toString(execTime) + " seconds");
                System.out.println();

                timesRepetitions[j] = execTime;
            }
            executionTimes[4][i] = Arrays.stream(timesRepetitions).average().getAsDouble();

        }

        output.write("EXECUTION TIMES");
        output.newLine();
        output.write("times_SA_Global = c" + Arrays.toString(executionTimes[0]).replace("[","(").replace("]",")"));
        output.newLine();
        output.write("times_SA_Local  = c" + Arrays.toString(executionTimes[1]).replace("[","(").replace("]",")"));
        output.newLine();
        output.write("times_HC_Global = c" + Arrays.toString(executionTimes[2]).replace("[","(").replace("]",")"));
        output.newLine();
        output.write("times_HC_Local  = c" + Arrays.toString(executionTimes[3]).replace("[","(").replace("]",")"));
        output.newLine();
        output.write("times_Sampling  = c" + Arrays.toString(executionTimes[4]).replace("[","(").replace("]",")"));
        output.newLine();

        output.close();
        outputStreamWriter.close();
        fileOutputStream.close();
    }


}
