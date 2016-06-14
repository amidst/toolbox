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

package eu.amidst.cim2015.examples;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.OptionParser;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by ana@cs.aau.dk on 01/07/15.
 */
public class ExperimentsParallelkMeans {


    static int numDiscVars = 5;
    static int numGaussVars = 5;
    static int numStates = 2;
    static int sampleSize = 100000;
    static boolean sampleData = true;
    static int batchSize = 100;
    static int k = 3;
    static String pathToFile = "datasets/simulated/tmp.arff";

    public static int getK() {
        return k;
    }

    public static void setK(int k) {
        ExperimentsParallelkMeans.k = k;
    }

    public static int getNumStates() {
        return numStates;
    }

    public static void setNumStates(int numStates) {
        ExperimentsParallelML.numStates = numStates;
    }

    public static int getNumDiscVars() {
        return numDiscVars;
    }

    public static void setNumDiscVars(int numDiscVars) {
        ExperimentsParallelML.numDiscVars = numDiscVars;
    }

    public static int getNumGaussVars() {
        return numGaussVars;
    }

    public static void setNumGaussVars(int numGaussVars) {
        ExperimentsParallelML.numGaussVars = numGaussVars;
    }

    public static int getSampleSize() {
        return sampleSize;
    }

    public static void setSampleSize(int sampleSize) {
        ExperimentsParallelML.sampleSize = sampleSize;
    }

    public static boolean isSampleData() {
        return sampleData;
    }

    public static void setSampleData(boolean sampleData) {
        ExperimentsParallelML.sampleData = sampleData;
    }

    public static String getPathToFile() {
        return pathToFile;
    }

    public static void setPathToFile(String pathToFile) {
        ExperimentsParallelkMeans.pathToFile = pathToFile;
    }

    public static void runParallelKMeans() throws IOException {

        DataStream<DataInstance> data;
        if(isSampleData()) {
            BayesianNetworkGenerator.setNumberOfGaussianVars(getNumGaussVars());
            BayesianNetworkGenerator.setNumberOfMultinomialVars(getNumDiscVars(), getNumStates());
            BayesianNetwork bn = BayesianNetworkGenerator.generateBayesianNetwork();
            data = new BayesianNetworkSampler(bn).sampleToDataStream(getSampleSize());
            DataStreamWriter.writeDataToFile(data, pathToFile);
        }

        data = DataStreamLoader.open(pathToFile);

        ParallelKMeans.setBatchSize(batchSize);
        double[][] centroids = ParallelKMeans.learnKMeans(getK(),data);
        for (int clusterID = 0; clusterID < centroids.length; clusterID++) {
            System.out.println("Cluster "+(clusterID+1)+": "+Arrays.toString(centroids[clusterID]));
        }

    }



    public static String classNameID(){
        return "eu.amidst.cim2015.examples.batchSizeComparisonsML";
    }

    public static String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    public static int getIntOption(String optionName){
        return Integer.parseInt(getOption(optionName));
    }

    public static boolean getBooleanOption(String optionName){
        return getOption(optionName).equalsIgnoreCase("true") || getOption(optionName).equalsIgnoreCase("T");
    }

    public static String listOptions(){

        return  classNameID() +",\\"+
                "-sampleSize, 1000000, Sample size of the dataset\\" +
                "-numStates, 10, Num states of all disc. variables (including the class)\\"+
                "-GV, 5000, Num of gaussian variables\\"+
                "-DV, 5000, Num of discrete variables\\"+
                "-k, 2, Num of clusters\\"+
                "-sampleData, true, Sample arff data (if not path to file must be specified)\\"+
                "-pathToFile, datasets/simulated/tmp.arff,Path to sample file if sampleData is set to false\\";
    }

    public static void loadOptions() {
        setNumGaussVars(getIntOption("-GV"));
        setNumDiscVars(getIntOption("-DV"));
        setNumStates(getIntOption("-numStates"));
        setSampleSize(getIntOption("-sampleSize"));
        setK(getIntOption("-k"));
        setSampleData(getBooleanOption("-sampleData"));
        setPathToFile(getOption("-pathToFile"));
    }




    public static void main(String[] args) throws Exception {
        runParallelKMeans();
    }
}
