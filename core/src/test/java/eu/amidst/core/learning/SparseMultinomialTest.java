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

package eu.amidst.core.learning;

import eu.amidst.core.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.exponentialfamily.EF_LearningBayesianNetwork;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 2/5/16.
 */
public class SparseMultinomialTest extends TestCase {
    public static void testMultinomials1() throws IOException, ClassNotFoundException {
        Variables variables = new Variables();
        Variable varA = variables.newMultinomialVariable("A", 2);

        DAG dag = new DAG(variables);


        BayesianNetwork bn = new BayesianNetwork(dag);

        bn.randomInitialization(new Random(0));

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(2);
        DataStream<DataInstance> data = sampler.sampleToDataStream(1000);

        SVB svb = new SVB();
        svb.setWindowsSize(1000);
        svb.setSeed(5);
        VMP vmp = svb.getPlateuStructure().getVMP();
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);


        svb.setDAG(bn.getDAG());
        svb.setDataStream(data);
        svb.runLearning();

        BayesianNetwork learnBN = svb.getLearntBayesianNetwork();

        if (Main.VERBOSE) System.out.println(bn.toString());
        if (Main.VERBOSE) System.out.println(learnBN.toString());
        assertTrue(bn.equalBNs(learnBN, 0.05));
    }

    public static void test2(){
        Variables variables = new Variables();
        Variable word = variables.newSparseMultionomialVariable("Word", 100);
        //word = variables.newMultionomialVariable(attributes.getAttributeByName(wordDocumentName));

        Variable topicIndicator = variables.newMultinomialVariable("TopicIndicator",2);

        DAG dagLDA = new DAG(variables);

        dagLDA.getParentSet(word).addParent(topicIndicator);


        List<EF_ConditionalDistribution> dists = dagLDA.getParentSets().stream()
                .map(pSet -> pSet.getMainVar().getDistributionType().<EF_ConditionalDistribution>newEFConditionalDistribution(pSet.getParents()))
                .collect(Collectors.toList());

        EF_LearningBayesianNetwork ef_learningmodel = new EF_LearningBayesianNetwork(dists);


    }

}
