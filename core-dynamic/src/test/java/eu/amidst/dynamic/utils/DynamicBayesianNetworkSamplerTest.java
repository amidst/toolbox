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

package eu.amidst.dynamic.utils;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import junit.framework.TestCase;

import java.util.Random;

/**
 * Created by andresmasegosa on 25/11/15.
 */
public class DynamicBayesianNetworkSamplerTest extends TestCase {


    public static void test(){

        Random random = new Random(0);

        //We first generate a dynamic Bayesian network (NB structure, only class is temporally linked)
        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(0);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(1);
        DynamicBayesianNetworkGenerator.setNumberOfStates(2);
        DynamicBayesianNetwork extendedDBN = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,true);
        //We select the target variable for inference, in this case the class variable
        Variable classVar = extendedDBN.getDynamicVariables().getVariableByName("ClassVar");

        //We create a dynamic dataset with 3 sequences for prediction
        DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(extendedDBN);
        dynamicSampler.setMARVar(classVar,0.5);
        DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(3, 100);

        assertEquals(300,dataPredict.stream().count());

        assertEquals(6,dataPredict.streamOfBatches(50).count());
    }

}