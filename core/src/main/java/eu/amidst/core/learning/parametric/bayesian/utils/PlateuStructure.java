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

package eu.amidst.core.learning.parametric.bayesian.utils;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.models.DAG;
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
import java.util.stream.Stream;

/**
 * This class defines a Plateu Structure.
 */
public abstract class PlateuStructure implements Serializable {

    /**
     * Represents the serial version ID for serializing the object.
     */
    private static final long serialVersionUID = 4107783324901370839L;

    /**
     * Represents a map describing which variables are replicated
     */
    protected Map<Variable, Boolean> replicatedVariables = new HashMap<>();

    /**
     * Represents the list of non replicated {@link Node}s.
     */
    transient protected List<Node> nonReplictedNodes = new ArrayList();

    /**
     * Represents the list of replicated nodes {@link Node}s.
     */
    transient protected List<List<Node>> replicatedNodes = new ArrayList<>();

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
    transient protected Map<Variable, Node> nonReplicatedVarsToNode = new ConcurrentHashMap<>();

    /**
     * Represents the list of {@code Map} objects that map {@link Variable}s to the corresponding {@link Node}s.
     */
    transient protected List<Map<Variable, Node>> replicatedVarsToNode = new ArrayList<>();


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
     * @param initialNonReplicatedVariablesList list of variables
     */
    public PlateuStructure(List<Variable> initialNonReplicatedVariablesList) {
        this.initialNonReplicatedVariablesList = new ArrayList<>();
        this.initialNonReplicatedVariablesList.addAll(initialNonReplicatedVariablesList);
    }


    /**
     * Initializes the interal transient data structures.
     */
    public void initTransientDataStructure(){
        replicatedVarsToNode = new ArrayList<>();
        nonReplicatedVarsToNode = new ConcurrentHashMap<>();
        replicatedNodes = new ArrayList<>();
        nonReplictedNodes = new ArrayList();
    }

    public Stream<Node> getNonReplictedNodes() {
        return nonReplictedNodes.stream();
    }

    public Stream<Node> getReplicatedNodes() {
        return replicatedNodes.stream().flatMap(l -> l.stream());
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


    public void setVmp(VMP vmp) {
        this.vmp = vmp;
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
     * @return list of variables
     */
    public List<Variable> getNonReplicatedVariables() {
        return this.nonReplicatedVariablesList;
    }


    public List<Variable> getReplicatedVariables() {
        return this.replicatedVariables.entrySet().stream().filter(entry -> entry.getValue()).map(entry -> entry.getKey()).sorted((a,b) -> a.getVarID()-b.getVarID()).collect(Collectors.toList());
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
        if (!this.nonReplicatedVariablesList.contains(var) && !var.isParameterVariable())
            throw new IllegalArgumentException("Only non replicated variables or parameters can be queried");

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
        if (this.nonReplicatedVariablesList.contains(var) || var.isParameterVariable())
            throw new IllegalArgumentException("Only replicated variables can be queried");

        return (E) this.getNodeOfVar(var, slice).getQDist();
    }

    /**
     * Replicates the model of this PlateuStructure.
     */
    public abstract void replicateModel();

    /**
     * Sets the evidence for this PlateuStructure.
     *
     * @param data a {@code List} of {@link DataInstance}.
     */
    public abstract void setEvidence(List<? extends DataInstance> data);

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
                    NaturalParameters copy = this.ef_learningmodel.getDistribution(var).createZeroNaturalParameters();
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
                    EF_UnivariateDistribution qDist = this.getNodeOfNonReplicatedVar(var).getQDist();
                    NaturalParameters parameter = qDist.getNaturalParameters();
                    NaturalParameters copy = qDist.createZeroNaturalParameters();
                    copy.copy(parameter);
                    return copy;
                }).collect(Collectors.toList());

        return new CompoundVector(naturalPlateauParametersPriors);
    }

    public CompoundVector getPlateauMomentParameterPosterior() {

        List<Vector> momentPlateauParametersPriors = ef_learningmodel.getDistributionList().stream()
                .map(dist -> dist.getVariable())
                .filter(var -> isNonReplicatedVar(var))
                .map(var -> {
                    EF_UnivariateDistribution qDist = this.getNodeOfNonReplicatedVar(var).getQDist();
                    MomentParameters parameter = qDist.getMomentParameters();
                    MomentParameters copy = qDist.createZeroMomentParameters();
                    copy.copy(parameter);
                    return copy;
                }).collect(Collectors.toList());

        return new CompoundVector(momentPlateauParametersPriors);
    }

    public void updateNaturalParameterPosteriors(CompoundVector parameterVector) {

        final int[] count = new int[1];
        count[0] = 0;

        ef_learningmodel.getDistributionList().stream()
                .map(dist -> dist.getVariable())
                .filter(var -> isNonReplicatedVar(var))
                .forEach(var -> {
                    EF_UnivariateDistribution uni = this.getNodeOfNonReplicatedVar(var).getQDist();
                    uni.getNaturalParameters().copy(parameterVector.getVectorByPosition(count[0]));
                    uni.fixNumericalInstability();
                    uni.updateMomentFromNaturalParameters();
                    count[0]++;
                });
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


    public double getPosteriorSampleSize(){
        return Double.NaN;
    }

}