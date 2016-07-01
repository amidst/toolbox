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
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.exponentialfamily.EF_DynamicBayesianNetwork;
import eu.amidst.dynamic.learning.parametric.DynamicNaiveBayesClassifier;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DataSetGenerator;
import eu.amidst.dynamic.variables.DynamicAssignment;
import eu.amidst.dynamic.variables.HashMapDynamicAssignment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class implements the interfaces {@link InferenceAlgorithmForDBN}.
 * It handles and implements the Variational message passing (VMP) algorithm to perform inference on {@link DynamicBayesianNetwork} models.
 * Winn, J.M., Bishop, C.M.: Variational message passing. Journal of Machine Learning Research 6 (2005) 661–694.
 */
public class DynamicVMP implements InferenceAlgorithmForDBN {

    /** Represents the {@link DynamicBayesianNetwork} model. */
    DynamicBayesianNetwork model;

    /** Represents the {@link EF_DynamicBayesianNetwork} model. */
    EF_DynamicBayesianNetwork ef_model;

    /** Represents an {@link DynamicAssignment} object. */
    DynamicAssignment assignment = new HashMapDynamicAssignment(0);

    /** Represents the list of {@link Node}s at time T. */
    List<Node> nodesTimeT;

    /** Represents the list of clone {@link Node}s. */
    List<Node> nodesClone;

    /** Represents a {@link VMP} object for time 0. */
    VMP vmpTime0;

    /** Represents a {@link VMP} object for time 0. */
    VMP vmpTimeT;

    /** Represents the time ID. */
    long timeID;

    /** Represents the sequence ID. */
    long sequenceID;

    /**
     * Creates a new DynamicVMP object.
     */
    public DynamicVMP(){
        this.vmpTime0 = new VMP();
        this.vmpTimeT = new VMP();
        this.setSeed(0);
        this.timeID=-1;
    }

    /**
     * Sets the seed.
     * @param seed an {@code int} that represents the seed value to be set.
     */
    public void setSeed(int seed) {
        this.vmpTime0.setSeed(seed);
        this.vmpTimeT.setSeed(seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setModel(DynamicBayesianNetwork model_) {
        model = model_;
        ef_model = new EF_DynamicBayesianNetwork(this.model);

        this.vmpTime0.setEFModel(ef_model.getBayesianNetworkTime0());

        nodesTimeT = this.ef_model.getBayesianNetworkTimeT().getDistributionList()
                .stream()
                .map(dist ->  new Node(dist))
                .collect(Collectors.toList());

        nodesClone = this.ef_model.getBayesianNetworkTime0().getDistributionList()
                .stream()
                .map(dist -> {
                    Variable temporalClone = this.model.getDynamicVariables().getInterfaceVariable(dist.getVariable());
                    EF_UnivariateDistribution uni = temporalClone.getDistributionType().newUnivariateDistribution().toEFUnivariateDistribution();
                    Node node = new Node(uni);
                    node.setActive(false);
                    return node;
                })
                .collect(Collectors.toList());

        List<Node> allNodes = new ArrayList();
        allNodes.addAll(nodesTimeT);
        allNodes.addAll(nodesClone);
        this.vmpTimeT.setNodes(allNodes);
        this.vmpTimeT.updateChildrenAndParents();

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
    public void reset() {
        this.timeID = -1;
        this.sequenceID = -1;
        this.vmpTime0.resetQs();
        this.vmpTimeT.resetQs();
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
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        return (getTimeIDOfPosterior()==0)? this.vmpTime0.getPosterior(var): this.vmpTimeT.getPosterior(var);
    }

    private static void moveNodeQDist(Node toTemporalCloneNode, Node fromNode){
            EF_UnivariateDistribution uni = fromNode.getQDist().deepCopy(toTemporalCloneNode.getMainVariable());
            toTemporalCloneNode.setPDist(uni);
            toTemporalCloneNode.setQDist(uni);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead) {

        if (timeID==-1){
            this.vmpTime0.setEvidence(null);
            this.vmpTime0.runInference();
            this.vmpTime0.getNodes().stream().filter(node -> !node.isObserved()).forEach(node -> {
                Variable temporalClone = this.model.getDynamicVariables().getInterfaceVariable(node.getMainVariable());
                moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
            });
            this.moveWindow(nTimesAhead-1);
            E resultQ = this.getFilteredPosterior(var);
            this.vmpTime0.resetQs();
            this.vmpTimeT.resetQs();

            return resultQ;
        }else {

            Map<Variable, EF_UnivariateDistribution> map = new HashMap<>();

            //Create at copy of Qs
            this.vmpTimeT.getNodes().stream().filter(node -> !node.isObserved()).forEach(node -> map.put(node.getMainVariable(), node.getQDist().deepCopy()));

            this.moveWindow(nTimesAhead);
            E resultQ = this.getFilteredPosterior(var);

            //Come to the original state
            map.entrySet().forEach(e -> this.vmpTimeT.getNodeOfVar(e.getKey()).setQDist(e.getValue()));

            return resultQ;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeIDOfPosterior() {
        return this.timeID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeIDOfLastEvidence(){
        return this.assignment.getTimeID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runInference(){

        if (this.timeID==-1 && assignment.getTimeID()>0) {
            this.vmpTime0.setEvidence(null);
            this.vmpTime0.runInference();
            this.timeID=0;
            this.vmpTime0.getNodes().stream().filter(node -> !node.isObserved()).forEach(node -> {
                Variable temporalClone = this.model.getDynamicVariables().getInterfaceVariable(node.getMainVariable());
                moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
            });
        }

        if (assignment.getTimeID()==0) {

            this.vmpTime0.setEvidence(this.assignment);
            this.vmpTime0.runInference();
            this.timeID=0;

            this.vmpTime0.getNodes().stream()
                    .filter(node -> !node.isObserved())
                    .forEach(node -> {
                Variable temporalClone = this.model.getDynamicVariables().getInterfaceVariable(node.getMainVariable());
                moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
            });

        }else{

            if ((this.assignment.getTimeID() - this.timeID)>1)
                this.moveWindow((int)(this.assignment.getTimeID() - this.timeID - 1));

            this.timeID=this.assignment.getTimeID();
            this.vmpTimeT.setEvidence(this.assignment);
            this.vmpTimeT.runInference();
            this.vmpTimeT.getNodes().stream()
                    .filter(node -> !node.getMainVariable().isInterfaceVariable())
                    .filter(node -> !node.isObserved())
                    .forEach(node -> {
                        Variable temporalClone = this.model.getDynamicVariables().getInterfaceVariable(node.getMainVariable());
                        moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
                    });
        }

    }

    /**
     * Moves the window ahead for a given number of time steps.
     * @param nsteps an {@link int} that represents a given number of time steps.
     */
    private void moveWindow(int nsteps){
        //The first step we need to manually move the evidence from master to clone variables.
        HashMapDynamicAssignment newassignment = null;

        if (this.assignment!=null) {
            newassignment=new HashMapDynamicAssignment(this.model.getNumberOfDynamicVars());
            for (Variable var : this.model.getDynamicVariables()) {
                newassignment.setValue(this.model.getDynamicVariables().getInterfaceVariable(var), this.assignment.getValue(var));
                newassignment.setValue(var, Utils.missingValue());
            }
        }

        for (int i = 0; i < nsteps; i++) {
            this.vmpTimeT.setEvidence(newassignment);
            this.vmpTimeT.runInference();
            this.vmpTimeT.getNodes().stream()
                    .filter(node -> !node.getMainVariable().isInterfaceVariable())
                    .filter(node -> !node.isObserved())
                    .forEach(node -> {
                        Variable temporalClone = this.model.getDynamicVariables().getInterfaceVariable(node.getMainVariable());
                        moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
                    });
            newassignment=null;
        }
    }


    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        DataStream<DynamicDataInstance> data = DataSetGenerator.generate(20,10000,10,0);

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(0);
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork bn = model.getDynamicBNModel();

        data = DataSetGenerator.generate(50,10000,10,0);

        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new DynamicVMP());
        InferenceEngineForDBN.setModel(bn);
        Variable defaultVar = bn.getDynamicVariables().getVariableByName("DiscreteVar0");
        UnivariateDistribution dist = null;
        UnivariateDistribution distAhead = null;

        for(DynamicDataInstance instance: data){

            if (instance.getTimeID()==0 && dist != null) {
                System.out.println(dist.toString());
                System.out.println(distAhead.toString());
                InferenceEngineForDBN.reset();
            }
            instance.setValue(defaultVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(defaultVar);
            distAhead = InferenceEngineForDBN.getPredictivePosterior(defaultVar,2);
        }
    }
}
