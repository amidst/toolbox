/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.inference;

//import cern.jet.random.engine.RandomGenerator;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.inference.messagepassing.VMP;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.*;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.ParentSet;

import eu.amidst.core.utils.Utils;
import eu.amidst.core.utils.LocalRandomGenerator;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Created by dario on 01/06/15.
 */
public class MAPInference implements InferenceAlgorithm {

    private BayesianNetwork model;
    private List<Variable> causalOrder;

    private int sampleSize;
    private int seed = 0;
    //TODO The sampling distributions must be restricted to the evidence
    private Assignment evidence;
    private Assignment MAPestimate;
    private boolean parallelMode = true;


    public void setParallelMode(boolean parallelMode_) {
        this.parallelMode = parallelMode_;
    }

    public MAPInference() {

    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    @Override
    public void setSeed(int seed) {
        this.seed=seed;
    }

    @Override
    public void setModel(BayesianNetwork model_) {
        this.model = model_;
        this.causalOrder = Utils.getCausalOrder(this.model.getDAG());
    }

    @Override
    public BayesianNetwork getOriginalModel() {
        return this.model;
    }

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

    public Assignment getMAPestimate() {
        return MAPestimate;
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

    @Override
    public void runInference() {
        this.runInference(1);
    }


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
            case -1:    // NO OPTIMIZATION ALGORITHM, JUST PICKING THE SAMPLE WITH HIGHEST PROBABILITY
                MAPestimate = sample.reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                break;
            case -2:   // DETERMINISTIC, MAY BE VERY SLOW ON BIG NETWORKS
                MAPestimate = this.sequentialSearch();
                break;
            case 0:     // ORIGINAL SIMULATED ANNEALING
                MAPestimate = sample.map(this::simulatedAnnealing).reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                break;
            default:    // IMPROVED SIMULATED ANNEALING
                MAPestimate = sample.map(this::improvedSimulatedAnnealing2).reduce((s1, s2) -> (model.getLogProbabiltyOf(s1) > model.getLogProbabiltyOf(s2) ? s1 : s2)).get();
                break;
        }
    }

    private void CLGmode(Assignment values, Variable currentVariable, Variable children) {

        if (!Double.isNaN(evidence.getValue(currentVariable))) {
            return;
        }

        if (!Double.isNaN(values.getValue(currentVariable))) {
            return;
        }

        if (currentVariable.isMultinomial()) {
            return;
        }

        ParentSet parents = model.getDAG().getParentSet(currentVariable);

        boolean allParentsAreDiscrete=true;
        for( int j=0; j<parents.getNumberOfParents(); j++) {

            Variable parent = parents.getParents().get(j);

            if(parent.isNormal()) {
                CLGmode(values, parent, currentVariable);
                allParentsAreDiscrete=false;
                UnivariateDistribution distParent = model.getConditionalDistribution(currentVariable).getUnivariateDistribution(values);

                System.out.println(distParent.toString());
            }
        }
    }


    private Assignment improvedSimulatedAnnealing2(Assignment initialGuess) {
        Assignment result = this.evidence;

        double R=0.5; // Temperature
        double eps=0.1;
        double alpha=0.00008;

        double currentProbability;
        double nextProbability;
        int numberOfVariables = this.model.getNumberOfVars();

        Variable selectedVariable;
        double selectedVariableNewValue;
        ConditionalDistribution conDist;


        //Random random = new Random(this.seed+initialGuess.hashCode());
        Random random = new Random();
        while (R>eps) {
            result = initialGuess;
            List<Variable> contVarEvidence = new ArrayList<>();

            // ASSIGN FIRSTLY VALUES FOR DISCRETE VARIABLES
            for( int i=0; i<numberOfVariables; i++ ) {

                //selectedVariable = this.model.getStaticVariables().getVariableById(i);
                selectedVariable = causalOrder.get(i);
                conDist = this.model.getConditionalDistributions().get(i);

                if (selectedVariable.isMultinomial() && Double.isNaN(this.evidence.getValue(selectedVariable))) {

                    selectedVariableNewValue = conDist.getUnivariateDistribution(result).sample(random); // ??? Is this correct?
                    result.setValue(selectedVariable, selectedVariableNewValue);
                }

                if ( selectedVariable.isNormal() && !Double.isNaN(this.evidence.getValue(selectedVariable))) {
                    contVarEvidence.add(selectedVariable);
                }

            }

            // NOW THAT ALL DISCRETE VARIABLES HAVE VALUES, SET VALUES FOR GAUSSIANS, STARTING WITH ANCESTORS OF OBSERVED VARS
            boolean ended=false;
            int indexCV=0;
            int indexMAX=contVarEvidence.size();
            while(!ended) {
                ended=true;

                for(;indexCV<indexMAX; indexCV++) {

                    indexMAX=contVarEvidence.size();
                    Variable currentVariable = contVarEvidence.get(indexCV);
                    ParentSet parents = model.getDAG().getParentSet(currentVariable);

                    //System.out.println("Children: " + currentVariable.getName() + model.getConditionalDistribution(currentVariable).toString());
                    for (Variable currentParent : parents.getParents()) {
                        if (currentParent.isNormal()) {
                            ended = false;
                            contVarEvidence.add(currentParent);
                            //double currentParentNewValue=0;
                            //System.out.println("Parent: " + currentParent.getName() + model.getConditionalDistribution(currentParent).toString());
                            //System.out.println(model.getConditionalDistribution(currentParent).getUnivariateDistribution(result).toString());
                        }
                    }
                }
            }

            for(Variable current : contVarEvidence) {
                System.out.println(current.getName());
            }

            Collections.reverse(contVarEvidence);

            for(Variable current : contVarEvidence) {
                System.out.println(current.getName());
                System.out.println(model.getConditionalDistribution(current).getUnivariateDistribution(result).toString());
                UnivariateDistribution univariateDistribution = model.getConditionalDistribution(current).getUnivariateDistribution(result);
                double newValue = univariateDistribution.sample(random);
                result.setValue(current,newValue);
            }

            for( int i=0; i<numberOfVariables; i++ ) {

                //selectedVariable = this.model.getStaticVariables().getVariableById(i);
                selectedVariable = causalOrder.get(i);
                conDist = this.model.getConditionalDistributions().get(i);


                if ( selectedVariable.isNormal() && Double.isNaN(this.evidence.getValue(selectedVariable))) {
                    UnivariateDistribution univariateDistribution = model.getConditionalDistribution(selectedVariable).getUnivariateDistribution(result);
                    double newValue = univariateDistribution.sample(random);
                    result.setValue(selectedVariable, newValue);
                }

            }


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


    public BayesianNetwork changeCausalOrder(BayesianNetwork bn, Assignment evidence) {

        BayesianNetwork result = BayesianNetwork.newBayesianNetwork(bn.getDAG(),bn.getConditionalDistributions());
        int numberOfVariables = bn.getNumberOfVars();

        causalOrder = Utils.getCausalOrder(bn.getDAG());

        // while(!acabado) .......
        boolean acabado=false;
        for( int i=0; i<numberOfVariables; i++ ) {
            Variable var = causalOrder.get(i);



            if( var.isNormal() && !Double.isNaN(evidence.getValue(var))) {

                ConditionalDistribution condChildren = bn.getConditionalDistribution(var);

                System.out.println(condChildren.toString());
                System.out.println(Arrays.toString(condChildren.getParameters()));



                if (condChildren instanceof Normal_MultinomialParents) {
                    // DO NOTHING
                    System.out.println("1");
                }

                if (condChildren instanceof Normal_MultinomialNormalParents) {
                    System.out.println("2");
                }

                if (condChildren instanceof ConditionalLinearGaussian) {
                    System.out.println("3");
                }

                ParentSet parentSet = bn.getDAG().getParentSet(var);
                List<Variable> listParents = parentSet.getParents();

                for(Variable parent : listParents) {
                    if(parent.isNormal()) {
                        acabado=false;

                        System.out.println(bn.getConditionalDistribution(parent).toString());


                        List<Variable> listGrandParents = bn.getDAG().getParentSet(parent).getParents();
                    }

                }
            }
        }
        return result;
    }


    private Assignment improvedSimulatedAnnealing(Assignment initialGuess) {
        Assignment result = this.evidence;

        double R=0.5; // Temperature
        double eps=0.1;
        double alpha=0.00008;

        double currentProbability;
        double nextProbability;
        int numberOfVariables = this.model.getNumberOfVars();

        Variable selectedVariable;
        double selectedVariableNewValue;
        ConditionalDistribution conDist;


        //Random random = new Random(this.seed+initialGuess.hashCode());
        Random random = new Random();
        while (R>eps) {
            result = initialGuess;

            // ASSIGN FIRSTLY VALUES FOR DISCRETE VARIABLES
            for( int i=0; i<numberOfVariables; i++ ) {

                //selectedVariable = this.model.getStaticVariables().getVariableById(i);
                selectedVariable = causalOrder.get(i);
                conDist = this.model.getConditionalDistributions().get(i);

                if (selectedVariable.isMultinomial() && Double.isNaN(this.evidence.getValue(selectedVariable))) {

                    selectedVariableNewValue = conDist.getUnivariateDistribution(result).sample(random); // ??? Is this correct?
                    result.setValue(selectedVariable, selectedVariableNewValue);
                }


            }

            // NOW ALL DISCRETE VARIABLES HAVE VALUES, SET VALUES FOR GAUSSIANS, STARTING WITH ANCESTORS OF OBSERVED VARS
            for( int i=0; i<numberOfVariables; i++ ) {

                //selectedVariable = this.model.getStaticVariables().getVariableById(i);
                selectedVariable = causalOrder.get(i);
                conDist = this.model.getConditionalDistributions().get(i);



                if ( selectedVariable.isNormal() && !Double.isNaN(this.evidence.getValue(selectedVariable))) {




                    ParentSet parents = model.getDAG().getParentSet(selectedVariable);

                    for( int j=0; j<parents.getNumberOfParents(); j++) {

                        //System.out.println(i + "," + j);
                        Variable parent = parents.getParents().get(j);
                        //System.out.println(parent.getName());

                        CLGmode(result, parent, selectedVariable);

                                /*
                        if(parent.isNormal()) {
                            System.out.println(model.getConditionalDistribution(parent).toString());
                            System.out.println(model.getConditionalDistribution(parent).getUnivariateDistribution(result).toString());
                        }*/
                    }


                    /*if (selectedVariable.isMultinomial()) {
                        selectedVariableNewValue = conDist.getUnivariateDistribution(result).sample(random); // ??? Is this correct?

                    } else {

                        selectedVariableNewValue = conDist.getUnivariateDistribution(result).getParameters()[0];
                    }
                    result.setValue(selectedVariable, selectedVariableNewValue);*/
                }
            }

            /*for( int i=0; i<numberOfVariables; i++ ) {

                //selectedVariable = this.model.getStaticVariables().getVariableById(i);
                selectedVariable = causalOrder.get(i);
                conDist = this.model.getConditionalDistributions().get(i);
                if (R==5000) {
                    System.out.println(selectedVariable.getName());
                    if(selectedVariable.getName().equals("GaussianVar0")) {
                        System.out.println(conDist.toString());
                        System.out.println(conDist.getUnivariateDistribution(result).toString());
                    }
                    if(selectedVariable.getName().equals("GaussianVar9")) {
                        System.out.println(model.getDAG().getParentSet(selectedVariable).toString());
                    }
                }
                if ( Double.isNaN(this.evidence.getValue(selectedVariable))) {
                    if (selectedVariable.isMultinomial()) {
                        selectedVariableNewValue = conDist.getUnivariateDistribution(result).sample(random); // ??? Is this correct?

                    } else {

                        selectedVariableNewValue = conDist.getUnivariateDistribution(result).getParameters()[0];
                    }
                    result.setValue(selectedVariable, selectedVariableNewValue);
                }
            }*/




            /*
            result = initialGuess;

            int indexSelectedVariable; // = random.nextInt(this.model.getNumberOfVars());

            //selectedVariable = this.model.getStaticVariables().getVariableById(indexSelectedVariable);
            //conDist = this.model.getConditionalDistributions().get(indexSelectedVariable);

            do {
                indexSelectedVariable = random.nextInt(this.model.getNumberOfVars());

                selectedVariable = this.model.getStaticVariables().getVariableById(indexSelectedVariable);
                conDist = this.model.getConditionalDistributions().get(indexSelectedVariable);

            } while( !selectedVariable.isMultinomial() );

            selectedVariableNewValue = conDist.getUnivariateDistribution(result).sample(random); // ??? Is this correct?

            result.setValue(selectedVariable,selectedVariableNewValue);


            for( int i=0; i<numberOfVariables; i++ ) {


                selectedVariable = this.model.getStaticVariables().getVariableById(i);

                if ( Double.isNaN(this.evidence.getValue(selectedVariable))) {
                    conDist = this.model.getConditionalDistributions().get(i);

                    if (selectedVariable.isNormal()) {
                        selectedVariableNewValue = conDist.getUnivariateDistribution(result).getParameters()[0];
                        result.setValue(selectedVariable, selectedVariableNewValue);
                    }
                    //System.out.println("variable not in evidence");

                }
                //else {
                    //System.out.println("Variable in evidence");
                //}
            }
            */

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

    private Assignment bestConfig(Assignment current, int varIndex) {

        int numVars=this.model.getNumberOfVars();

        if (varIndex>(numVars-1)) {
            return current;
        }

        Variable currentVariable = this.model.getStaticVariables().getVariableById(varIndex);

        if (Double.isNaN(this.evidence.getValue(currentVariable))) {

            // DUPLICATE CURRENT ASSIGNMENT
            Assignment config0 = new HashMapAssignment(numVars);
            Assignment config1 = new HashMapAssignment(numVars);

            for(Variable var: model.getStaticVariables()) {

                if ( Double.isNaN(this.evidence.getValue(var))) {
                    config0.setValue(var,current.getValue(var));
                    config1.setValue(var,current.getValue(var));
                }
                else {
                    config0.setValue(var,evidence.getValue(var));
                    config1.setValue(var,evidence.getValue(var));
                }
            }

            if (currentVariable.isMultinomial()) {

                config0.setValue(currentVariable, 0);
                config1.setValue(currentVariable, 1);

                if (varIndex < (numVars - 1)) {
                    config0 = bestConfig(config0, varIndex + 1);
                    config1 = bestConfig(config1, varIndex + 1);
                }
                return (model.getLogProbabiltyOf(config0) > model.getLogProbabiltyOf(config1) ? config0 : config1);
            }
            else {
                double newValue;

                newValue = model.getConditionalDistributions().get(varIndex).getUnivariateDistribution(config0).getParameters()[0];

                config0.setValue(currentVariable, newValue);
                if (varIndex < (numVars - 1)) {
                    config0 = bestConfig(config0, varIndex + 1);
                }
                return config0;
            }
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

    private Assignment sequentialSearch() {

        int numberOfVariables = this.model.getNumberOfVars();
        Assignment currentEstimator = new HashMapAssignment(numberOfVariables);

        Variable selectedVariable;
        double selectedVariableNewValue;
        ConditionalDistribution conDist;

        // INITIALIZE THE ESTIMATOR
        for( int i=0; i<numberOfVariables; i++ ) {

            selectedVariable = this.model.getStaticVariables().getVariableById(i);
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


    private Assignment simulatedAnnealing(Assignment initialGuess) {
        Assignment result = initialGuess;

        double R=1000; // Temperature
        double eps=0.01;
        double alpha=0.7;

        double currentProbability;
        double nextProbability;

        while (R>eps) {

            Random random = new Random(this.seed);
            int indexSelectedVariable = random.nextInt(this.model.getNumberOfVars());
            double selectedVariableNewValue;

            // Choose a new value for ONE of the variables and check whether the probability grows or not
            Variable selectedVariable = this.model.getStaticVariables().getVariableById(indexSelectedVariable);


            //if (selectedVariable.isMultinomial()) {
            //selectedVariableNewValue = selectedVariable

            ConditionalDistribution cd = this.model.getConditionalDistributions().get(indexSelectedVariable);
            selectedVariableNewValue = cd.getUnivariateDistribution(initialGuess).sample(random); // ??? Is this correct?

            //}
            //else if (selectedVariable.isNormal()) {



            //}

            result.setValue(selectedVariable,selectedVariableNewValue);

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

    public <E extends UnivariateDistribution> E getPosterior(Variable var) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getLogProbabilityOfEvidence() {
        throw new UnsupportedOperationException();
    }





    public static void main(String[] args) throws IOException, ClassNotFoundException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/asia.bn");
        System.out.println(bn.getDAG());

        System.out.println(bn.toString());


        MAPInference mapInference = new MAPInference();
        mapInference.setModel(bn);
        mapInference.setParallelMode(true);
        mapInference.setSampleSize(100);

        System.out.println("CausalOrder: " + Arrays.toString(mapInference.causalOrder.stream().map(v -> v.getName()).toArray()));
        System.out.println();

        // Including evidence:
        Variable variable1 = mapInference.causalOrder.get(1);  // causalOrder: A, S, L, T, E, X, B, D
        Variable variable2 = mapInference.causalOrder.get(2);
        Variable variable3 = mapInference.causalOrder.get(4);

        int var1value=0;
        int var2value=1;
        int var3value=0;

        System.out.println("Evidence: Variable " + variable1.getName() + " = " + var1value + ", Variable " + variable2.getName() + " = " + var2value + " and Variable " + variable3.getName() + " = " + var3value);
        System.out.println();

        HashMapAssignment assignment = new HashMapAssignment(3);

        assignment.setValue(variable1,var1value);
        assignment.setValue(variable2,var2value);
        assignment.setValue(variable3,var3value);

        mapInference.setEvidence(assignment);

        long timeStart = System.nanoTime();
        mapInference.runInference(0);


        Assignment mapEstimate = mapInference.getMAPestimate();
        List<Variable> modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
        System.out.println("MAP estimate: " + mapEstimate.toString());   //toString(modelVariables)

        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        long timeStop = System.nanoTime();
        double execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
        //System.out.println(.toString(mapInference.getOriginalModel().getStaticVariables().iterator().));



        // MAP INFERENCE WITH A BIG SAMPLE TO CHECK
        mapInference.setSampleSize(1000000);
        timeStart = System.nanoTime();
        mapInference.runInference(0);

        mapEstimate = mapInference.getMAPestimate();
        modelVariables = mapInference.getOriginalModel().getStaticVariables().getListOfVariables();
        System.out.println("MAP estimate: " + mapEstimate.toString());
        System.out.println("with probability: " + Math.exp(mapInference.getOriginalModel().getLogProbabiltyOf(mapEstimate)));
        timeStop = System.nanoTime();
        execTime = (double) (timeStop - timeStart) / 1000000000.0;
        System.out.println("computed in: " + Double.toString(execTime) + " seconds");
    }

}
