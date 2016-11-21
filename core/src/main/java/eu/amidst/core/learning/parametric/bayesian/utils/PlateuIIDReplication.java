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
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class extends the abstract class {@link PlateuStructure} and defines Plateu IID Replication.
 */
public class PlateuIIDReplication extends PlateuStructure{


    /**
     * Empty builder.
     */
    public PlateuIIDReplication() {
        super();
    }

    /**
     * Builder which initially specify a list of non-replicated variables.
     *
     * @param initialNonReplicatedVariablesList list of variables
     */
    public PlateuIIDReplication(List<Variable> initialNonReplicatedVariablesList) {
        super(initialNonReplicatedVariablesList);
    }


    /**
     * Sets the evidence for this PlateuStructure.
     *
     * @param data a {@code List} of {@link DataInstance}.
     */
    @Override
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



        //Non-replicated nodes can have evidende, which is taken from the first data sample in the list
        for (Node nonReplictedNode : this.nonReplictedNodes) {
            nonReplictedNode.setAssignment(data.get(0));
        }


    }


    /**
     * Replicates this model.
     */
    @Override
    public void replicateModel(){

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


        for (int i = 0; i < nReplications; i++) {
            allNodes.addAll(this.replicatedNodes.get(i));
        }

        allNodes.addAll(this.nonReplictedNodes);

        this.vmp.setNodes(allNodes);
    }

}