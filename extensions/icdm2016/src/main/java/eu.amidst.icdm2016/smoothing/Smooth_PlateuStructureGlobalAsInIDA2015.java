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

package eu.amidst.icdm2016.smoothing;

import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.core.models.DAG;
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
public class Smooth_PlateuStructureGlobalAsInIDA2015 extends PlateuStructure implements Serializable {

    DAG dag;

    /**
     * Represents the serial version ID for serializing the object.
     */
    private static final long serialVersionUID = 4107783324901370839L;

    /**
     * Builder which initially specify a list of non-replicated variables.
     *
     * @param initialNonReplicatedVariablesList
     */
    public Smooth_PlateuStructureGlobalAsInIDA2015(List<Variable> initialNonReplicatedVariablesList) {
        this.initialNonReplicatedVariablesList = new ArrayList<>();
        this.initialNonReplicatedVariablesList.addAll(initialNonReplicatedVariablesList);
    }

    public void setDAG(DAG dag) {
        this.dag = dag;
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
     * Replicates the model of this PlateuStructure.
     */
    public void replicateModel() {
        nonReplictedNodes = new ArrayList();
        replicatedNodes = new ArrayList<>(nReplications);


        replicatedVarsToNode = new ArrayList<>();
        nonReplicatedVarsToNode = new ConcurrentHashMap<>();

        nonReplictedNodes = ef_learningmodel.getDistributionList().stream()
                .filter(dist -> isNonReplicatedVar(dist.getVariable()))
                .filter(dist -> !dist.getVariable().getName().startsWith("GlobalHidden"))
                .filter(dist -> dist.getVariable().getName().startsWith("DEFAULTING") || dist.getVariable().getName().split("__")[1].split("_")[0].compareTo("0")==0)
                .map(dist -> {
                    Node node = new Node(dist);
                    nonReplicatedVarsToNode.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());


        for (Variable hiddenVar : initialNonReplicatedVariablesList) {
            Node nodeHiddenVar = new Node(ef_learningmodel.getDistribution(hiddenVar));
            nonReplicatedVarsToNode.put(hiddenVar, nodeHiddenVar);
            nonReplictedNodes.add(nodeHiddenVar);
        }

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
            int index = 0;
            for (Node node : replicatedNodes.get(i)) {
                final int slice = i;
                List<Node> parents = new ArrayList<>();
                for (Variable variable : node.getPDist().getConditioningVariables()) {
                    if (!variable.getName().contains("Beta") && !variable.getName().contains("Gamma")) {
                        parents.add(this.getNodeOfVar(variable, slice));
                        this.getNodeOfVar(variable,slice).getChildren().add(node);
                    }
                }

                //Get reference variable
                Variable referenceVariable = this.dag.getVariables().getVariableByName(node.getMainVariable().getName().split("__")[0]+"__0");

                for (Variable variable : this.getNodeOfVar(referenceVariable,0).getPDist().getConditioningVariables()) {
                    if (variable.getName().contains("Beta") || variable.getName().contains("Gamma")) {
                        parents.add(this.getNodeOfNonReplicatedVar(variable));
                        this.getNodeOfNonReplicatedVar(variable).getChildren().add(node);
                    }
                }
                node.setParents(parents);


                for (Variable variable : node.getPDist().getConditioningVariables()) {
                    if (variable.getName().contains("Beta") || variable.getName().contains("Gamma")) {
                        Variable referenceParent = this.ef_learningmodel.getParametersVariables().getVariableById(variable.getVarID() - 7 * index);
                        node.setVariableToNodeParent(variable, this.getNodeOfNonReplicatedVar(referenceParent));
                    }
                }

                if (node.getMainVariable().getName().startsWith("DEFAULTING"))
                    index++;

            }
        }

        for (Node node : nonReplictedNodes) {
            node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfNonReplicatedVar(var)).collect(Collectors.toList()));
            node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfNonReplicatedVar(var).getChildren().add(node));
        }

        List<Node> allNodes = new ArrayList();

        allNodes.addAll(this.nonReplictedNodes);

        for (int i = 0; i < nReplications; i++) {
            allNodes.addAll(this.replicatedNodes.get(i));
        }

        this.vmp.setNodes(allNodes);
    }



}