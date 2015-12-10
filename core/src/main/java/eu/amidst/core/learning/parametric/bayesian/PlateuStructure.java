/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class defines a Plateu Structure.
 */
public class PlateuStructure implements Serializable {

    /**
     * Represents the serial version ID for serializing the object.
     */
    private static final long serialVersionUID = 4107783324901370839L;

    /**
     * Represents a map describing which variables are replicated
     */
    Map<Variable, Boolean> replicatedVariables;

    /**
     * Represents the list of non replicated {@link Node}s.
     */
    protected List<Node> nonReplictedNodes;

    /**
     * Represents the list of replicated nodes {@link Node}s.
     */
    protected List<List<Node>> replicatedNodes;

    /**
     * Represents the {@link EF_LearningBayesianNetwork} model.
     */
    protected EF_LearningBayesianNetwork ef_learningmodel;

    /**
     * Represents the number of replications.
     */
    protected int nReplications = 100;

    /**
     * Represents the {@link VMP} object.
     */
    protected VMP vmp = new VMP();

    /**
     * Represents a {@code Map} object that maps {@link Variable} parameters to the corresponding {@link Node}s.
     */
    protected Map<Variable, Node> nonReplicatedVarsToNode;

    /**
     * Represents the list of {@code Map} objects that map {@link Variable}s to the corresponding {@link Node}s.
     */
    protected List<Map<Variable, Node>> replicatedVarsToNode;


    /**
     * Represents the initial list of non-replicated variables
     */
    protected List<Variable> initialNonReplicatedVariablesList;


    /**
     * Represents the list of non-replicated variables
     */
    protected List<Variable> nonReplicatedVariablesList;


    /**
     * Empty builder.
     */
    public PlateuStructure() {
        initialNonReplicatedVariablesList = new ArrayList<>();
    }

    /**
     * Builder which initially specify a list of non-replicated variables.
     *
     * @param initialNonReplicatedVariablesList
     */
    public PlateuStructure(List<Variable> initialNonReplicatedVariablesList) {
        this.initialNonReplicatedVariablesList = new ArrayList<>();
        this.initialNonReplicatedVariablesList.addAll(initialNonReplicatedVariablesList);
    }

    /**
     * Returns the number of replications of this PlateuStructure.
     *
     * @return the number of replications.
     */
    public int getNumberOfReplications() {
        return nReplications;
    }

    /**
     * Returns the {@link VMP} object of this PlateuStructure.
     *
     * @return the {@link VMP} object.
     */
    public VMP getVMP() {
        return vmp;
    }

    /**
     * Resets the exponential family distributions of all nodes for the {@link VMP} object of this PlateuStructure.
     */
    public void resetQs() {
        this.vmp.resetQs();
    }

    /**
     * Sets the seed for the {@link VMP} object of this PlateuStructure.
     *
     * @param seed an {@code int} that represents the seed value.
     */
    public void setSeed(int seed) {
        this.vmp.setSeed(seed);
    }

    /**
     * Returns the {@link EF_LearningBayesianNetwork} of this PlateuStructure.
     *
     * @return an {@link EF_LearningBayesianNetwork} object.
     */
    public EF_LearningBayesianNetwork getEFLearningBN() {
        return ef_learningmodel;
    }

    /**
     * Sets the {@link DAG} of this PlateuStructure. By default,
     * all parameter variables are set as non-replicated and all non-parameter variables
     * are set as replicated.
     *
     * @param dag the {@link DAG} model to be set.
     */
    public void setDAG(DAG dag) {

        List<EF_ConditionalDistribution> dists = dag.getParentSets().stream()
                .map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents()))
                .collect(Collectors.toList());

        ef_learningmodel = new EF_LearningBayesianNetwork(dists, this.initialNonReplicatedVariablesList);
        this.replicatedVariables = new HashMap<>();
        this.ef_learningmodel.getListOfParametersVariables().stream().forEach(var -> this.replicatedVariables.put(var, false));
        this.ef_learningmodel.getListOfNonParameterVariables().stream().forEach(var -> this.replicatedVariables.put(var, true));

        this.initialNonReplicatedVariablesList.stream().forEach(var -> this.replicatedVariables.put(var, false));


        this.nonReplicatedVariablesList = this.replicatedVariables.entrySet().stream().filter(entry -> !entry.getValue()).map(entry -> entry.getKey()).sorted((a,b) -> a.getVarID()-b.getVarID()).collect(Collectors.toList());
    }

    /**
     * Sets a given variable as a non replicated variable.
     *
     * @param var, a {@link Variable} object.
     */
    private void setVariableAsNonReplicated(Variable var) {
        this.replicatedVariables.put(var, false);
    }

    /**
     * Sets a given variable as a replicated variable.
     *
     * @param var, a {@link Variable} object.
     */
    private void setVariableAsReplicated(Variable var) {
        this.replicatedVariables.put(var, true);
    }

    /**
     * Returns the list of non replicated Variables
     *
     * @return
     */
    public List<Variable> getNonReplicatedVariables() {
        return this.nonReplicatedVariablesList;
    }



    /**
     * Sets the number of repetitions for this PlateuStructure.
     *
     * @param nRepetitions_ an {@code int} that represents the number of repetitions to be set.
     */
    public void setNRepetitions(int nRepetitions_) {
        this.nReplications = nRepetitions_;
    }

    /**
     * Runs inference.
     */
    public void runInference() {
        this.vmp.runInference();
    }

    /**
     * Returns the log probability of the evidence.
     *
     * @return the log probability of the evidence.
     */
    public double getLogProbabilityOfEvidence() {
        return this.vmp.getLogProbabilityOfEvidence();
    }

    /**
     * Returns the {@link Node} for a given variable and slice.
     *
     * @param variable a {@link Variable} object.
     * @param slice    an {@code int} that represents the slice value.
     * @return a {@link Node} object.
     */
    public Node getNodeOfVar(Variable variable, int slice) {
        if (isNonReplicatedVar(variable))
            return this.nonReplicatedVarsToNode.get(variable);
        else
            return this.replicatedVarsToNode.get(slice).get(variable);
    }

    /**
     * Returns the exponential family parameter posterior for a given {@link Variable} object.
     *
     * @param var a given {@link Variable} object.
     * @param <E> a subtype distribution of {@link EF_UnivariateDistribution}.
     * @return an {@link EF_UnivariateDistribution} object.
     */
    public <E extends EF_UnivariateDistribution> E getEFParameterPosterior(Variable var) {
        if (!var.isParameterVariable())
            throw new IllegalArgumentException("Only parameter variables can be queried");

        return (E) this.nonReplicatedVarsToNode.get(var).getQDist();
    }

    /**
     * Returns the exponential family variable posterior for a given {@link Variable} object and a slice value.
     *
     * @param var   a given {@link Variable} object.
     * @param slice an {@code int} that represents the slice value.
     * @param <E>   a subtype distribution of {@link EF_UnivariateDistribution}.
     * @return an {@link EF_UnivariateDistribution} object.
     */
    public <E extends EF_UnivariateDistribution> E getEFVariablePosterior(Variable var, int slice) {
        if (var.isParameterVariable())
            throw new IllegalArgumentException("Only non parameter variables can be queried");

        return (E) this.getNodeOfVar(var, slice).getQDist();
    }

    /**
     * Replicates the model of this PlateuStructure.
     */
    public void replicateModel() {
        nonReplictedNodes = new ArrayList();
        replicatedNodes = new ArrayList<>(nReplications);

        replicatedVarsToNode = new ArrayList<>();
        nonReplicatedVarsToNode = new ConcurrentHashMap<>();
        nonReplictedNodes = ef_learningmodel.getDistributionList().stream()
                .filter(dist -> isNonReplicatedVar(dist.getVariable()))
                .map(dist -> {
                    Node node = new Node(dist);
                    nonReplicatedVarsToNode.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        for (int i = 0; i < nReplications; i++) {

            Map<Variable, Node> map = new ConcurrentHashMap<>();
            List<Node> tmpNodes = ef_learningmodel.getDistributionList().stream()
                    .filter(dist -> isReplicatedVar(dist.getVariable()))
                    .map(dist -> {
                        Node node = new Node(dist);
                        map.put(dist.getVariable(), node);
                        return node;
                    })
                    .collect(Collectors.toList());
            this.replicatedVarsToNode.add(map);
            replicatedNodes.add(tmpNodes);
        }

        for (int i = 0; i < nReplications; i++) {
            for (Node node : replicatedNodes.get(i)) {
                final int slice = i;
                node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVar(var, slice)).collect(Collectors.toList()));
                node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVar(var, slice).getChildren().add(node));
            }
        }

        List<Node> allNodes = new ArrayList();

        allNodes.addAll(this.nonReplictedNodes);

        for (int i = 0; i < nReplications; i++) {
            allNodes.addAll(this.replicatedNodes.get(i));
        }

        this.vmp.setNodes(allNodes);
    }

    /**
     * Sets the evidence for this PlateuStructure.
     *
     * @param data a {@code List} of {@link DataInstance}.
     */
    public void setEvidence(List<? extends DataInstance> data) {
        if (data.size() > nReplications)
            throw new IllegalArgumentException("The size of the data is bigger than the number of repetitions");

        for (int i = 0; i < nReplications && i < data.size(); i++) {
            final int slice = i;
            this.replicatedNodes.get(i).forEach(node -> {
                node.setAssignment(data.get(slice));
                node.setActive(true);
            });
        }

        for (int i = data.size(); i < nReplications; i++) {
            this.replicatedNodes.get(i).forEach(node -> {
                node.setAssignment(null);
                node.setActive(false);
            });
        }
    }

    public Node getNodeOfNonReplicatedVar(Variable variable) {
        if (isNonReplicatedVar(variable))
            return this.nonReplicatedVarsToNode.get(variable);
        else
            throw new IllegalArgumentException("This variable is a replicated var.");
    }

    public boolean isNonReplicatedVar(Variable var){
        return !this.replicatedVariables.get(var);
    }

    public boolean isReplicatedVar(Variable var){
        return this.replicatedVariables.get(var);
    }

    public CompoundVector getPlateauNaturalParameterPrior() {

        List<Vector> naturalPlateauParametersPriors = ef_learningmodel.getDistributionList().stream()
                .map(dist -> dist.getVariable())
                .filter(var -> isNonReplicatedVar(var))
                .map(var -> {
                    NaturalParameters parameter = this.ef_learningmodel.getDistribution(var).getNaturalParameters();
                    NaturalParameters copy = new ArrayVector(parameter.size());
                    copy.copy(parameter);
                    return copy;
                }).collect(Collectors.toList());

        return new CompoundVector(naturalPlateauParametersPriors);
    }

    public CompoundVector getPlateauNaturalParameterPosterior() {

        List<Vector> naturalPlateauParametersPriors = ef_learningmodel.getDistributionList().stream()
                .map(dist -> dist.getVariable())
                .filter(var -> isNonReplicatedVar(var))
                .map(var -> {
                    NaturalParameters parameter =this.getNodeOfNonReplicatedVar(var).getQDist().getNaturalParameters();
                    NaturalParameters copy = new ArrayVector(parameter.size());
                    copy.copy(parameter);
                    return copy;
                }).collect(Collectors.toList());

        return new CompoundVector(naturalPlateauParametersPriors);
    }


    /**
     * Updates the Natural Parameter Prior from a given parameter vector.
     * @param parameterVector a {@link CompoundVector} object.
     */
    public void updateNaturalParameterPrior(CompoundVector parameterVector) {

        final int[] count = new int[1];
        count[0] = 0;

        ef_learningmodel.getDistributionList().stream()
                .map(dist -> dist.getVariable())
                .filter(var -> isNonReplicatedVar(var))
                .forEach(var -> {
                    EF_UnivariateDistribution uni = this.getNodeOfNonReplicatedVar(var).getQDist().deepCopy();
                    uni.getNaturalParameters().copy(parameterVector.getVectorByPosition(count[0]));
                    uni.fixNumericalInstability();
                    uni.updateMomentFromNaturalParameters();
                    this.ef_learningmodel.setDistribution(var, uni);
                    this.getNodeOfNonReplicatedVar(var).setPDist(uni);
                    count[0]++;
                });
    }


    public void desactiveParametersNodes(){
        this.ef_learningmodel.getParametersVariables().getListOfParamaterVariables().stream()
                .forEach(var -> this.getNodeOfNonReplicatedVar(var).setActive(false));
    }

    public void activeParametersNodes() {
        this.ef_learningmodel.getParametersVariables().getListOfParamaterVariables().stream()
                .forEach(var -> this.getNodeOfNonReplicatedVar(var).setActive(true));
    }

}