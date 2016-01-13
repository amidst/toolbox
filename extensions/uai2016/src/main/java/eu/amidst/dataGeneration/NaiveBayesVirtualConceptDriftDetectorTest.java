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

package eu.amidst.dataGeneration;


import eu.amidst.core.conceptdrift.NaiveBayesVirtualConceptDriftDetector;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.DataOnMemoryFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataFolderReader;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.variables.Variable;

/**
 * This example shows how to use the class NaiveBayesVirtualConceptDriftDetector to run the virtual concept drift
 * detector detailed in
 *
 * <i>Borchani et al. Modeling concept drift: A probabilistic graphical model based approach. IDA 2015.</i>
 *
 * for IDA like Data (class copied-pasted from eu.amidst.core.conceptdrift.NaiveBayesVirtualConceptDriftDetectorTest.java)
 */
public class NaiveBayesVirtualConceptDriftDetectorTest {

    public static void generateIDAData() throws Exception{
        GenerateCajaMarData generateData = new GenerateCajaMarData();
        generateData.setSeed(0);
        generateData.setIncludeSocioEconomicVars(false);
        generateData.setBatchSize(1000);
        generateData.setRscriptsPath("./extensions/uai2016/doc-experiments/dataGenerationForFlink");
        generateData.setNumFiles(3);
        generateData.setNumSamplesPerFile(100);
        generateData.setOutputFullPath("~/core/extensions/uai2016/doc-experiments/dataGenerationForFlink/IDAlikeData/" +
                "withoutIndex");
        generateData.setPrintINDEX(false);
        generateData.setAddConceptDrift(true);
        generateData.generateData();
    }

    public static void main(String[] args) throws Exception{

        //generateIDAData();

        String dataPath = args[0];

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.openFromFile(
                dataPath+"/MONTH1.arff");

        //We create a NaiveBayesVirtualConceptDriftDetector object
        NaiveBayesVirtualConceptDriftDetector virtualDriftDetector = new NaiveBayesVirtualConceptDriftDetector();

        //We set class variable as the last attribute
        virtualDriftDetector.setClassIndex(-1);

        //We set the data which is going to be used
        virtualDriftDetector.setData(data);

        //We fix the size of the window
        int windowSize = 3000;
        virtualDriftDetector.setWindowsSize(windowSize);

        //We fix the so-called transition variance
        virtualDriftDetector.setTransitionVariance(0.1);

        //We fix the number of global latent variables
        virtualDriftDetector.setNumberOfGlobalVars(1);

        //We should invoke this method before processing any data
        virtualDriftDetector.initLearning();

        //Some prints
        System.out.print("Month");
        for (Variable hiddenVar : virtualDriftDetector.getHiddenVars()) {
            System.out.print("\t" + hiddenVar.getName());
        }
        System.out.println();


        //Then we show how we can perform the sequential processing of
        // data batches. They must be of the same value than the window
        // size parameter set above.
        int countBatch = 1;
        for (int monthID = 1; monthID < 85; monthID++){

            DataOnMemory<DataInstance> dataMONTH = loadDataOnMemoryFromArffFolder(
                    dataPath+"/MONTH"+monthID+".arff");
            //We update the model by invoking this method. The output
            // is an array with a value associated
            // to each fo the global hidden variables
            double[] out = virtualDriftDetector.updateModel(dataMONTH);

            //We print the output
            System.out.print("MONTH"+countBatch + "\t");
            for (int i = 0; i < out.length; i++) {
                System.out.print(out[i]+"\t");
            }
            System.out.println();
            countBatch++;
        }
    }

    private static DataOnMemory<DataInstance> loadDataOnMemoryFromArffFolder(String path){
        ARFFDataFolderReader reader = new ARFFDataFolderReader();
        reader.loadFromFile(path);
        return new DataOnMemoryFromFile(reader);
    }
}
