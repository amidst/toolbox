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

package eu.amidst.core.examples.conceptdrift;


import eu.amidst.core.conceptdrift.NaiveBayesVirtualConceptDriftDetector;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.variables.Variable;

/**
 * This example shows how to use the class NaiveBayesVirtualConceptDriftDetector to run the virtual concept drift
 * detector detailed in
 *
 * <i>Borchani et al. Modeling concept drift: A probabilistic graphical model based approach. IDA 2015.</i>
 *
 */
public class NaiveBayesVirtualConceptDriftDetectorExample {
    public static void main(String[] args) {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.open("./datasets/DriftSets/sea.arff");

        //We create a NaiveBayesVirtualConceptDriftDetector object
        NaiveBayesVirtualConceptDriftDetector virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();

        //We set class variable as the last attribute
        virtualDriftDetector.setClassIndex(-1);

        //We set the data which is going to be used
        virtualDriftDetector.setData(data);

        //We fix the size of the window
        int windowSize = 1000;
        virtualDriftDetector.setWindowsSize(windowSize);

        //We fix the so-called transition variance
        virtualDriftDetector.setTransitionVariance(0.1);

        //We fix the number of global latent variables
        virtualDriftDetector.setNumberOfGlobalVars(1);

        //We should invoke this method before processing any data
        virtualDriftDetector.initLearning();

        //Some prints
        System.out.print("Batch");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }
        System.out.println();


        //Then we show how we can perform the sequential processing of
        // data batches. They must be of the same value than the window
        // size parameter set above.
        int countBatch = 0;
        for (DataOnMemory<DataInstance> batch : data.iterableOverBatches(windowSize)){

            //We update the model by invoking this method. The output
            // is an array with a value associated
            // to each fo the global hidden variables
            double[] out = virtualDriftDetector.updateModel(batch);

            //We print the output
            System.out.print(countBatch + "\t");
            for (int i = 0; i < out.length; i++) {
                System.out.print(out[i]+"\t");
            }
            System.out.println();
            countBatch++;
        }
    }
}
