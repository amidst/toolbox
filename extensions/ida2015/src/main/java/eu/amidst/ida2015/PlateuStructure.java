/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.ida2015;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * This class defines a Plateu Structure.
 */
public abstract class PlateuStructure extends eu.amidst.core.learning.parametric.bayesian.PlateuStructure implements Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    /** Represents the list of {@link Node}s. */
    protected List<Node> parametersNode;

    /** Represents the list of plateu {@link Node}s. */
    protected List<List<Node>> plateuNodes;

    /** Represents the {@link EF_LearningBayesianNetwork} model. */
    protected EF_LearningBayesianNetwork ef_learningmodel;

    /** Represents the number of replications. */
    protected int nReplications = 100;

    /** Represents the {@link VMP} object. */
    protected VMP vmp = new VMP();

    /** Represents a {@code Map} object that maps {@link Variable} parameters to the corresponding {@link Node}s. */
    protected Map<Variable, Node> parametersToNode;

    /** Represents the list of {@code Map} objects that map {@link Variable}s to the corresponding {@link Node}s. */
    protected List<Map<Variable, Node>> variablesToNode;

    /**
     * Returns the number of replications of this PlateuStructure.
     * @return the number of replications.
     */
    public int getNumberOfReplications() {
        return nReplications;
    }

    /**
     * Returns the {@link VMP} object of this PlateuStructure.
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
     * @param seed an {@code int} that represents the seed value.
     */
    public void setSeed(int seed) {
        this.vmp.setSeed(seed);
    }

    /**
     * Returns the {@link EF_LearningBayesianNetwork} of this PlateuStructure.
     * @return an {@link EF_LearningBayesianNetwork} object.
     */
    public EF_LearningBayesianNetwork getEFLearningBN() {
        return ef_learningmodel;
    }

    /**
     * Sets the {@link EF_LearningBayesianNetwork} of this PlateuStructure.
     * @param model the {@link EF_LearningBayesianNetwork} model to be set.
     */
    public void setEFBayesianNetwork(EF_LearningBayesianNetwork model) {
        ef_learningmodel = model;
    }

    /**
     * Sets the number of repetitions for this PlateuStructure.
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
     * @return the log probability of the evidence.
     */
    public double getLogProbabilityOfEvidence() {
        return this.vmp.getLogProbabilityOfEvidence();
    }

    /**
     * Returns the {@link Node} for a given variable and slice.
     * @param variable a {@link Variable} object.
     * @param slice an {@code int} that represents the slice value.
     * @return a {@link Node} object.
     */
    public Node getNodeOfVar(Variable variable, int slice) {
        if (variable.isParameterVariable())
            return this.parametersToNode.get(variable);
        else
            return this.variablesToNode.get(slice).get(variable);
    }

    /**
     * Returns the exponential family parameter posterior for a given {@link Variable} object.
     * @param var a given {@link Variable} object.
     * @param <E> a subtype distribution of {@link EF_UnivariateDistribution}.
     * @return an {@link EF_UnivariateDistribution} object.
     */
    public <E extends EF_UnivariateDistribution> E getEFParameterPosterior(Variable var) {
        if (!var.isParameterVariable())
            throw new IllegalArgumentException("Only parameter variables can be queried");

        return (E)this.parametersToNode.get(var).getQDist();
    }

    /**
     * Returns the exponential family variable posterior for a given {@link Variable} object and a slice value.
     * @param var a given {@link Variable} object.
     * @param slice an {@code int} that represents the slice value.
     * @param <E> a subtype distribution of {@link EF_UnivariateDistribution}.
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
    public abstract void replicateModel();

    /**
     * Sets the evidence for this PlateuStructure.
     * @param data a {@code List} of {@link DataInstance}.
     */
    public void setEvidence(List<? extends DataInstance> data) {
        if (data.size()> nReplications)
            throw new IllegalArgumentException("The size of the data is bigger than the number of repetitions");

        for (int i = 0; i < nReplications && i<data.size(); i++) {
            final int slice = i;
            this.plateuNodes.get(i).forEach(node -> {node.setAssignment(data.get(slice)); node.setActive(true);});
        }

        for (int i = data.size(); i < nReplications; i++) {
            this.plateuNodes.get(i).forEach(node -> {node.setAssignment(null); node.setActive(false);});
        }
    }

}
