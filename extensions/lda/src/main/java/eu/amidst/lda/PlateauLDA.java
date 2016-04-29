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

package eu.amidst.lda;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.learning.parametric.bayesian.PlateuStructure;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 28/4/16.
 */
public class PlateauLDA extends PlateuStructure {

    Variable word;
    Variable topicIndicator;
    Variable dirichletMixingTopics;

    final Attributes attributes;
    final String wordDocumentName;

    int nTopics= 2;


    List<? extends DataInstance> data;

    public PlateauLDA(Attributes attributes, String wordDocumentName) {
        this.attributes = attributes;
        this.wordDocumentName = wordDocumentName;
        this.setDAG(null);
    }

    public void setNTopics(int nTopics) {
        this.nTopics = nTopics;
    }

    @Override
    public void setDAG(DAG dag) {
        if (dag!=null)
            throw new IllegalStateException("No possbile to define DAG");


        Variables variables = new Variables(attributes);
        word = variables.getVariableByName(wordDocumentName);

        topicIndicator = variables.newMultionomialVariable("TopicIndicator",nTopics);

        DAG dagLDA = new DAG(variables);

        dagLDA.getParentSet(word).addParent(topicIndicator);


        List<EF_ConditionalDistribution> dists = dagLDA.getParentSets().stream()
                .map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents()))
                .collect(Collectors.toList());

        ef_learningmodel = new EF_LearningBayesianNetwork(dists);
        this.ef_learningmodel.getListOfParametersVariables().stream().forEach(var -> this.replicatedVariables.put(var, false));
        this.ef_learningmodel.getListOfNonParameterVariables().stream().forEach(var -> this.replicatedVariables.put(var, true));

        //The Dirichlet parent of the topic is not replicated
        dirichletMixingTopics = ef_learningmodel.getDistribution(topicIndicator).getConditioningVariables().get(0);
        this.replicatedVariables.put(dirichletMixingTopics,true);


        this.nonReplicatedVariablesList = this.replicatedVariables.entrySet().stream().filter(entry -> !entry.getValue()).map(entry -> entry.getKey()).sorted((a,b) -> a.getVarID()-b.getVarID()).collect(Collectors.toList());

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
        this.getReplicatedNodes().forEach(node -> {node.resetQDist(this.vmp.getRandom());});
    }


    private void replicateModelForDocs(){
        replicatedNodes = new ArrayList<>();
        Attribute seqIDAtt = this.attributes.getSeq_id();
        double currentID=0;
        List<Node> tmpNodes = new ArrayList<>();

        Node nodeDirichletMixingTopics=null;
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
    public void replicateModel(){
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

}
