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
package eu.amidst.core.conceptdrift;



import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 1/7/15.
 */
public class NaiveBayesVirtualConceptDriftDetectorTest {

    /**
     * There should be a change in the hidden output for sea level every 15000 samples.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testSea() throws IOException, ClassNotFoundException {
        int windowSize = 1000;
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("../datasets/DriftSets/sea.arff");
        NaiveBayesVirtualConceptDriftDetector virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();
        virtualDriftDetector.setSeed(1);
        virtualDriftDetector.setClassIndex(-1);
        virtualDriftDetector.setData(data);
        virtualDriftDetector.setWindowsSize(windowSize);
        virtualDriftDetector.setTransitionVariance(0.1);
        virtualDriftDetector.setNumberOfGlobalVars(1);
        virtualDriftDetector.initLearning();

        System.out.print("Batch");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }

        System.out.println();
        int countBatch = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)){
            double[] out = virtualDriftDetector.updateModel(batch);
            System.out.print(countBatch + "\t");
            for (int i = 0; i < out.length; i++) {
                System.out.print(out[i]+"\t");
            }
            System.out.println();
            countBatch++;
        }
    }


    /**
     * There should be a change in the hidden output for sea level every 15000 samples.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testBCC() throws IOException, ClassNotFoundException {
        int windowSize = 3000;
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("../datasets/bnaic2015/BCC/Month0.arff");
        NaiveBayesVirtualConceptDriftDetector virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();
        virtualDriftDetector.setClassIndex(-1);
        virtualDriftDetector.setData(data);
        virtualDriftDetector.setWindowsSize(windowSize);
        virtualDriftDetector.setTransitionVariance(0.1);
        virtualDriftDetector.setNumberOfGlobalVars(1);
        virtualDriftDetector.initLearning();

        System.out.print("Batch");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }

        System.out.println();
        int countBatch = 0;
        for (int k = 1; k < 60; k++) {
            data = DataStreamLoader.openFromFile("../datasets/bnaic2015/BCC/Month" + k + ".arff");
            DataOnMemory<DataInstance> batch = new DataOnMemoryListContainer<DataInstance>(data.getAttributes(),data.stream().collect(Collectors.toList()));
            double[] out = virtualDriftDetector.updateModel(batch);
            System.out.print(countBatch + "\t");
            for (int i = 0; i < out.length; i++) {
                System.out.print(out[i]+"\t");
            }
            System.out.println();
        }
    }

    @Test
    public void testHardConceptDrift() throws IOException, ClassNotFoundException {

        Variables variables = new Variables();
        Variable varA = variables.newGaussianVariable("A");
        Variable varB = variables.newGaussianVariable("B");
        Variable varC = variables.newGaussianVariable("C");

        DAG dag = new DAG(variables);
        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);


        BayesianNetwork bn = new BayesianNetwork(dag);
        bn.randomInitialization(new Random(2));

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);


        int windowSize = 100;
        NaiveBayesVirtualConceptDriftDetector virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();
        virtualDriftDetector.setSeed(10);
        virtualDriftDetector.setClassIndex(-1);
        virtualDriftDetector.setData(sampler.sampleToDataOnMemory(windowSize));
        virtualDriftDetector.setWindowsSize(windowSize);
        virtualDriftDetector.setTransitionVariance(0.1);
        virtualDriftDetector.setNumberOfGlobalVars(1);
        virtualDriftDetector.initLearning();



        System.out.print("Batch");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }

        System.out.println();
        int countBatch = 0;


        for (int k = 0; k < 10; k++) {
            if (k % 3 == 0) {
                bn.randomInitialization(new Random(k));
                //System.out.println(bn);
                sampler = new BayesianNetworkSampler(bn);
            }

            double[] out = virtualDriftDetector.updateModel(sampler.sampleToDataOnMemory(windowSize));
            System.out.print(countBatch + "\t");
            for (int i = 0; i < out.length; i++) {
                System.out.print(out[i] + "\t");
            }
            System.out.println();
            //System.out.println(virtualDriftDetector.getLearntBayesianNetwork());

        }

    }
}
