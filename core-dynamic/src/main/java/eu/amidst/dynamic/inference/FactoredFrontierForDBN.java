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

package eu.amidst.dynamic.inference;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.dynamic.utils.DataSetGenerator;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.learning.parametric.DynamicNaiveBayesClassifier;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


/**
 * This class implements the interfaces {@link InferenceAlgorithmForDBN}.
 * It handles and implements the Factored Frontier (FF) algorithm to perform inference on {@link DynamicBayesianNetwork} models.
 */
public class FactoredFrontierForDBN  implements InferenceAlgorithmForDBN {

    /** Represents the {@link InferenceAlgorithm} at time 0. */
    private InferenceAlgorithm infAlgTime0;

    /** Represents the {@link InferenceAlgorithm} at time T. */
    private InferenceAlgorithm infAlgTimeT;

    /** Represents the {@link BayesianNetwork} model at time 0. */
    private BayesianNetwork bnTime0;

    /** Represents the {@link BayesianNetwork} model at time T. */
    private BayesianNetwork bnTimeT;

    /** Represents the {@link DynamicBayesianNetwork} model. */
    private DynamicBayesianNetwork model;

    /** Represents an {@link DynamicAssignment} object. */
    private DynamicAssignment assignment = new HashMapDynamicAssignment(0);

    /** Represents the time ID. */
    private long timeID;

    /** Represents the sequence ID. */
    private long sequenceID;

    /**
     * Creates a new FactoredFrontierForDBN object.
     * @param inferenceAlgorithm an {@link InferenceAlgorithm} object.
     */
    public FactoredFrontierForDBN(InferenceAlgorithm inferenceAlgorithm){
        infAlgTime0 = inferenceAlgorithm;
        infAlgTimeT = Serialization.deepCopy(inferenceAlgorithm);
        timeID = -1;
        this.setSeed(0);
    }

    /**
     * Sets the seed.
     * @param seed an {@code int} that represents the seed value to be set.
     */
    public void setSeed(int seed) {
        infAlgTime0.setSeed(seed);
        infAlgTimeT.setSeed(seed);
    }


    /**
     * Return the list of non-observed and temporally connected variables for time T.
     * @return a {@code List} of {@link Variable}.
     */
    private List<Variable> getTargetVarsTimeT(){
        return this.model.getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(var -> !var.isInterfaceVariable())
                .filter(var -> Utils.isMissingValue(this.assignment.getValue(var)))
                .filter(var -> {
                    boolean notContainInterfaceVar = true;
                    for (Variable variable : this.model.getDynamicDAG().getParentSetTimeT(var)) {
                        notContainInterfaceVar = notContainInterfaceVar && !variable.isInterfaceVariable();
                    }

                    return !notContainInterfaceVar;
                })
                .collect(Collectors.toList());
    }

    /**
     * Return the list of non-observed variables for Time 0.
     * @return a {@code List} of {@link Variable}.
     */
    private List<Variable> getTargetVarsTime0(){
        return this.model.getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(var -> Utils.isMissingValue(this.assignment.getValue(var)))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference() {

        if (this.timeID==-1 && assignment.getTimeID()>0) {
            this.infAlgTime0.setModel(this.bnTime0);
            this.infAlgTime0.setEvidence(null);
            this.infAlgTime0.runInference();
            this.timeID=0;
            this.getTargetVarsTime0().stream()
                    .forEach(var -> moveNodeQDist(this.infAlgTime0, this.bnTimeT, var));
        }

        if (assignment.getTimeID()==0) {
            this.infAlgTime0.setModel(this.bnTime0);
            this.infAlgTime0.setEvidence(updateDynamicAssignmentTime0(this.assignment));
            this.infAlgTime0.runInference();
            this.timeID=0;
            this.getTargetVarsTimeT().stream()
                    .forEach(var -> moveNodeQDist(this.infAlgTime0, this.bnTimeT, var));

        }else{
            //If there is a missing instance
            if ((this.assignment.getTimeID() - this.timeID)>1)
                this.moveWindow((int)(this.assignment.getTimeID() - this.timeID - 1));

            this.timeID=this.assignment.getTimeID();
            this.infAlgTimeT.setModel(this.bnTimeT);
            this.infAlgTimeT.setEvidence(updateDynamicAssignmentTimeT(this.assignment));
            this.infAlgTimeT.runInference();
            this.getTargetVarsTimeT().stream()
                    .forEach(var -> moveNodeQDist(this.infAlgTimeT, this.bnTimeT, var));
        }
    }

    /**
     * Moves the posterior distribution of a given {@link Variable} from a {@link BayesianNetwork} object to another.
     * @param infAlg an {@link InferenceAlgorithm} object.
     * @param bnTo a {@link BayesianNetwork} object to which the posterior distribution should be moved.
     * @param var a given {@link Variable}.
     */
    private void moveNodeQDist(InferenceAlgorithm infAlg, BayesianNetwork bnTo, Variable var){

        //Recover original model and do the copy, then set again.
        Variable temporalClone = this.model.getDynamicVariables().getInterfaceVariable(var);
        UnivariateDistribution posteriorDist = infAlg.getPosterior(var).deepCopy(temporalClone);
        bnTo.setConditionalDistribution(temporalClone, posteriorDist);
    }

    /**
     * Moves the window ahead for a given number of time steps.
     * @param nsteps an {@link int} that represents a given number of time steps.
     */
    private void moveWindow(int nsteps){
        //The first step we need to manually move the evidence from master to clone variables.
        HashMapDynamicAssignment newassignment =null;

        if (this.assignment!=null) {
            newassignment=new HashMapDynamicAssignment(this.model.getNumberOfDynamicVars());
            for (Variable var : this.model.getDynamicVariables()) {
                newassignment.setValue(this.model.getDynamicVariables().getInterfaceVariable(var), this.assignment.getValue(var));
                newassignment.setValue(var, Utils.missingValue());
            }
        }

        for (int i = 0; i < nsteps; i++) {
            this.infAlgTimeT.setModel(this.bnTimeT);
            this.infAlgTimeT.setEvidence(updateDynamicAssignmentTimeT(newassignment));
            this.infAlgTimeT.runInference();
            this.getTargetVarsTimeT().stream()
                    .forEach(var -> moveNodeQDist(this.infAlgTimeT, this.bnTimeT, var));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(DynamicBayesianNetwork model_) {
        this.model = model_;
        this.bnTime0 = model.toBayesianNetworkTime0();
        this.bnTimeT = model.toBayesianNetworkTimeT();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamicBayesianNetwork getOriginalModel() {
        return this.model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDynamicEvidence(DynamicAssignment assignment_) {
        if (this.sequenceID!= -1 && this.sequenceID != assignment_.getSequenceID())
            throw new IllegalArgumentException("The sequence ID does not match. If you want to change the sequence, invoke reset method");

        if (this.timeID>= assignment_.getTimeID())
            throw new IllegalArgumentException("The provided assignment is not posterior to the previous provided assignment.");

        this.assignment = assignment_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        this.timeID = -1;
        this.sequenceID = -1;

        this.resetInfAlgorithms();
    }

    private void resetInfAlgorithms(){

        this.infAlgTime0.setModel(this.model.toBayesianNetworkTime0());
        this.infAlgTimeT.setModel(this.model.toBayesianNetworkTimeT());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        if(getTimeIDOfPosterior()==0){
            return this.infAlgTime0.getPosterior(var);
        }else{
            return this.infAlgTimeT.getPosterior(var);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead) {
        if (timeID==-1){
            this.infAlgTime0.setModel(this.bnTime0);
            this.infAlgTime0.setEvidence(null);
            this.infAlgTime0.runInference();
            this.getTargetVarsTimeT().stream()
                    .forEach(v -> moveNodeQDist(this.infAlgTime0, this.bnTimeT, v));

            this.moveWindow(nTimesAhead-1);
            E resultQ = this.getFilteredPosterior(var);
            this.resetInfAlgorithms();

            return resultQ;
        }else if(timeID==0){
            //Don't need to create a copy of the Q's because they will not be modified at Time 0
            this.moveWindow(nTimesAhead);
            E resultQ = this.getFilteredPosterior(var);

            //But we need to manually move the posteriors from Time 0 to the Interface Variables in Time T again
            this.getTargetVarsTime0().stream()
                    .forEach(v -> moveNodeQDist(this.infAlgTime0, this.bnTimeT, v));

            return resultQ;
        }
        else {

            Map<Variable, UnivariateDistribution> map = new HashMap<>();

            //Create at copy of Qs
            this.getTargetVarsTimeT().stream()
                    .forEach(v -> map.put(v,this.infAlgTimeT.getPosterior(v).deepCopy(v.getInterfaceVariable())));

            this.moveWindow(nTimesAhead);
            E resultQ = this.getFilteredPosterior(var);

            //Come to the original state
            map.entrySet().forEach(e -> this.bnTimeT.setConditionalDistribution(e.getKey().getInterfaceVariable(),
                    e.getValue()));

            return resultQ;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeIDOfLastEvidence() {
        return this.assignment.getTimeID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeIDOfPosterior() {
        return this.timeID;
    }

    private Assignment updateDynamicAssignmentTime0(DynamicAssignment dynamicAssignment){

        HashMapAssignment assignment = new HashMapAssignment();

        this.model.getDynamicVariables()
                .getListOfDynamicVariables()
                .stream()
                .forEach(var -> {
                    double value = dynamicAssignment.getValue(var);
                    assignment.setValue(var, value);
                });

        return assignment;
    }

    /**
     * Updates the {@link DynamicAssignment} at time T.
     * @param dynamicAssignment a valid {@link DynamicAssignment} object.
     * @return an {@link Assignment} object.
     */
    private Assignment updateDynamicAssignmentTimeT(DynamicAssignment dynamicAssignment){

        HashMapAssignment assignment = new HashMapAssignment();

        //Set evidence for all variables at time T
        this.model.getDynamicVariables()
                .getListOfDynamicVariables()
                .stream()
                .forEach(var -> {
                    double value = dynamicAssignment.getValue(var);
                    assignment.setValue(var,value);
                });

        //Set evidence for all interface variables temporally connected
        this.model.getDynamicVariables().getListOfDynamicVariables().stream()
                .filter(var -> {
                    boolean notContainInterfaceVar = true;
                    for (Variable variable : this.model.getDynamicDAG().getParentSetTimeT(var)) {
                        notContainInterfaceVar = notContainInterfaceVar && !variable.isInterfaceVariable();
                    }

                    return !notContainInterfaceVar;
                })
                .forEach(var -> {
                    Variable var_interface = var.getInterfaceVariable();
                    double value_interface = dynamicAssignment.getValue(var_interface);
                    assignment.setValue(var_interface,value_interface);

                });

        return assignment;
    }


    public static void main(String[] arguments) throws IOException, ClassNotFoundException {


        /************** SIMULATED DATA **************/

        DataStream<DynamicDataInstance> data = DataSetGenerator.generate(15,10000,10,0);

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(0);
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork bn = model.getDynamicBNModel();

        bn.randomInitialization(new Random(0));
        System.out.println(bn.toString());


        DataStream<DynamicDataInstance> dataPredict = DataSetGenerator.generate(50,1000,10,0);

        Variable targetVar = bn.getDynamicVariables().getVariableByName("DiscreteVar0");

        /************************************/

        UnivariateDistribution dist = null;
        UnivariateDistribution distAhead = null;
        //AtomicInteger countRightPred = new AtomicInteger();


        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new DynamicVMP());
        InferenceEngineForDBN.setModel(bn);

        System.out.println("---------------- Dynamic VMP----------------");
        for(DynamicDataInstance instance: dataPredict){

            if (instance.getTimeID()==0 && dist != null) {
                System.out.println("\nNew sequence #"+instance.getSequenceID());
                InferenceEngineForDBN.reset();
            }
            //double trueClass = instance.getValue(targetVar);
            instance.setValue(targetVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(targetVar);
            /* Get predicted class
            Double[] doubleArray = ArrayUtils.toObject(dist.getParameters());
            List<Double> doubleArr = Arrays.asList(doubleArray);
            IntStream.range(0, doubleArr.size())
                    .reduce((a,b)->doubleArr.get(a)<doubleArr.get(b)? b: a)
                    .ifPresent(ix->{
                        System.out.println("max index = "+ix); if(ix==trueClass) countRightPred.getAndIncrement();});
            */
            System.out.println("["+instance.getSequenceID()+","+instance.getTimeID()+"]"+dist.toString());
            distAhead = InferenceEngineForDBN.getPredictivePosterior(targetVar,1);
            System.out.println("PP: "+distAhead.toString());
        }
        //System.out.println("Right predictions for VMP = "+countRightPred.get());

        System.out.println("---------------- FF - VMP--------------");

        FactoredFrontierForDBN FFalgorithm = new FactoredFrontierForDBN(new VMP());
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(FFalgorithm);
        InferenceEngineForDBN.setModel(bn);
        dist=null;
        //countRightPred.set(0);
        dataPredict = DataSetGenerator.generate(50,1000,10,0);

        for(DynamicDataInstance instance: dataPredict){

            if (instance.getTimeID()==0 && dist != null) {
                System.out.println("\nNew sequence #"+instance.getSequenceID());
                InferenceEngineForDBN.reset();
            }
            //double trueClass = instance.getValue(targetVar);
            instance.setValue(targetVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(targetVar);
            /*
            Double[] doubleArray = ArrayUtils.toObject(dist.getParameters());
            List<Double> doubleArr = Arrays.asList(doubleArray);
            IntStream.range(0, doubleArr.size())
                    .reduce((a,b)->doubleArr.get(a)<doubleArr.get(b)? b: a)
                    .ifPresent(ix->{
                        System.out.println("max index = "+ix); if(ix==trueClass) countRightPred.getAndIncrement();});
            */
            System.out.println("["+instance.getSequenceID()+","+instance.getTimeID()+"]"+dist.toString());
            distAhead = InferenceEngineForDBN.getPredictivePosterior(targetVar,1);
            System.out.println("PP: "+distAhead.toString());
        }

        System.out.println("---------------- FF - Importance Sampling--------------");

        ImportanceSampling importanceSampling = new ImportanceSampling();
        importanceSampling.setKeepDataOnMemory(true);
        FFalgorithm = new FactoredFrontierForDBN(importanceSampling);
        InferenceEngineForDBN.setInferenceAlgorithmForDBN(FFalgorithm);
        InferenceEngineForDBN.setModel(bn);
        dist=null;
        //countRightPred.set(0);
        dataPredict = DataSetGenerator.generate(50,1000,10,0);

        for(DynamicDataInstance instance: dataPredict){

            if (instance.getTimeID()==0 && dist != null) {
                System.out.println("\nNew sequence #"+instance.getSequenceID());
                InferenceEngineForDBN.reset();
            }
            //double trueClass = instance.getValue(targetVar);
            instance.setValue(targetVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(targetVar);
            /*
            Double[] doubleArray = ArrayUtils.toObject(dist.getParameters());
            List<Double> doubleArr = Arrays.asList(doubleArray);
            IntStream.range(0, doubleArr.size())
                    .reduce((a,b)->doubleArr.get(a)<doubleArr.get(b)? b: a)
                    .ifPresent(ix->{
                        System.out.println("max index = "+ix); if(ix==trueClass) countRightPred.getAndIncrement();});
            */
            System.out.println("["+instance.getSequenceID()+","+instance.getTimeID()+"]"+dist.toString());
            distAhead = InferenceEngineForDBN.getPredictivePosterior(targetVar,1);
            System.out.println("PP: "+distAhead.toString());
        }
        //System.out.println("Right predictions for IS = "+countRightPred.get());


    }
}
