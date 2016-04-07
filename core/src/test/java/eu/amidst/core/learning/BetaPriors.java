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

import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import junit.framework.TestCase;

import java.util.Random;

/**
 * Created by andresmasegosa on 4/4/16.
 */
public class BetaPriors extends TestCase {

    public static void test() {

        Variables variables = new Variables();

        Variable varA = variables.newGaussianVariable("A");

        Variable varB = variables.newGaussianVariable("B");

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varB);

        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(0));

        System.out.println(bn);

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);

        SVB svb = new SVB();

        svb.setWindowsSize(100);
        svb.setDAG(dag);
        svb.setDataStream(sampler.sampleToDataStream(100));

        svb.runLearning();


        System.out.println(svb.getLearntBayesianNetwork());

    }
}
