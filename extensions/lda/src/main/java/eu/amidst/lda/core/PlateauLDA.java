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
import eu.amidst.core.inference.messagepassing.Node;

import java.util.*;

/**
 * Created by andresmasegosa on 28/4/16.
 */
public class PlateauLDA extends PlateauLDAReduced {

    public PlateauLDA(Attributes attributes, String wordDocumentName, String wordCountName) {
        super(attributes,wordDocumentName,wordCountName);
    }

    @Override
    protected void replicateModelForDocs() {
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

            for (int j = 0; j < data.get(i).getValue(wordCountAtt); j++) {
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

}