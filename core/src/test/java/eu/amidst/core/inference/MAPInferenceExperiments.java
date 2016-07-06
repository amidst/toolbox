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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by dario on 01/06/15.
 */
public class MAPInferenceExperiments {

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


        String filename=""; //Filename with the Bayesian Network
        //filename = "networks/randomlyGeneratedBN.bn";
        //BayesianNetworkGenerator.generateBNtoFile(nDiscrete, nStates, nContin, nLinks, seedNetwork, filename);
        //BayesianNetwork bn = BayesianNetworkLoader.loadFromFile(filename);



//        int seedNetwork = 61236719 + 123;
//
//        int nDiscrete = 20;
//        int nStates = 2;
//        int nContin = 0;
//        int nLinks = (int)Math.round(1.3*(nDiscrete+nContin));
//
//
//        BayesianNetworkGenerator.setSeed(seedNetwork);
//        BayesianNetworkGenerator.setNumberOfGaussianVars(nContin);
//        BayesianNetworkGenerator.setNumberOfMultinomialVars(nDiscrete, nStates);
//        BayesianNetworkGenerator.setNumberOfLinks(nLinks);
//
//        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
//
//
//        int seed = seedNetwork + 2315;
//
//
//        if (Main.VERBOSE) System.out.println(bn.getDAG());
//
//        if (Main.VERBOSE) System.out.println(bn.toString());
//
//
//
//        MAPInference mapInference = new MAPInference();
//        mapInference.setModel(bn);
//        mapInference.setParallelMode(true);
//        mapInference.setSampleSize(1);
//
//        List<Variable> causalOrder = Utils.getTopologicalOrder(mapInference.getOriginalModel().getDAG());
//
//        if (Main.VERBOSE) System.out.println("CausalOrder: " + Arrays.toString(Utils.getTopologicalOrder(mapInference.getOriginalModel().getDAG()).stream().map(Variable::getName).toArray()));
//        if (Main.VERBOSE) System.out.println();
//
//
//
//        int parallelSamples=20;
//        int samplingMethodSize=50000;
//        mapInference.setSampleSize(parallelSamples);
//
//
//
//        long timeStart;
//        long timeStop;
//        double execTime;
//        Assignment mapEstimate;
//
//
//        /***********************************************
//         *        INCLUDING EVIDENCE
//         ************************************************/
//
//        double observedVariablesRate = 0.05;
//        Assignment evidence = randomEvidence(seed, observedVariablesRate, bn);
//
//        mapInference.setEvidence(evidence);
//        //if (Main.VERBOSE) System.out.println(evidence.outputString());
//
//
//
//        /***********************************************
//         *        VARIABLES OF INTEREST
//         ************************************************/
//
//        Variable varInterest1 = causalOrder.get(6);
//        Variable varInterest2 = causalOrder.get(7);
//
//
//        List<Variable> varsInterest = new ArrayList<>();
//        varsInterest.add(varInterest1);
//        varsInterest.add(varInterest2);
//        mapInference.setMAPVariables(varsInterest);
//
//        if (Main.VERBOSE) System.out.println("MAP Variables of Interest: " + Arrays.toString(varsInterest.stream().map(Variable::getName).toArray()));
//        if (Main.VERBOSE) System.out.println();
//
//
//
//
//        /***********************************************
//         *        SIMULATED ANNEALING
//         ************************************************/
//
//
//        // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME
//        timeStart = System.nanoTime();
//        mapInference.runInference(1);
//
//        mapEstimate = mapInference.getMAPestimate();
//        if (Main.VERBOSE) System.out.println("MAP estimate  (SA.All): " + mapEstimate.outputString(varsInterest));
//        if (Main.VERBOSE) System.out.println("with (unnormalized) probability: " + mapInference.getMAPestimateProbability());
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
//        if (Main.VERBOSE) System.out.println();
//
//
//        // MAP INFERENCE WITH SIMULATED ANNEALING, SOME VARIABLES EACH TIME
//        timeStart = System.nanoTime();
//        mapInference.runInference(0);
//
//        mapEstimate = mapInference.getMAPestimate();
//        if (Main.VERBOSE) System.out.println("MAP estimate  (SA.Some): " + mapEstimate.outputString(varsInterest));
//        if (Main.VERBOSE) System.out.println("with (unnormalized) probability: " + mapInference.getMAPestimateProbability());
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
//        if (Main.VERBOSE) System.out.println();
//
//
//        /***********************************************
//         *        HILL CLIMBING
//         ************************************************/
//
//        //  MAP INFERENCE WITH HILL CLIMBING, MOVING ALL VARIABLES EACH TIME
//        timeStart = System.nanoTime();
//        mapInference.runInference(3);
//
//        mapEstimate = mapInference.getMAPestimate();
//        if (Main.VERBOSE) System.out.println("MAP estimate  (HC.All): " + mapEstimate.outputString(varsInterest));
//        if (Main.VERBOSE) System.out.println("with (unnormalized) probability: " + mapInference.getMAPestimateProbability());
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        if (Main.VERBOSE) System.out.println();
//
//
//
//        //  MAP INFERENCE WITH HILL CLIMBING, SOME VARIABLES EACH TIME
//        timeStart = System.nanoTime();
//        mapInference.runInference(2);
//
//        mapEstimate = mapInference.getMAPestimate();
//        if (Main.VERBOSE) System.out.println("MAP estimate  (HC.Some): " + mapEstimate.outputString(varsInterest));
//        if (Main.VERBOSE) System.out.println("with (unnormalized) probability: " + mapInference.getMAPestimateProbability());
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        if (Main.VERBOSE) System.out.println();
//
//
//        /***********************************************
//         *        SAMPLING
//         ************************************************/
//
//        // MAP INFERENCE WITH SIMULATION AND PICKING MAX
//        mapInference.setSampleSize(samplingMethodSize);
//        timeStart = System.nanoTime();
//        mapInference.runInference(-1);
//
//        mapEstimate = mapInference.getMAPestimate();
//        if (Main.VERBOSE) System.out.println("MAP estimate (SAMPLING): " + mapEstimate.outputString(varsInterest));
//        if (Main.VERBOSE) System.out.println("with probability: " + mapInference.getMAPestimateProbability());
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        if (Main.VERBOSE) System.out.println();
//
//
//
//
//
//
//        // PROBABILITIES OF INDIVIDUAL CONFIGURATIONS
//
//
//        double s1 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
//        if (Main.VERBOSE) System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s1);
//
//        mapEstimate.setValue(varInterest2, 1);
//        double s2 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
//        if (Main.VERBOSE) System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s2);
//
//        mapEstimate.setValue(varInterest1, 1);
//        mapEstimate.setValue(varInterest2, 0);
//        double s3 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
//        if (Main.VERBOSE) System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s3);
//
//        mapEstimate.setValue(varInterest2, 1);
//        double s4 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
//        if (Main.VERBOSE) System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s4);
//
//        double sumNonStateless = s1+s2+s3+s4;
//
//        if (Main.VERBOSE) System.out.println();
//        if (Main.VERBOSE) System.out.println("Sum = " + sumNonStateless + "; Normalized probs: [V1=0,V2=0]=" + s1/sumNonStateless + ", [V1=0,V2=1]=" + s2/sumNonStateless + ", [V1=1,V2=0]=" + s3/sumNonStateless + ", [V1=1,V2=1]=" + s4/sumNonStateless );





























        int seedNetwork = 1253473;

        int nDiscrete = 50;
        int nStates = 2;
        int nContin = 50;
        int nLinks = (int)Math.round(1.3*(nDiscrete+nContin));


        BayesianNetworkGenerator.setSeed(seedNetwork);
        BayesianNetworkGenerator.setNumberOfGaussianVars(nContin);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(nDiscrete, nStates);
        BayesianNetworkGenerator.setNumberOfLinks(nLinks);

        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();


        int seed = seedNetwork + 23715;


        if (Main.VERBOSE) System.out.println(bn.getDAG());

        if (Main.VERBOSE) System.out.println(bn.toString());



        MAPInference mapInference = new MAPInference();
        mapInference.setModel(bn);
        mapInference.setParallelMode(true);
        mapInference.setSampleSize(1);

        List<Variable> causalOrder = Utils.getTopologicalOrder(mapInference.getOriginalModel().getDAG());

        if (Main.VERBOSE) System.out.println("CausalOrder: " + Arrays.toString(Utils.getTopologicalOrder(mapInference.getOriginalModel().getDAG()).stream().map(Variable::getName).toArray()));
        if (Main.VERBOSE) System.out.println();



        int parallelSamples=20;
        int samplingMethodSize=100000;
        mapInference.setSampleSize(parallelSamples);



        long timeStart;
        long timeStop;
        double execTime;
        Assignment mapEstimate;


        /***********************************************
         *        INCLUDING EVIDENCE
         ************************************************/

        double observedVariablesRate = 0.05;
        Assignment evidence = randomEvidence(seed, observedVariablesRate, bn);

        mapInference.setEvidence(evidence);
        //if (Main.VERBOSE) System.out.println(evidence.outputString());



        /***********************************************
         *        VARIABLES OF INTEREST
         ************************************************/

        Variable varInterest1 = causalOrder.get(6);
        Variable varInterest2 = causalOrder.get(7);
        Variable varInterest3 = causalOrder.get(60);

        List<Variable> varsInterest = new ArrayList<>();
        varsInterest.add(varInterest1);
        varsInterest.add(varInterest2);
        varsInterest.add(varInterest3);
        mapInference.setMAPVariables(varsInterest);

        if (Main.VERBOSE) System.out.println("MAP Variables of Interest: " + Arrays.toString(varsInterest.stream().map(Variable::getName).toArray()));
        if (Main.VERBOSE) System.out.println();




        /***********************************************
         *        SIMULATED ANNEALING
         ************************************************/


        // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInference.SearchAlgorithm.SA_GLOBAL);

        mapEstimate = mapInference.getEstimate();
        if (Main.VERBOSE) System.out.println("MAP estimate  (SA.All): " + mapEstimate.outputString(varsInterest));
        if (Main.VERBOSE) System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        if (Main.VERBOSE) System.out.println();


        // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInference.SearchAlgorithm.SA_LOCAL);

        mapEstimate = mapInference.getEstimate();
        if (Main.VERBOSE) System.out.println("MAP estimate  (SA.Some): " + mapEstimate.outputString(varsInterest));
        if (Main.VERBOSE) System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        if (Main.VERBOSE) System.out.println();


        /***********************************************
         *        HILL CLIMBING
         ************************************************/

        //  MAP INFERENCE WITH HILL CLIMBING, MOVING ALL VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInference.SearchAlgorithm.HC_GLOBAL);

        mapEstimate = mapInference.getEstimate();
        if (Main.VERBOSE) System.out.println("MAP estimate  (HC.All): " + mapEstimate.outputString(varsInterest));
        if (Main.VERBOSE) System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        if (Main.VERBOSE) System.out.println();



        //  MAP INFERENCE WITH HILL CLIMBING, MOVING SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInference.SearchAlgorithm.HC_LOCAL);

        mapEstimate = mapInference.getEstimate();
        if (Main.VERBOSE) System.out.println("MAP estimate  (HC.Some): " + mapEstimate.outputString(varsInterest));
        if (Main.VERBOSE) System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        if (Main.VERBOSE) System.out.println();


        /***********************************************
         *        SAMPLING
         ************************************************/

        // MAP INFERENCE WITH SIMULATION AND PICKING MAX
        mapInference.setSampleSize(samplingMethodSize);
        timeStart = System.nanoTime();
        mapInference.runInference(MAPInference.SearchAlgorithm.SAMPLING);

        mapEstimate = mapInference.getEstimate();
        if (Main.VERBOSE) System.out.println("MAP estimate (SAMPLING): " + mapEstimate.outputString(varsInterest));
        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        if (Main.VERBOSE) System.out.println();






        // PROBABILITIES OF INDIVIDUAL CONFIGURATIONS


        double s1 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
        if (Main.VERBOSE) System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s1);

        mapEstimate.setValue(varInterest2, 1);
        double s2 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
        if (Main.VERBOSE) System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s2);

        mapEstimate.setValue(varInterest1, 1);
        mapEstimate.setValue(varInterest2, 0);
        double s3 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
        if (Main.VERBOSE) System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s3);

        mapEstimate.setValue(varInterest2, 1);
        double s4 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
        if (Main.VERBOSE) System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s4);

        double sum = s1+s2+s3+s4;

        if (Main.VERBOSE) System.out.println();
        if (Main.VERBOSE) System.out.println("Sum = " + sum + "; Normalized probs: [V1=0,V2=0]=" + s1/sum + ", [V1=0,V2=1]=" + s2/sum + ", [V1=1,V2=0]=" + s3/sum + ", [V1=1,V2=1]=" + s4/sum );































//        long timeStart = System.nanoTime();
//        mapInference.runInference(1);
//
//        Assignment mapEstimate1 = mapInference.getMAPestimate();
//        List<Variable> modelVariables = mapInference.getOriginalModel().getVariables().getListOfVariables();
//        if (Main.VERBOSE) System.out.println("MAP estimate: " + mapEstimate1.toString()); //toString(modelVariables);
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate1)));
//        long timeStop = System.nanoTime();
//        double execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getVariables().iterator().));
//
//
//
//
//        // MAP INFERENCE WITH A BIG SAMPLE TO CHECK
//        mapInference.setSampleSize(50000);
//        timeStart = System.nanoTime();
//        mapInference.runInference(-1);
//
//        Assignment mapEstimate2 = mapInference.getMAPestimate();
//        modelVariables = mapInference.getOriginalModel().getVariables().getListOfVariables();
//        if (Main.VERBOSE) System.out.println("MAP estimate (huge sample): " + mapEstimate2.toString());
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate2)));
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//
//
//
//        // DETERMINISTIC SEQUENTIAL SEARCH ON DISCRETE VARIABLES
//        timeStart = System.nanoTime();
//        mapInference.runInference(-2);
//
//        Assignment mapEstimate3 = mapInference.getMAPestimate();
//        modelVariables = mapInference.getOriginalModel().getVariables().getListOfVariables();
//        if (Main.VERBOSE) System.out.println("MAP estimate (sequential): " + mapEstimate3.toString());
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate3)));
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//
//        if (Main.VERBOSE) System.out.println();
//        if (Main.VERBOSE) System.out.println();
//        if (Main.VERBOSE) System.out.println();
//        if (Main.VERBOSE) System.out.println();

        //mapInference.changeCausalOrder(bn,evidence);

        /*// AD-HOC MAP
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar0"),-0.11819702417804305);
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar1"),-1.706);
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar2"),4.95);
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar3"),14.33);
        mapEstimate.setValue(bn.getVariables().getVariableByName("GaussianVar4"),11.355);

        modelVariables = mapInference.getOriginalModel().getVariables().getListOfParamaterVariables();
        if (Main.VERBOSE) System.out.println("Other estimate: " + mapEstimate.toString(modelVariables));
        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        */





























//
//
//
//
//
//        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/randomlyGeneratedBN.bn");
//        //BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/asia.bn");
//        if (Main.VERBOSE) System.out.println(bn.getDAG());
//
//        if (Main.VERBOSE) System.out.println(bn.toString());
//
//
//        MAPInference mapInference = new MAPInference();
//        mapInference.setModel(bn);
//        mapInference.setParallelMode(true);
//
//
//        if (Main.VERBOSE) System.out.println("CausalOrder: " + Arrays.toString(mapInference.causalOrder.stream().map(v -> v.getName()).toArray()));
//        if (Main.VERBOSE) System.out.println();
//
//        // Including evidence:
//        Variable variable1 = mapInference.causalOrder.get(1);  // causalOrder: A, S, L, T, E, X, B, D
//        Variable variable2 = mapInference.causalOrder.get(2);
//        //Variable variable3 = mapInference.causalOrder.get(11);
//        Variable variable3 = mapInference.causalOrder.get(4);
//
//        int var1value=0;
//        int var2value=1;
//        //double var3value=1.27;
//        int var3value=1;
//
//        if (Main.VERBOSE) System.out.println("Evidence: Variable " + variable1.getName() + " = " + var1value + ", Variable " + variable2.getName() + " = " + var2value + " and Variable " + variable3.getName() + " = " + var3value);
//        if (Main.VERBOSE) System.out.println();
//
//        HashMapAssignment evidenceAssignment = new HashMapAssignment(3);
//
//        evidenceAssignment.setValue(variable1,var1value);
//        evidenceAssignment.setValue(variable2,var2value);
//        evidenceAssignment.setValue(variable3,var3value);
//
//        mapInference.setEvidence(evidenceAssignment);
//
//        List<Variable> modelVariables = mapInference.getOriginalModel().getVariables().getListOfVariables();
//        //if (Main.VERBOSE) System.out.println(evidenceAssignment.outputString(modelVariables));
//
//
//
//        long timeStart;
//        long timeStop;
//        double execTime;
//        Assignment mapEstimate;
//
//
//        /*
//        // MAP INFERENCE WITH A SMALL SAMPLE AND SIMULATED ANNEALING
//        mapInference.setSampleSize(100);
//        timeStart = System.nanoTime();
//        mapInference.runInference(1);
//
//
//        Assignment mapEstimate = mapInference.getMAPestimate();
//        if (Main.VERBOSE) System.out.println("MAP estimate (SA): " + mapEstimate.outputString(modelVariables));   //toString(modelVariables)
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
//        if (Main.VERBOSE) System.out.println();
//
//
//        // MAP INFERENCE WITH A BIG SAMPLE  AND SIMULATED ANNEALING
//        mapInference.setSampleSize(100);
//        timeStart = System.nanoTime();
//        mapInference.runInference(1);
//
//        mapEstimate = mapInference.getMAPestimate();
//        modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
//        if (Main.VERBOSE) System.out.println("MAP estimate (SA): " + mapEstimate.outputString(modelVariables));
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        if (Main.VERBOSE) System.out.println();
//
//
//
//
//        // MAP INFERENCE WITH A BIG SAMPLE AND SIMULATED ANNEALING ON ONE VARIABLE EACH TIME
//        mapInference.setSampleSize(100);
//        timeStart = System.nanoTime();
//        mapInference.runInference(0);
//
//
//        mapEstimate = mapInference.getMAPestimate();
//        if (Main.VERBOSE) System.out.println("MAP estimate  (SA.1V): " + mapEstimate.outputString(modelVariables));   //toString(modelVariables)
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
//        if (Main.VERBOSE) System.out.println();
//
//
//
//
//        // MAP INFERENCE WITH A BIG SAMPLE  AND HILL CLIMBING
//        mapInference.setSampleSize(100);
//        timeStart = System.nanoTime();
//        mapInference.runInference(3);
//
//        mapEstimate = mapInference.getMAPestimate();
//        modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
//        if (Main.VERBOSE) System.out.println("MAP estimate (HC): " + mapEstimate.outputString(modelVariables));
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        if (Main.VERBOSE) System.out.println();
//
//
//
//
//        // MAP INFERENCE WITH A BIG SAMPLE AND HILL CLIMBING ON ONE VARIABLE EACH TIME
//        mapInference.setSampleSize(100);
//        timeStart = System.nanoTime();
//        mapInference.runInference(2);
//
//
//        mapEstimate = mapInference.getMAPestimate();
//        if (Main.VERBOSE) System.out.println("MAP estimate  (HC.1V): " + mapEstimate.outputString(modelVariables));   //toString(modelVariables)
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        //if (Main.VERBOSE) System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
//        if (Main.VERBOSE) System.out.println();
//
//
//
//
//
//        // MAP INFERENCE WITH SIMULATION AND PICKING MAX
//        mapInference.setSampleSize(100);
//        timeStart = System.nanoTime();
//        mapInference.runInference(-1);
//
//        mapEstimate = mapInference.getMAPestimate();
//        modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
//        if (Main.VERBOSE) System.out.println("MAP estimate (SAMPLING): " + mapEstimate.outputString(modelVariables));
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        if (Main.VERBOSE) System.out.println();
//
//
//
//        // MAP INFERENCE, DETERMINISTIC
//        mapInference.setSampleSize(1);
//        timeStart = System.nanoTime();
//        mapInference.runInference(-2);
//
//        mapEstimate = mapInference.getMAPestimate();
//        modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
//        if (Main.VERBOSE) System.out.println("MAP estimate (DETERM.): " + mapEstimate.outputString(modelVariables));
//        if (Main.VERBOSE) System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        if (Main.VERBOSE) System.out.println();
//        */
//
//
//
//
//        /**
//         *
//         *
//         *   MAP OVER SPECIFIC VARIABLES
//         *
//         *
//         */
//
//
//        if (Main.VERBOSE) System.out.println();
//        if (Main.VERBOSE) System.out.println();
//
//        Variable varInterest1 = mapInference.causalOrder.get(6);  // causalOrder: A, S, L, T, E, X, B, D
//        Variable varInterest2 = mapInference.causalOrder.get(7);
//
//
//        Set<Variable> varsInterest = new HashSet<>();
//        varsInterest.add(varInterest1);
//        varsInterest.add(varInterest2);
//        mapInference.setMAPVariables(varsInterest);
//
//        if (Main.VERBOSE) System.out.println("MAP Variables of Interest: " + Arrays.toString(mapInference.MAPvariables.stream().map(Variable::getName).toArray()));
//        if (Main.VERBOSE) System.out.println();
//
//
//
//        // MAP INFERENCE
//        mapInference.setSampleSize(1);
//        timeStart = System.nanoTime();
//        mapInference.runInference(1);
//
//        mapEstimate = mapInference.getMAPestimate();
//        modelVariables = mapInference.getOriginalModel().getVariables().getListOfVariables();
//        if (Main.VERBOSE) System.out.println("MAP estimate: " + mapEstimate.outputString(new ArrayList(mapInference.MAPvariables)));
//        if (Main.VERBOSE) System.out.println("with probability: " +  + mapInference.getMAPestimateProbability());
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        if (Main.VERBOSE) System.out.println();
//
//
//
//
//        // MAP INFERENCE
//        mapInference.setSampleSize(100);
//        timeStart = System.nanoTime();
//        mapInference.runInference(-3);
//
//        mapEstimate = mapInference.getMAPestimate();
//        modelVariables = mapInference.getOriginalModel().getVariables().getListOfVariables();
//        if (Main.VERBOSE) System.out.println("MAP estimate (-3): " + mapEstimate.outputString(new ArrayList(mapInference.MAPvariables)));
//        if (Main.VERBOSE) System.out.println("with probability: " + mapInference.getMAPestimateProbability());
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        if (Main.VERBOSE) System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        if (Main.VERBOSE) System.out.println();
//
//
//        // MAP Assignments
//
//
//        double s1 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
//        if (Main.VERBOSE) System.out.println(mapEstimate.outputString() + " with prob. " + s1);
//
//        mapEstimate.setValue((Variable)mapEstimate.getVariables().toArray()[0],1);
//        double s2 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
//        if (Main.VERBOSE) System.out.println(mapEstimate.outputString() + " with prob. " + s2);
//
//        mapEstimate.setValue((Variable)mapEstimate.getVariables().toArray()[1],1);
//        double s3 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
//        if (Main.VERBOSE) System.out.println(mapEstimate.outputString() + " with prob. " + s3);
//
//        mapEstimate.setValue((Variable)mapEstimate.getVariables().toArray()[0],0);
//        double s4 = mapInference.estimateProbabilityOfPartialAssignment(mapEstimate);
//        if (Main.VERBOSE) System.out.println(mapEstimate.outputString() + " with prob. " + s4);
//
//        double sumNonStateless = s1+s2+s3+s4;
//
//        if (Main.VERBOSE) System.out.println("Probs: " + s1/sumNonStateless + ", " + s2/sumNonStateless + ", " + s3/sumNonStateless + ", " + s4/sumNonStateless + ", suma = " + sumNonStateless );




    }
}
