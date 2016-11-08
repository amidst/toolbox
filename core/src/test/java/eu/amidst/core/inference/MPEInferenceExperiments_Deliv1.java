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

package eu.amidst.core.inference;


import eu.amidst.core.Main;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by dario on 01/06/15.
 */
public class MPEInferenceExperiments_Deliv1 {

    private static Assignment randomEvidence(long seed, double evidenceRatio, BayesianNetwork bn) throws UnsupportedOperationException {

        if (evidenceRatio<=0 || evidenceRatio>=1) {
            throw new UnsupportedOperationException("Error: invalid ratio");
        }

        int numVariables = bn.getVariables().getNumberOfVars();

        Random random=new Random(seed); //1823716125
        int numVarEvidence = (int) Math.ceil(numVariables*evidenceRatio); // Evidence on 20% of variables
        //numVarEvidence = 0;
        //List<Variable> varEvidence = new ArrayList<>(numVarEvidence);
        double [] evidence = new double[numVarEvidence];
        Variable aux;
        HashMapAssignment assignment = new HashMapAssignment(numVarEvidence);

        int[] indexesEvidence = new int[numVarEvidence];
        //indexesEvidence[0]=varInterest.getVarID();
        //if (Main.VERBOSE) System.out.println(variable.getVarID());

        if (Main.VERBOSE) System.out.println("Evidence:");
        for( int k=0; k<numVarEvidence; k++ ) {
            int varIndex=-1;
            do {
                varIndex = random.nextInt( bn.getNumberOfVars() );
                //if (Main.VERBOSE) System.out.println(varIndex);
                aux = bn.getVariables().getVariableById(varIndex);

                double thisEvidence;
                if (aux.isMultinomial()) {
                    thisEvidence = random.nextInt( aux.getNumberOfStates() );
                }
                else {
                    thisEvidence = random.nextGaussian();
                }
                evidence[k] = thisEvidence;

            } while (ArrayUtils.contains(indexesEvidence, varIndex) );

            indexesEvidence[k]=varIndex;
            //if (Main.VERBOSE) System.out.println(Arrays.toString(indexesEvidence));
            if (Main.VERBOSE) System.out.println("Variable " + aux.getName() + " = " + evidence[k]);

            assignment.setValue(aux,evidence[k]);
        }
        if (Main.VERBOSE) System.out.println();
        return assignment;
    }


    /**
     * The class constructor.
     * @param args Array of options: "filename variable a b N useVMP" if variable is continuous or "filename variable w N useVMP" for discrete
     */
    public static void main(String[] args) throws Exception { // args: seedNetwork numberGaussians numberDiscrete seedAlgorithms

        int seedNetwork = 23423523;
        int numberOfGaussians = 20;
        int numberOfMultinomials = 20;

        int seed = 634634534;

        int parallelSamples = 100;
        int samplingMethodSize = 10000;

        int repetitions=10;

        int numberOfIterations=200;

        if(args.length!=8) {
            if (Main.VERBOSE) System.out.println("Invalid number of parameters. Using default values");
        }
        else {
            try {
                seedNetwork = Integer.parseInt(args[0]);
                numberOfGaussians = Integer.parseInt(args[1]);
                numberOfMultinomials = Integer.parseInt(args[2]);

                seed = Integer.parseInt(args[3]);

                parallelSamples = Integer.parseInt(args[4]);
                samplingMethodSize = Integer.parseInt(args[5]);

                repetitions = Integer.parseInt(args[6]);

                numberOfIterations = Integer.parseInt(args[7]);

            } catch (NumberFormatException ex) {
                if (Main.VERBOSE) System.out.println("Invalid parameters. Provide integers: seedNetwork numberGaussians numberDiscrete seedAlgorithms parallelSamples sampleSize repetitions");
                if (Main.VERBOSE) System.out.println("Using default parameters");
                if (Main.VERBOSE) System.out.println(ex.toString());
                System.exit(20);
            }
        }
        int numberOfLinks=(int) 1.3 * (numberOfGaussians + numberOfMultinomials);

        BayesianNetworkGenerator.setSeed(seedNetwork);
        BayesianNetworkGenerator.setNumberOfGaussianVars(numberOfGaussians);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(numberOfMultinomials, 2);
        BayesianNetworkGenerator.setNumberOfLinks(numberOfLinks);

        String filename = "./networks/simulated/RandomBN_" + Integer.toString(numberOfMultinomials) + "D_" + Integer.toString(numberOfGaussians) + "C_" + Integer.toString(seedNetwork) + "_Seed.bn";
        //BayesianNetworkGenerator.generateBNtoFile(numberOfMultinomials,2,numberOfGaussians,numberOfLinks,seedNetwork,filename);
        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();


        //if (Main.VERBOSE) System.out.println(bn.getDAG());
        //if (Main.VERBOSE) System.out.println(bn.toString());


        MPEInference mpeInference = new MPEInference();
        mpeInference.setModel(bn);
        mpeInference.setParallelMode(true);


        //if (Main.VERBOSE) System.out.println("CausalOrder: " + Arrays.toString(Utils.getCausalOrder(mpeInference.getOriginalModel().getDAG()).stream().map(Variable::getName).toArray()));
        List<Variable> modelVariables = Utils.getTopologicalOrder(bn.getDAG());
        if (Main.VERBOSE) System.out.println();



        // Including evidence:
        //double observedVariablesRate = 0.00;
        //Assignment evidence = randomEvidence(seed, observedVariablesRate, bn);
        //mpeInference.setEvidence(evidence);


        mpeInference.setSampleSize(parallelSamples);
        mpeInference.setSeed(seed);
        mpeInference.setNumberOfIterations(numberOfIterations);

        double[] SA_All_prob = new double[repetitions];
        double[] SA_Some_prob = new double[repetitions];
        double[] HC_All_prob = new double[repetitions];
        double[] HC_Some_prob = new double[repetitions];
        double[] sampling_prob = new double[repetitions];

        double[] SA_All_time = new double[repetitions];
        double[] SA_Some_time = new double[repetitions];
        double[] HC_All_time = new double[repetitions];
        double[] HC_Some_time = new double[repetitions];
        double[] sampling_time = new double[repetitions];

        long timeStart;
        long timeStop;
        double execTime;

        Assignment mpeEstimate;

        mpeInference.setParallelMode(true);

        for(int k=0; k<repetitions; k++) {

            mpeInference.setSampleSize(parallelSamples);

            /***********************************************
             *        SIMULATED ANNEALING
             ************************************************/


            // MPE INFERENCE WITH SIMULATED ANNEALING, ALL VARIABLES
            //if (Main.VERBOSE) System.out.println();
            timeStart = System.nanoTime();
            mpeInference.runInference(MPEInference.SearchAlgorithm.SA_GLOBAL);


            //mpeEstimate = mpeInference.getEstimate();
            //if (Main.VERBOSE) System.out.println("MPE estimate (SA.All): " + mpeEstimate.outputString(modelVariables));   //toString(modelVariables)
            //if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            //if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
            //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
            //if (Main.VERBOSE) System.out.println();
            SA_All_prob[k] = mpeInference.getLogProbabilityOfEstimate();
            SA_All_time[k] = execTime;



            // MPE INFERENCE WITH SIMULATED ANNEALING, SOME VARIABLES AT EACH TIME
            timeStart = System.nanoTime();
            mpeInference.runInference(MPEInference.SearchAlgorithm.SA_LOCAL);


            //mpeEstimate = mpeInference.getEstimate();
            //if (Main.VERBOSE) System.out.println("MPE estimate  (SA.Some): " + mpeEstimate.outputString(modelVariables));   //toString(modelVariables)
            //if (Main.VERBOSE) System.out.println("with probability: "+ Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            //if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
            //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
            //if (Main.VERBOSE) System.out.println();
            SA_Some_prob[k] = mpeInference.getLogProbabilityOfEstimate();
            SA_Some_time[k] = execTime;

            /***********************************************
             *        HILL CLIMBING
             ************************************************/


            // MPE INFERENCE WITH HILL CLIMBING, ALL VARIABLES
            timeStart = System.nanoTime();
            mpeInference.runInference(MPEInference.SearchAlgorithm.HC_GLOBAL);

            //mpeEstimate = mpeInference.getEstimate();
            //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
            //if (Main.VERBOSE) System.out.println("MPE estimate (HC.All): " + mpeEstimate.outputString(modelVariables));
            //if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            //if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
            //if (Main.VERBOSE) System.out.println();
            HC_All_prob[k] = mpeInference.getLogProbabilityOfEstimate();
            HC_All_time[k] = execTime;



            //  MPE INFERENCE WITH HILL CLIMBING, ONE VARIABLE AT EACH TIME
            timeStart = System.nanoTime();
            mpeInference.runInference(MPEInference.SearchAlgorithm.HC_LOCAL);


            //mpeEstimate = mpeInference.getEstimate();
            //if (Main.VERBOSE) System.out.println("MPE estimate  (HC.Some): " + mpeEstimate.outputString(modelVariables));   //toString(modelVariables)
            //if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            //if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
            //if (Main.VERBOSE) System.out.println();
            HC_Some_prob[k] = mpeInference.getLogProbabilityOfEstimate();
            HC_Some_time[k] = execTime;

            /***********************************************
             *        SAMPLING AND DETERMINISTIC
             ************************************************/


            // MPE INFERENCE WITH SIMULATION AND PICKING MAX

            mpeInference.setSampleSize(samplingMethodSize);

            timeStart = System.nanoTime();
            mpeInference.runInference(MPEInference.SearchAlgorithm.SAMPLING);

            //mpeEstimate = mpeInference.getEstimate();
            //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
            //if (Main.VERBOSE) System.out.println("MPE estimate (SAMPLING): " + mpeEstimate.outputString(modelVariables));
            //if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            //if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
            //if (Main.VERBOSE) System.out.println();
            sampling_prob[k] = mpeInference.getLogProbabilityOfEstimate();
            sampling_time[k] = execTime;
        }

        double determ_prob=0;
        double determ_time=0;

        if(bn.getNumberOfVars()<=50) {

            // MPE INFERENCE, DETERMINISTIC
            timeStart = System.nanoTime();
            //mpeInference.runInference(-2);

            //mpeEstimate = mpeInference.getEstimate();
            //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
            //if (Main.VERBOSE) System.out.println("MPE estimate (DETERM.): " + mpeEstimate.outputString(modelVariables));
            //if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            //if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
            //if (Main.VERBOSE) System.out.println();

            determ_prob = mpeInference.getLogProbabilityOfEstimate();
            determ_time = execTime;
            determ_prob = -34.64743236365595;
            determ_time=0;

        }
        else {
            if (Main.VERBOSE) System.out.println("Too many variables for deterministic method");
        }

        /***********************************************
         *        DISPLAY OF RESULTS
         ************************************************/

        if (Main.VERBOSE) System.out.println("*** RESULTS ***");

//        if (Main.VERBOSE) System.out.println("SA_All log-probabilities");
//        if (Main.VERBOSE) System.out.println(Arrays.toString(SA_All_prob));
//        if (Main.VERBOSE) System.out.println("SA_Some log-probabilities");
//        if (Main.VERBOSE) System.out.println(Arrays.toString(SA_Some_prob));
//        if (Main.VERBOSE) System.out.println("HC_All log-probabilities");
//        if (Main.VERBOSE) System.out.println(Arrays.toString(HC_All_prob));
//        if (Main.VERBOSE) System.out.println("HC_Some log-probabilities");
//        if (Main.VERBOSE) System.out.println(Arrays.toString(HC_Some_prob));
//        if (Main.VERBOSE) System.out.println("Sampling log-probabilities");
//        if (Main.VERBOSE) System.out.println(Arrays.toString(sampling_prob));
//
//        if(bn.getNumberOfVars()<=50) {
//            if (Main.VERBOSE) System.out.println("Deterministic log-probability");
//            if (Main.VERBOSE) System.out.println(Double.toString(determ_prob));
//        }
//        if (Main.VERBOSE) System.out.println();

        final double determ_prob_FINAL = determ_prob;

//        int SA_All_success = (int) Arrays.stream(SA_All_prob).filter(db -> (db <= determ_prob_FINAL+0.001 && db >=determ_prob_FINAL-0.001)).count();
//        int SA_Some_success = (int) Arrays.stream(SA_Some_prob).filter(db -> (db <= determ_prob_FINAL+0.001 && db >=determ_prob_FINAL-0.001)).count();
//        int HC_All_success = (int) Arrays.stream(HC_All_prob).filter(db -> (db <= determ_prob_FINAL+0.001 && db >=determ_prob_FINAL-0.001)).count();
//        int HC_Some_success = (int) Arrays.stream(HC_Some_prob).filter(db -> (db <= determ_prob_FINAL+0.001 && db >=determ_prob_FINAL-0.001)).count();
//        int sampling_success = (int) Arrays.stream(sampling_prob).filter(db -> (db <= determ_prob_FINAL+0.001 && db >=determ_prob_FINAL-0.001)).count();
//
//        if (Main.VERBOSE) System.out.println("SA_All % success");
//        if (Main.VERBOSE) System.out.println(Double.toString( 100 * SA_All_success/repetitions ));
//        if (Main.VERBOSE) System.out.println("SA_Some % success");
//        if (Main.VERBOSE) System.out.println(Double.toString( 100 * SA_Some_success/repetitions ));
//        if (Main.VERBOSE) System.out.println("HC_All % success");
//        if (Main.VERBOSE) System.out.println(Double.toString( 100 * HC_All_success/repetitions ));
//        if (Main.VERBOSE) System.out.println("HC_Some % success");
//        if (Main.VERBOSE) System.out.println(Double.toString( 100 * HC_Some_success/repetitions ));
//        if (Main.VERBOSE) System.out.println("Sampling % success");
//        if (Main.VERBOSE) System.out.println(Double.toString( 100 * sampling_success/repetitions ));
//        if (Main.VERBOSE) System.out.println();

        if (Main.VERBOSE) System.out.println("SA_All RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString(Arrays.stream(SA_All_prob).map(value -> Math.pow(value - determ_prob_FINAL, 2)).average().getAsDouble()) );
        if (Main.VERBOSE) System.out.println("SA_Some RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString( Arrays.stream(SA_Some_prob).map(value -> Math.pow(value-determ_prob_FINAL,2)).average().getAsDouble()) );
        if (Main.VERBOSE) System.out.println("HC_All RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString( Arrays.stream(HC_All_prob).map(value -> Math.pow(value-determ_prob_FINAL,2)).average().getAsDouble()) );
        if (Main.VERBOSE) System.out.println("HC_Some RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString( Arrays.stream(HC_Some_prob).map(value -> Math.pow(value-determ_prob_FINAL,2)).average().getAsDouble()) );
        if (Main.VERBOSE) System.out.println("Sampling RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString( Arrays.stream(sampling_prob).map(value -> Math.pow(value-determ_prob_FINAL,2)).average().getAsDouble()) );
        if (Main.VERBOSE) System.out.println();


        if (Main.VERBOSE) System.out.println("SA_All times");
        //if (Main.VERBOSE) System.out.println(Arrays.toString(SA_All_time));
        if (Main.VERBOSE) System.out.println("Mean time: " + Double.toString(Arrays.stream(SA_All_time).average().getAsDouble()));
        if (Main.VERBOSE) System.out.println("SA_Some times");
        //if (Main.VERBOSE) System.out.println(Arrays.toString(SA_Some_time));
        if (Main.VERBOSE) System.out.println("Mean time: " + Double.toString(Arrays.stream(SA_Some_time).average().getAsDouble()));
        if (Main.VERBOSE) System.out.println("HC_All times");
        //if (Main.VERBOSE) System.out.println(Arrays.toString(HC_All_time));
        if (Main.VERBOSE) System.out.println("Mean time: " + Double.toString(Arrays.stream(HC_All_time).average().getAsDouble()));
        if (Main.VERBOSE) System.out.println("HC_Some times");
        //if (Main.VERBOSE) System.out.println(Arrays.toString(HC_Some_time));
        if (Main.VERBOSE) System.out.println("Mean time: " + Double.toString(Arrays.stream(HC_Some_time).average().getAsDouble()));
        if (Main.VERBOSE) System.out.println("Sampling times");
        //if (Main.VERBOSE) System.out.println(Arrays.toString(sampling_time));
        if (Main.VERBOSE) System.out.println("Mean time: " + Double.toString(Arrays.stream(sampling_time).average().getAsDouble()));

        if(bn.getNumberOfVars()<=50) {
            if (Main.VERBOSE) System.out.println("Deterministic time");
            if (Main.VERBOSE) System.out.println(Double.toString(determ_time));
            if (Main.VERBOSE) System.out.println("and probability");
            if (Main.VERBOSE) System.out.println(determ_prob);
        }


    }
}
