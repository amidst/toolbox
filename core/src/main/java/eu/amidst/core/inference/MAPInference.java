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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * This class implements the interface {@link PointEstimator} and defines the MAP Inference algorithm.
 */
public class MAPInference implements PointEstimator {

    private BayesianNetwork model;
    private List<Variable> causalOrder;

    private int sampleSize;
    private int seed = 0;
    private int numberOfIterations=100;

    private Assignment evidence;

    private long numberOfDiscreteVariables = 0;
    private long numberOfDiscreteVariablesInEvidence = 0;

    private boolean parallelMode = true;


    private List<Variable> MAPvariables;
    private Assignment MAPestimate;
    private double MAPestimateLogProbability;



    private class WeightedAssignment {
        private Assignment assignment;
        private double weight;

        public WeightedAssignment(Assignment assignment_, double weight_){
            this.assignment = assignment_;
            this.weight = weight_;
        }

        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("[ ");

            str.append(this.assignment.outputString());
            str.append("Weight = " + weight + " ]");
            return str.toString();
        }
    }

    public MAPInference() {

        this.evidence = new HashMapAssignment(0);
        this.sampleSize = 10;
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
        this.causalOrder = Utils.getTopologicalOrder(this.model.getDAG());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEvidence(Assignment evidence_) {
        this.evidence = evidence_;
/*
        // MODIFY THE CAUSAL ORDER, VARIABLES WITH EVIDENCE FIRST
        List<Variable> newCausalOrder = new ArrayList<>();

        for(Variable variable : causalOrder) {
            if ( variable.isMultinomial() && !Double.isNaN(evidence.getValue(variable)) ) {
                newCausalOrder.add(variable);
            }
        }

        for(Variable variable : causalOrder) {
            if ( variable.isMultinomial() && Double.isNaN(evidence.getValue(variable)) ) {
                newCausalOrder.add(variable);
            }
        }

        for(Variable variable : causalOrder) {
            if ( variable.isNormal() && !Double.isNaN(evidence.getValue(variable)) ) {
                newCausalOrder.add(variable);
            }
        }

        for(Variable variable : causalOrder) {
            if ( variable.isNormal() && Double.isNaN(evidence.getValue(variable)) ) {
                newCausalOrder.add(variable);
            }
        }
        causalOrder = newCausalOrder;*/
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public void setMAPVariables(List<Variable> varsOfInterest1) {
        this.MAPvariables = varsOfInterest1;
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = numberOfIterations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getOriginalModel() {
        return this.model;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Assignment getEstimate() {

        /*if(MAPvariables!=null) {
            Assignment auxMAPEstimate = new HashMapAssignment(MAPestimate);

            for(Variable var : this.causalOrder) {
                if( !MAPvariables.contains(var) ) {
                    auxMAPEstimate.setValue(var,Double.NaN);
                }
            }
            return auxMAPEstimate;
        }
        else {*/
            return MAPestimate;
        //}

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogProbabilityOfEstimate() {
        return MAPestimateLogProbability;
    }

    /*    private class AssignmentWithProbability {
        private HashMapAssignment assignment;
        private double probability;

        public AssignmentWithProbability(HashMapAssignment assignment_, double weight_){
            this.assignment = assignment_;
            this.probability = weight_;
        }

        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("[ ");

            for (Map.Entry<Variable, Double> entry : this.assignment.entrySet()) {
                str.append(entry.getKey().getName() + " = " + entry.getValue());
                str.append(", ");
            }
            str.append("Probability = " + probability + " ]");
            return str.toString();
        }
    }*/



    private double getProbabilityOf(Assignment as1) {
        return Math.exp(this.model.getLogProbabiltyOf(as1));
    }



    private String getMAPVariablesFromAssignment(Assignment assignment) {
        if (this.MAPvariables!=null) {
            Assignment MAPVarsValues = new HashMapAssignment(MAPvariables.size());
            for(Variable var : MAPvariables) {
                MAPVarsValues.setValue(var,assignment.getValue(var));
            }
            return MAPVarsValues.outputString();
        }
        else {
            return assignment.outputString();
        }
    }

    private Assignment fullAssignmentToMAPassignment(Assignment fullAssignment) {
        Assignment MAPassignment = new HashMapAssignment(MAPvariables.size());
        MAPvariables.stream().forEach(MAPvar -> MAPassignment.setValue(MAPvar, fullAssignment.getValue(MAPvar)));
        return MAPassignment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference() {
        this.runInference("HC_local"); // Uses Hill climbing with local search, by default
    }

    /**
     * Runs inference with an specific method.
     * @param inferenceAlgorithm an {@code String} that represents the search algorithm to use (sampling: Sampling;  SA_local: Simulated annealing, local; SA_global: Simulated annealing, global; HC_local: Hill climbing, local (default); HC_global: Hill climbing, global)
     */
    public void runInference(String inferenceAlgorithm) {


        ImportanceSampling ISaux = new ImportanceSampling();
        ISaux.setModel(this.model);
        ISaux.setSamplingModel(this.model);
        ISaux.setSampleSize(this.sampleSize);
        ISaux.setParallelMode(this.parallelMode);
        ISaux.setEvidence(this.evidence);
        ISaux.setKeepDataOnMemory(true);

        Random random = new Random();
        //random.setSeed(this.seed);
        ISaux.setSeed(random.nextInt());
        ISaux.runInference();

        Stream<Assignment> sample = ISaux.getSamples().parallel();
        WeightedAssignment weightedAssignment;

        /*
        if(this.MAPvariables==null) {
            // MPE: MAP WITH ALL NON-OBSERVED VARIABLES
            MAPvariables=new HashSet<Variable>(this.model.getStaticVariables().getListOfVariables());
        }*/

        switch(inferenceAlgorithm) {
            case "sampling":

                //Map<String, Double> groupedSample = sample.collect(Collectors.groupingBy(this::getMAPVariablesFromAssignment, HashMap::new, Collectors.averagingDouble(this::getProbabilityOf)));
                //groupedSample.forEach((smp,values) -> System.out.println(smp + Double.toString(values)));

                ISaux.runInference();
                sample = ISaux.getSamples();



                Map<Assignment, List<Assignment>> groupedSamples =
                        sample.collect(Collectors.groupingBy(this::getMAPVariablesFromAssignment))
                                .values().stream()
                                .collect(Collectors.toMap(lst -> lst.get(0), lst->lst));

                Map<Assignment, Double> newMap = new HashMap<>();

                for(Map.Entry<Assignment, List<Assignment>> entry : groupedSamples.entrySet()) {
                    newMap.put(fullAssignmentToMAPassignment(entry.getKey()), entry.getValue().stream().mapToDouble(this::getProbabilityOf).average().getAsDouble());
                }

                double normalizationFactor = newMap.values().stream().mapToDouble(a->a).sum();
                //System.out.println(normalizationFactor);

                //groupedSample.forEach((smp,values) -> System.out.println(smp + Double.toString(values)));

                Map.Entry<Assignment, Double> MAPentry = newMap.entrySet().stream().reduce((e1, e2) -> (e1.getValue() > e2.getValue() ? e1 : e2)).get();

                MAPestimate = MAPentry.getKey();
                MAPestimateLogProbability = Math.log(MAPentry.getValue()); //   /normalizationFactor);
                break;



//            case -1:    // NO OPTIMIZATION ALGORITHM, JUST PICKING THE SAMPLE WITH HIGHEST PROBABILITY
//                MAPestimate = sample.reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
//                break;
//            case -2:   // DETERMINISTIC, MAY BE VERY SLOW ON BIG NETWORKS
//                MAPestimate = this.sequentialSearch();
//                break;


            case "SA_local":     // "SIMULATED ANNEALING", MOVING SOME VARIABLES AT EACH ITERATION
                //MAPestimate = sample.map(this::simulatedAnnealingOneVar).reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                //weightedAssignment = sample.map(this::simulatedAnnealingOneVar).reduce((wa1, wa2) -> (model.getLogProbabiltyOf(wa1.assignment) > model.getLogProbabiltyOf(wa2.assignment) ? wa1 : wa2)).get();
                weightedAssignment = sample.map(this::simulatedAnnealingOneVar).reduce((wa1, wa2) -> (wa1.weight > wa2.weight ? wa1 : wa2)).get();
                //MAPestimate = weightedAssignment.assignment;
                MAPestimate = fullAssignmentToMAPassignment(weightedAssignment.assignment);
                MAPestimateLogProbability = Math.log(weightedAssignment.weight);
                break;

            case "SA_global":
                // SIMULATED ANNEALING, MOVING ALL VARIABLES AT EACH ITERATION
                //MAPestimate = sample.map(this::simulatedAnnealingAllVars).reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                //weightedAssignment = sample.map(this::simulatedAnnealingAllVars).reduce((wa1, wa2) -> (model.getLogProbabiltyOf(wa1.assignment) > model.getLogProbabiltyOf(wa2.assignment) ? wa1 : wa2)).get();
                weightedAssignment = sample.map(this::simulatedAnnealingAllVars).reduce((wa1, wa2) -> (wa1.weight > wa2.weight ? wa1 : wa2)).get();
                //MAPestimate = weightedAssignment.assignment;
                MAPestimate = fullAssignmentToMAPassignment(weightedAssignment.assignment);
                MAPestimateLogProbability = Math.log(weightedAssignment.weight);
                break;

            case "HC_global":     // HILL CLIMBING, MOVING ALL VARIABLES AT EACH ITERATION
                //MAPestimate = sample.map(this::hillClimbingAllVars).reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                //weightedAssignment = sample.map(this::hillClimbingAllVars).reduce((wa1, wa2) -> (model.getLogProbabiltyOf(wa1.assignment) > model.getLogProbabiltyOf(wa2.assignment) ? wa1 : wa2)).get();
                weightedAssignment = sample.map(this::hillClimbingAllVars).reduce((wa1, wa2) -> (wa1.weight > wa2.weight ? wa1 : wa2)).get();
                //MAPestimate = weightedAssignment.assignment;
                MAPestimate = fullAssignmentToMAPassignment(weightedAssignment.assignment);
                MAPestimateLogProbability = Math.log(weightedAssignment.weight);
                break;

            case "HC_local":     // HILL CLIMBING, MOVING SOME VARIABLES AT EACH ITERATION
            default:
                //MAPestimate = sample.map(this::hillClimbingOneVar).reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                //weightedAssignment = sample.map(this::hillClimbingOneVar).reduce((wa1, wa2) -> (model.getLogProbabiltyOf(wa1.assignment) > model.getLogProbabiltyOf(wa2.assignment) ? wa1 : wa2)).get();
                weightedAssignment = sample.map(this::hillClimbingOneVar).reduce((wa1, wa2) -> (wa1.weight > wa2.weight ? wa1 : wa2)).get();
                //MAPestimate = weightedAssignment.assignment;
                MAPestimate = fullAssignmentToMAPassignment(weightedAssignment.assignment);
                MAPestimateLogProbability = Math.log(weightedAssignment.weight);
                break;

        }
    }

    /*
    private Assignment obtainValues(Assignment initialGuess, Assignment evidence, Assignment varsInterest, Random random) {

    }*/

    private Assignment obtainValues(Assignment initialGuess, Assignment evidence, Random random) {

        int numberOfVariables = this.model.getNumberOfVars();
        Assignment result = new HashMapAssignment(initialGuess);
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


        //List<Variable> modelVariables = model.getVariables().getListOfVariables();

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



    private Assignment obtainValuesRandomly(Assignment initialGuess, Assignment evidence, Random random) {

        int numberOfVariables = this.model.getNumberOfVars();
        Assignment result = new HashMapAssignment(initialGuess);
        List<Variable> contVarEvidence = new ArrayList<>();

        Variable selectedVariable;
        ConditionalDistribution conDist;
        double selectedVariableNewValue;



        // FIRST, ASSIGN VALUES FOR ALL DISCRETE VARIABLES

        evidence.getVariables().stream()
                .filter(Variable::isMultinomial)
                .forEach(va -> result.setValue(va, evidence.getValue(va)));

        causalOrder.stream().filter(va -> va.isMultinomial() && Double.isNaN(evidence.getValue(va))).forEach(va -> result.setValue(va,random.nextInt(va.getNumberOfStates())));





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
                //double newValue = univariateDistribution.sample(random);

                double univDistMean = univariateDistribution.getParameters()[0];
                double univDistStDev = univariateDistribution.getParameters()[1];


                double newValue = univDistMean - 3*univDistStDev + random.nextDouble()*6*univDistStDev;
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
                //double newValue = univariateDistribution.getParameters()[0];


                double univDistMean = univariateDistribution.getParameters()[0];
                double univDistStDev = univariateDistribution.getParameters()[1];


                double newValue = univDistMean - 3*univDistStDev + random.nextDouble()*6*univDistStDev;
                result.setValue(selectedVariable, newValue);
            }

        }

        //System.out.println(initialGuess.outputString(modelVariables));
        //System.out.println(result.outputString(modelVariables));

        return result;
    }


    /*

    private double getLogProbabiltyOf(Assignment assignment) {
        if(this.MAPvariables==null) {
            return this.model.getLogProbabiltyOf(assignment);
        }
        else {
            MAPvariables.stream().
        }
    }*/


    public double estimateProbabilityOfPartialAssignment(Assignment MAPassignment) {

        double probabilityEstimate = 0;
        final int numSamplesAverage = 50;

        Assignment evidenceAugmented=new HashMapAssignment(evidence);
        MAPvariables.forEach(voi -> evidenceAugmented.setValue(voi, MAPassignment.getValue(voi)));

        final Assignment finalAssignment=new HashMapAssignment(MAPassignment);

        IntStream auxIntStream = IntStream.range(0, numSamplesAverage);
        //probabilityEstimate = auxIntStream.mapToObj(i -> obtainValuesRandomly(finalAssignment,evidenceAugmented,new Random())).mapToDouble(as -> Math.exp(this.model.getLogProbabiltyOf(as))).average().getAsDouble();
        probabilityEstimate = auxIntStream.mapToObj(i -> obtainValues(finalAssignment, evidenceAugmented, new Random())).mapToDouble(as -> Math.exp(this.model.getLogProbabiltyOf(as))).average().getAsDouble();

        return probabilityEstimate;

    }


    public double estimateRandomlyProbabilityOfPartialAssignment(Assignment MAPassignment) {

        double probabilityEstimate = 0;
        final int numSamplesAverage = 50;

        Assignment evidenceAugmented=new HashMapAssignment(evidence);
        MAPvariables.forEach(voi -> evidenceAugmented.setValue(voi, MAPassignment.getValue(voi)));

        final Assignment finalAssignment=new HashMapAssignment(MAPassignment);

        IntStream auxIntStream = IntStream.range(0, numSamplesAverage);
        probabilityEstimate = auxIntStream.mapToObj(i -> obtainValuesRandomly(finalAssignment, evidenceAugmented, new Random())).mapToDouble(as -> Math.exp(this.model.getLogProbabiltyOf(as))).average().getAsDouble();
        //probabilityEstimate = auxIntStream.mapToObj(i -> obtainValues(finalAssignment, evidenceAugmented, new Random())).mapToDouble(as -> Math.exp(this.model.getLogProbabiltyOf(as))).average().getAsDouble();

        return probabilityEstimate;

    }


    /*
    * "Simulated annealing": changes All variables at each iteration. If improves, accept, if not, sometimes accept.
     */
    private WeightedAssignment simulatedAnnealingAllVars(Assignment initialGuess) {


        double R=1000; // Temperature
        double alpha=0.90;
        double eps=R * Math.pow(alpha,this.numberOfIterations);

        Assignment currentAssignment = new HashMapAssignment(initialGuess);
        double currentProbability = estimateProbabilityOfPartialAssignment(currentAssignment);

        Assignment nextAssignment;
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

            nextAssignment = obtainValues(currentAssignment, evidence, random);

            //currentProbability=this.model.getLogProbabiltyOf(currentAssignment);
            nextProbability=estimateProbabilityOfPartialAssignment(nextAssignment);

            if (nextProbability > currentProbability) {
                currentAssignment = nextAssignment;
                currentProbability = nextProbability;
            }
            else {
                double diff = currentProbability - nextProbability;

                double aux = random.nextDouble();

                if (aux < Math.exp( -diff/R )) {
                    currentAssignment = nextAssignment;
                    currentProbability = nextProbability;
                }
            }
            R = alpha * R;
        }

        return new WeightedAssignment(currentAssignment,currentProbability);

    }

//    private WeightedAssignment simulatedAnnealingAllVars(Assignment initialGuessAllVars) {
//
//
//        Assignment initialGuessMAPvars = new HashMapAssignment(MAPvariables.size());
//        Assignment newGuessMAPvars;
//
//        for(Variable var : MAPvariables) {
//            initialGuessMAPvars.setValue(var, initialGuessAllVars.getValue(var));
//        }
//
//
//        double R=10; // Temperature
//        double eps=0.01;
//        double alpha=0.90;
//
//        double initialProbability=0;
//        double newProbability=0;
//
//
//        Random random = new Random();
//        while (R>eps) {
//
//            //System.out.println(Double.toString(R));
//
//            // GIVE VALUES
//            //newGuessMAPvars=obtainValues(initialGuessMAPvars, evidence, new Random());
//            newGuessMAPvars = new HashMapAssignment(initialGuessMAPvars);
//
//            //Randomly change 1 map variable value
//
//            int randomIndex = random.nextInt(MAPvariables.size());
//
//            final int numberOfSteps=2;
//            Variable MAPchangingVariable;
//            double MAPchangingVariableValue;
//
//            for (int i = 0; i < numberOfSteps; i++) {
//                MAPchangingVariable = (Variable) MAPvariables.toArray()[randomIndex];
//                MAPchangingVariableValue = random.nextInt(MAPchangingVariable.getNumberOfStates());
//
//                //System.out.println(randomIndex + ", " + MAPchangingVariableValue);
//                newGuessMAPvars.setValue(MAPchangingVariable,MAPchangingVariableValue);
//            }
//
//
//
//
//
//
//
//
//            initialProbability = estimateProbabilityOfPartialAssignment(initialGuessMAPvars);
//            newProbability = estimateProbabilityOfPartialAssignment(newGuessMAPvars);
//
//
////            System.out.println("Initial guess");
////            System.out.println(initialGuessMAPvars.outputString());
////            System.out.println("with probability:" + Double.toString(initialProbability));
////
////            System.out.println("New guess");
////            System.out.println(newGuessMAPvars.outputString());
////            System.out.println("with probability:" + Double.toString(newProbability));
//
//
//            if (newProbability>initialProbability) {
//                initialGuessMAPvars=newGuessMAPvars;
//                initialProbability=newProbability;
//            }
//            /*else {
//                double diff = initialProbability - newProbability;
//
//                double aux = random.nextDouble();
//
//                if (aux < Math.exp( -diff/R )) {
//                    initialGuessMAPvars = newGuessMAPvars;
//                    initialProbability=newProbability;
//                }
//            }
//            */
//
//
////            System.out.println("Final guess");
////            System.out.println(initialGuessMAPvars.outputString());
//
//            R = alpha * R;
//        }
//
//        WeightedAssignment finalResult = new WeightedAssignment(initialGuessMAPvars,initialProbability);
//        return finalResult;
//
//    }



    /*
    * "Simulated annealing": changes ONE variable at each iteration. If improves, accept, if not, sometimes accept.
     */
    private WeightedAssignment simulatedAnnealingOneVar(Assignment initialGuess) {


        double R=1000; // Temperature
        double alpha=0.90;
        double eps=R * Math.pow(alpha,this.numberOfIterations);

        Assignment currentAssignment=new HashMapAssignment(initialGuess);
        double currentProbability = estimateProbabilityOfPartialAssignment(currentAssignment);

        Assignment nextAssignment;
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

            nextAssignment = moveDiscreteVariables(currentAssignment, 3);
            nextAssignment = assignContinuousVariables(nextAssignment);


            //currentProbability=this.model.getLogProbabiltyOf(currentAssignment);
            nextProbability=estimateProbabilityOfPartialAssignment(nextAssignment);

            if (nextProbability > currentProbability) {
                currentAssignment = nextAssignment;
                currentProbability = nextProbability;
            }
            else {
                double diff = currentProbability - nextProbability;

                double aux = random.nextDouble();

                if (aux < Math.exp( -diff/R )) {
                    currentAssignment = nextAssignment;
                    currentProbability = nextProbability;
                }
            }
            R = alpha * R;
        }

        return new WeightedAssignment(currentAssignment,currentProbability);

    }

//    /*
//    * "Simulated annealing": changes ONE variable at each iteration. If improves, accept, if not, sometimes accept.
//     */
//    private Assignment simulatedAnnealingOneVar(Assignment initialGuess) {
//        Assignment result = new HashMapAssignment(initialGuess);
//
//        double R=1000; // Temperature
//        double eps=0.01;
//        double alpha=0.7;
//
//        double currentProbability;
//        double nextProbability;
//
//        while (R>eps) {
//
//            Random random = new Random(this.seed);
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
//
//            currentProbability=this.model.getLogProbabiltyOf(initialGuess);
//            nextProbability=this.model.getLogProbabiltyOf(result);
//
//            if (nextProbability>currentProbability) {
//                initialGuess=result;
//            }
//            else {
//                double diff = currentProbability - nextProbability;
//
//                double aux = random.nextDouble();
//
//                if (aux < Math.exp( -diff/R )) {
//                    initialGuess = result;
//                }
//            }
//            R = alpha * R;
//        }
//
//        return result;
//
//    }





    private WeightedAssignment hillClimbingAllVars(Assignment initialGuess) {


        double R=this.numberOfIterations;
        double eps=0;

        Assignment currentAssignment=new HashMapAssignment(initialGuess);
        double currentProbability=estimateProbabilityOfPartialAssignment(currentAssignment);

        Assignment nextAssignment;
        double nextProbability;

        //Random random = new Random(this.seed+initialGuess.hashCode());
        Random random = new Random();
        while (R>eps) {

            // GIVE VALUES
            //result=obtainValues(currentAssignment, evidence, random);

            nextAssignment=obtainValues(currentAssignment, evidence, random);
            nextProbability=estimateProbabilityOfPartialAssignment(nextAssignment);

//            final Assignment finalResult=new HashMapAssignment(result);
//            final Assignment finalInitialGuess=new HashMapAssignment(currentAssignment);
//
//            Assignment evidenceAugmented=new HashMapAssignment(evidence);
//
//            if(MAPvariables!=null) {
//                MAPvariables.forEach(voi -> evidenceAugmented.setValue(voi, finalResult.getValue(voi)));
//
//                final int numSamplesAverage = 50;
//
//                IntStream auxIntStream = IntStream.range(0, numSamplesAverage);
//                currentProbability = auxIntStream.mapToObj(i -> obtainValues(finalInitialGuess,evidenceAugmented,new Random())).mapToDouble(this.model::getLogProbabiltyOf).average().getAsDouble();
//
//                auxIntStream = IntStream.range(0, numSamplesAverage);
//                nextProbability = auxIntStream.mapToObj(i -> obtainValues(finalResult,evidenceAugmented,new Random())).mapToDouble(this.model::getLogProbabiltyOf).average().getAsDouble();
//            }
//            else {
//                currentProbability=this.model.getLogProbabiltyOf(initialGuess);
//                nextProbability=this.model.getLogProbabiltyOf(result);
//            }


            if (nextProbability > currentProbability) {
                currentAssignment = nextAssignment;
                currentProbability = nextProbability;
            }

            R = R - 1;
        }

        return new WeightedAssignment(currentAssignment, currentProbability);

    }

    /*
* "Hill climbing": changes ONE variable at each iteration. If improves, accept.
*/
    private WeightedAssignment hillClimbingOneVar(Assignment initialGuess) {
        //Assignment result = new HashMapAssignment(initialGuess);

        double R=this.numberOfIterations;;
        double eps=0;

        //Random random = new Random();

        Assignment currentAssignment=new HashMapAssignment(initialGuess);
        double currentProbability=estimateProbabilityOfPartialAssignment(currentAssignment);


        Assignment nextAssignment;
        double nextProbability;

        while (R>eps) {

            nextAssignment = moveDiscreteVariables(currentAssignment, 3);
            nextAssignment = assignContinuousVariables(nextAssignment);

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

            //currentProbability=this.model.getLogProbabiltyOf(initialGuess);
            //nextProbability=this.model.getLogProbabiltyOf(result);

            nextProbability=estimateProbabilityOfPartialAssignment(nextAssignment);


            if (nextProbability > currentProbability) {
                currentAssignment = nextAssignment;
                currentProbability = nextProbability;
            }

            //System.out.println(currentAssignment.outputString(this.MAPvariables));

            R = R - 1;
        }

        return new WeightedAssignment(currentAssignment,currentProbability);

    }
//
//    /*
//    * "Hill climbing": changes ONE variable at each iteration. If improves, accept.
//    */
//    private Assignment hillClimbingOneVar(Assignment initialGuess) {
//        Assignment result = new HashMapAssignment(initialGuess);
//
//        double R=50;
//        double eps=0;
//
//        double currentProbability;
//        double nextProbability;
//
//        while (R>eps) {
//
//            Random random = new Random(this.seed);
//            int indexSelectedVariable = random.nextInt(this.model.getNumberOfVars());
//            double selectedVariableNewValue;
//
//            // Choose a new value for ONE of the variables and check whether the probability grows or not
//            Variable selectedVariable = this.model.getVariables().getVariableById(indexSelectedVariable);
//
//
//            ConditionalDistribution cd = this.model.getConditionalDistributions().get(indexSelectedVariable);
//            selectedVariableNewValue = cd.getUnivariateDistribution(initialGuess).sample(random);
//
//
//            result.setValue(selectedVariable,selectedVariableNewValue);
//
//            currentProbability=this.model.getLogProbabiltyOf(initialGuess);
//            nextProbability=this.model.getLogProbabiltyOf(result);
//
//            if (nextProbability>currentProbability) {
//                initialGuess=result;
//            }
//
//            R = R - 1;
//        }
//
//        return result;
//
//    }





    private Assignment moveDiscreteVariables(Assignment initialGuess, int numberOfMovements) {

        Assignment result = new HashMapAssignment(initialGuess);
        Random random = new Random();
        ArrayList<Integer> indicesVariablesMoved = new ArrayList<>();

        if(numberOfMovements > numberOfDiscreteVariables - numberOfDiscreteVariablesInEvidence) { // this.model.getNumberOfVars()-this.evidence.getVariables().size()) {
            numberOfMovements = (int) (numberOfDiscreteVariables - numberOfDiscreteVariablesInEvidence);
        }

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




    public static void main(String[] args) throws IOException, ClassNotFoundException {


        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/asia.bn");

        System.out.println(bn.toString());

        MAPInference mapInference = new MAPInference();
        mapInference.setModel(bn);
        mapInference.setParallelMode(true);

        System.out.println("CausalOrder: " + Arrays.toString(mapInference.causalOrder.stream().map(v -> v.getName()).toArray()));
        System.out.println();


        int parallelSamples=20;
        int samplingMethodSize=1000;
        mapInference.setSampleSize(parallelSamples);


        /***********************************************
         *        INCLUDING EVIDENCE
         ************************************************/

        Variable variable1 = mapInference.causalOrder.get(1);   // causalOrder: A, S, L, T, E, X, B, D
        Variable variable2 = mapInference.causalOrder.get(2);
        Variable variable3 = mapInference.causalOrder.get(4);

        int var1value=0;
        int var2value=1;
        int var3value=1;

        System.out.println("Evidence: Variable " + variable1.getName() + " = " + var1value + ", Variable " + variable2.getName() + " = " + var2value + ", " + " and Variable " + variable3.getName() + " = " + var3value);
        System.out.println();

        HashMapAssignment evidenceAssignment = new HashMapAssignment(3);

        evidenceAssignment.setValue(variable1, var1value);
        evidenceAssignment.setValue(variable2, var2value);
        evidenceAssignment.setValue(variable3, var3value);

        mapInference.setEvidence(evidenceAssignment);

        //List<Variable> modelVariables = Utils.getTopologicalOrder(bn.getDAG());


//
//        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/asia.bn");
//        System.out.println(bn.getDAG());
//
//        System.out.println(bn.toString());
//
//
//        MAPInference mapInference = new MAPInference();
//        mapInference.setModel(bn);
//        mapInference.setParallelMode(true);
//
//
//        System.out.println("CausalOrder: " + Arrays.toString(mapInference.causalOrder.stream().map(v -> v.getName()).toArray()));
//        System.out.println();
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
//        System.out.println("Evidence: Variable " + variable1.getName() + " = " + var1value + ", Variable " + variable2.getName() + " = " + var2value + " and Variable " + variable3.getName() + " = " + var3value);
//        System.out.println();
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
//        //System.out.println(evidenceAssignment.outputString(modelVariables));



        long timeStart;
        long timeStop;
        double execTime;
        Assignment mapEstimate;


        /*
        // MAP INFERENCE WITH A SMALL SAMPLE AND SIMULATED ANNEALING
        mapInference.setSampleSize(100);
        timeStart = System.nanoTime();
        mapInference.runInference(1);


        Assignment mapEstimate = mapInference.getMAPestimate();
        System.out.println("MAP estimate (SA): " + mapEstimate.outputString(modelVariables));   //toString(modelVariables)
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();


        // MAP INFERENCE WITH A BIG SAMPLE  AND SIMULATED ANNEALING
        mapInference.setSampleSize(100);
        timeStart = System.nanoTime();
        mapInference.runInference(1);

        mapEstimate = mapInference.getMAPestimate();
        modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
        System.out.println("MAP estimate (SA): " + mapEstimate.outputString(modelVariables));
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();




        // MAP INFERENCE WITH A BIG SAMPLE AND SIMULATED ANNEALING ON ONE VARIABLE EACH TIME
        mapInference.setSampleSize(100);
        timeStart = System.nanoTime();
        mapInference.runInference(0);


        mapEstimate = mapInference.getMAPestimate();
        System.out.println("MAP estimate  (SA.1V): " + mapEstimate.outputString(modelVariables));   //toString(modelVariables)
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();




        // MAP INFERENCE WITH A BIG SAMPLE  AND HILL CLIMBING
        mapInference.setSampleSize(100);
        timeStart = System.nanoTime();
        mapInference.runInference(3);

        mapEstimate = mapInference.getMAPestimate();
        modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
        System.out.println("MAP estimate (HC): " + mapEstimate.outputString(modelVariables));
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();




        // MAP INFERENCE WITH A BIG SAMPLE AND HILL CLIMBING ON ONE VARIABLE EACH TIME
        mapInference.setSampleSize(100);
        timeStart = System.nanoTime();
        mapInference.runInference(2);


        mapEstimate = mapInference.getMAPestimate();
        System.out.println("MAP estimate  (HC.1V): " + mapEstimate.outputString(modelVariables));   //toString(modelVariables)
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();





        // MAP INFERENCE WITH SIMULATION AND PICKING MAX
        mapInference.setSampleSize(100);
        timeStart = System.nanoTime();
        mapInference.runInference(-1);

        mapEstimate = mapInference.getMAPestimate();
        modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
        System.out.println("MAP estimate (SAMPLING): " + mapEstimate.outputString(modelVariables));
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();



        // MAP INFERENCE, DETERMINISTIC
        mapInference.setSampleSize(1);
        timeStart = System.nanoTime();
        mapInference.runInference(-2);

        mapEstimate = mapInference.getMAPestimate();
        modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
        System.out.println("MAP estimate (DETERM.): " + mapEstimate.outputString(modelVariables));
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();
        */



        /***********************************************
         *        VARIABLES OF INTEREST
         ************************************************/

        Variable varInterest1 = mapInference.causalOrder.get(6);  // causalOrder: A, S, L, T, E, X, B, D
        Variable varInterest2 = mapInference.causalOrder.get(7);


        List<Variable> varsInterest = new ArrayList<>();
        varsInterest.add(varInterest1);
        varsInterest.add(varInterest2);
        mapInference.setMAPVariables(varsInterest);

        System.out.println("MAP Variables of Interest: " + Arrays.toString(mapInference.MAPvariables.stream().map(Variable::getName).toArray()));
        System.out.println();



//        // MAP INFERENCE
//        mapInference.setSampleSize(1);
//        timeStart = System.nanoTime();
//        mapInference.runInference(1);
//
//        mapEstimate = mapInference.getMAPestimate();
//        System.out.println("MAP estimate: " + mapEstimate.outputString(varsInterest));
//        System.out.println("with probability: " +  + mapInference.getMAPestimateProbability());
//        timeStop = System.nanoTime();
//        execTime = (double) (timeStop - timeStart) / 1000000000.0;
//        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
//        System.out.println();


        /***********************************************
         *        SIMULATED ANNEALING
         ************************************************/


        // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference("SA_global");

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (SA.All): " + mapEstimate.outputString(varsInterest));
        System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();


        // MAP INFERENCE WITH SIMULATED ANNEALING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference("SA_local");

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (SA.Some): " + mapEstimate.outputString(varsInterest));
        System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();


        /***********************************************
         *        HILL CLIMBING
         ************************************************/

        //  MAP INFERENCE WITH HILL CLIMBING, MOVING ALL VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference("HC_global");

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (HC.All): " + mapEstimate.outputString(varsInterest));
        System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();



        //  MAP INFERENCE WITH HILL CLIMBING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference("HC_local");

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (HC.Some): " + mapEstimate.outputString(varsInterest));
        System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();


        /***********************************************
         *        SAMPLING
         ************************************************/

        // MAP INFERENCE WITH SIMULATION AND PICKING MAX
        mapInference.setSampleSize(samplingMethodSize);
        timeStart = System.nanoTime();
        mapInference.runInference("sampling");

        mapEstimate = mapInference.getEstimate();

        System.out.println("MAP estimate (SAMPLING): " + mapEstimate.outputString(varsInterest));
        System.out.println("with probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();







        // PROBABILITIES OF INDIVIDUAL CONFIGURATIONS
        Variable varB=bn.getVariables().getVariableByName("B");
        Variable varD=bn.getVariables().getVariableByName("D");

        double s1 = mapInference.estimateRandomlyProbabilityOfPartialAssignment(mapEstimate);
        System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s1);

        mapEstimate.setValue(varD, 1);
        double s2 = mapInference.estimateRandomlyProbabilityOfPartialAssignment(mapEstimate);
        System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s2);

        mapEstimate.setValue(varB, 1);
        mapEstimate.setValue(varD, 0);
        double s3 = mapInference.estimateRandomlyProbabilityOfPartialAssignment(mapEstimate);
        System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s3);

        mapEstimate.setValue(varD, 1);
        double s4 = mapInference.estimateRandomlyProbabilityOfPartialAssignment(mapEstimate);
        System.out.println(mapEstimate.outputString(varsInterest) + " with prob. " + s4);

        double sum = s1+s2+s3+s4;

        System.out.println();
        System.out.println("Sum = " + sum + "; Normalized probs: [B=0,D=0]=" + s1/sum + ", [B=0,D=1]=" + s2/sum + ", [B=1,D=0]=" + s3/sum + ", [B=1,D=1]=" + s4/sum );
        System.out.println("Exact probs: 0.48, 0.12, 0.04, 0.36");


    }

}
