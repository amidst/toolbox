/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.learning.parametric.bayesian;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;

/**
 * Created by andresmasegosa on 10/03/15.
 */
public abstract class PlateuStructure {
    protected List<Node> parametersNode;
    protected List<List<Node>> plateuNodes;
    protected EF_LearningBayesianNetwork ef_learningmodel;
    protected int nReplications = 100;
    protected VMP vmp = new VMP();

    protected Map<Variable, Node> parametersToNode;

    protected List<Map<Variable, Node>> variablesToNode;

    public int getNumberOfReplications() {
        return nReplications;
    }

    public VMP getVMP() {
        return vmp;
    }

    public void resetQs() {
        this.vmp.resetQs();
    }

    public void setSeed(int seed) {
        this.vmp.setSeed(seed);
    }

    public EF_LearningBayesianNetwork getEFLearningBN() {
        return ef_learningmodel;
    }

    public void setNRepetitions(int nRepetitions_) {
        this.nReplications = nRepetitions_;
    }

    public void runInference() {
        this.vmp.runInference();
    }

    public double getLogProbabilityOfEvidence() {
        return this.vmp.getLogProbabilityOfEvidence();
    }

    public void setEFBayesianNetwork(EF_LearningBayesianNetwork model) {
        ef_learningmodel = model;
    }

    public Node getNodeOfVar(Variable variable, int slice) {
        if (variable.isParameterVariable())
            return this.parametersToNode.get(variable);
        else
            return this.variablesToNode.get(slice).get(variable);
    }

    public <E extends EF_UnivariateDistribution> E getEFParameterPosterior(Variable var) {
        if (!var.isParameterVariable())
            throw new IllegalArgumentException("Only parameter variables can be queried");

        return (E)this.parametersToNode.get(var).getQDist();
    }

    public <E extends EF_UnivariateDistribution> E getEFVariablePosterior(Variable var, int slice) {
        if (var.isParameterVariable())
            throw new IllegalArgumentException("Only non parameter variables can be queried");

        return (E) this.getNodeOfVar(var, slice).getQDist();
    }

    public abstract void replicateModel();

    public void setEvidence(List<DataInstance> data) {
        if (data.size()> nReplications)
            throw new IllegalArgumentException("The size of the data is bigger than the number of repetitions");

        for (int i = 0; i < nReplications && i<data.size(); i++) {
            final int slice = i;
            this.plateuNodes.get(i).forEach(node -> node.setAssignment(data.get(slice)));
        }

        for (int i = data.size(); i < nReplications; i++) {
            this.plateuNodes.get(i).forEach(node -> node.setAssignment(null));
        }
    }

}
