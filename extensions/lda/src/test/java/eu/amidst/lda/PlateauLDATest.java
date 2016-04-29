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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_Dirichlet;
import eu.amidst.core.inference.messagepassing.Node;
import eu.amidst.core.io.DataStreamLoader;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 29/4/16.
 */
public class PlateauLDATest extends TestCase {

    public static void test1() {

        DataStream<DataInstance> dataInstances = DataStreamLoader.openFromFile("./datasets/simulated/simulatedText.arff");

        List<DataOnMemory<DataInstance>> listA =
                BatchSpliteratorByID.toFixedBatchStream(dataInstances, 2).collect(Collectors.toList());


        PlateauLDA plateauLDA = new PlateauLDA(dataInstances.getAttributes(),"word");

        plateauLDA.setNTopics(2);

        plateauLDA.setEvidence(listA.get(0).getList());


        int nwordsDoc1 = 3;
        int nwordsDoc2 = 3;

        assertEquals(2,plateauLDA.getNonReplictedNodes().count());
        assertEquals(2*nwordsDoc1+1 + 2*nwordsDoc2 +1,plateauLDA.getReplicatedNodes().count());

        plateauLDA.getNonReplictedNodes().forEach(node -> assertEquals(nwordsDoc1 + nwordsDoc2, node.getChildren().size()));
        plateauLDA.getNonReplictedNodes().forEach(node -> assertEquals(0, node.getParents().size()));

        assertEquals(nwordsDoc1 + nwordsDoc2, plateauLDA.getReplicatedNodes().filter(node -> node.getName().compareTo(plateauLDA.topicIndicator.getName())==0).count());
        assertEquals(2, plateauLDA.getReplicatedNodes().filter(node -> node.getName().compareTo(plateauLDA.dirichletMixingTopics.getName())==0).count());

        plateauLDA.getReplicatedNodes()
                .filter(node -> node.getName().compareTo(plateauLDA.topicIndicator.getName())==0)
                .forEach(node -> assertEquals(1, node.getChildren().size()));

        plateauLDA.getReplicatedNodes()
                .filter(node -> node.getName().compareTo(plateauLDA.topicIndicator.getName())==0)
                .forEach(node -> assertEquals(1, node.getParents().size()));

        plateauLDA.getReplicatedNodes()
                .filter(node -> node.getName().compareTo(plateauLDA.dirichletMixingTopics.getName())==0)
                .forEach(node -> assertEquals(3, node.getChildren().size()));

        plateauLDA.getReplicatedNodes()
                .filter(node -> node.getName().compareTo(plateauLDA.dirichletMixingTopics.getName())==0)
                .forEach(node -> assertEquals(0, node.getParents().size()));


        List<Node> listNodeWords = plateauLDA.getReplicatedNodes()
                .filter(node -> node.getName().compareTo(plateauLDA.word.getName())==0)
                .collect(Collectors.toList());

        assertEquals(6,listNodeWords.size());

        for (int i = 0; i < listA.get(0).getList().size(); i++) {
            assertEquals(1.0,listNodeWords.get(i).getSufficientStatistics().get((int)listA.get(0).getDataInstance(i).getValue(plateauLDA.word)));
        }


    }

    public static void test2() {
        DataStream<DataInstance> dataInstances = DataStreamLoader.openFromFile("./datasets/simulated/simulatedText.arff");

        List<DataOnMemory<DataInstance>> listA =
                BatchSpliteratorByID.toFixedBatchStream(dataInstances, 2).collect(Collectors.toList());


        PlateauLDA plateauLDA = new PlateauLDA(dataInstances.getAttributes(),"word");

        plateauLDA.setNTopics(2);

        plateauLDA.setEvidence(listA.get(0).getList());

        plateauLDA.getVMP().setTestELBO(true);
        plateauLDA.getVMP().setMaxIter(1000);
        plateauLDA.getVMP().setOutput(true);
        plateauLDA.getVMP().setThreshold(0.0001);
        plateauLDA.getVMP().resetQs();


        plateauLDA.runInference();

        plateauLDA.getNonReplictedNodes().forEach( node -> {
            EF_Dirichlet dist = (EF_Dirichlet)node.getQDist();

            System.out.println(dist.getExpectedParameters().output());
        });

    }

}