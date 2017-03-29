package eu.amidst.impSampling2017;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.inference.DistributedImportanceSamplingCLG;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by dario on 19/1/17.
 */
public class DistributedISTimeScalability {


    public static void main(String[] args) throws Exception {

        int sizeBayesianNetwork;

        int sampleSize;

        int numberOfRepetitions;

        // ARGS: sizeBayesianNetwork sampleSize repetitions
        if (args.length!=3) {

            sizeBayesianNetwork = 1000;

            sampleSize = 1000;

            numberOfRepetitions = 5;
        }
        else {

            sizeBayesianNetwork = Integer.parseInt(args[0]);

            sampleSize = Integer.parseInt(args[1]);

            numberOfRepetitions = Integer.parseInt(args[2]);
        }



        // OTHER PARAMETERS:

        int nVarsDiscrete = (sizeBayesianNetwork / 2);
        int nVarsCont = sizeBayesianNetwork - nVarsDiscrete;

        int nVarsEvidence = (int) (0.20 * sizeBayesianNetwork);
        int nVarsInterest = (int) (0.10 * sizeBayesianNetwork);



        int seedBN = 23532;
        int seedIS = 122;



        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        final int maxParallelism = env.getParallelism();


        System.out.println("DISTRIBUTED IMPORTANCE SAMPLING, TIME SCALABILITY EXPERIMENT");
        System.out.println("Environment parallelism: " + maxParallelism);
        System.out.println("Parameters:");
        System.out.println("Bayesian Network size " + sizeBayesianNetwork + " with seed " + seedBN);
        System.out.println("(half discrete with 2 states and half Gaussians, number of links " + (int) (1.5 * sizeBayesianNetwork) + ")");
        System.out.println("Number of repetitions to measure times: " + numberOfRepetitions);
        System.out.println();
        System.out.println("Seed for choosing variables of interest and evidence: " + (seedBN + 1000));
        System.out.println("Number of variables in the evidence: " + nVarsEvidence);
        System.out.println("Number of variables of interest: " + nVarsInterest);
        System.out.println();
        System.out.println("Seed for the DistributedImportanceSampling object: " + seedIS);
        System.out.println("Samples size for IS: " + sampleSize);


        long timeStart;
        long timeStop;
        double execTime1, execTime2, execTime3;


        //int log2MaxParallelism = (int) ( Math.log(maxParallelism)/Math.log(2) );

        // double[][] executionTimes_Queries = new double[log2MaxParallelism+1][numberOfRepetitions];
        double[] executionTimes_Gaussian = new double[numberOfRepetitions];
        double[] executionTimes_GaussianMixtures = new double[numberOfRepetitions];


//        for (int i = 0; i <= log2MaxParallelism; i++) {

//            int nCoresToUse = (int) Math.pow(2,i);
//            env.setParallelism(maxParallelism);


            System.out.println("Computing execution times...");


            for (int j = 0; j < numberOfRepetitions; j++) {

                /**********************************************
                 *    INITIALIZATION
                 *********************************************/

            /*
             *  RANDOM GENERATION OF A BAYESIAN NETWORK
             */
                BayesianNetworkGenerator.setSeed(seedBN);
                BayesianNetworkGenerator.setNumberOfMultinomialVars(nVarsDiscrete, 2);
                BayesianNetworkGenerator.setNumberOfGaussianVars(nVarsCont);
                BayesianNetworkGenerator.setNumberOfLinks((int) (1.5 * sizeBayesianNetwork));
                BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
                System.out.println(bn);


            /*
             *  RANDOM CHOICE OF A CONTINUOUS VARIABLE OF INTEREST
             */
                Random variablesChoiceRandom = new Random(seedBN + 1000);

                List<Variable> variableList = bn.getVariables().getListOfVariables();
                List<Variable> normalVariablesList = variableList.stream().filter(Variable::isNormal).collect(Collectors.toList());

                List<Variable> varsOfInterestList = new ArrayList<>();

                while (varsOfInterestList.size() < nVarsInterest) {
                    int indexVarOfInterest = variablesChoiceRandom.nextInt(normalVariablesList.size());
                    Variable varOfInterest = normalVariablesList.get(indexVarOfInterest);
                    varsOfInterestList.add(varOfInterest);
                }


            /*
             *  RANDOM GENERATION OF AN EVIDENCE (EXCLUDING VARS OF INTEREST)
             */
                List<Variable> varsEvidence = new ArrayList<>();

                while (varsEvidence.size() < nVarsEvidence) {
                    int varIndex = variablesChoiceRandom.nextInt(bn.getNumberOfVars());
                    Variable variable = bn.getVariables().getVariableById(varIndex);
                    if (!varsEvidence.contains(variable) && varsOfInterestList.stream().allMatch(var -> !variable.equals(var))) {
                        varsEvidence.add(variable);
                    }
                }

                varsEvidence.sort((variable1, variable2) -> (variable1.getVarID() > variable2.getVarID() ? 1 : -1));

//            System.out.println("\nVARIABLES IN THE EVIDENCE: ");
//            varsEvidence.forEach(variable -> System.out.println(variable.getName()));

                BayesianNetworkSampler bayesianNetworkSampler = new BayesianNetworkSampler(bn);
                bayesianNetworkSampler.setSeed(variablesChoiceRandom.nextInt());
                DataStream<DataInstance> fullSample = bayesianNetworkSampler.sampleToDataStream(1);

                HashMapAssignment evidence = new HashMapAssignment(nVarsEvidence);
                varsEvidence.forEach(variable -> evidence.setValue(variable, fullSample.stream().findFirst().get().getValue(variable)));

                System.out.println("EVIDENCE: ");
                System.out.println(evidence.outputString(varsEvidence));



                /*
                 *  INFERENCE WITH DISTRIBUTED IMPORTANCE SAMPLING
                 */
                DistributedImportanceSamplingCLG distributedIS = new DistributedImportanceSamplingCLG();

                distributedIS.setSeed(seedIS+j);
                distributedIS.setModel(bn);
                distributedIS.setSampleSize(sampleSize);
                distributedIS.setVariablesOfInterest(varsOfInterestList);
                distributedIS.setNumberOfCores(maxParallelism);

                // FIRST, RUN INFERENCE WITHOUT HEATING-UP
                distributedIS.runInference();


                // OBTAIN THE POSTERIOR AS A SINGLE GAUSSIAN
                distributedIS.setGaussianMixturePosteriors(false);

                timeStart = System.nanoTime();
                distributedIS.runInference();
                timeStop = System.nanoTime();
                execTime1 = (double) (timeStop - timeStart) / 1000000000.0;



                // OBTAIN THE POSTERIOR AS A GAUSSIAN MIXTURE
                distributedIS.setGaussianMixturePosteriors(true);

                timeStart = System.nanoTime();
                distributedIS.runInference();
                timeStop = System.nanoTime();
                execTime2 = (double) (timeStop - timeStart) / 1000000000.0;


//
//                // AND ALSO QUERY THE PROBABILITY OF THE VARIABLE BEING IN A CERTAIN INTERVAL
//                distributedIS.setGaussianMixturePosteriors(false);
//                Variable varQuery = varsOfInterestList.get(0);
//                double a = -3; // Lower endpoint of the interval
//                double b = +3; // Upper endpoint of the interval
//
//                final double finalA = a;
//                final double finalB = b;
//                distributedIS.setQuery(varQuery, (Function<Double, Double> & Serializable) (v -> (finalA < v && v < finalB) ? 1.0 : 0.0));
//
//                timeStart = System.nanoTime();
//                distributedIS.runInference();
//                timeStop = System.nanoTime();
//                execTime3 = (double) (timeStop - timeStart) / 1000000000.0;



                System.out.println("IS exec time with single Gaussian: " + Double.toString(execTime1) + " seconds");
                System.out.println("IS exec time with Gaussian Mixture: " + Double.toString(execTime2) + " seconds");
//                System.out.println("IS exec time with Query: " + Double.toString(execTime3) + " seconds");

                executionTimes_Gaussian[j]          = execTime1;
                executionTimes_GaussianMixtures[j]  = execTime2;
//                executionTimes_Queries[i][j]           = execTime3;


            }





        System.out.println(maxParallelism);

//        for (int i = 0; i <= log2MaxParallelism; i++) {
            System.out.println("Gaussian:        " + Arrays.toString(executionTimes_Gaussian));
            System.out.println("GaussianMixture: " + Arrays.toString(executionTimes_GaussianMixtures));

            System.out.println("Gausian,         mean execution time: " + Arrays.stream(executionTimes_Gaussian).average().getAsDouble() );
            System.out.println("GaussianMixture, mean execution time: " + Arrays.stream(executionTimes_GaussianMixtures).average().getAsDouble() );

//        }










//        DistributedImportanceSamplingCLG distributedIS = new DistributedImportanceSamplingCLG();
//
//
//        int seedBN = 326762;
//        int nDiscreteVars = 1000;
//        int nContVars = 1000;
//        BayesianNetworkGenerator.setSeed(seedBN);
//        BayesianNetworkGenerator.setNumberOfMultinomialVars(nDiscreteVars, 2);
//        BayesianNetworkGenerator.setNumberOfGaussianVars(nContVars);
//        BayesianNetworkGenerator.setNumberOfLinks( (int)2.5*(nDiscreteVars+nContVars));
//        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
//
//        System.out.println(bn);
//
//        List<Variable> variableList = bn.getVariables().getListOfVariables();
//
//        int seedIS = 111235236;
//        int sampleSize = 50000;
//
//        distributedIS.setSeed(seedIS);
//        distributedIS.setModel(bn);
//        distributedIS.setSampleSize(sampleSize);
//        distributedIS.setVariablesOfInterest(variableList);
//        distributedIS.setGaussianMixturePosteriors(true);
//
//        distributedIS.runInference();
//
//        for (int i = 0; i < variableList.size(); i++) {
//            Variable var = variableList.get(i);
//            System.out.println("Var: " + var.getName() + ", conditional=" + bn.getConditionalDistribution(var) + ", posterior=" + distributedIS.getPosterior(var));
//        }

        
        

    }
}
