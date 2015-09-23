/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.inference;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * This class implements the interface {@link InferenceAlgorithm} and makes MPE (Most Probable Explanation) Inference.
 *
 */
public class MPEInference implements PointEstimator {

    private BayesianNetwork model;
    private List<Variable> causalOrder;
//    private Set<Variable> varsOfInterest;

    private int sampleSize;
    private int seed = 0;

    private Assignment evidence;
    private Assignment MPEestimate;
    private double MPEestimateLogProbability;
    private boolean parallelMode = true;



//
//    private class WeightedAssignment {
//        private Assignment assignment;
//        private double weight;
//
//        public WeightedAssignment(Assignment assignment_, double weight_){
//            this.assignment = assignment_;
//            this.weight = weight_;
//        }
//
//        public String toString() {
//            StringBuilder str = new StringBuilder();
//            str.append("[ ");
//
//            str.append(this.assignment.outputString());
//            str.append("Weight = " + weight + " ]");
//            return str.toString();
//        }
//    }


    public MPEInference() {
        this.evidence = new HashMapAssignment(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParallelMode(boolean parallelMode_) {
        this.parallelMode = parallelMode_;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSeed(int seed) {
        this.seed=seed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(BayesianNetwork model_) {
        this.model = model_;
        this.causalOrder = Utils.getCausalOrder(this.model.getDAG());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEvidence(Assignment evidence_) {
        this.evidence = evidence_;

    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getOriginalModel() {
        return this.model;
    }


    public Assignment getEstimate() {
        return MPEestimate;
    }

    public double getLogProbabilityOfEstimate() {
        return MPEestimateLogProbability;
    }

//    private double getProbabilityOf(Assignment as1) {
//        return Math.exp(this.model.getLogProbabiltyOf(as1));
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference() {

        this.runInference(2); // Uses Hill climbing with local search, by default
    }

    /**
     * Runs inference with an specific method.
     * @param inferenceAlgorithm an {@code int} that represents the search algorithm to use (-2: Deterministic sequential search; -1: Sampling;  0: Simulated annealing, local; 1: Simulated annealing, global; 2: Hill climbing, local (default); 3: Hill climbing, global)
     */
    public void runInference(int inferenceAlgorithm) {

        ImportanceSampling ISaux = new ImportanceSampling();
        ISaux.setModel(this.model);
        ISaux.setSamplingModel(this.model);
        ISaux.setSampleSize(this.sampleSize);
        ISaux.setParallelMode(this.parallelMode);
        ISaux.setEvidence(this.evidence);

        Random random = new Random();
        //random.setSeed(this.seed);
        ISaux.setSeed(random.nextInt());
        ISaux.runInference();

        Stream<Assignment> sample = ISaux.getSamples();


        switch(inferenceAlgorithm) {

            case -2:   // DETERMINISTIC, MAY BE VERY SLOW ON BIG NETWORKS
                MPEestimate = this.sequentialSearch();
                break;

            case -1:    // NO OPTIMIZATION ALGORITHM, JUST PICKING THE SAMPLE WITH HIGHEST PROBABILITY
                MPEestimate = sample.reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                break;



            case 0:     // "SIMULATED ANNEALING", MOVING SOME VARIABLES AT EACH ITERATION
                MPEestimate = sample.map(this::simulatedAnnealingOneVar).reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                break;

            case 1:
              // SIMULATED ANNEALING, MOVING ALL VARIABLES AT EACH ITERATION
                MPEestimate = sample.map(this::simulatedAnnealingAllVars).reduce((a1, a2) -> (model.getLogProbabiltyOf(a1) > model.getLogProbabiltyOf(a2) ? a1 : a2)).get();
                break;



            case 3:     // HILL CLIMBING, MOVING ALL VARIABLES AT EACH ITERATION
                MPEestimate = sample.map(this::hillClimbingAllVars).reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                break;

            case 2:     // HILL CLIMBING, MOVING SOME VARIABLES AT EACH ITERATION
            default:
                MPEestimate = sample.map(this::hillClimbingOneVar).reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                break;


        }
        MPEestimateLogProbability = model.getLogProbabiltyOf(MPEestimate);
    }



    private Assignment obtainValues(Assignment evidence, Random random) {

        int numberOfVariables = this.model.getNumberOfVars();
        Assignment result = new HashMapAssignment(evidence);
        List<Variable> contVarEvidence = new ArrayList<>();

        Variable selectedVariable;
        ConditionalDistribution conDist;
        double selectedVariableNewValue;



        // FIRST, ASSIGN VALUES FOR ALL DISCRETE VARIABLES
        for( int i=0; i<numberOfVariables; i++ ) {

            //selectedVariable = this.model.getStaticVariables().getVariableById(i);
            selectedVariable = causalOrder.get(i);
            conDist = this.model.getConditionalDistributions().get(i);

            if (selectedVariable.isMultinomial() && Double.isNaN(evidence.getValue(selectedVariable))) {

                selectedVariableNewValue = conDist.getUnivariateDistribution(result).sample(random);
                result.setValue(selectedVariable, selectedVariableNewValue);
                //System.out.println(selectedVariableNewValue);
            }


        }


        List<Variable> modelVariables = model.getVariables().getListOfVariables();

        //System.out.println(initialGuess.outputString(modelVariables));
        //System.out.println(result.outputString(modelVariables));






        // NOW SET VALUES FOR GAUSSIANS, STARTING WITH CONT. ANCESTORS OF CONT. VARS IN EVIDENCE (WITH SIMULATION)

        DAG graph = model.getDAG();

        for( int i=0; i<numberOfVariables; i++ ) {

            selectedVariable = causalOrder.get(i);

            if (selectedVariable.isNormal() && !Double.isNaN(evidence.getValue(selectedVariable))) {

                contVarEvidence.add(selectedVariable);

            }
        }


        boolean ended=false;
        int indexCheckedVars=0;

        while(!ended) {
            ended=true;

            for(;indexCheckedVars<contVarEvidence.size(); indexCheckedVars++) {

                Variable currentVariable = contVarEvidence.get(indexCheckedVars);
                ParentSet parents = graph.getParentSet(currentVariable);

                for (Variable currentParent : parents.getParents()) {
                    if (currentParent.isNormal() && !contVarEvidence.contains(currentParent)) {
                        ended = false;
                        contVarEvidence.add(currentParent);

                    }
                }
            }
        }

        Collections.reverse(contVarEvidence);

        //contVarEvidence.forEach(var -> System.out.println(var.getName() + " = " + evidence.getValue(var)));

        for(Variable current : contVarEvidence) {

            if(Double.isNaN(evidence.getValue(current))) {
                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(current).getUnivariateDistribution(result);
                double newValue = univariateDistribution.sample(random);
                result.setValue(current, newValue);
            }
        }


        //System.out.println(initialGuess.outputString(modelVariables));
        //System.out.println(result.outputString(modelVariables));



        // FINALLY, ASSIGN CONT. VARS. THAT ARE DESCENDANTS OF CONT. VARS. IN EVIDENCE (WITH THEIR MODE=MEAN VALUE)

        for( int i=0; i<numberOfVariables; i++ ) {

            selectedVariable = causalOrder.get(i);

            if ( selectedVariable.isNormal() && Double.isNaN(result.getValue(selectedVariable))) {
                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(selectedVariable).getUnivariateDistribution(result);
                double newValue = univariateDistribution.getParameters()[0];
                result.setValue(selectedVariable, newValue);
            }

        }

        //System.out.println(initialGuess.outputString(modelVariables));
        //System.out.println(result.outputString(modelVariables));

        return result;
    }



    private Assignment moveDiscreteVariables(Assignment initialGuess, int numberOfMovements) {

        Assignment result = new HashMapAssignment(initialGuess);
        Random random = new Random();
        ArrayList<Integer> indicesVariablesMoved = new ArrayList<>();

        int indexSelectedVariable;
        Variable selectedVariable;
        int newValue;

        while(indicesVariablesMoved.size()<numberOfMovements) {

            indexSelectedVariable = random.nextInt(this.model.getNumberOfVars());
            selectedVariable = this.model.getVariables().getVariableById(indexSelectedVariable);

            if(indicesVariablesMoved.contains(indexSelectedVariable) || selectedVariable.isNormal() || !Double.isNaN(evidence.getValue(selectedVariable))) {
                continue;
            }
            indicesVariablesMoved.add(indexSelectedVariable);
            newValue=random.nextInt(selectedVariable.getNumberOfStates());
            result.setValue(selectedVariable,newValue);
        }

        return result;
    }

    private Assignment assignContinuousVariables(Assignment initialGuess) {

        Assignment result = new HashMapAssignment(initialGuess);
        int numberOfVariables = this.model.getNumberOfVars();
        Random random = new Random();
        Variable selectedVariable;
        List<Variable> contVarEvidence = new ArrayList<>();

        DAG graph = model.getDAG();

        for( int i=0; i<numberOfVariables; i++ ) {

            selectedVariable = causalOrder.get(i);
            if (selectedVariable.isNormal() && !Double.isNaN(evidence.getValue(selectedVariable))) {
                contVarEvidence.add(selectedVariable);
            }
        }

        boolean ended=false;
        int indexCheckedVars=0;

        while(!ended) {
            ended=true;

            for(;indexCheckedVars<contVarEvidence.size(); indexCheckedVars++) {

                Variable currentVariable = contVarEvidence.get(indexCheckedVars);
                ParentSet parents = graph.getParentSet(currentVariable);

                for (Variable currentParent : parents.getParents()) {
                    if (currentParent.isNormal() && !contVarEvidence.contains(currentParent)) {
                        ended = false;
                        contVarEvidence.add(currentParent);

                    }
                }
            }
        }

        Collections.reverse(contVarEvidence);

        //contVarEvidence.forEach(var -> System.out.println(var.getName() + " = " + evidence.getValue(var)));

        for(Variable current : contVarEvidence) {

            if(Double.isNaN(evidence.getValue(current))) {
                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(current).getUnivariateDistribution(result);
                double newValue = univariateDistribution.sample(random);
                result.setValue(current, newValue);
            }
        }


        //System.out.println(initialGuess.outputString(modelVariables));
        //System.out.println(result.outputString(modelVariables));



        // FINALLY, ASSIGN CONT. VARS. THAT ARE DESCENDANTS OF CONT. VARS. IN EVIDENCE (WITH THEIR MODE=MEAN VALUE)

        for( int i=0; i<numberOfVariables; i++ ) {

            selectedVariable = causalOrder.get(i);

            if ( selectedVariable.isNormal() && Double.isNaN(this.evidence.getValue(selectedVariable))) {
                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(selectedVariable).getUnivariateDistribution(result);
                double newValue = univariateDistribution.getParameters()[0];
                result.setValue(selectedVariable, newValue);
            }

        }

        return result;
    }




    private Assignment simulatedAnnealingAllVars(Assignment initialGuess) {
        //System.out.println("SA ALL VARS");
        Assignment newGuess; // = new HashMapAssignment(this.evidence);
        Assignment bestGuess = initialGuess;

        double R=10000; // Temperature
        double eps=0.01;
        double alpha=0.90;

        double currentProbability=0;
        double nextProbability;

        //Random random = new Random(this.seed+initialGuess.hashCode());
        Random random = new Random(this.seed);
        while (R>eps) {
            //random = new Random();
            //System.out.println(R);
            //System.out.println(bestGuess.outputString());
            // GIVE VALUES
            newGuess=obtainValues(evidence, random);
            //System.out.println(newGuess.outputString());

            currentProbability=this.model.getLogProbabiltyOf(initialGuess);
            nextProbability=this.model.getLogProbabiltyOf(newGuess);



            if (nextProbability>currentProbability) {
                bestGuess=newGuess;
            }
            else {
                double diff = currentProbability - nextProbability;

                double aux = random.nextDouble();

                if (aux < Math.exp( -diff/R )) {
                    bestGuess = newGuess;
                }
            }

            R = alpha * R;
        }


        return bestGuess;

    }


    /*
    * "Simulated annealing": changes ONE variable at each iteration. If improves, accept, if not, sometimes accept.
     */
    private Assignment simulatedAnnealingOneVar(Assignment initialGuess) {
        Assignment result = new HashMapAssignment(initialGuess);

        double R=10000; // Temperature
        double eps=0.01;
        double alpha=0.90;

        double currentProbability;
        double nextProbability;

        Random random = new Random(this.seed);

        while (R>eps) {


//            int indexSelectedVariable = random.nextInt(this.model.getNumberOfVars());
//            double selectedVariableNewValue;
//
//            // Choose a new value for ONE of the variables and check whether the probability grows or not
//            Variable selectedVariable = this.model.getVariables().getVariableById(indexSelectedVariable);
//
//
//            //if (selectedVariable.isMultinomial()) {
//            //selectedVariableNewValue = selectedVariable
//
//            ConditionalDistribution cd = this.model.getConditionalDistributions().get(indexSelectedVariable);
//            selectedVariableNewValue = cd.getUnivariateDistribution(initialGuess).sample(random);
//
//            //}
//            //else if (selectedVariable.isNormal()) {
//
//
//
//            //}
//
//            result.setValue(selectedVariable,selectedVariableNewValue);

            result = moveDiscreteVariables(initialGuess, 3);
            result = assignContinuousVariables(result);


            currentProbability=this.model.getLogProbabiltyOf(initialGuess);
            nextProbability=this.model.getLogProbabiltyOf(result);

            if (nextProbability>currentProbability) {
                initialGuess=result;
            }
            else {
                double diff = currentProbability - nextProbability;

                double aux = random.nextDouble();

                if (aux < Math.exp( -diff/R )) {
                    initialGuess = result;
                }
            }
            R = alpha * R;
        }

        return result;

    }





    private Assignment hillClimbingAllVars(Assignment initialGuess) {
        Assignment result = new HashMapAssignment(this.evidence);

        double R=130;
        double eps=0;

        double currentProbability;
        double nextProbability;

        //Random random = new Random(this.seed+initialGuess.hashCode());
        Random random = new Random();
        while (R>eps) {

            // GIVE VALUES
            result=obtainValues(evidence, random);


            currentProbability=this.model.getLogProbabiltyOf(initialGuess);
            nextProbability=this.model.getLogProbabiltyOf(result);


            if (nextProbability>=currentProbability) {
                initialGuess=result;
            }

            R = R - 1;
        }

        return result;

    }

    /*
    * "Hill climbing": changes ONE variable at each iteration. If improves, accept.
    */
    private Assignment hillClimbingOneVar(Assignment initialGuess) {
        Assignment result = new HashMapAssignment(initialGuess);

        double R=130;
        double eps=0;

        double currentProbability;
        double nextProbability;

        //Random random = new Random();

        while (R>eps) {

            result = moveDiscreteVariables(initialGuess, 3);
            result = assignContinuousVariables(result);

//            int indexSelectedVariable = random.nextInt(this.model.getNumberOfVars());
//            double selectedVariableNewValue;
//
//            // Choose a new value for ONE of the variables and check whether the probability grows or not
//            Variable selectedVariable = this.model.getVariables().getVariableById(indexSelectedVariable);
//
//            if (!Double.isNaN(this.evidence.getValue(selectedVariable)) || selectedVariable.isNormal()) {
//                continue;
//            }
//
//            ConditionalDistribution cd = this.model.getConditionalDistributions().get(indexSelectedVariable);
//            selectedVariableNewValue = cd.getUnivariateDistribution(initialGuess).sample(random);
//
//
//            result.setValue(selectedVariable,selectedVariableNewValue);

            currentProbability=this.model.getLogProbabiltyOf(initialGuess);
            nextProbability=this.model.getLogProbabiltyOf(result);

            if (nextProbability>currentProbability) {
                initialGuess=result;
            }

            R = R - 1;
        }

        return result;

    }



    // DETERMINISTIC SEARCH OF THE BEST CONFIGURATION FOR DISCRETE (BINARY) VARS
    private Assignment bestConfig(Assignment current, int varIndex) {

        int numVars=this.model.getNumberOfVars();

        if (varIndex>(numVars-1)) {
            return current;
        }

        Variable currentVariable = this.model.getVariables().getVariableById(varIndex);

        if (Double.isNaN(this.evidence.getValue(currentVariable))) {



//            // DUPLICATE CURRENT ASSIGNMENT
//
//            Assignment config0 = new HashMapAssignment(numVars);
//            Assignment config1 = new HashMapAssignment(numVars);
//
//            for(Variable var: model.getStaticVariables()) {
//
//                if ( Double.isNaN(this.evidence.getValue(var))) {
//                    config0.setValue(var,current.getValue(var));
//                    config1.setValue(var,current.getValue(var));
//                }
//                else {
//                    config0.setValue(var,evidence.getValue(var));
//                    config1.setValue(var,evidence.getValue(var));
//                }
//            }

            if (currentVariable.isMultinomial()) {


                int numberOfStates = currentVariable.getNumberOfStates();

                ArrayList<Assignment> configs = new ArrayList<>(numberOfStates);


                for(int i=0; i<numberOfStates; i++) {
                    configs.add(new HashMapAssignment(current));
                    configs.get(i).setValue(currentVariable,i);
                }
                //config0.setValue(currentVariable, 0);
                //config1.setValue(currentVariable, 1);

                if (varIndex < (numVars - 1)) {
                    //config0 = bestConfig(config0, varIndex + 1);
                    //config1 = bestConfig(config1, varIndex + 1);

                    for(int i=0; i<numberOfStates; i++) {
                        configs.set(i,bestConfig(configs.get(i),varIndex+1));
                    }
                }
                return configs.stream().max((cnf1, cnf2) -> Double.compare(model.getLogProbabiltyOf(cnf1), model.getLogProbabiltyOf(cnf2))).get();
                //return (model.getLogProbabiltyOf(config0) > model.getLogProbabiltyOf(config1) ? config0 : config1);
            }
            else {
                Assignment config0 = new HashMapAssignment(current);
                double newValue;

                newValue = model.getConditionalDistributions().get(varIndex).getUnivariateDistribution(config0).getParameters()[0];

                config0.setValue(currentVariable, newValue);
                if (varIndex < (numVars - 1)) {
                    config0 = bestConfig(config0, varIndex + 1);
                }
                return config0;
            }


//            // DUPLICATE CURRENT ASSIGNMENT
//            Assignment config0 = new HashMapAssignment(numVars);
//            Assignment config1 = new HashMapAssignment(numVars);
//
//            for(Variable var: model.getStaticVariables()) {
//
//                if ( Double.isNaN(this.evidence.getValue(var))) {
//                    config0.setValue(var,current.getValue(var));
//                    config1.setValue(var,current.getValue(var));
//                }
//                else {
//                    config0.setValue(var,evidence.getValue(var));
//                    config1.setValue(var,evidence.getValue(var));
//                }
//            }
//
//            if (currentVariable.isMultinomial()) {
//
//                config0.setValue(currentVariable, 0);
//                config1.setValue(currentVariable, 1);
//
//                if (varIndex < (numVars - 1)) {
//                    config0 = bestConfig(config0, varIndex + 1);
//                    config1 = bestConfig(config1, varIndex + 1);
//                }
//                return (model.getLogProbabiltyOf(config0) > model.getLogProbabiltyOf(config1) ? config0 : config1);
//            }
//            else {
//                double newValue;
//
//                newValue = model.getConditionalDistributions().get(varIndex).getUnivariateDistribution(config0).getParameters()[0];
//
//                config0.setValue(currentVariable, newValue);
//                if (varIndex < (numVars - 1)) {
//                    config0 = bestConfig(config0, varIndex + 1);
//                }
//                return config0;
//            }
        }
        else {
            if (varIndex < (numVars - 1)) {
                return bestConfig(current, varIndex + 1);
            }
            else {
                return current;
            }
        }
    }

    // DETERMINISTIC SEARCH OF THE BEST CONFIGURATION FOR DISCRETE (BINARY) VARS
    private Assignment sequentialSearch() {

        int numberOfVariables = this.model.getNumberOfVars();
        Assignment currentEstimator = new HashMapAssignment(numberOfVariables);

        Variable selectedVariable;
        double selectedVariableNewValue;
        ConditionalDistribution conDist;

        // INITIALIZE THE ESTIMATOR
        for( int i=0; i<numberOfVariables; i++ ) {

            selectedVariable = this.model.getVariables().getVariableById(i);
            conDist = this.model.getConditionalDistributions().get(i);

            if ( Double.isNaN(this.evidence.getValue(selectedVariable))) {
                if (selectedVariable.isMultinomial()) {
                    selectedVariableNewValue = 0;

                } else {

                    selectedVariableNewValue = conDist.getUnivariateDistribution(currentEstimator).getParameters()[0];
                }
                currentEstimator.setValue(selectedVariable, selectedVariableNewValue);
            }
            else {
                currentEstimator.setValue(selectedVariable,evidence.getValue(selectedVariable));
            }
        }
        return bestConfig(currentEstimator,0);
    }



    public static void main(String[] args) throws IOException, ClassNotFoundException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/asia.bn");

        System.out.println(bn.toString());

        MPEInference mpeInference = new MPEInference();
        mpeInference.setModel(bn);
        mpeInference.setParallelMode(true);

        System.out.println("CausalOrder: " + Arrays.toString(mpeInference.causalOrder.stream().map(v -> v.getName()).toArray()));
        System.out.println();


        List<Variable> modelVariables = Utils.getCausalOrder(bn.getDAG());


        int parallelSamples=10;
        int samplingMethodSize=1000;
        mpeInference.setSampleSize(parallelSamples);


        /***********************************************
         *        INCLUDING EVIDENCE
         ************************************************/

        Variable variable1 = mpeInference.causalOrder.get(1);   // causalOrder: A, S, L, T, E, X, B, D
        Variable variable2 = mpeInference.causalOrder.get(2);
        Variable variable3 = mpeInference.causalOrder.get(4);

        int var1value=0;
        int var2value=1;
        int var3value=1;

        System.out.println("Evidence: Variable " + variable1.getName() + " = " + var1value + ", Variable " + variable2.getName() + " = " + var2value + ", " + " and Variable " + variable3.getName() + " = " + var3value);
        System.out.println();

        HashMapAssignment evidenceAssignment = new HashMapAssignment(3);

        evidenceAssignment.setValue(variable1, var1value);
        evidenceAssignment.setValue(variable2, var2value);
        evidenceAssignment.setValue(variable3, var3value);

        mpeInference.setEvidence(evidenceAssignment);



        /***********************************************
        *        SIMULATED ANNEALING
        ************************************************/

        // MPE INFERENCE WITH SIMULATED ANNEALING, ALL VARIABLES
        System.out.println();
        long timeStart = System.nanoTime();
        mpeInference.runInference(1);


        Assignment mpeEstimate = mpeInference.getEstimate();
        System.out.println("MPE estimate (SA.All): " + mpeEstimate.outputString(modelVariables));
        System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        long timeStop = System.nanoTime();
        double execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();




        // MPE INFERENCE WITH SIMULATED ANNEALING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mpeInference.runInference(0);


        mpeEstimate = mpeInference.getEstimate();
        System.out.println("MPE estimate  (SA.Some): " + mpeEstimate.outputString(modelVariables));
        System.out.println("with probability: "+ Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();


        /***********************************************
         *        HILL CLIMBING
         ************************************************/

        // MPE INFERENCE WITH HILL CLIMBING, ALL VARIABLES
        timeStart = System.nanoTime();
        mpeInference.runInference(3);

        mpeEstimate = mpeInference.getEstimate();
        //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
        System.out.println("MPE estimate (HC.All): " + mpeEstimate.outputString(modelVariables));
        System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();




        //  MPE INFERENCE WITH HILL CLIMBING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mpeInference.runInference(2);


        mpeEstimate = mpeInference.getEstimate();
        System.out.println("MPE estimate  (HC.Some): " + mpeEstimate.outputString(modelVariables));
        System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();


        /***********************************************
         *        SAMPLING AND DETERMINISTIC
         ************************************************/

        // MPE INFERENCE WITH SIMULATION AND PICKING MAX

        mpeInference.setSampleSize(samplingMethodSize);

        timeStart = System.nanoTime();
        mpeInference.runInference(-1);

        mpeEstimate = mpeInference.getEstimate();
        //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
        System.out.println("MPE estimate (SAMPLING): " + mpeEstimate.outputString(modelVariables));
        System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();




        // MPE INFERENCE, DETERMINISTIC
        timeStart = System.nanoTime();
        mpeInference.runInference(-2);

        mpeEstimate = mpeInference.getEstimate();
        //modelVariables = mpeInference.getOriginalModel().getVariables().getListOfVariables();
        System.out.println("MPE estimate (DETERM.): " + mpeEstimate.outputString(modelVariables));
        System.out.println("with probability: " + Math.exp(mpeInference.getLogProbabilityOfEstimate()) + ", logProb: " + mpeInference.getLogProbabilityOfEstimate());
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();


    }


}
