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

package eu.amidst.lda.core;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.messagepassing.Message;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuStructure;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 28/4/16.
 */
public class PlateauLDA extends PlateuStructure {

    boolean globalUpdate = true;

    public static double TOPIC_PRIOR=0.01;
    public static double MIXING_PRIOR=0.1;

    Variable word;
    Variable topicIndicator;
    Variable dirichletMixingTopics;

    final Attributes attributes;
    final String wordDocumentName;
    final Attribute wordCountAtt;

    int nTopics = 2;


    transient List<? extends DataInstance> data;

    double local_elbo;
    double local_iter;

    DAG dagLDA;

    public PlateauLDA(Attributes attributes, String wordDocumentName, String wordCountName) {
        this.attributes = attributes;
        this.wordDocumentName = wordDocumentName;
        this.wordCountAtt = this.attributes.getAttributeByName(wordCountName);
    }

    public boolean isGlobalUpdate() {
        return globalUpdate;
    }

    public void setGlobalUpdate(boolean globalUpdate) {
        this.globalUpdate = globalUpdate;
    }

    public DAG getDagLDA() {
        return dagLDA;
    }

    public void setNTopics(int nTopics) {
        this.nTopics = nTopics;
    }

    @Override
    public void setDAG(DAG dag) {
        if (dag != null)
            throw new IllegalStateException("No possbile to define DAG");


        Variables variables = new Variables();
        word = variables.newSparseMultionomialVariable(attributes.getAttributeByName(wordDocumentName));
        //word = variables.newMultionomialVariable(attributes.getAttributeByName(wordDocumentName));

        topicIndicator = variables.newMultinomialVariable("TopicIndicator", nTopics);

        dagLDA = new DAG(variables);
        dagLDA.setName("LDA");

        dagLDA.getParentSet(word).addParent(topicIndicator);


        List<EF_ConditionalDistribution> dists = dagLDA.getParentSets().stream()
                .map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents()))
                .collect(Collectors.toList());

        ef_learningmodel = new EF_LearningBayesianNetwork(dists);
        this.ef_learningmodel.getListOfParametersVariables().stream().forEach(var -> this.replicatedVariables.put(var, false));
        this.ef_learningmodel.getListOfNonParameterVariables().stream().forEach(var -> this.replicatedVariables.put(var, true));

        //The Dirichlet parent of the topic is not replicated
        dirichletMixingTopics = ef_learningmodel.getDistribution(topicIndicator).getConditioningVariables().get(0);
        this.replicatedVariables.put(dirichletMixingTopics, true);


        this.nonReplicatedVariablesList = this.replicatedVariables.entrySet().stream().filter(entry -> !entry.getValue()).map(entry -> entry.getKey()).sorted((a, b) -> a.getVarID() - b.getVarID()).collect(Collectors.toList());

        for (int i = 0; i < nTopics; i++) {
            ef_learningmodel.getDistribution(dirichletMixingTopics).getNaturalParameters().set(i, MIXING_PRIOR);
        }


        for (Variable variable : this.nonReplicatedVariablesList) {
//            SparseVectorDefaultValue  vec = (SparseVectorDefaultValue)this.ef_learningmodel.getDistribution(variable).getNaturalParameters();
//            vec.setDefaultValue(0.01);

            NaturalParameters vec = this.ef_learningmodel.getDistribution(variable).getNaturalParameters();

            for (int i = 0; i < vec.size(); i++) {
                vec.set(i, TOPIC_PRIOR);
            }
        }

    }

    /**
     * Sets the evidence for this PlateuStructure.
     *
     * @param data a {@code List} of {@link DataInstance}.
     */
    public void setEvidence(List<? extends DataInstance> data) {
        this.data = data;
        this.replicateModelForDocs();

        //And reset the Q's of the new replicated nodes.
        this.getReplicatedNodes().filter(node -> !node.isObserved()).forEach(node -> {
            node.resetQDist(this.vmp.getRandom());
        });
    }


    private void replicateModelForDocs() {
        replicatedNodes = new ArrayList<>();
        Attribute seqIDAtt = this.attributes.getSeq_id();
        double currentID = 0;
        List<Node> tmpNodes = new ArrayList<>();

        Node nodeDirichletMixingTopics = null;
        Node nodeTopic = null;
        Node nodeWord = null;
        List<Node> wordNodes = new ArrayList<>();
        for (int i = 0; i < this.data.size(); i++) {

            if (i == 0) {
                currentID = data.get(i).getValue(seqIDAtt);
                nodeDirichletMixingTopics = new Node(ef_learningmodel.getDistribution(dirichletMixingTopics));
                tmpNodes.add(nodeDirichletMixingTopics);
            }


            if (currentID != data.get(i).getValue(seqIDAtt)) {
                currentID = data.get(i).getValue(seqIDAtt);
                this.replicatedNodes.add(tmpNodes);
                tmpNodes = new ArrayList<>();

                nodeDirichletMixingTopics = new Node(ef_learningmodel.getDistribution(dirichletMixingTopics));
                tmpNodes.add(nodeDirichletMixingTopics);


            }


            nodeTopic = new Node(ef_learningmodel.getDistribution(topicIndicator));
            nodeWord = new Node(ef_learningmodel.getDistribution(word));
            nodeWord.setAssignment(data.get(i));

            nodeWord.getSufficientStatistics().multiplyBy(data.get(i).getValue(wordCountAtt));

            tmpNodes.add(nodeTopic);
            tmpNodes.add(nodeWord);

            //Set Parents and children
            nodeTopic.setParents(Arrays.asList(nodeDirichletMixingTopics));
            nodeTopic.setChildren(Arrays.asList(nodeWord));

            nodeDirichletMixingTopics.getChildren().add(nodeTopic);

            List<Node> p = new ArrayList<>();
            p.addAll(this.nonReplictedNodes);
            p.add(nodeTopic);
            nodeWord.setParents(p);

            wordNodes.add(nodeWord);
        }

        //Add the last ones created.
        this.replicatedNodes.add(tmpNodes);


        for (Node nonReplictedNode : nonReplictedNodes) {
            nonReplictedNode.setChildren(wordNodes);
        }

        List<Node> allNodes = new ArrayList<>();

        allNodes.addAll(this.nonReplictedNodes);

        for (int i = 0; i < replicatedNodes.size(); i++) {
            allNodes.addAll(this.replicatedNodes.get(i));
        }

        this.vmp.setNodes(allNodes);
    }


    /**
     * Replicates this model.
     */
    @Override
    public void replicateModel() {
        //nonReplicatedNodes are the Dirichlet storing topics distributions.
        nonReplictedNodes = ef_learningmodel.getDistributionList().stream()
                .filter(dist -> isNonReplicatedVar(dist.getVariable()))
                .map(dist -> {
                    Node node = new Node(dist);
                    nonReplicatedVarsToNode.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());


        List<Node> allNodes = new ArrayList<>();

        allNodes.addAll(this.nonReplictedNodes);

        this.vmp.setNodes(allNodes);


    }

    /**
     * Resets the exponential family distributions of all nodes for the {@link VMP} object of this PlateuStructure.
     */
    public void resetQs() {
        //And reset the Q's of the new replicated nodes.
        this.getNonReplictedNodes().filter(node -> node.isActive()).forEach(node -> {
            node.resetQDist(this.vmp.getRandom());
        });
    }

    public void runInferenceGlobal() {

        boolean convergence = false;
        local_elbo = Double.NEGATIVE_INFINITY;
        local_iter = 0;
        while (!convergence && (local_iter++) < this.vmp.getMaxIter()) {

            long start = System.nanoTime();

            this.replicatedNodes
                    .parallelStream()
                    .forEach(nodes -> {
                        for (Node node : nodes) {


                            if (!node.isActive() || node.isObserved())
                                continue;

                            Message<NaturalParameters> selfMessage = this.vmp.newSelfMessage(node);

                            Optional<Message<NaturalParameters>> message = node.getChildren()
                                    .stream()
                                    .filter(children -> children.isActive())
                                    .map(children -> this.vmp.newMessageToParent(children, node))
                                    .reduce(Message::combineNonStateless);

                            if (message.isPresent())
                                selfMessage.combine(message.get());

                            //for (Node child: node.getChildren()){
                            //    selfMessage = Message.combine(newMessageToParent(child, node), selfMessage);
                            //}

                            this.vmp.updateCombinedMessage(node, selfMessage);
                        }
                    });

            this.nonReplictedNodes
                    .parallelStream()
                    .filter(node -> node.isActive() && !node.isObserved())
                    .forEach(node -> {
                        Message<NaturalParameters> selfMessage = this.vmp.newSelfMessage(node);

                        Optional<Message<NaturalParameters>> message = node.getChildren()
                                .stream()
                                .filter(children -> children.isActive())
                                .map(children -> this.vmp.newMessageToParent(children, node))
                                .reduce(Message::combineNonStateless);

                        if (message.isPresent())
                            selfMessage.combine(message.get());


                        this.vmp.updateCombinedMessage(node, selfMessage);
                    });

            convergence = this.testConvergence();

            //System.out.println(local_iter + " " + local_elbo + " " + (System.nanoTime() - start) / (double) 1e09);

        }

        if (this.vmp.isOutput()) {
            System.out.println("N Iter: " + local_iter + ", elbo:" + local_elbo);
        }
    }

    public void runInferenceLocal() {

        boolean convergence = false;
        local_elbo = Double.NEGATIVE_INFINITY;
        local_iter = 0;
        while (!convergence && (local_iter++) < this.vmp.getMaxIter()) {

            long start = System.nanoTime();

            this.replicatedNodes
                    .parallelStream()
                    .forEach(nodes -> {
                        for (Node node : nodes) {


                            if (!node.isActive() || node.isObserved())
                                continue;

                            Message<NaturalParameters> selfMessage = this.vmp.newSelfMessage(node);

                            Optional<Message<NaturalParameters>> message = node.getChildren()
                                    .stream()
                                    .filter(children -> children.isActive())
                                    .map(children -> this.vmp.newMessageToParent(children, node))
                                    .reduce(Message::combineNonStateless);

                            if (message.isPresent())
                                selfMessage.combine(message.get());

                            //for (Node child: node.getChildren()){
                            //    selfMessage = Message.combine(newMessageToParent(child, node), selfMessage);
                            //}

                            this.vmp.updateCombinedMessage(node, selfMessage);
                        }
                    });

            this.nonReplictedNodes
                    .parallelStream()
                    .filter(node -> node.isActive() && !node.isObserved())
                    .forEach(node -> {
                        Message<NaturalParameters> selfMessage = this.vmp.newSelfMessage(node);

                        Optional<Message<NaturalParameters>> message = node.getChildren()
                                .stream()
                                .filter(children -> children.isActive())
                                .map(children -> this.vmp.newMessageToParent(children, node))
                                .reduce(Message::combineNonStateless);

                        if (message.isPresent())
                            selfMessage.combine(message.get());


                        this.vmp.updateCombinedMessage(node, selfMessage);
                    });

            convergence = this.testConvergence();

            //System.out.println(local_iter + " " + local_elbo + " " + (System.nanoTime() - start) / (double) 1e09);

        }

        if (this.vmp.isOutput()) {
            System.out.println("N Iter: " + local_iter + ", elbo:" + local_elbo);
        }
    }

    /**
     * Runs inference.
     */
    public void runInference() {

        if (this.globalUpdate)
            this.runInferenceGlobal();
        else
            this.runInferenceLocal();

    }

    /**
     * Returns the log probability of the evidence.
     *
     * @return the log probability of the evidence.
     */
    public double getLogProbabilityOfEvidence() {
        return local_elbo;
    }

    private boolean testConvergence() {
        boolean convergence = false;

        //Compute lower-bound
        //double newelbo = this.vmp.getNodes().parallelStream().filter(node -> node.isActive()).mapToDouble(node -> this.vmp.computeELBO(node)).sum();
        double newelbo = computeELBO();

        double percentage = 100 * Math.abs(newelbo - local_elbo) / Math.abs(local_elbo);
        if (percentage < this.vmp.getThreshold() || local_iter > this.vmp.getMaxIter()) {
            convergence = true;
        }

        if ((!convergence && (newelbo / this.vmp.getNodes().size() < (local_elbo / this.vmp.getNodes().size() - 0.01)) && local_iter > -1) || Double.isNaN(local_elbo)) {
            throw new IllegalStateException("The elbo is not monotonically increasing at iter " + local_iter + ": " + percentage + ", " + local_elbo + ", " + newelbo);
        }


        local_elbo = newelbo;
        //System.out.println("ELBO: " + local_elbo);
        return convergence;
    }

    private double computeELBO() {


        double elbo = this.vmp.getNodes().parallelStream().filter(node -> node.isActive() && !node.isObserved()).mapToDouble(node -> this.vmp.computeELBO(node)).sum();

        elbo += this.vmp.getNodes()
                .parallelStream()
                .filter(node -> node.isActive() && node.isObserved()).mapToDouble(node -> {

                    EF_BaseDistribution_MultinomialParents base = (EF_BaseDistribution_MultinomialParents) node.getPDist();
                    Variable topicVariable = (Variable) base.getMultinomialParents().get(0);
                    Map<Variable, MomentParameters> momentParents = node.getMomentParents();

                    double localELBO = 0;

                    MomentParameters topicMoments = momentParents.get(topicVariable);
                    int wordIndex = (int) node.getAssignment().getValue(node.getMainVariable())%node.getMainVariable().getNumberOfStates();

                    for (int i = 0; i < topicMoments.size(); i++) {
                        EF_SparseMultinomial_Dirichlet dist = (EF_SparseMultinomial_Dirichlet)base.getBaseEFConditionalDistribution(i);
                        MomentParameters dirichletMoments = momentParents.get(dist.getDirichletVariable());
                        localELBO += node.getSufficientStatistics().get(wordIndex)*dirichletMoments.get(wordIndex)*topicMoments.get(i);
                    }

                    return localELBO;
                }).sum();


        return elbo;
    }

    @Override
    public double getPosteriorSampleSize() {

        double sum = 0;
        for (Variable variable : this.nonReplicatedVariablesList) {
            sum+=this.ef_learningmodel.getDistribution(variable).getNaturalParameters().sum();

        }

        return sum;
    }
}