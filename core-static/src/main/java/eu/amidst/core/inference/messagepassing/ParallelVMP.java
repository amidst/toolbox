package eu.amidst.core.inference.messagepassing;

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

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AtomicDouble;
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
 * Created by andresmasegosa on 03/02/15.
 */
public class ParallelVMP implements InferenceAlgorithm, Sampler {

    BayesianNetwork model;
    EF_BayesianNetwork ef_model;
    Assignment assignment = new HashMapAssignment(0);
    List<Node> nodes;
    Map<Variable,Node> variablesToNode;
    double probOfEvidence = Double.NaN;
    Random random = new Random(0);
    int seed=0;
    int maxIter = 1000;
    double threshold = 0.0001;
    boolean output = false;
    int nIter = 0;

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

    @Override
    public void setSeed(int seed) {
        this.seed=seed;
        random = new Random(seed);
    }


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

            //nodesTimeT.stream().forEach( node -> node.setActive(node.getMainVariable().getVarID()%2==0));
            nodes.stream().filter(Node::isActive).forEach(node -> node.setParallelActivated(random.nextBoolean()));
            //nodes.stream().forEach( node -> node.setActive(rand.nextInt()%100==0));

            //Send and combine messages
            Map<Node, Optional<Message<NaturalParameters>>> group = nodes.parallelStream()
                    //.peek(node -> newelbo.addAndGet(node.computeELBO()))
                    .flatMap(node -> computeMessagesParallelVMP(node))
                    .collect(
                            Collectors.groupingBy(Message::getNode,ConcurrentHashMap::new,
                                    Collectors.reducing(Message::combine))
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
                                    Collectors.reducing(Message::combine))
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
            //newelbo.set(this.nodes.parallelStream().mapToDouble(Node::computeELBO).sum());
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


    public void updateCombinedMessage(Node node, Message<NaturalParameters> message) {
        node.getQDist().setNaturalParameters(message.getVector());
        node.setIsDone(message.isDone());
    }

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

    public Message<NaturalParameters> newSelfMessage(Node node, Map<Variable, MomentParameters> momentParents) {
        Message<NaturalParameters> message = new Message(node);
        message.setVector(node.getPDist().getExpectedNaturalFromParents(momentParents));
        message.setDone(node.messageDoneFromParents());

        return message;
    }

    public Message<NaturalParameters> newMessageToParent(Node children, Node parent, Map<Variable, MomentParameters> momentChildCoParents){
        Message<NaturalParameters> message = new Message(parent);
        message.setVector(children.getPDist().getExpectedNaturalToParent(parent.getMainVariable(), momentChildCoParents));
        message.setDone(children.messageDoneToParent(parent.getMainVariable()));

        return message;
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
    public void setParallelMode(boolean parallelMode_) {

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

    @Override
    public BayesianNetwork getSamplingModel() {

        DAG dag = new DAG(this.model.getStaticVariables());

        List<ConditionalDistribution> distributionList =
                this.model.getStaticVariables().getListOfVariables().stream()
                        .map(var -> (ConditionalDistribution)this.getPosterior(var))
                        .collect(Collectors.toList());

        return BayesianNetwork.newBayesianNetwork(dag, distributionList);
    }


    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/Munin1.bn");
        System.out.println(bn.getNumberOfVars());
        System.out.println(bn.getConditionalDistributions().stream().mapToInt(p->p.getNumberOfParameters()).max().getAsInt());

        ParallelVMP vmp = new ParallelVMP();

        InferenceEngine.setInferenceAlgorithm(vmp);
        Variable var = bn.getStaticVariables().getVariableById(0);
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