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
 * This class implements the interface {@link InferenceAlgorithm} and defines the Message Passing algorithm.
 */
public abstract class MessagePassingAlgorithm<E extends Vector> implements InferenceAlgorithm {

    /** Represents the {@link BayesianNetwork} model. */
    protected BayesianNetwork model;

    /** Represents the {@link EF_BayesianNetwork} model. */
    protected EF_BayesianNetwork ef_model;

    /** Represents an {@link Assignment} object. */
    protected Assignment assignment = new HashMapAssignment(0);

    /** Represents the list of {@link Node}s. */
    protected List<Node> nodes;

    /** Represents a {@code Map} object that maps variables to nodes. */
    protected Map<Variable,Node> variablesToNode;

    /** Represents the algorithm running mode, parallel or not. */
    protected boolean parallelMode = false;

    /** Represents the probability of evidence. */
    protected double probOfEvidence = Double.NaN;

    /** Represents a {@link Random} object. */
    protected Random random = new Random(0);

    /** Represents the initial seed. */
    protected int seed=0;

    /** Represents the maximum number of iterations. */
    protected int maxIter = 100;

    /** Represents a threshold. */
    protected double threshold = 0.0001;

    /** Represents the output. */
    protected boolean output = false;

    /** Represents the number of iterations. */
    protected int nIter = 0;

    /** Represents the evidence lower bound. */
    protected double local_elbo = Double.NEGATIVE_INFINITY;

    /** Represents the number of local iterations. */
    protected int local_iter = 0;

    /**
     * Sets the output for this MessagePassingAlgorithm.
     * @param output a {@code boolean} that represents the output value to be set.
     */
    public void setOutput(boolean output) {
        this.output = output;
    }

    /**
     * Sets the threshold for this MessagePassingAlgorithm.
     * @param threshold a {@code double} that represents the threshold value to be set.
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * Returns the threshold of this MessagePassingAlgorithm.
     * @return the threshold of this MessagePassingAlgorithm.
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Sets the maximum number of iterations for this MessagePassingAlgorithm.
     * @param maxIter a {@code double} that represents the  maximum number of iterations to be set.
     */
    public void setMaxIter(int maxIter) {
        this.maxIter = maxIter;
    }

    /**
     * Returns the maximum number of iterations of this MessagePassingAlgorithm.
     * @return the maximum number of iterations of this MessagePassingAlgorithm.
     */
    public int getMaxIter() {
        return maxIter;
    }

    public void resetQs(){
        this.nodes.stream().forEach(node -> {node.resetQDist(random);});
    }

    /**
     * Tests whether this MessagePassingAlgorithm runs in parallel or not.
     * @return {@code true} if the algorithm runs in parallel, {@code false} otherwise.
     */
    public boolean isParallelMode() {
        return parallelMode;
    }

    /**
     *Sets the parallel mode for this MessagePassingAlgorithm.
     * @param parallelMode a {@code boolean} that represents the parallel mode value to be set.
     */
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSeed(int seed) {
        this.seed=seed;
        random = new Random(seed);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * Returns the number of iterations of this MessagePassingAlgorithm.
     * @return the number of iterations of this MessagePassingAlgorithm.
     */
    public int getNumberOfIterations(){
        return nIter;
    }

    /**
     * {@inheritDoc}
     */
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
    public void setEvidence(Assignment assignment_) {
        this.assignment = assignment_;
        nodes.stream().forEach(node -> node.setAssignment(assignment));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends UnivariateDistribution> E getPosterior(Variable var) {
        return this.getNodeOfVar(var).getQDist().toUnivariateDistribution();
    }

    /**
     * {@inheritDoc}
     */
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