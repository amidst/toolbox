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
public class MPEInferenceExperiments {

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
    public static void main(String[] args) throws Exception {

        int seedNetwork = 61236719 + 123;

        int numberOfGaussians = 20;

        int numberOfMultinomials = numberOfGaussians;
        BayesianNetworkGenerator.setSeed(seedNetwork);
        BayesianNetworkGenerator.setNumberOfGaussianVars(numberOfGaussians);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(numberOfMultinomials, 2);
        BayesianNetworkGenerator.setNumberOfLinks((int) 1.3 * (numberOfGaussians + numberOfMultinomials));

        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();


        int seed = seedNetwork + 231591;

        //if (Main.VERBOSE) System.out.println(bn.getDAG());
        if (Main.VERBOSE) System.out.println(bn.toString());


        MPEInference mpeInference = new MPEInference();
        mpeInference.setModel(bn);
        mpeInference.setParallelMode(true);


        if (Main.VERBOSE) System.out.println("CausalOrder: " + Arrays.toString(Utils.getTopologicalOrder(mpeInference.getOriginalModel().getDAG()).stream().map(Variable::getName).toArray()));
        List<Variable> modelVariables = Utils.getTopologicalOrder(bn.getDAG());
        if (Main.VERBOSE) System.out.println();



        // Including evidence:
        //double observedVariablesRate = 0.00;
        //Assignment evidence = randomEvidence(seed, observedVariablesRate, bn);
        //mpeInference.setEvidence(evidence);


        int parallelSamples = 100;
        int samplingMethodSize = 10000;


        mpeInference.setSampleSize(parallelSamples);



        /***********************************************
         *        SIMULATED ANNEALING
         ************************************************/


        // MPE INFERENCE WITH SIMULATED ANNEALING, ALL VARIABLES
        if (Main.VERBOSE) System.out.println();
        long timeStart = System.nanoTime();
        mpeInference.runInference(MPEInference.SearchAlgorithm.SA_GLOBAL);


        Assignment mpeEstimate = mpeInference.getEstimate();
        if (Main.VERBOSE) System.out.println("MPE estimate (SA.All): " + mpeEstimate.outputString(modelVariables));   //toString(modelVariables)
        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        long timeStop = System.nanoTime();
        double execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        if (Main.VERBOSE) System.out.println();




        // MPE INFERENCE WITH SIMULATED ANNEALING, SOME VARIABLES AT EACH TIME
        timeStart = System.nanoTime();
        mpeInference.runInference(MPEInference.SearchAlgorithm.SA_LOCAL);


        mpeEstimate = mpeInference.getEstimate();
        if (Main.VERBOSE) System.out.println("MPE estimate  (SA.Some): " + mpeEstimate.outputString(modelVariables));   //toString(modelVariables)
        if (Main.VERBOSE) System.out.println("with probability: "+ Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        if (Main.VERBOSE) System.out.println();


        /***********************************************
         *        HILL CLIMBING
         ************************************************/


        // MPE INFERENCE WITH HILL CLIMBING, ALL VARIABLES
        timeStart = System.nanoTime();
        mpeInference.runInference(MPEInference.SearchAlgorithm.HC_GLOBAL);

        mpeEstimate = mpeInference.getEstimate();
        //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
        if (Main.VERBOSE) System.out.println("MPE estimate (HC.All): " + mpeEstimate.outputString(modelVariables));
        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        if (Main.VERBOSE) System.out.println();




        //  MPE INFERENCE WITH HILL CLIMBING, ONE VARIABLE AT EACH TIME
        timeStart = System.nanoTime();
        mpeInference.runInference(MPEInference.SearchAlgorithm.HC_LOCAL);


        mpeEstimate = mpeInference.getEstimate();
        if (Main.VERBOSE) System.out.println("MPE estimate  (HC.Some): " + mpeEstimate.outputString(modelVariables));   //toString(modelVariables)
        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        if (Main.VERBOSE) System.out.println();


        /***********************************************
         *        SAMPLING AND DETERMINISTIC
         ************************************************/


        // MPE INFERENCE WITH SIMULATION AND PICKING MAX

        mpeInference.setSampleSize(samplingMethodSize);

        timeStart = System.nanoTime();
        mpeInference.runInference(MPEInference.SearchAlgorithm.SAMPLING);

        mpeEstimate = mpeInference.getEstimate();
        //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
        if (Main.VERBOSE) System.out.println("MPE estimate (SAMPLING): " + mpeEstimate.outputString(modelVariables));
        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        if (Main.VERBOSE) System.out.println();


        if(bn.getNumberOfVars()<=50) {

            // MPE INFERENCE, DETERMINISTIC
            timeStart = System.nanoTime();
            mpeInference.runInference(MPEInference.SearchAlgorithm.EXHAUSTIVE);

            mpeEstimate = mpeInference.getEstimate();
            //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
            if (Main.VERBOSE) System.out.println("MPE estimate (DETERM.): " + mpeEstimate.outputString(modelVariables));
            if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
            timeStop = System.nanoTime();
            execTime = (double) (timeStop - timeStart) / 1000000000.0;
            if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
            if (Main.VERBOSE) System.out.println();

        }
    }
}
