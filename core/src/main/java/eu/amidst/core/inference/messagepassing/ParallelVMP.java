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


import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AtomicDouble;
import eu.amidst.core.ModelFactory;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.inference.InferenceEngine;
import eu.amidst.core.inference.Sampler;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class implements the interfaces {@link InferenceAlgorithm} and {@link Sampler}.
 * It handles and implements the parallel Variational message passing (parallelVMP) algorithm.
 */
public class ParallelVMP implements InferenceAlgorithm, Sampler {

    /** Represents the {@link BayesianNetwork} model. */
    BayesianNetwork model;

    /** Represents the {@link EF_BayesianNetwork} model. */
    EF_BayesianNetwork ef_model;

    /** Represents an {@link Assignment} object. */
    Assignment assignment = new HashMapAssignment(0);

    /** Represents the list of {@link Node}s. */
    List<Node> nodes;

    /** Represents a {@code Map} object that maps variables to nodes. */
    Map<Variable,Node> variablesToNode;

    /** Represents the probability of evidence. */
    double probOfEvidence = Double.NaN;

    /** Represents a {@link Random} object. */
    Random random = new Random(0);

    /** Represents the initial seed. */
    int seed=0;

    /** Represents the maximum number of iterations. */
    int maxIter = 1000;

    /** Represents a threshold. */
    double threshold = 0.0001;

    /** Represents the output. */
    boolean output = false;

    /** Represents the number of iterations. */
    int nIter = 0;

    /**
     * Sets the output for this ParallelVMP.
     * @param output a {@code boolean} that represents the output value to be set.
     */
    public void setOutput(boolean output) {
        this.output = output;
    }

    /**
     * Sets the threshold for this ParallelVMP.
     * @param threshold a {@code double} that represents the threshold value to be set.
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * Sets the maximum number of iterations for this ParallelVMP.
     * @param maxIter a {@code double} that represents the  maximum number of iterations to be set.
     */
    public void setMaxIter(int maxIter) {
        this.maxIter = maxIter;
    }

    /**
     * Resets the exponential family distributions of all nodes.
     */
    public void resetQs(){
        this.nodes.stream().forEach(node -> {node.resetQDist(random);});
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
        nodes.stream().filter(Node::isActive).forEach( node -> node.setParallelActivated(false));

        boolean convergence = false;
        double elbo = Double.NEGATIVE_INFINITY;
        int niter = 0;
        while (!convergence && (niter++)<maxIter) {
            AtomicDouble newelbo = new AtomicDouble(0);
            int numberOfNotDones = 0;

            nodes.stream().filter(Node::isActive).forEach(node -> node.setParallelActivated(random.nextBoolean()));

            //Send and combine messages
            Map<Node, Optional<Message<NaturalParameters>>> group = nodes.parallelStream()
                    //.peek(node -> newelbo.addAndGet(node.computeELBO()))
                    .flatMap(node -> computeMessagesParallelVMP(node))
                    .collect(
                            Collectors.groupingBy(Message::getNode,ConcurrentHashMap::new,
                                    Collectors.reducing(Message::combineNonStateless))
                    );

            //Set Messages
            numberOfNotDones += group.entrySet().parallelStream()
                    .mapToInt(e -> {
                        Node node = e.getKey();
                        updateCombinedMessage(node, e.getValue().get());
                        return (node.isDone()) ? 0 : 1;
                    })
                    .sum();

            nodes.stream().filter(Node::isActive).forEach(node -> node.setParallelActivated(!node.isParallelActivated()));

            //Send and combine messages
            group = nodes.parallelStream()
                    .peek(node -> newelbo.addAndGet(computeELBO(node)))
                    .flatMap(node -> computeMessagesParallelVMP(node))
                    .collect(
                            Collectors.groupingBy(Message::getNode,ConcurrentHashMap::new,
                                    Collectors.reducing(Message::combineNonStateless))
                    );

            //Set Messages
            numberOfNotDones += group.entrySet().parallelStream()
                    .mapToInt(e -> {
                        Node node = e.getKey();
                        updateCombinedMessage(node, e.getValue().get());
                        return (node.isDone()) ? 0 : 1;
                    })
                    .sum();


            //Test whether all nodesTimeT are done.
            if (numberOfNotDones == 0) {
                convergence = true;
            }

            //Compute lower-bound
            //newelbo.set(this.nodes.parallelStream().mapToDouble(Node::computeELBO).sumNonStateless());
            if (Math.abs(newelbo.get() - elbo) < threshold) {
                convergence = true;
            }
            //if (testELBO && (!convergence && newelbo.doubleValue()/nodes.size() < (elbo/nodes.size() - 0.1)) || Double.isNaN(elbo)){
            //                throw new UnsupportedOperationException("The elbo is NaN or is not monotonically increasing: " + elbo + ", "+ newelbo.doubleValue());
            //}
            elbo = newelbo.get();
            //System.out.println(elbo);
        }
        probOfEvidence = elbo;
        if (output) {
            System.out.println("N Iter: " + niter + ", elbo:" + elbo);
        }
        nIter=niter;
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
    public void setParallelMode(boolean parallelMode_) {

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

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getSamplingModel() {

        DAG dag = ModelFactory.newDAG(this.model.getVariables());

        List<ConditionalDistribution> distributionList =
                this.model.getVariables().getListOfVariables().stream()
                        .map(var -> (ConditionalDistribution)this.getPosterior(var))
                        .collect(Collectors.toList());

        return ModelFactory.newBayesianNetwork(dag, distributionList);
    }

    /**
     * Updates the combined message for a given {@link Node}.
     * @param node a {@link Node} object.
     * @param message a {@link Message} object.
     */
    public void updateCombinedMessage(Node node, Message<NaturalParameters> message) {
        node.getQDist().setNaturalParameters(message.getVector());
        node.setIsDone(message.isDone());
    }

    /**
     * Computes the evidence lower bound (ELBO) for a given {@link Node}.
     * @param node a given {@link Node} object.
     * @return a {@code double} that represents the ELBO value.
     */
    private double computeELBO(Node node){

        Map<Variable, MomentParameters> momentParents = node.getMomentParents();

        double elbo=0;
        NaturalParameters expectedNatural = node.getPDist().getExpectedNaturalFromParents(momentParents);

        if (!node.isObserved()) {
            expectedNatural.substract(node.getQDist().getNaturalParameters());
            elbo += expectedNatural.dotProduct(node.getQDist().getMomentParameters());
            elbo -= node.getPDist().getExpectedLogNormalizer(momentParents);
            elbo += node.getQDist().computeLogNormalizer();
        }else {
            elbo += expectedNatural.dotProduct(node.getSufficientStatistics());
            elbo -= node.getPDist().getExpectedLogNormalizer(momentParents);
            elbo += node.getPDist().computeLogBaseMeasure(this.assignment);
        }

        if (elbo>0 && !node.isObserved() && Math.abs(expectedNatural.sum())<0.01) {
            elbo=0;
        }

        if ((elbo>2 && !node.isObserved()) || Double.isNaN(elbo)) {
            node.getPDist().getExpectedLogNormalizer(momentParents);
            throw new IllegalStateException("NUMERICAL ERROR!!!!!!!!: " + node.getMainVariable().getName() + ", " +  elbo + ", " + expectedNatural.sum());
        }

        return  elbo;
    }

    /**
     * Computes the messages for a given {@link Node} in this ParallelVMP algorithm.
     * @param node a given {@link Node} object.
     * @return a {@code Stream} of messages.
     */
    public Stream<Message<NaturalParameters>> computeMessagesParallelVMP(Node node){

        Map<Variable, MomentParameters> momentParents = node.getMomentParents();

        List<Message<NaturalParameters>> messages = node.getParents().stream()
                .filter(parent -> parent.isActive())
                .filter(parent -> !parent.isObserved())
                .filter(parent -> parent.isParallelActivated())
                .map(parent -> newMessageToParent(node, parent, momentParents))
                .collect(Collectors.toList());

        if (node.isActive() && node.isParallelActivated() && !node.isObserved()) {
            messages.add(newSelfMessage(node, momentParents));
        }

        return messages.stream();
    }

    /**
     * Creates a new self message for a given {@link Node} and moment parents.
     * @param node a {@link Node} object.
     * @param momentParents a {@code Map} object that maps the parent variables to their corresponding {@link MomentParameters}.
     * @return a {@link Message} object.
     */
    public Message<NaturalParameters> newSelfMessage(Node node, Map<Variable, MomentParameters> momentParents) {
        Message<NaturalParameters> message = new Message(node);
        message.setVector(node.getPDist().getExpectedNaturalFromParents(momentParents));
        message.setDone(node.messageDoneFromParents());

        return message;
    }

    /**
     * Creates a new message from  a given child {@link Node} to its parent.
     * @param child a child {@link Node}.
     * @param parent a parent {@link Node}.
     * @param momentChildCoParents a {@code Map} object that maps the co-parent variables to their corresponding {@link MomentParameters}.
     * @return a {@link Message} object.
     */
    public Message<NaturalParameters> newMessageToParent(Node child, Node parent, Map<Variable, MomentParameters> momentChildCoParents){
        Message<NaturalParameters> message = new Message(parent);
        message.setVector(child.getPDist().getExpectedNaturalToParent(parent.getMainVariable(), momentChildCoParents));
        message.setDone(child.messageDoneToParent(parent.getMainVariable()));

        return message;
    }

    /**
     * Returns the number of iterations of this ParallelVMP algorithm.
     * @return the number of iterations of this ParallelVMP algorithm.
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

    /**
     * Sets the {@link EF_BayesianNetwork} model for this ParallelVMP algorithm.
     * @param model the {@link EF_BayesianNetwork} model to be set.
     */
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

    /**
     * Returns the {@link EF_BayesianNetwork} model of this ParallelVMP algorithm.
     * @return the {@link EF_BayesianNetwork} model.
     */
    public EF_BayesianNetwork getEFModel() {
        return ef_model;
    }

    /**
     * Returns the {@link Node} associated with a given {@link Variable}.
     * @param variable a given {@link Variable} object
     * @return a {@link Node} object.
     */
    public Node getNodeOfVar(Variable variable){
        return this.variablesToNode.get(variable);
    }

    /**
     * Returns the list of nodes.
     * @return a {@code List} of {@link Node}s.
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * Sets the list of nodes.
     * @param nodes a {@code List} of {@link Node}s to be set.
     */
    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
        variablesToNode = new ConcurrentHashMap();
        nodes.stream().forEach( node -> variablesToNode.put(node.getMainVariable(),node));
    }

    /**
     * Updates the set of children and parents for each node.
     */
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
     * Returns the exponential family posterior of a given {@link Variable}.
     * @param <E> a class extending {@link EF_UnivariateDistribution}
     * @param var a {@link Variable} object.
     * @return an {@link EF_UnivariateDistribution} object.
     */
    public <E extends EF_UnivariateDistribution> E getEFPosterior(Variable var) {
        return (E)this.getNodeOfVar(var).getQDist();
    }

    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/Munin1.bn");
        System.out.println(bn.getNumberOfVars());
        System.out.println(bn.getConditionalDistributions().stream().mapToInt(p->p.getNumberOfParameters()).max().getAsInt());

        ParallelVMP vmp = new ParallelVMP();

        InferenceEngine.setInferenceAlgorithm(vmp);
        Variable var = bn.getVariables().getVariableById(0);
        UnivariateDistribution uni = null;

        double avg  = 0;
        for (int i = 0; i < 20; i++)
        {
            Stopwatch watch = Stopwatch.createStarted();
            uni = InferenceEngine.getPosterior(var, bn);
            System.out.println(watch.stop());
            avg += watch.elapsed(TimeUnit.MILLISECONDS);
        }
        System.out.println(avg/20);
        System.out.println(uni);

    }


}