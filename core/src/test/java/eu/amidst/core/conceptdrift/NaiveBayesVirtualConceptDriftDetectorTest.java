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


import eu.amidst.core.Main;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.variables.Variable;
import org.junit.Test;

import java.io.IOException;
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
        DataStream<DataInstance> data = DataStreamLoader.open("../datasets/DriftSets/sea.arff");
        NaiveBayesVirtualConceptDriftDetector virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();
        virtualDriftDetector.setClassIndex(-1);
        virtualDriftDetector.setData(data);
        virtualDriftDetector.setWindowsSize(windowSize);
        virtualDriftDetector.setTransitionVariance(0.1);
        virtualDriftDetector.setNumberOfGlobalVars(1);
        virtualDriftDetector.initLearning();

        if (Main.VERBOSE) System.out.print("Batch");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            if (Main.VERBOSE) System.out.print("\t" + hiddenVar.getName());
        }

        if (Main.VERBOSE) System.out.println();
        int countBatch = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)){
            double[] out = virtualDriftDetector.updateModel(batch);
            if (Main.VERBOSE) System.out.print(countBatch + "\t");
            for (int i = 0; i < out.length; i++) {
                if (Main.VERBOSE) System.out.print(out[i]+"\t");
            }
            if (Main.VERBOSE) System.out.println();
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
        DataStream<DataInstance> data = DataStreamLoader.open("../datasets/bnaic2015/BCC/Month0.arff");
        NaiveBayesVirtualConceptDriftDetector virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();
        virtualDriftDetector.setClassIndex(-1);
        virtualDriftDetector.setData(data);
        virtualDriftDetector.setWindowsSize(windowSize);
        virtualDriftDetector.setTransitionVariance(0.1);
        virtualDriftDetector.setNumberOfGlobalVars(1);
        virtualDriftDetector.initLearning();

        if (Main.VERBOSE) System.out.print("Batch");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            if (Main.VERBOSE) System.out.print("\t" + hiddenVar.getName());
        }

        if (Main.VERBOSE) System.out.println();
        int countBatch = 0;
        for (int k = 1; k < 60; k++) {
            data = DataStreamLoader.open("../datasets/bnaic2015/BCC/Month" + k + ".arff");
            DataOnMemory<DataInstance> batch = new DataOnMemoryListContainer<DataInstance>(data.getAttributes(),data.stream().collect(Collectors.toList()));
            double[] out = virtualDriftDetector.updateModel(batch);
            if (Main.VERBOSE) System.out.print(countBatch + "\t");
            for (int i = 0; i < out.length; i++) {
                if (Main.VERBOSE) System.out.print(out[i]+"\t");
            }
            if (Main.VERBOSE) System.out.println();
        }
    }
}
