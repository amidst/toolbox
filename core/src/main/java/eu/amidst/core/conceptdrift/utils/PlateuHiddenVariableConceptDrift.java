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
package eu.amidst.core.conceptdrift.utils;



import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 10/03/15.
 */
public class PlateuHiddenVariableConceptDrift extends PlateuStructure {

    List<Variable> localHiddenVars;
    List<Node> localHiddenNodes;
    Map<Variable,Node> hiddenToNode;
    boolean dynamic;
    public PlateuHiddenVariableConceptDrift(List<Variable> localHiddenVars_, boolean dynamic_){
        this.localHiddenVars = localHiddenVars_;
        this.dynamic = dynamic_;
    }

    public void replicateModel(){
        parametersNode = new ArrayList();
        plateuNodes = new ArrayList<>(nReplications);

        variablesToNode = new ArrayList<>();
        parametersToNode = new ConcurrentHashMap<>();
        parametersNode = ef_learningmodel.getDistributionList().stream()
                .filter(dist -> dist.getVariable().isParameterVariable())
                .map(dist -> {
                    Node node = new Node(dist);
                    parametersToNode.put(dist.getVariable(), node);
                    return node;
                })
                .collect(Collectors.toList());

        localHiddenNodes = new ArrayList();
        hiddenToNode = new ConcurrentHashMap();
        for (Variable localVar : this.localHiddenVars) {
            Node node  = new Node(ef_learningmodel.getDistribution(localVar));
            hiddenToNode.put(localVar, node);
            localHiddenNodes.add(node);
        }

        for (int i = 0; i < nReplications; i++) {

            Map<Variable, Node> map = new ConcurrentHashMap();
            List<Node> tmpNodes = ef_learningmodel.getDistributionList().stream()
                    .filter(dist -> !dist.getVariable().isParameterVariable())
                    .filter(dist -> !hiddenToNode.containsKey(dist.getVariable()))
                    .map(dist -> {
                        Node node = new Node(dist);
                        map.put(dist.getVariable(), node);
                        return node;
                    })
                    .collect(Collectors.toList());

            this.variablesToNode.add(map);
            plateuNodes.add(tmpNodes);
        }


        for (int i = 0; i < nReplications; i++) {
            for (Node node : plateuNodes.get(i)) {
                final int slice = i;
                node.setParents(node.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVar(var, slice)).collect(Collectors.toList()));
                node.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVar(var, slice).getChildren().add(node));
            }
        }

        for (Variable localVar : this.localHiddenVars) {
            Node localNode = this.hiddenToNode.get(localVar);
            localNode.setParents(localNode.getPDist().getConditioningVariables().stream().map(var -> this.getNodeOfVar(var, 0)).collect(Collectors.toList()));
            localNode.getPDist().getConditioningVariables().stream().forEach(var -> this.getNodeOfVar(var, 0).getChildren().add(localNode));
            if (dynamic)
                localNode.getParents().stream().forEach(node -> node.setActive(false));
        }



        List<Node> allNodes = new ArrayList();

        allNodes.addAll(this.parametersNode);

        allNodes.addAll(localHiddenNodes);

        for (int i = 0; i < nReplications; i++) {
            allNodes.addAll(this.plateuNodes.get(i));
        }

        this.vmp.setNodes(allNodes);
    }

    public <E extends EF_UnivariateDistribution> E getEFParameterPosterior(Variable var) {

        return (E)this.parametersToNode.get(var).getQDist();
    }

    public Node getNodeOfVar(Variable variable, int slice) {
        if (variable.isParameterVariable())
            return this.parametersToNode.get(variable);
        else if (this.hiddenToNode.containsKey(variable))
            return this.hiddenToNode.get(variable);
        else
            return this.variablesToNode.get(slice).get(variable);
    }

}