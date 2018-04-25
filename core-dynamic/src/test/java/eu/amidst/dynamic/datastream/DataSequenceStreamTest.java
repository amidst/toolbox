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

package eu.amidst.dynamic.datastream;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.exponentialfamily.EF_DynamicBayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class DataSequenceStreamTest extends TestCase {


    @Test
    public void test1() {


        //Generate a dynamic Naive Bayes with only Multinomial variables
        DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();

        //Set the number of Discrete variables, their number of states, the number of Continuous variables
        dbnGenerator.setNumberOfContinuousVars(0);
        dbnGenerator.setNumberOfDiscreteVars(5);
        dbnGenerator.setNumberOfStates(2);

        //The number of states for the class variable is equal to 2
        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, false);

        //Sampling from the generated Dynamic NB
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
        sampler.setSeed(0);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        int nsquences = 20;
        int sizesequences = 200;
        assertTrue(sampler.sampleToDataBase(nsquences, sizesequences).stream().count() == nsquences * sizesequences);

        Iterator<DynamicDataInstance> it = sampler.sampleToDataBase(nsquences, sizesequences).iterator();
        for (int i = 0; i < nsquences; i++) {
            for (int j = 0; j < sizesequences; j++) {
                DynamicDataInstance data = it.next();
                assertTrue(data.getSequenceID() == i);
                assertTrue(data.getTimeID() == j);
            }
        }
        assertTrue(DataSequenceStream.streamOfDataSequences(sampler.sampleToDataBase(nsquences, sizesequences)).count() == nsquences);

        Iterator<DataSequence> sequences = DataSequenceStream.streamOfDataSequences(sampler.sampleToDataBase(nsquences, sizesequences)).iterator();
        for (int i = 0; i < nsquences; i++) {
            assertTrue(sequences.next().getSequenceID() == i);
        }
        DataSequenceStream.parallelStreamOfDataSequences(sampler.sampleToDataBase(nsquences, sizesequences)).forEach(batch -> assertTrue(batch.stream().count() == sizesequences));
    }


    @Test
    public void test2() {

        DynamicBayesianNetworkGenerator dbnGenerator = new DynamicBayesianNetworkGenerator();

        //Set the number of Discrete variables, their number of states, the number of Continuous variables
        dbnGenerator.setNumberOfContinuousVars(0);
        dbnGenerator.setNumberOfDiscreteVars(5);
        dbnGenerator.setNumberOfStates(2);

        //The number of states for the class variable is equal to 2
        DynamicBayesianNetwork dynamicNB = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(new Random(0), 2, false);

        //Sampling from the generated Dynamic NB
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dynamicNB);
        sampler.setSeed(0);

        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        int nsquences = 20;
        int sizesequences = 200;

        /*******************************************************************************/

        EF_DynamicBayesianNetwork efDynamicBayesianNetwork = new EF_DynamicBayesianNetwork(dynamicNB.getDynamicDAG());


        AtomicInteger dataInstanceCount = new AtomicInteger(0);

        SufficientStatistics sumSS = sampler.sampleToDataBase(nsquences, sizesequences).stream()
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(efDynamicBayesianNetwork::getSufficientStatistics)
                .reduce(SufficientStatistics::sumVectorNonStateless).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efDynamicBayesianNetwork.setMomentParameters(sumSS);
        DynamicBayesianNetwork bn1 = efDynamicBayesianNetwork.toDynamicBayesianNetwork(dynamicNB.getDynamicDAG());

        /*******************************************************************************/
        /*

        efDynamicBayesianNetwork = new EF_DynamicBayesianNetwork(dynamicNB.getDynamicDAG());

        sumSS = sampler.sampleToDataStream(nsquences, sizesequences).streamOfBatches(10)
                .map( batch -> {
                    EF_DynamicBayesianNetwork efDynamicBayesianNetworkLocal = new EF_DynamicBayesianNetwork(dynamicNB.getDynamicDAG());
                    return batch.stream().map(efDynamicBayesianNetworkLocal::getSufficientStatistics).reduce(SufficientStatistics::sumVectorNonStateless).get();
                })
                .reduce(SufficientStatistics::sumVectorNonStateless).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efDynamicBayesianNetwork.setMomentParameters(sumSS);
        DynamicBayesianNetwork bn2 = efDynamicBayesianNetwork.toDynamicBayesianNetwork(dynamicNB.getDynamicDAG());

        assertTrue(bn1.equalDBNs(bn2, 0.01));

        /*******************************************************************************/


        efDynamicBayesianNetwork = new EF_DynamicBayesianNetwork(dynamicNB.getDynamicDAG());

        sumSS = sampler.sampleToDataBase(nsquences, sizesequences).parallelStreamOfBatches(10)
                .map( batch -> {
                    EF_DynamicBayesianNetwork efDynamicBayesianNetworkLocal = new EF_DynamicBayesianNetwork(dynamicNB.getDynamicDAG());
                    return batch.stream().map(efDynamicBayesianNetworkLocal::getSufficientStatistics).reduce(SufficientStatistics::sumVectorNonStateless).get();
                })
                .reduce(SufficientStatistics::sumVectorNonStateless).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efDynamicBayesianNetwork.setMomentParameters(sumSS);
        DynamicBayesianNetwork bn3 = efDynamicBayesianNetwork.toDynamicBayesianNetwork(dynamicNB.getDynamicDAG());

        assertTrue(bn1.equalDBNs(bn3, 0.01));

        /*******************************************************************************/


        /*efDynamicBayesianNetwork = new EF_DynamicBayesianNetwork(dynamicNB.getDynamicDAG());

        sumSS = DataSequenceStream.streamOfDataSequences(sampler.sampleToDataStream(nsquences, sizesequences))
                .map( sequence -> {
                    EF_DynamicBayesianNetwork efDynamicBayesianNetworkLocal = new EF_DynamicBayesianNetwork(dynamicNB.getDynamicDAG());
                    return sequence.stream().map(efDynamicBayesianNetworkLocal::getSufficientStatistics).reduce(SufficientStatistics::sumVectorNonStateless).get();
                })
                .reduce(SufficientStatistics::sumVectorNonStateless).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efDynamicBayesianNetwork.setMomentParameters(sumSS);
        DynamicBayesianNetwork bn4 = efDynamicBayesianNetwork.toDynamicBayesianNetwork(dynamicNB.getDynamicDAG());

        assertTrue(bn1.equalDBNs(bn4, 0.01));*/
        /*******************************************************************************/


        /*efDynamicBayesianNetwork = new EF_DynamicBayesianNetwork(dynamicNB.getDynamicDAG());

        sumSS = DataSequenceStream.parallelStreamOfDataSequences(sampler.sampleToDataStream(nsquences, sizesequences))
                .map( sequence -> {
                    EF_DynamicBayesianNetwork efDynamicBayesianNetworkLocal = new EF_DynamicBayesianNetwork(dynamicNB.getDynamicDAG());
                    return sequence.stream().map(efDynamicBayesianNetworkLocal::getSufficientStatistics).reduce(SufficientStatistics::sumVectorNonStateless).get();
                })
                .reduce(SufficientStatistics::sumVectorNonStateless).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efDynamicBayesianNetwork.setMomentParameters(sumSS);
        DynamicBayesianNetwork bn5 = efDynamicBayesianNetwork.toDynamicBayesianNetwork(dynamicNB.getDynamicDAG());

        assertTrue(bn1.equalDBNs(bn5, 0.01));*/

        /*******************************************************************************/

    }

    @Test
    public void test3() {



        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile("../datasets/simulated/exampleDS_d5_c0.arff");


        //Sample from the dynamic NB given as inputs both nSequences (= 10000) and sequenceLength (= 100)

        int nsquences = 20;
        int sizesequences = 50;
        assertTrue(data.stream().count() == nsquences * sizesequences);

        Iterator<DynamicDataInstance> it = data.iterator();
        for (int i = 0; i < nsquences; i++) {
            for (int j = 0; j < sizesequences; j++) {
                DynamicDataInstance dataInstance = it.next();
                assertTrue(dataInstance.getSequenceID() == i);
                assertTrue(dataInstance.getTimeID() == j);
            }
        }
        assertTrue(DataSequenceStream.streamOfDataSequences(data).count() == nsquences);

        Iterator<DataSequence> sequences = DataSequenceStream.streamOfDataSequences(data).iterator();
        for (int i = 0; i < nsquences; i++) {
            assertTrue(sequences.next().getSequenceID() == i);
        }
        DataSequenceStream.parallelStreamOfDataSequences(data).forEach(batch -> assertTrue(batch.stream().count() == sizesequences));
    }

}