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
public class MPEInferenceExperiments_Deliv2 {

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

        int seedNetwork = 234235;
        int numberOfGaussians = 100;
        int numberOfMultinomials = 100;

        int seed = 125634 ;

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

        mpeInference.setNumberOfIterations(numberOfIterations);

        mpeInference.setSampleSize(parallelSamples);
        mpeInference.setSeed(seed);

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

        Assignment bestMpeEstimate=new HashMapAssignment(bn.getNumberOfVars());
        double bestMpeEstimateLogProb=-100000;
        int bestMpeEstimateMethod=-5;

        mpeInference.setParallelMode(true);


        final double bestProbability = -171.81983739975342;
//        BEST MPE ESTIMATE FOUND:
//        {DiscreteVar0 = 0, DiscreteVar1 = 0, DiscreteVar2 = 1, DiscreteVar3 = 0, DiscreteVar4 = 0, DiscreteVar5 = 0, DiscreteVar6 = 0, DiscreteVar7 = 0, DiscreteVar8 = 0, DiscreteVar9 = 0, DiscreteVar10 = 0, DiscreteVar11 = 1, DiscreteVar12 = 1, DiscreteVar13 = 1, DiscreteVar14 = 0, DiscreteVar15 = 0, DiscreteVar16 = 0, DiscreteVar17 = 1, DiscreteVar18 = 1, DiscreteVar19 = 0, DiscreteVar20 = 0, DiscreteVar21 = 0, DiscreteVar22 = 1, DiscreteVar23 = 1, DiscreteVar24 = 0, DiscreteVar25 = 0, DiscreteVar26 = 0, DiscreteVar27 = 0, DiscreteVar28 = 1, DiscreteVar29 = 1, DiscreteVar30 = 0, DiscreteVar31 = 0, DiscreteVar32 = 1, DiscreteVar33 = 1, DiscreteVar34 = 0, DiscreteVar35 = 1, DiscreteVar36 = 0, DiscreteVar37 = 0, DiscreteVar38 = 0, DiscreteVar39 = 0, DiscreteVar40 = 0, DiscreteVar41 = 1, DiscreteVar42 = 1, DiscreteVar43 = 1, DiscreteVar44 = 0, DiscreteVar45 = 1, DiscreteVar46 = 1, DiscreteVar47 = 0, DiscreteVar48 = 1, DiscreteVar49 = 1, DiscreteVar50 = 0, DiscreteVar51 = 0, DiscreteVar52 = 0, DiscreteVar53 = 1, DiscreteVar54 = 0, DiscreteVar55 = 1, DiscreteVar56 = 1, DiscreteVar57 = 0, DiscreteVar58 = 1, DiscreteVar59 = 0, DiscreteVar60 = 0, DiscreteVar61 = 1, DiscreteVar62 = 0, DiscreteVar63 = 0, DiscreteVar64 = 0, DiscreteVar65 = 1, DiscreteVar66 = 1, DiscreteVar67 = 1, DiscreteVar68 = 1, DiscreteVar69 = 1, DiscreteVar70 = 1, DiscreteVar71 = 0, DiscreteVar72 = 0, DiscreteVar73 = 0, DiscreteVar74 = 0, DiscreteVar75 = 1, DiscreteVar76 = 0, DiscreteVar77 = 1, DiscreteVar78 = 1, DiscreteVar79 = 0, DiscreteVar80 = 1, DiscreteVar81 = 1, DiscreteVar82 = 1, DiscreteVar83 = 0, DiscreteVar84 = 1, DiscreteVar85 = 1, DiscreteVar86 = 1, DiscreteVar87 = 1, DiscreteVar88 = 0, DiscreteVar89 = 0, DiscreteVar90 = 1, DiscreteVar91 = 0, DiscreteVar92 = 0, DiscreteVar93 = 0, DiscreteVar94 = 0, DiscreteVar95 = 0, DiscreteVar96 = 0, DiscreteVar97 = 1, DiscreteVar98 = 1, DiscreteVar99 = 1, GaussianVar0 = -4,551, GaussianVar1 = 14,731, GaussianVar2 = -1,108, GaussianVar3 = -6,564, GaussianVar4 = -2,415, GaussianVar5 = 10,265, GaussianVar6 = 6,058, GaussianVar7 = 6,367, GaussianVar8 = 26,731, GaussianVar9 = 0,807, GaussianVar10 = -19,410, GaussianVar11 = 18,070, GaussianVar12 = -14,177, GaussianVar13 = 7,765, GaussianVar14 = 3,596, GaussianVar15 = -7,757, GaussianVar16 = -1,705, GaussianVar17 = -5,476, GaussianVar18 = -17,932, GaussianVar19 = 22,843, GaussianVar20 = -9,860, GaussianVar21 = 3,844, GaussianVar22 = 8,262, GaussianVar23 = -9,080, GaussianVar24 = 1,750, GaussianVar25 = 11,532, GaussianVar26 = 0,700, GaussianVar27 = 12,206, GaussianVar28 = 8,532, GaussianVar29 = -40,395, GaussianVar30 = 19,981, GaussianVar31 = -30,713, GaussianVar32 = 0,476, GaussianVar33 = -12,406, GaussianVar34 = 4,942, GaussianVar35 = -0,245, GaussianVar36 = -176,861, GaussianVar37 = 8,474, GaussianVar38 = -8,849, GaussianVar39 = -3,844, GaussianVar40 = -8,495, GaussianVar41 = 4,664, GaussianVar42 = -4,730, GaussianVar43 = 4,063, GaussianVar44 = -1,631, GaussianVar45 = -103,340, GaussianVar46 = -1,598, GaussianVar47 = -11,460, GaussianVar48 = 14,123, GaussianVar49 = -0,135, GaussianVar50 = 1,487, GaussianVar51 = -4,859, GaussianVar52 = 0,370, GaussianVar53 = -10,038, GaussianVar54 = 18,145, GaussianVar55 = 225,324, GaussianVar56 = 1,059, GaussianVar57 = -1,170, GaussianVar58 = 83,480, GaussianVar59 = 7,375, GaussianVar60 = 5,091, GaussianVar61 = 61,381, GaussianVar62 = 42,955, GaussianVar63 = -712,533, GaussianVar64 = 21,460, GaussianVar65 = -19,337, GaussianVar66 = 213,903, GaussianVar67 = -10,197, GaussianVar68 = -65,619, GaussianVar69 = 41,045, GaussianVar70 = 133,452, GaussianVar71 = -1,997, GaussianVar72 = 17,485, GaussianVar73 = -40,691, GaussianVar74 = -16,378, GaussianVar75 = -72,550, GaussianVar76 = -1,761, GaussianVar77 = 12,647, GaussianVar78 = -31,531, GaussianVar79 = -41,444, GaussianVar80 = -14,190, GaussianVar81 = 17,387, GaussianVar82 = -12,333, GaussianVar83 = -57,795, GaussianVar84 = -20,386, GaussianVar85 = 49,735, GaussianVar86 = 14,593, GaussianVar87 = -168,778, GaussianVar88 = -6,157, GaussianVar89 = 82,897, GaussianVar90 = -30,018, GaussianVar91 = -2,366, GaussianVar92 = -12,753, GaussianVar93 = -141,490, GaussianVar94 = 17,844, GaussianVar95 = 99,703, GaussianVar96 = -37,859, GaussianVar97 = 123,045, GaussianVar98 = -4,054, GaussianVar99 = 3,024}
//        with method:2
//        and log probability: -171.81983739975342


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

            if(mpeInference.getLogProbabilityOfEstimate() > bestMpeEstimateLogProb) {
                bestMpeEstimate = mpeInference.getEstimate();
                bestMpeEstimateLogProb = mpeInference.getLogProbabilityOfEstimate();
                bestMpeEstimateMethod=1;
            }


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

            if(mpeInference.getLogProbabilityOfEstimate() > bestMpeEstimateLogProb) {
                bestMpeEstimate = mpeInference.getEstimate();
                bestMpeEstimateLogProb = mpeInference.getLogProbabilityOfEstimate();
                bestMpeEstimateMethod=0;
            }

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

            if(mpeInference.getLogProbabilityOfEstimate() > bestMpeEstimateLogProb) {
                bestMpeEstimate = mpeInference.getEstimate();
                bestMpeEstimateLogProb = mpeInference.getLogProbabilityOfEstimate();
                bestMpeEstimateMethod=3;
            }

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

            if(mpeInference.getLogProbabilityOfEstimate() > bestMpeEstimateLogProb) {
                bestMpeEstimate = mpeInference.getEstimate();
                bestMpeEstimateLogProb = mpeInference.getLogProbabilityOfEstimate();
                bestMpeEstimateMethod=2;
            }

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

            if(mpeInference.getLogProbabilityOfEstimate() > bestMpeEstimateLogProb) {
                bestMpeEstimate = mpeInference.getEstimate();
                bestMpeEstimateLogProb = mpeInference.getLogProbabilityOfEstimate();
                bestMpeEstimateMethod=-1;
            }
        }

        double determ_prob=0;
        double determ_time=0;

//        if(bn.getNumberOfVars()<=50) {
//
//            // MPE INFERENCE, DETERMINISTIC
//            timeStart = System.nanoTime();
//            mpeInference.runInference(-2);
//
//            //mpeEstimate = mpeInference.getEstimate();
//            //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
//            //if (Main.VERBOSE) System.out.println("MPE estimate (DETERM.): " + mpeEstimate.outputString(modelVariables));
//            //if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
//            timeStop = System.nanoTime();
//            execTime = (double) (timeStop - timeStart) / 1000000000.0;
//            //if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//            //if (Main.VERBOSE) System.out.println();
//            determ_prob = mpeInference.getLogProbabilityOfEstimate();
//            determ_time = execTime;
//
//        }
//        else {
//            if (Main.VERBOSE) System.out.println("Too many variables for deterministic method");
//        }

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
//        if(bn.getNumberOfVars()<=50) {
//            if (Main.VERBOSE) System.out.println("Deterministic log-probability");
//            if (Main.VERBOSE) System.out.println(Double.toString(determ_prob));
//        }

        if (Main.VERBOSE) System.out.println("SA_All RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString( Math.sqrt(Arrays.stream(SA_All_prob).map(value -> Math.pow(value - bestProbability, 2)).average().getAsDouble())) );
        if (Main.VERBOSE) System.out.println("SA_Some RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString( Math.sqrt(Arrays.stream(SA_Some_prob).map(value -> Math.pow(value - bestProbability, 2)).average().getAsDouble())) );
        if (Main.VERBOSE) System.out.println("HC_All RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString( Math.sqrt(Arrays.stream(HC_All_prob).map(value -> Math.pow(value - bestProbability, 2)).average().getAsDouble())) );
        if (Main.VERBOSE) System.out.println("HC_Some RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString( Math.sqrt(Arrays.stream(HC_Some_prob).map(value -> Math.pow(value - bestProbability, 2)).average().getAsDouble())) );
        if (Main.VERBOSE) System.out.println("Sampling RMS probabilities");
        if (Main.VERBOSE) System.out.println(Double.toString( Math.sqrt(Arrays.stream(sampling_prob).map(value -> Math.pow(value - bestProbability, 2)).average().getAsDouble())) );
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
        if (Main.VERBOSE) System.out.println();
//        if(bn.getNumberOfVars()<=50) {
//            if (Main.VERBOSE) System.out.println("Deterministic time");
//            if (Main.VERBOSE) System.out.println(Double.toString(determ_time));
//        }

        if (Main.VERBOSE) System.out.println("BEST MPE ESTIMATE FOUND:");
        if (Main.VERBOSE) System.out.println(bestMpeEstimate.outputString(Utils.getTopologicalOrder(bn.getDAG())));
        if (Main.VERBOSE) System.out.println("with method:" + bestMpeEstimateMethod);
        if (Main.VERBOSE) System.out.println("and log probability: " + bestMpeEstimateLogProb);
    }
}
