/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.dynamic.examples.learning;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.inference.messagepassing.VMP;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.learning.parametric.bayesian.SVB;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;

import java.io.IOException;
import java.util.Random;

/**
 * Created by ana@cs.aau.dk on 01/12/15.
 */
public class SVBforDBN {
    public static void main(String[] args) throws IOException {
        Random random = new Random(1);

        //We first generate a dynamic Bayesian network (NB structure with class and attributes temporally linked)
        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(3);
        DynamicBayesianNetwork dbnRandom = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,true);

        //Sample dynamic data from the created dbn with random parameters
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbnRandom);
        sampler.setSeed(0);
        //Sample 3 sequences of 100K instances
        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(3,10000);

        /*Parameter Learning with Streaming variational Bayes VMP*/
        SVB svb = new SVB();
        //We set the desired options for the svb
        svb.setWindowsSize(100);
        svb.setSeed(0);
        //If desired, we also set some options for the VMP
        VMP vmp = svb.getPlateauStructure().getVMPTimeT();
        vmp.setOutput(false);
        vmp.setTestELBO(true);
        vmp.setMaxIter(1000);
        vmp.setThreshold(0.0001);

        //We set the dynamicDAG, the data and start learning
        svb.setDynamicDAG(dbnRandom.getDynamicDAG());
        svb.setDataStream(data);
        svb.runLearning();

        //We get the learnt DBN
        DynamicBayesianNetwork dbnLearnt = svb.getLearntDBN();

        //We print the model
        System.out.println(dbnLearnt.toString());
    }

}
