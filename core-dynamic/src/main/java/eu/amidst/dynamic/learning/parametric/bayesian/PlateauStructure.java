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
package eu.amidst.dynamic.learning.parametric.bayesian;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class defines a Plateau Structure.
 */
public class PlateauStructure {

    /* Represents the list of parameters {@link Node}s at time 0. */
    List<Node> parametersNodeTime0;

    /* Represents the list of parameters {@link Node}s at time T. */
    List<Node> parametersNodeTimeT;

    /* Represents the list of {@link Node}s at time 0. */
    List<Node> nodesTime0;

    /** Represents the list of plateau {@link Node}s at time T. */
    List<List<Node>> plateuNodesTimeT;

    /* Represents the list of clone {@link Node}s at time T. */
    List<Node> cloneNodesTimeT;

    /* Represents the dynamic model as a {@link DynamicDAG} object. */
    DynamicDAG dbnModel;

    /** Represents the {@link EF_LearningBayesianNetwork} model at time T. */
    EF_LearningBayesianNetwork ef_learningmodelTimeT;

    /** Represents the {@link EF_LearningBayesianNetwork} model at time 0. */
    EF_LearningBayesianNetwork ef_learningmodelTime0;

    /** Represents the number of replications. */
    int nRepetitions = 100;

    /** Represents the {@link VMP} object at time 0. */
    VMP vmpTime0 = new VMP();

    /** Represents the {@link VMP} object at time T. */
    VMP vmpTimeT = new VMP();

    /** Represents a {@code Map} object that maps clone {@link Variable}s to the corresponding {@link Node}s. */
    Map<Variable, Node> cloneVariablesToNode;

    /** Represents a {@code Map} object that maps {@link Variable} parameters to the corresponding {@link Node}s at time T. */
    Map<Variable, Node> parametersToNodeTimeT;

    /** Represents a {@code Map} object that maps {@link Variable} parameters to the corresponding {@link Node}s at time 0. */
    Map<Variable, Node> parametersToNodeTime0;

    /** Represents a {@code Map} object that maps {@link Variable} to the corresponding {@link Node}s at time 0. */
    Map<Variable, Node> variablesToNodeTime0;

    /** Represents the list of {@code Map} objects that map {@link Variable}s to the corresponding {@link Node}s at time T. */
    List<Map<Variable, Node>> variablesToNodeTimeT;

    /**
     * Resets the exponential family distributions of all nodes for the {@link VMP} objects at time 0 and T for this DynamicPlateauStructure.
     */
    public void resetQs() {
        this.vmpTime0.resetQs();
        this.vmpTimeT.resetQs();
    }

    /**
     * Sets the dynamic DAG for this this DynamicPlateauStructure.
     * @param dbnModel a {@link DynamicDAG} object.
     */
    public void setDBNModel(DynamicDAG dbnModel) {
        this.dbnModel = dbnModel;

        List<EF_ConditionalDistribution> distTim0 = this.dbnModel.getParentSetsTime0().stream().map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents())).collect(Collectors.toList());
        this.setEFBayesianNetworkTime0(new EF_LearningBayesianNetwork(distTim0));

        List<EF_ConditionalDistribution> distTimT = this.dbnModel.getParentSetsTimeT().stream().map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents())).collect(Collectors.toList());
        this.setEFBayesianNetworkTimeT(new EF_LearningBayesianNetwork(distTimT));
    }

    /**
     * Returns the {@link VMP} object at time 0 of this DynamicPlateauStructure.
     * @return a {@link VMP} object.
     */
    public VMP getVMPTime0() {
        return vmpTime0;
    }

    /**
     * Returns the {@link VMP} object at time T of this DynamicPlateauStructure.
     * @return a {@link VMP} object.
     */
    public VMP getVMPTimeT() {
        return vmpTimeT;
    }

    /**
     * Sets the seed for the {@link VMP} object at times 0 and T for this DynamicPlateauStructure.
     * @param seed an {@code int} that represents the seed value.
     */
    public void setSeed(int seed) {
        this.vmpTime0.setSeed(seed);
        this.vmpTimeT.setSeed(seed);
    }

    /**
     * Returns the {@link EF_LearningBayesianNetwork} at time T of this DynamicPlateauStructure.
     * @return an {@link EF_LearningBayesianNetwork} object.
     */
    public EF_LearningBayesianNetwork getEFLearningBNTimeT() {
        return ef_learningmodelTimeT;
    }

    /**
     * Returns the {@link EF_LearningBayesianNetwork} at time 0 of this DynamicPlateauStructure.
     * @return an {@link EF_LearningBayesianNetwork} object.
     */
    public EF_LearningBayesianNetwork getEFLearningBNTime0() {
        return ef_learningmodelTime0;
    }

    /**
     * Sets the number of repetitions for this DynamicPlateauStructure.
     * @param nRepetitions_ an {@code int} that represents the number of repetitions to be set.
     */
    public void setNRepetitions(int nRepetitions_) {
        this.nRepetitions = nRepetitions_;
    }

    /**
     * Runs inference at time T.
     */
    public void runInferenceTimeT() {
        this.vmpTimeT.runInference();
        this.plateuNodesTimeT.get(this.nRepetitions-1).stream().filter(node -> !node.isObserved() && !node.getMainVariable().isParameterVariable()).forEach(node -> {
            Variable temporalClone = this.dbnModel.getDynamicVariables().getInterfaceVariable(node.getMainVariable());
            moveNodeQDist(this.getNodeOfVarTimeT(temporalClone,0), node);
        });
    }

    /**
     * Runs inference at time 0.
     */
    public void runInferenceTime0() {
        this.vmpTime0.runInference();
        this.vmpTime0.getNodes().stream().filter(node -> !node.isObserved() && !node.getMainVariable().isParameterVariable()).forEach(node -> {
            Variable temporalClone = this.dbnModel.getDynamicVariables().getInterfaceVariable(node.getMainVariable());
            moveNodeQDist(this.getNodeOfVarTimeT(temporalClone,0), node);
        });
    }

    /**
     * Moves the exponential family distributions.
     * @param toTemporalCloneNode a {@link Node} object.
     * @param fromNode a {@link Node} object.
     */
    private static void moveNodeQDist(Node toTemporalCloneNode, Node fromNode){
        EF_UnivariateDistribution uni = fromNode.getQDist().deepCopy(toTemporalCloneNode.getMainVariable());
        toTemporalCloneNode.setPDist(uni);
        toTemporalCloneNode.setQDist(uni);
    }

    /**
     * Returns the log probability of the evidence at time T.
     * @return a {@link double} value that represents the log probability of the evidence at time T.
     */
    public double getLogProbabilityOfEvidenceTimeT() {
        return this.vmpTimeT.getLogProbabilityOfEvidence();
    }

    /**
     * Returns the log probability of the evidence at time T.
     * @return a {@link double} value that represents the log probability of the evidence at time T.
     */
    public double getLogProbabilityOfEvidenceTime0() {
        return this.vmpTime0.getLogProbabilityOfEvidence();
    }

    /**
     * Sets the {@link EF_LearningBayesianNetwork} model at time 0.
     * @param modelTime0 a {@link EF_LearningBayesianNetwork} object.
     */
    private void setEFBayesianNetworkTime0(EF_LearningBayesianNetwork modelTime0) {
        ef_learningmodelTime0 = modelTime0;
        parametersNodeTime0 = new ArrayList();

        parametersToNodeTime0 = new ConcurrentHashMap<>();
        parametersNodeTime0 = ef_learningmodelTime0.getDistributionList()
                .stream()
                .filter(dist -> dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    parametersToNodeTime0.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());
        this.variablesToNodeTime0 =  new ConcurrentHashMap<>();

        this.nodesTime0 = ef_learningmodelTime0.getDistributionList().stream()
                .filter(dist -> !dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    variablesToNodeTime0.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        for (Node node : nodesTime0) {
            node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVarTime0(var)).collect(Collectors.toList()));
            node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVarTime0(var).getChildren().add(node));
        }

        List<Node> allNodesTime0 = new ArrayList();
        allNodesTime0.addAll(this.parametersNodeTime0);
        allNodesTime0.addAll(nodesTime0);
        this.vmpTime0.setNodes(allNodesTime0);

    }

    /**
     * Sets the {@link EF_LearningBayesianNetwork} model at time T.
     * @param modelTimeT a {@link EF_LearningBayesianNetwork} object.
     */
    private void setEFBayesianNetworkTimeT(EF_LearningBayesianNetwork modelTimeT) {

        ef_learningmodelTimeT = modelTimeT;
        parametersNodeTimeT = new ArrayList();
        plateuNodesTimeT = new ArrayList(nRepetitions);
        cloneNodesTimeT = new ArrayList();
        variablesToNodeTimeT = new ArrayList();
        parametersToNodeTimeT = new ConcurrentHashMap<>();

        parametersNodeTimeT = ef_learningmodelTimeT.getDistributionList()
                .stream()
                .filter(dist -> dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    parametersToNodeTimeT.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        cloneVariablesToNode = new ConcurrentHashMap<>();
        cloneNodesTimeT = ef_learningmodelTimeT.getDistributionList()
                .stream()
                .filter(dist -> !dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Variable temporalClone = this.dbnModel.getDynamicVariables().getInterfaceVariable(dist.getVariable());
                    EF_UnivariateDistribution uni = temporalClone.getDistributionType().newUnivariateDistribution().toEFUnivariateDistribution();
                    Node node = new Node(uni);
                    node.setActive(false);
                    cloneVariablesToNode.put(temporalClone, node);
                    return node;
                })
                .collect(Collectors.toList());

        for (int i = 0; i < nRepetitions; i++) {
            final int slice = i;
            Map<Variable, Node> map = new ConcurrentHashMap<>();
            List<Node> tmpNodes = ef_learningmodelTimeT.getDistributionList().stream()
                    .filter(dist -> !dist.getVariable().isParameterVariable())
                    .map(dist -> {
                        Node node = new Node(dist, dist.getVariable().getName()+"_Slice_"+slice);
                        map.put(dist.getVariable(), node);
                        return node;
                    })
                    .collect(Collectors.toList());
            this.variablesToNodeTimeT.add(map);
            plateuNodesTimeT.add(tmpNodes);
        }

        for (int i = 0; i < nRepetitions; i++) {
            for (Node node : plateuNodesTimeT.get(i)) {
                final int slice = i;
                node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVarTimeT(var, slice)).collect(Collectors.toList()));

                node.getPDist().getConditioningVariables().stream().filter(var -> var.isInterfaceVariable()).forEach(var -> node.setVariableToNodeParent(var, this.getNodeOfVarTimeT(var, slice)));

                node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVarTimeT(var, slice).getChildren().add(node));
            }
        }

        List<Node> allNodesTimeT = new ArrayList();

        allNodesTimeT.addAll(this.parametersNodeTimeT);

        for (int i = 0; i < nRepetitions; i++) {
            allNodesTimeT.addAll(this.plateuNodesTimeT.get(i));
        }

        this.vmpTimeT.setNodes(allNodesTimeT);
    }

    /**
     * Sets the evidence at time 0.
     * @param data a {@link DynamicDataInstance} object.
     */
    public void setEvidenceTime0(DynamicDataInstance data) {
        this.vmpTime0.setEvidence(data);
    }

    /**
     * Sets the evidence at time T.
     * @param data a {@code List} of {@link DynamicDataInstance} objects.
     */
    public void setEvidenceTimeT(List<DynamicDataInstance> data) {
        if (data.size()>nRepetitions)
            throw new IllegalArgumentException("The size of the data is bigger than the number of repetitions");

        this.cloneNodesTimeT.forEach( node -> node.setAssignment(data.get(0)));

        for (int i = 0; i < nRepetitions && i<data.size(); i++) {
            final int slice = i;
            this.plateuNodesTimeT.get(i).forEach(node -> {
                node.setAssignment(data.get(slice));
                node.setActive(true);

                //No barrent nodes
                if (!node.isObserved() && node.getChildren().isEmpty())
                    node.setActive(false);


            });
        }

        for (int i = data.size(); i < nRepetitions; i++) {
            this.plateuNodesTimeT.get(i).forEach(node -> {
                node.setAssignment(null);
                node.setActive(false);
            });
        }
    }

    /**
     * Returns the {@link Node} corresponding to a given {@link Variable} at time 0.
     * @param variable a given {@link Variable} object.
     * @return a {@link Node} object.
     */
    public Node getNodeOfVarTime0(Variable variable) {
        if (variable.isParameterVariable()) {
            return this.parametersToNodeTime0.get(variable);
        }else{
            return this.variablesToNodeTime0.get(variable);
        }
    }

    /**
     * Returns the {@link Node} corresponding to a given {@link Variable} at time T.
     * @param variable a given {@link Variable} object.
     * @param slice an {@code int} that represents the slice value.
     * @return a {@link Node} object.
     */
    public Node getNodeOfVarTimeT(Variable variable, int slice) {
        if (variable.isParameterVariable()) {
            return this.parametersToNodeTimeT.get(variable);
        } else if (!variable.isInterfaceVariable()){
            return this.variablesToNodeTimeT.get(slice).get(variable);
        }else if (variable.isInterfaceVariable() && slice>0){
            return this.variablesToNodeTimeT.get(slice - 1).get(this.dbnModel.getDynamicVariables().getVariableFromInterface(variable));
        }else if (variable.isInterfaceVariable() && slice==0){
            return this.cloneVariablesToNode.get(variable);
        }else{
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the exponential family variable posterior at time T for a given {@link Variable}.
     * @param var a given {@link Variable} object.
     * @param <E> a subtype distribution of {@link EF_UnivariateDistribution}.
     * @return an {@link EF_UnivariateDistribution} object.
     */
    public <E extends EF_UnivariateDistribution> E getEFParameterPosteriorTimeT(Variable var) {
        if (!var.isParameterVariable())
            throw new IllegalArgumentException("Only parameter variables can be queried");

        return (E)this.parametersToNodeTimeT.get(var).getQDist();
    }

    /**
     * Returns the exponential family variable posterior at time 0 for a given {@link Variable}.
     * @param var a given {@link Variable} object.
     * @param <E> a subtype distribution of {@link EF_UnivariateDistribution}.
     * @return an {@link EF_UnivariateDistribution} object.
     */
    public <E extends EF_UnivariateDistribution> E getEFParameterPosteriorTime0(Variable var) {
        if (!var.isParameterVariable())
            throw new IllegalArgumentException("Only parameter variables can be queried");

        return (E)this.parametersToNodeTime0.get(var).getQDist();
    }

}