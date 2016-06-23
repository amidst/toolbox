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

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.RobustOperations;
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
public class MAPInferenceRobust implements PointEstimator {

    public enum SearchAlgorithm {
        SAMPLING, SA_LOCAL, SA_GLOBAL,
        HC_LOCAL, HC_GLOBAL
    }

    private BayesianNetwork model;
    private List<Variable> causalOrder;

    private int sampleSize;
    private int seed = 0;
    private Random MAPrandom;
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

    public MAPInferenceRobust() {

        this.evidence = new HashMapAssignment(0);
        this.sampleSize = 10;
        MAPrandom = new Random();
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
        MAPrandom=new Random(seed);
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

    public void setNumberOfStartingPoints(int sampleSize) {
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
            return MAPVarsValues.outputString(MAPvariables);
        }
        else {
            return assignment.outputString(MAPvariables);
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
        this.runInference(SearchAlgorithm.HC_LOCAL); // Uses Hill climbing with local search, by default
    }

    /**
     * Runs inference with an specific method.
     * @param searchAlgorithm an {@code SearchAlgorithm} that represents the search algorithm to use
     *                        (SAMPLING: Sampling; SA_LOCAL: Simulated annealing, local;
     *                        SA_GLOBAL: Simulated annealing, global; HC_LOCAL: Hill climbing, local (default);
     *                        HC_GLOBAL: Hill climbing, global)
     */
    public void runInference(SearchAlgorithm searchAlgorithm) {

        WeightedAssignment weightedAssignment;

//        BayesianNetworkSampler bnSampler = new BayesianNetworkSampler(this.model);
//        Stream<Assignment > samples = bnSampler.sampleToDataStream(this.sampleSize).stream().map(sample -> (Assignment)sample);
        ImportanceSamplingRobust isSampler = new ImportanceSamplingRobust();
        isSampler.setSeed(this.seed);
        this.seed = new Random(this.seed).nextInt();
        isSampler.setModel(this.model);
        isSampler.setEvidence(this.evidence);
        isSampler.setSampleSize(this.sampleSize);
        isSampler.setVariablesAPosteriori(new ArrayList<>());
        isSampler.setParallelMode(this.parallelMode);
        //isSampler.runInference();
        Stream<Assignment> samples = isSampler.getSamples();

        samples = samples.sequential();
        if(this.parallelMode) {
            samples = samples.parallel();
        }
        switch(searchAlgorithm) {
            case SAMPLING:
//                System.out.println("Using " + searchAlgorithm);

                Map<Assignment, List<Assignment>> groupedSamples =
                        samples.collect(Collectors.groupingBy(this::getMAPVariablesFromAssignment))
                                .values().stream()
                                .collect(Collectors.toMap(lst -> lst.get(0), lst->lst));

                Map<Assignment, Double> newMap = new HashMap<>();

                for(Map.Entry<Assignment, List<Assignment>> entry : groupedSamples.entrySet()) {
//                    newMap.put(fullAssignmentToMAPassignment(entry.getKey()), entry.getValue().stream().mapToDouble(this::getProbabilityOf).average().getAsDouble());
                    long groupSize = entry.getValue().size();
//                    System.out.println("Group size: " + groupSize);
                    newMap.put(fullAssignmentToMAPassignment(entry.getKey()), entry.getValue().stream()
                            .mapToDouble(assignment -> {
                                double logProb = this.model.getLogProbabiltyOf(assignment);
//                                System.out.println(logProb);
                                return logProb;
                            })
                            .reduce(RobustOperations::robustSumOfLogarithms)
                            .getAsDouble() - Math.log(groupSize) );
                }

                Map.Entry<Assignment, Double> MAPentry = newMap.entrySet().stream().reduce((e1, e2) -> (e1.getValue() > e2.getValue() ? e1 : e2)).get();

                MAPestimate = MAPentry.getKey();
//                MAPestimateLogProbability = Math.log(MAPentry.getValue());
                MAPestimateLogProbability = MAPentry.getValue();
                break;

            default:   // HILL CLIMBING OR SIMULATED ANNEALING METHODS WITH DIFFERENT STARTING POINTS
//                System.out.println("Using " + searchAlgorithm);

                weightedAssignment = samples
                        .map(sample -> this.runOptimizationAlgorithm(sample, searchAlgorithm))
                        .filter(partialResult -> {
//                            System.out.println("Partial result weight: " + partialResult.weight);
                            return Double.isFinite(partialResult.weight);
                        })
                        .reduce((wa1, wa2) -> (wa1.weight > wa2.weight ? wa1 : wa2)).get();
                MAPestimate = fullAssignmentToMAPassignment(weightedAssignment.assignment);
//                MAPestimateLogProbability = Math.log(weightedAssignment.weight);
                MAPestimateLogProbability = weightedAssignment.weight;
                break;

        }
    }


    private Assignment obtainValues(Assignment initialGuess, Assignment evidence, Random random) {

        int numberOfVariables = this.model.getNumberOfVars();
        Assignment result = new HashMapAssignment(initialGuess);
        List<Variable> contVarEvidence = new ArrayList<>();

        Variable selectedVariable;
        ConditionalDistribution conDist;
        double selectedVariableNewValue=0;



        // FIRST, ASSIGN VALUES FOR ALL DISCRETE VARIABLES
        for( int i=0; i<numberOfVariables; i++ ) {

            //selectedVariable = this.model.getStaticVariables().getVariableById(i);
            selectedVariable = causalOrder.get(i);
            conDist = this.model.getConditionalDistribution(selectedVariable);

            if (selectedVariable.isMultinomial() && Double.isNaN(evidence.getValue(selectedVariable))) {


                try {
                    Assignment parentsConfiguration = new HashMapAssignment(1);
                    conDist.getConditioningVariables().forEach(parent -> parentsConfiguration.setValue(parent, result.getValue(parent)));
                    UnivariateDistribution uniDist = conDist.getUnivariateDistribution(parentsConfiguration);
                    selectedVariableNewValue = uniDist.sample(random);
                    result.setValue(selectedVariable, selectedVariableNewValue);
                }
                catch (Exception e) {
//                    System.out.println("Exception");
//                    System.out.println(selectedVariable.getName());
//                    System.out.println(result.outputString());
//                    System.out.println(conDist.toString());
//                    System.out.println(selectedVariableNewValue);
//                    //System.out.println(conDist.getUnivariateDistribution(result));
//                    System.exit(-20);
                }
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
                Assignment parentsConfiguration = new HashMapAssignment(1);
                model.getConditionalDistribution(current).getConditioningVariables().forEach(parent -> parentsConfiguration.setValue(parent, result.getValue(parent)));
                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(current).getUnivariateDistribution(parentsConfiguration);
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
                Assignment parentsConfiguration = new HashMapAssignment(1);
                model.getConditionalDistribution(selectedVariable).getConditioningVariables().forEach(parent -> parentsConfiguration.setValue(parent, result.getValue(parent)));
                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(selectedVariable).getUnivariateDistribution(parentsConfiguration);
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

    protected double estimateLogProbabilityOfPartialAssignment(Assignment MAPassignment) {
        return estimateLogProbabilityOfPartialAssignment(MAPassignment,true);
    }

    private double estimateLogProbabilityOfPartialAssignment(Assignment MAPassignment, boolean useConditionalDistributions) {

        double logProbabilityEstimate;
        final int numSamplesAverage = 200;

        Assignment evidenceAugmented=new HashMapAssignment(evidence);
        MAPvariables.forEach(voi -> evidenceAugmented.setValue(voi, MAPassignment.getValue(voi)));

        final Assignment finalAssignment=new HashMapAssignment(MAPassignment);

        IntStream auxIntStream = IntStream.range(0, numSamplesAverage);
        //logProbabilityEstimate = auxIntStream.mapToObj(i -> obtainValuesRandomly(finalAssignment,evidenceAugmented,new Random())).mapToDouble(as -> Math.exp(this.model.getLogProbabiltyOf(as))).average().getAsDouble();
        try {
            logProbabilityEstimate = auxIntStream.mapToObj(i -> {
                if (useConditionalDistributions)
                    return obtainValues(finalAssignment, evidenceAugmented, new Random(MAPrandom.nextInt()));
                else
                    return obtainValuesRandomly(finalAssignment, evidenceAugmented, new Random(MAPrandom.nextInt()));
            })
//                .mapToDouble(as -> Math.exp(this.model.getLogProbabiltyOf(as)))
//                .filter(Double::isFinite).average().getAsDouble();
                    .mapToDouble(as -> this.model.getLogProbabiltyOf(as))
                    //.filter(Double::isFinite)
                    .reduce(RobustOperations::robustSumOfLogarithms)
                    .getAsDouble() - Math.log(numSamplesAverage);
        }
        catch(Exception e) {
            logProbabilityEstimate=0;
        }

        return logProbabilityEstimate;
    }



    private WeightedAssignment runOptimizationAlgorithm(Assignment initialGuess, SearchAlgorithm optAlgorithm) {

        final int movingVariablesLocalSearch = 3;
        int optAlg;
        switch(optAlgorithm) {
            case SA_GLOBAL:
                optAlg=-2;
                break;
            case SA_LOCAL:
                optAlg=-1;
                break;
            case HC_GLOBAL:
                optAlg=2;
                break;
            case HC_LOCAL:
            default:
                optAlg=1;
        }


        double R, alpha, eps;
        if(optAlg>0) { // Hill climbing
            R=this.numberOfIterations;
            eps=0;
            alpha=0;
        }
        else { // Simulated annealing
            R=1000; // Temperature
            alpha=0.90; // Annealing factor
            eps=R * Math.pow(alpha,this.numberOfIterations); // Final temperature
        }

//        System.out.println("Initial log-prob: " + model.getLogProbabiltyOf(initialGuess));


        Assignment currentAssignment = this.fullAssignmentToMAPassignment(new HashMapAssignment(initialGuess));
        double currentLogProbability = estimateLogProbabilityOfPartialAssignment(currentAssignment, true);
//        System.out.println("Current log-prob: " + currentLogProbability);

        Assignment nextAssignment;
        double nextLogProbability;

        Random random = new Random(MAPrandom.nextInt());

        while (R>eps) {

            if (optAlg%2==0) { // GLOBAL SEARCH
                nextAssignment = obtainValues(currentAssignment, evidence, random);
            }
            else { // LOCAL SEARCH
                nextAssignment = moveDiscreteVariables(currentAssignment, movingVariablesLocalSearch);
                nextAssignment = assignContinuousVariables(nextAssignment);
            }
//            System.out.println("Current log-prob: " + currentLogProbability);

            nextLogProbability = estimateLogProbabilityOfPartialAssignment(nextAssignment, true);
//            System.out.println("Next log-prob: " + nextLogProbability);
//            System.out.println();
            if (nextLogProbability > currentLogProbability) {
                currentAssignment = nextAssignment;
                currentLogProbability = nextLogProbability;
            }
            else if (optAlg<0) {
//                System.out.println("Simulated Annealing");
//                double diff = currentLogProbability - nextLogProbability;
                double diff = Math.exp(RobustOperations.robustDifferenceOfLogarithms(currentLogProbability, nextLogProbability));
//                System.out.println("Current: " + currentLogProbability + ", next: " + nextLogProbability + ", sum: " + ImportanceSamplingRobust.robustDiffereceOfLogarithms(currentLogProbability, -nextLogProbability) + ", diff: " + diff);
                if(diff<0) {
                    System.out.println("Error in Simulated Annealing from MAPInferenceRobust");
                    System.exit(-50);
                }

                double aux = random.nextDouble();

                if (aux < Math.exp( -diff/R )) {
                    currentAssignment = nextAssignment;
                    currentLogProbability = nextLogProbability;
                }
            }

            if (optAlg>0) {
                R = R - 1;
            }
            else {
                R = alpha*R;
            }
        }
        return new WeightedAssignment(currentAssignment,currentLogProbability);
    }



    private Assignment moveDiscreteVariables(Assignment initialGuess, int numberOfMovements) {

        Assignment result = new HashMapAssignment(initialGuess);
        Random random = new Random(MAPrandom.nextInt());
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
        Random random = new Random(MAPrandom.nextInt());
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


        BayesianNetworkGenerator.setSeed(2152364);

        BayesianNetworkGenerator.setNumberOfGaussianVars(100);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(100,2);
        BayesianNetworkGenerator.setNumberOfLinks(250);

        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();


        System.out.println(bn.toString());

        MAPInferenceRobust mapInference = new MAPInferenceRobust();
        mapInference.setModel(bn);
        mapInference.setParallelMode(true);

        System.out.println("CausalOrder: " + Arrays.toString(mapInference.causalOrder.stream().map(v -> v.getName()).toArray()));
        System.out.println();


        int parallelSamples=10;
        int samplingMethodSize=100000;
        mapInference.setNumberOfStartingPoints(parallelSamples);
        mapInference.setNumberOfIterations(200);
        //mapInference.setParallelMode(false);

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


        long timeStart;
        long timeStop;
        double execTime;
        Assignment mapEstimate;



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


        /***********************************************
         *        SIMULATED ANNEALING
         ************************************************/


        // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(SearchAlgorithm.SA_GLOBAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (SA.All): " + mapEstimate.outputString(varsInterest));
        System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();


        // MAP INFERENCE WITH SIMULATED ANNEALING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(SearchAlgorithm.SA_LOCAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (SA.Some): " + mapEstimate.outputString(varsInterest));
        System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

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
        mapInference.runInference(SearchAlgorithm.HC_GLOBAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (HC.All): " + mapEstimate.outputString(varsInterest));
        System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();



        //  MAP INFERENCE WITH HILL CLIMBING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(SearchAlgorithm.HC_LOCAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (HC.Some): " + mapEstimate.outputString(varsInterest));
        System.out.println("with (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();


        /************************************************
         *        SAMPLING
         ************************************************/

        // MAP INFERENCE WITH SIMULATION AND PICKING MAX
        mapInference.setNumberOfStartingPoints(samplingMethodSize);
        timeStart = System.nanoTime();
        mapInference.runInference(SearchAlgorithm.SAMPLING);

        mapEstimate = mapInference.getEstimate();

        System.out.println("MAP estimate (SAMPLING): " + mapEstimate.outputString(varsInterest));
        System.out.println("with probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();




    }

}