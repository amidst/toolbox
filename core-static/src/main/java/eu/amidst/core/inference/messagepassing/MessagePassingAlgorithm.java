/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package eu.amidst.core.inference.messagepassing;



import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 03/02/15.
 */
public abstract class MessagePassingAlgorithm<E extends Vector> implements InferenceAlgorithm {

    protected BayesianNetwork model;
    protected EF_BayesianNetwork ef_model;
    protected Assignment assignment = new HashMapAssignment(0);
    protected List<Node> nodes;
    protected Map<Variable,Node> variablesToNode;
    protected boolean parallelMode = false;
    protected double probOfEvidence = Double.NaN;
    protected Random random = new Random(0);
    protected int seed=0;
    protected int maxIter = 1000;
    protected double threshold = 0.0001;
    protected boolean output = false;
    protected int nIter = 0;

    protected double local_elbo = Double.NEGATIVE_INFINITY;
    protected int local_iter = 0;

    public void setOutput(boolean output) {
        this.output = output;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setMaxIter(int maxIter) {
        this.maxIter = maxIter;
    }

    public void resetQs(){
        this.nodes.stream().forEach(node -> {node.resetQDist(random);});
    }

    public boolean isParallelMode() {
        return parallelMode;
    }

    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    @Override
    public void setSeed(int seed) {
        this.seed=seed;
        random = new Random(seed);
    }


    @Override
    public void runInference() {

        nIter = 0;

        boolean convergence = false;
        local_elbo = Double.NEGATIVE_INFINITY;
        local_iter = 0;
        while (!convergence && (local_iter++)<maxIter) {

            boolean done = true;
            for (Node node : nodes) {
                if (!node.isActive() || node.isObserved())
                    continue;

                Message<E> selfMessage = newSelfMessage(node);

                //Optional<Message<NaturalParameters>> childrenMessage = node.getChildren().parallelStream().map(children -> children.newMessageToParent(node)).reduce(Message::combine);
                //if (childrenMessage.isPresent())
                //    selfMessage = Message.combine(childrenMessage.get(), selfMessage);

                for (Node children: node.getChildren()){
                    selfMessage = Message.combine(newMessageToParent(children, node), selfMessage);
                }

                updateCombinedMessage(node, selfMessage);
                done &= node.isDone();
            }

            convergence = this.testConvergence();

            if (done) {
                convergence = true;
            }

        }

        probOfEvidence = local_elbo;
        if (output){
            System.out.println("N Iter: "+local_iter +", elbo:"+local_elbo);
        }
        nIter=local_iter;
    }

    public int getNumberOfIterations(){
        return nIter;
    }

    @Override
    public void setModel(BayesianNetwork model_) {
        model = model_;
        this.setEFModel(new EF_BayesianNetwork(this.model));
    }

    public void setEFModel(EF_BayesianNetwork model){
        ef_model = model;

        variablesToNode = new ConcurrentHashMap<>();
        nodes = ef_model.getDistributionList()
                .stream()
                .map(dist -> {
                    Node node = new Node(dist);
                    variablesToNode.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        for (Node node : nodes){
            node.setParents(node.getPDist().getConditioningVariables().stream().map(this::getNodeOfVar).collect(Collectors.toList()));
            node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVar(var).getChildren().add(node));
        }
    }

    public EF_BayesianNetwork getEFModel() {
        return ef_model;
    }

    public Node getNodeOfVar(Variable variable){
        return this.variablesToNode.get(variable);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
        variablesToNode = new ConcurrentHashMap();
        nodes.stream().forEach( node -> variablesToNode.put(node.getMainVariable(),node));
    }

    public void updateChildrenAndParents(){


        for (Node node : nodes){
            node.setParents(
                    node.getPDist()
                            .getConditioningVariables()
                            .stream()
                            .map(this::getNodeOfVar)
                            .collect(Collectors.toList())
            );

            node.getPDist().getConditioningVariables().stream()
                    .forEach(var -> this.getNodeOfVar(var).getChildren().add(node));
        }
    }
    @Override
    public BayesianNetwork getOriginalModel() {
        return this.model;
    }


    @Override
    public void setEvidence(Assignment assignment_) {
        this.assignment = assignment_;
        nodes.stream().forEach(node -> node.setAssignment(assignment));
    }

    @Override
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {
        return this.getNodeOfVar(var).getQDist().toUnivariateDistribution();
    }

    @Override
    public double getLogProbabilityOfEvidence() {
        return this.probOfEvidence;
    }

    public <E extends EF_UnivariateDistribution> E getEFPosterior(Variable var) {
        return (E)this.getNodeOfVar(var).getQDist();
    }

    public abstract Message<E> newSelfMessage(Node node);

    public abstract Message<E> newMessageToParent(Node childrenNode, Node parentNode);

    public abstract void updateCombinedMessage(Node node, Message<E> message);

    public abstract boolean testConvergence();

    public abstract double computeLogProbabilityOfEvidence();

}