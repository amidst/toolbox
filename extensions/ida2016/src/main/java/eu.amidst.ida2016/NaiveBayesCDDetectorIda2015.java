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


package eu.amidst.ida2016;

import eu.amidst.core.datastream.*;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.variables.Variable;

/**
 * This example shows how to use the class eu.amidst.ida2016.NaiveBayesVirtualConceptDriftDetector to run the virtual concept drift
 * detector detailed in
 *
 * <i>Borchani et al. Modeling concept drift: A probabilistic graphical model based approach. IDA 2015.</i>
 *
 */
public class NaiveBayesCDDetectorIda2015 {
    public static void main(String[] args) {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("/Users/ana/Documents/Amidst-MyFiles/CajaMar/" +
                "datosWeka.arff");

        //DataStream<DataInstance> data = DataStreamLoader.openFromFile("./datasets/DynamicDataContinuous.arff");

        //DataStream<DataInstance> data = DataStreamLoader.openFromFile("/Users/ana/Documents/Amidst-MyFiles/CajaMar/" +
        //        "dataWekaUnemploymentRate.arff");


        //We create a eu.amidst.ida2016.NaiveBayesVirtualConceptDriftDetector object
        NaiveBayesVirtualConceptDriftDetector virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();

        //We set class variable as the last attribute
        virtualDriftDetector.setClassIndex(-1);

        virtualDriftDetector.setSeed(1);

        //We set the data which is going to be used
        virtualDriftDetector.setData(data);

        //We fix the size of the window
        int windowSize = 100;
        virtualDriftDetector.setWindowsSize(windowSize);

        //We fix the so-called transition variance
        virtualDriftDetector.setTransitionVariance(0.1);

        //We fix the number of global latent variables
        virtualDriftDetector.setNumberOfGlobalVars(1);

        //We should invoke this method before processing any data
        virtualDriftDetector.initLearning();

        //virtualDriftDetector.deactivateTransitionMethod();

        //Some prints
        System.out.print("Month \t");
        System.out.print("Batch \t");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }

        int countBatch = 0;

        Attributes attributes = data.getAttributes();
        Attribute timeID = attributes.getTime_id();

        Variable unemploymentRateVar = null;
        String unemploymentRateAttName = "UNEMPLOYMENT_RATE_ALMERIA";
        try {
            unemploymentRateVar = virtualDriftDetector.getSvb().getDAG().getVariables().getVariableByName(unemploymentRateAttName);
            System.out.println("\t UnempRate");
        }catch (UnsupportedOperationException e){}

        System.out.println();


        DataOnMemoryListContainer<DataInstance> batch = new DataOnMemoryListContainer(attributes);
        double currentTimeID = 0;
        double[] meanHiddenVars = new double[virtualDriftDetector.getHiddenVars().size()];;
        for (DataInstance dataInstance : data) {

            if (dataInstance.getValue(timeID) != currentTimeID) {

                virtualDriftDetector.setTransitionVariance(0);

                double[] out = null;
                for (DataOnMemory<DataInstance> minibatch: batch.iterableOverBatches(windowSize)) {
                    out= virtualDriftDetector.updateModel(minibatch);

                    //System.out.println(virtualDriftDetector.getLearntBayesianNetwork());
                    for (int i = 0; i < meanHiddenVars.length; i++) {
                        meanHiddenVars[i] += out[i];
                    }
                }

                virtualDriftDetector.setTransitionVariance(0.1);
                virtualDriftDetector.getSvb().applyTransition();

                //We print the output
                System.out.print(currentTimeID + "\t");
                System.out.print(countBatch + "\t");
                for (int i = 0; i < meanHiddenVars.length; i++) {
                    System.out.print(meanHiddenVars[i]+"\t");
                    meanHiddenVars[i]=0;
                }
                if(unemploymentRateVar!=null) {
                    System.out.print(virtualDriftDetector.getSvb().getPlateuStructure().getNodeOfNonReplicatedVar(unemploymentRateVar).getAssignment().getValue(unemploymentRateVar)+"\t");
                }

                System.out.println();
                currentTimeID = dataInstance.getValue(timeID);
                batch = new DataOnMemoryListContainer(attributes);
            }
            batch.add(dataInstance);
            countBatch++;
        }


       // }
    }
}
