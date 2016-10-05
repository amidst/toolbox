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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.RobustOperations;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * This class implements the interface {@link PointEstimator} and defines the MAP Inference algorithm.
 */
public class MAPInferenceRobustNew implements PointEstimator {

    public enum SearchAlgorithm {
        SAMPLING, SA_LOCAL, SA_GLOBAL,
        HC_LOCAL, HC_GLOBAL
    }

    private BayesianNetwork model;
    private List<Variable> causalOrder;

    private int numberOfStartingPoints = 10;
    private int sampleSize = 10000;
    private int numberOfIterations=100;
    private int localSearchSteps=3;

    private int sampleSizeForEstimatingProbabilities = 500;
    private int seed = 0;
    private Random MAPRandom;

    private Assignment evidence;
    private List<Variable> varsEvidence;
    private List<Variable> continuousVarsWithChildrenInEvidence;


//    private int numberOfDiscreteVariables = 0;
//    private int numberOfDiscreteVariablesInEvidence = 0;
    private int numberOfDiscreteVariablesOfInterest = 0;

    private ImportanceSamplingCLG importanceSamplingCLG;

    private boolean parallelMode = true;

    private List<Variable> MAPVariables;
    private List<Variable> MAPDiscreteVariables;

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

    public MAPInferenceRobustNew() {

        this.evidence = new HashMapAssignment(0);
        this.sampleSize = 10;
        MAPRandom = new Random();
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
        MAPRandom =new Random(seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(BayesianNetwork model_) {
        this.model = model_;
        this.causalOrder = Utils.getTopologicalOrder(this.model.getDAG());
//        this.numberOfDiscreteVariables = (int)model.getVariables().getListOfVariables().stream().filter(Variable::isMultinomial).count();
//        System.out.println("Discrete vars:" + numberOfDiscreteVariables);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEvidence(Assignment evidence_) {

        if (evidence_.getVariables().stream().anyMatch(MAPVariables::contains)) {
            throw new UnsupportedOperationException("The evidence should not include any of the MAP variabes");
        }
        this.evidence = evidence_;
        this.varsEvidence = evidence.getVariables().stream().collect(Collectors.toList());
//        this.numberOfDiscreteVariablesInEvidence = (int)varsEvidence.stream().filter(Variable::isMultinomial).count();
//        System.out.println("Discrete vars in evidence:" + numberOfDiscreteVariablesInEvidence);

        this.findContinuousVariablesWithObservedChildren();
    }

    public void setNumberOfStartingPoints(int numberOfStartingPoints) {
        this.numberOfStartingPoints = numberOfStartingPoints;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }


    public void setMAPVariables(List<Variable> varsOfInterest1) {
        this.MAPVariables = varsOfInterest1;
        this.numberOfDiscreteVariablesOfInterest = (int)varsOfInterest1.stream().filter(Variable::isMultinomial).count();
        MAPDiscreteVariables = MAPVariables.stream().filter(Variable::isMultinomial).collect(Collectors.toList());
//        System.out.println("Discrete vars of interest:" + numberOfDiscreteVariablesOfInterest);
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = numberOfIterations;
    }

    public void setSampleSizeEstimatingProbabilities(int sampleSizeEstimatingProbability) {
        this.sampleSizeForEstimatingProbabilities = sampleSizeEstimatingProbability;
        if(importanceSamplingCLG !=null) {
            importanceSamplingCLG.setSampleSize(this.sampleSizeForEstimatingProbabilities);
        }
    }

    public void setLocalSearchSteps(int localSearchSteps) {
        this.localSearchSteps = localSearchSteps;
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
        return MAPestimate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogProbabilityOfEstimate() {
        return MAPestimateLogProbability;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference() {

        this.importanceSamplingCLG = new ImportanceSamplingCLG();
        importanceSamplingCLG.setModel(this.model);
        importanceSamplingCLG.setParallelMode(this.parallelMode);
        importanceSamplingCLG.setSampleSize(this.sampleSizeForEstimatingProbabilities);
        importanceSamplingCLG.setVariablesAPosteriori(new ArrayList<>(0));

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


        this.importanceSamplingCLG = new ImportanceSamplingCLG();
        importanceSamplingCLG.setModel(this.model);
        importanceSamplingCLG.setParallelMode(this.parallelMode);
        importanceSamplingCLG.setSampleSize(this.sampleSizeForEstimatingProbabilities);
        importanceSamplingCLG.setVariablesAPosteriori(new ArrayList<>(0));


        int thisInferenceSampleSize = this.numberOfStartingPoints;
        if(searchAlgorithm==SearchAlgorithm.SAMPLING) {
            thisInferenceSampleSize = this.sampleSize;
        }


        this.seed = new Random(this.seed).nextInt();

        WeightedAssignment weightedAssignment;
        BayesianNetworkSampler bayesianNetworkSampler = new BayesianNetworkSampler(this.model);
        bayesianNetworkSampler.setSeed(this.seed);
        Stream<Assignment> samples = bayesianNetworkSampler.sampleWithEvidence(thisInferenceSampleSize,this.evidence);

//        ImportanceSamplingCLG isSampler = new ImportanceSamplingCLG();
//        isSampler.setSeed(this.seed);
//        this.seed = new Random(this.seed).nextInt();
//        isSampler.setModel(this.model);
//        isSampler.setEvidence(this.evidence);
//        isSampler.setSampleSize(thisInferenceSampleSize);
//        isSampler.setVariablesAPosteriori(new ArrayList<>());
//        isSampler.setParallelMode(this.parallelMode);
//        //isSampler.runInference();
//        Stream<Assignment> samples = isSampler.getSamples();

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

                                double logProbDenominator = 0;
                                List<Variable> causalOrder = Utils.getTopologicalOrder(model.getDAG());
                                for(Variable var: causalOrder) {
                                    if (!MAPVariables.contains(var) && !varsEvidence.contains(var)) {
                                        Assignment parentsConfig = new HashMapAssignment();
                                        for (Variable parent : model.getDAG().getParentSet(var)) {
                                            parentsConfig.setValue(parent, assignment.getValue(parent));
                                        }
                                        logProbDenominator = logProbDenominator + model.getConditionalDistribution(var).getUnivariateDistribution(parentsConfig).getLogProbability(assignment.getValue(var));
                                    }
                                }

                                return logProb-logProbDenominator;
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

    private void findContinuousVariablesWithObservedChildren() {

        int numberOfVariables = model.getNumberOfVars();
        DAG graph = model.getDAG();
        Variable selectedVariable;
        List<Variable> contVarEvidence = new ArrayList<>();

        for( int i=0; i<numberOfVariables; i++ ) {
            selectedVariable = causalOrder.get(i);
            if (selectedVariable.isNormal() && !Double.isNaN(evidence.getValue(selectedVariable))) {
                contVarEvidence.add(selectedVariable);
            }
        }

        boolean ended=false;
        int indexCheckedVars=0;

        this.continuousVarsWithChildrenInEvidence = new ArrayList<>();

        while(!ended) {
            ended=true;

            for(;indexCheckedVars<contVarEvidence.size(); indexCheckedVars++) {

                Variable currentVariable = contVarEvidence.get(indexCheckedVars);
                ParentSet parents = graph.getParentSet(currentVariable);

                for (Variable currentParent : parents.getParents()) {
                    if (currentParent.isNormal() && !contVarEvidence.contains(currentParent)) {
                        ended = false;
                        continuousVarsWithChildrenInEvidence.add(currentParent);
                    }
                }
            }
        }

        Collections.reverse(continuousVarsWithChildrenInEvidence);
    }

    private boolean sameMAPConfigurations(Assignment configuration1, Assignment configuration2) {
        return MAPVariables.stream().allMatch(variable -> configuration1.getValue(variable)==configuration2.getValue(variable));
    }

    public Assignment generateNewConfiguration(Assignment initialMAPConfiguration, int nMovementsDiscreteVars, Random random) {

        Assignment newMAPConfiguration = new HashMapAssignment(numberOfDiscreteVariablesOfInterest);
        MAPDiscreteVariables.stream().forEachOrdered(variable -> newMAPConfiguration.setValue(variable,initialMAPConfiguration.getValue(variable)));

        /************************************************************************************
         *    MOVE SOME (OR ALL) THE VALUES OF THE DISCRETE VARIABLES OF INTEREST
         ************************************************************************************/
        int[] indicesDiscreteVarsOfInterest = new int[MAPDiscreteVariables.size()];
        for (int i = 0; i < MAPDiscreteVariables.size(); i++) {
            indicesDiscreteVarsOfInterest[i] = MAPDiscreteVariables.get(i).getVarID();
        }
        ArrayList<Integer> indicesVariablesMoved = new ArrayList<>();

        int numberOfMovements = nMovementsDiscreteVars;
        if (numberOfMovements > numberOfDiscreteVariablesOfInterest) {
            numberOfMovements = numberOfDiscreteVariablesOfInterest;
        }

        int selectedDiscreteMAPVariable;
        Variable selectedVariable;
        int newValue;

        while (indicesVariablesMoved.size() < numberOfMovements) {

            selectedDiscreteMAPVariable = random.nextInt(indicesDiscreteVarsOfInterest.length);
            selectedVariable = this.model.getVariables().getVariableById(indicesDiscreteVarsOfInterest[selectedDiscreteMAPVariable]);
            if ( indicesVariablesMoved.contains(selectedDiscreteMAPVariable)) {
                continue;
            }
            indicesVariablesMoved.add(selectedDiscreteMAPVariable);
            newValue = random.nextInt(selectedVariable.getNumberOfStates());
            newMAPConfiguration.setValue(selectedVariable, newValue);
        }
//        System.out.println("INITIAL CONFIGURATION");
//        System.out.println(initialMAPConfiguration.outputString(causalOrder));
//        System.out.println(initialMAPConfiguration.outputString(MAPVariables));


        // USE AN AUXILIARY FULL CONFIGURATION TO OBTAIN VALUES FOR THE CONTINUOUS VARIABLES
        Assignment fullConfiguration = new HashMapAssignment(numberOfDiscreteVariablesOfInterest);
        MAPDiscreteVariables.stream().forEachOrdered(variable -> fullConfiguration.setValue(variable,newMAPConfiguration.getValue(variable)));
        evidence.getVariables().forEach(variable -> fullConfiguration.setValue(variable,evidence.getValue(variable)));

        causalOrder.stream().filter(Variable::isMultinomial).forEachOrdered(variable -> {
            if (Double.isNaN(fullConfiguration.getValue(variable))) {
                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(variable).getUnivariateDistribution(fullConfiguration);
                fullConfiguration.setValue(variable,univariateDistribution.sample(random));
            }
        });
//        System.out.println("NEW MAP CONFIGURATION");
//        System.out.println(newMAPConfiguration.outputString(causalOrder));
//        System.out.println(newMAPConfiguration.outputString(MAPVariables));
//
//        System.out.println("FULL CONFIGURATION");
//        System.out.println(fullConfiguration.outputString(causalOrder));

        /************************************************************************************************************
         *   SIMULATE VALUES FOR GAUSSIANS, STARTING WITH THOSE WHOSE (CONTINUOUS) CHILDREN ARE OBSERVED
         ************************************************************************************************************/
        for(Variable currentContVariable : continuousVarsWithChildrenInEvidence) {
            if (!varsEvidence.contains(currentContVariable)) {

                Assignment parentsConfiguration = new HashMapAssignment(1);
                model.getConditionalDistribution(currentContVariable).getConditioningVariables().forEach(parent -> parentsConfiguration.setValue(parent, fullConfiguration.getValue(parent)));

                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(currentContVariable).getUnivariateDistribution(parentsConfiguration);
                double newValue1 = univariateDistribution.sample(random);
                fullConfiguration.setValue(currentContVariable, newValue1);
//                System.out.println("UNIV DISTRIB 1 FOR " + currentContVariable.getName() + ": " + univariateDistribution.toString());


                if (MAPVariables.contains(currentContVariable)) {
                    newMAPConfiguration.setValue(currentContVariable, newValue1);
                }
            }
        }

//        System.out.println("NEW MAP CONFIGURATION");
//        System.out.println(newMAPConfiguration.outputString(causalOrder));
//        System.out.println(newMAPConfiguration.outputString(MAPVariables));
//
//        System.out.println("FULL CONFIGURATION");
//        System.out.println(fullConfiguration.outputString(causalOrder));

        /************************************************************************************************************
         *   FINALLY, ASSIGN CONT. VARS. THAT DO NOT HAVE OBSERVED DESCENDANTS (ASSIGN THEIR MODE=MEAN VALUE)
         ************************************************************************************************************/
        for( int i=0; i < model.getNumberOfVars(); i++ ) {
            selectedVariable = causalOrder.get(i);
            if ( selectedVariable.isNormal() && Double.isNaN(fullConfiguration.getValue(selectedVariable))) {

                Assignment parentsConfiguration = new HashMapAssignment(1);
                model.getConditionalDistribution(selectedVariable).getConditioningVariables().forEach(parent -> parentsConfiguration.setValue(parent, fullConfiguration.getValue(parent)));

                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(selectedVariable).getUnivariateDistribution(parentsConfiguration);
                double newValue2 = univariateDistribution.getParameters()[0];
                fullConfiguration.setValue(selectedVariable, newValue2);
//                System.out.println("UNIV DISTRIB 2 FOR " + selectedVariable.getName() + ": " + univariateDistribution.toString());

                if (MAPVariables.contains(selectedVariable)) {
                    newMAPConfiguration.setValue(selectedVariable, newValue2);
                }
            }
        }

//        System.out.println("NEW MAP CONFIGURATION");
//        System.out.println(newMAPConfiguration.outputString(causalOrder));
//        System.out.println(newMAPConfiguration.outputString(MAPVariables));
//
//        System.out.println("FULL CONFIGURATION");
//        System.out.println(fullConfiguration.outputString(causalOrder));

        return newMAPConfiguration;
    }



    private String getMAPVariablesFromAssignment(Assignment assignment) {
        if (this.MAPVariables !=null) {
            Assignment MAPVarsValues = new HashMapAssignment(MAPVariables.size());
            for(Variable var : MAPVariables) {
                MAPVarsValues.setValue(var,assignment.getValue(var));
            }
            return MAPVarsValues.outputString(MAPVariables);
        }
        else {
            return assignment.outputString();
        }
    }

    private Assignment fullAssignmentToMAPassignment(Assignment fullAssignment) {
        Assignment MAPassignment = new HashMapAssignment(MAPVariables.size());
        MAPVariables.stream().forEach(MAPvar -> MAPassignment.setValue(MAPvar, fullAssignment.getValue(MAPvar)));
        return MAPassignment;
    }

    public double estimateLogProbabilityOfPartialAssignment(Assignment MAPAssignment) {

        if(this.importanceSamplingCLG == null) {
            this.importanceSamplingCLG = new ImportanceSamplingCLG();
            importanceSamplingCLG.setModel(this.model);
            importanceSamplingCLG.setParallelMode(this.parallelMode);
            importanceSamplingCLG.setSampleSize(this.sampleSizeForEstimatingProbabilities);
            importanceSamplingCLG.setVariablesAPosteriori(new ArrayList<>(0));
        }

        Assignment MAPConfigurationPlusEvidence = new HashMapAssignment(MAPVariables.size() + this.varsEvidence.size());
        MAPAssignment.getVariables().forEach(variable -> MAPConfigurationPlusEvidence.setValue(variable,MAPAssignment.getValue(variable)));
        evidence.getVariables().forEach(variable -> MAPConfigurationPlusEvidence.setValue(variable,evidence.getValue(variable)));

        importanceSamplingCLG.setEvidence(MAPConfigurationPlusEvidence);

        importanceSamplingCLG.setSeed(this.MAPRandom.nextInt());
        importanceSamplingCLG.runInference();

        return importanceSamplingCLG.getLogProbabilityOfEvidence();
    }


    private WeightedAssignment runOptimizationAlgorithm(Assignment initialGuess, SearchAlgorithm optAlgorithm) {

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
        if(optAlg>0) { // HILL CLIMBING
            R=this.numberOfIterations;
            eps=0;
            alpha=0;
        }
        else { // SIMULATED ANNEALING
            R=1000; // Temperature
            alpha=0.90; // Annealing factor
            eps=R * Math.pow(alpha,this.numberOfIterations); // Final temperature to give the max number of iterations
        }

        Assignment currentAssignment = this.fullAssignmentToMAPassignment(new HashMapAssignment(initialGuess));
        double currentLogProbability = estimateLogProbabilityOfPartialAssignment(currentAssignment);

        Assignment nextAssignment;
        double nextLogProbability;

        Random random = new Random(MAPRandom.nextInt());

        while (R>eps) {

            if (optAlg%2==0) { // GLOBAL SEARCH
                nextAssignment = generateNewConfiguration(currentAssignment, this.numberOfDiscreteVariablesOfInterest, random);
            }
            else { // LOCAL SEARCH
                nextAssignment = generateNewConfiguration(currentAssignment, this.localSearchSteps, random);
            }

            // IF THE ASSIGNMENTS ARE EQUAL, NOTHING TO DO
            if (!this.sameMAPConfigurations(currentAssignment,nextAssignment)) {

                nextLogProbability = estimateLogProbabilityOfPartialAssignment(nextAssignment);

                if (nextLogProbability > currentLogProbability) {
                    currentAssignment = nextAssignment;
                    currentLogProbability = nextLogProbability;
                }
                else if (optAlg < 0) {
//                    double diff = Math.exp(ImportanceSamplingCLG.robustDifferenceOfLogarithms(currentLogProbability, nextLogProbability));
                    double diff = Math.exp(currentLogProbability - nextLogProbability); // Not the difference in probabilities, but the ratio

                    double aux = random.nextDouble();

                    if (aux < Math.exp(- diff / R)) {
                        currentAssignment = nextAssignment;
                        currentLogProbability = nextLogProbability;

                    }
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






    public static void main(String[] args) throws IOException, ClassNotFoundException {

        int seedBayesianNetwork = 98983;
        int seedVariablesChoice = 82125;

        int samplingMethodSize = 100000;
        int startingPoints = 20;
        int numberOfIterations = 1000;


        int nVarsEvidence = 5;
        int nVarsInterest = 8;


        long timeStart;
        long timeStop;
        double execTime;
        Assignment mapEstimate;


        /**********************************************
         *    INITIALIZATION
         *********************************************/

        BayesianNetworkGenerator.setSeed(seedBayesianNetwork);

        BayesianNetworkGenerator.setNumberOfGaussianVars(20);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(20,2);
        BayesianNetworkGenerator.setNumberOfLinks(50);

        BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
        System.out.println(bn.toString());



        /****************************************************************
         *   CHOOSE VARIABLES OF INTEREST AND THOSE TO BE OBSERVED
         ****************************************************************/

        Random variablesChoiceRandom = new Random(seedVariablesChoice);

        List<Variable> varsEvidence = new ArrayList<>(nVarsEvidence);
        List<Variable> varsInterest = new ArrayList<>(nVarsInterest);

//        for (int i = 0; i < nVarsInterest; i++) {
//            varsInterest.add(bn.getVariables().getVariableById(i));
//        }
//        for (int i = 0; i < nVarsEvidence; i++) {
//            varsEvidence.add(bn.getVariables().getVariableById(nVarsInterest + i));
//        }

        while(varsEvidence.size()<nVarsEvidence) {
            int varIndex = variablesChoiceRandom.nextInt(bn.getNumberOfVars());
            Variable variable = bn.getVariables().getVariableById(varIndex);
            if (! varsEvidence.contains(variable)) {
                varsEvidence.add(variable);
            }
        }

        while(varsInterest.size()<nVarsInterest) {
            int varIndex = variablesChoiceRandom.nextInt(bn.getNumberOfVars());
            Variable variable = bn.getVariables().getVariableById(varIndex);
            if (! varsInterest.contains(variable) && ! varsEvidence.contains(variable)) {
                varsInterest.add(variable);
            }
        }

        varsEvidence.sort((variable1,variable2) -> (variable1.getVarID() > variable2.getVarID() ? 1 : -1));
        varsInterest.sort((variable1,variable2) -> (variable1.getVarID() > variable2.getVarID() ? 1 : -1));


        System.out.println("\nVARIABLES OF INTEREST:");
        varsInterest.forEach(var -> System.out.println(var.getName()));


        System.out.println("\nVARIABLES IN THE EVIDENCE:");
        varsEvidence.forEach(var -> System.out.println(var.getName()));



        /***********************************************
         *     GENERATE AND INCLUDE THE EVIDENCE
         ************************************************/

        BayesianNetworkSampler bayesianNetworkSampler = new BayesianNetworkSampler(bn);
        bayesianNetworkSampler.setSeed(variablesChoiceRandom.nextInt());
        DataStream<DataInstance> fullSample = bayesianNetworkSampler.sampleToDataStream(1);

        HashMapAssignment evidence = new HashMapAssignment(nVarsEvidence);
        varsEvidence.stream().forEach(variable -> evidence.setValue(variable,fullSample.stream().findFirst().get().getValue(variable)));

        System.out.println("\nEVIDENCE: ");
        System.out.println(evidence.outputString(varsEvidence));



        /***********************************************
         *     INITIALIZE MAP INFERENCE OBJECT
         ************************************************/

        MAPInferenceRobustNew mapInference = new MAPInferenceRobustNew();
        mapInference.setModel(bn);

        mapInference.setSampleSize(samplingMethodSize);
        mapInference.setNumberOfStartingPoints(startingPoints);
        mapInference.setNumberOfIterations(numberOfIterations);
        mapInference.setSeed(362371);

        mapInference.setParallelMode(true);

        mapInference.setMAPVariables(varsInterest);
        mapInference.setEvidence(evidence);


        DataStream<DataInstance> fullSample2 = bayesianNetworkSampler.sampleToDataStream(1);
        HashMapAssignment configuration = new HashMapAssignment(bn.getNumberOfVars());

        bn.getVariables().getListOfVariables().stream().forEach(variable -> configuration.setValue(variable,fullSample2.stream().findFirst().get().getValue(variable)));


        System.out.println();

        mapInference.setSampleSizeEstimatingProbabilities(100);

//        int nVarsMover = 3;
//        Random random = new Random(23326);
//        Assignment config2 = new HashMapAssignment(configuration);
//        config2 = mapInference.fullAssignmentToMAPassignment(config2);
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println("NEW FINAL CONFIG: " + config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//
//        config2 = (HashMapAssignment)mapInference.generateNewConfiguration(config2, nVarsMover, random);
//        System.out.println(config2.outputString(varsInterest));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));
//        System.out.println(mapInference.estimateLogProbabilityOfPartialAssignment(config2));


//         DUMB EXECUTION FOR 'HEATING UP'
        mapInference.runInference(SearchAlgorithm.SA_GLOBAL);


        /***********************************************
         *        SIMULATED ANNEALING
         ************************************************/


        // MAP INFERENCE WITH SIMULATED ANNEALING, MOVING ALL VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(SearchAlgorithm.SA_GLOBAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (SA.Global): " + mapEstimate.outputString(varsInterest));
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));
        System.out.println();


        // MAP INFERENCE WITH SIMULATED ANNEALING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(SearchAlgorithm.SA_LOCAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (SA.Local): " + mapEstimate.outputString(varsInterest));
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

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
        System.out.println("MAP estimate  (HC.Global): " + mapEstimate.outputString(varsInterest));
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();



        //  MAP INFERENCE WITH HILL CLIMBING, SOME VARIABLES EACH TIME
        timeStart = System.nanoTime();
        mapInference.runInference(SearchAlgorithm.HC_LOCAL);

        mapEstimate = mapInference.getEstimate();
        System.out.println("MAP estimate  (HC.Local): " + mapEstimate.outputString(varsInterest));
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

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
        System.out.println("with estimated (unnormalized) probability: " + Math.exp(mapInference.getLogProbabilityOfEstimate()));
        System.out.println("with estimated (unnormalized) log-probability: " + mapInference.getLogProbabilityOfEstimate());

        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;

        mapInference.setSampleSizeEstimatingProbabilities(5000);
        double estimatedProbability = mapInference.estimateLogProbabilityOfPartialAssignment(mapEstimate);
        System.out.println("with PRECISE RE-estimated probability: " + Math.exp(estimatedProbability));
        System.out.println("with PRECISE RE-estimated log-probability: " + estimatedProbability);

        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        System.out.println();




    }

}