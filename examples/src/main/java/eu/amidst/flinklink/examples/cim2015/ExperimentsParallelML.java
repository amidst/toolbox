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

package eu.amidst.flinklink.examples.cim2015;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.OptionParser;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import  eu.amidst.flinklink.core.learning.parametric.*;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.io.IOException;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 04/11/15.
 */
public class ExperimentsParallelML {

    /*Options for core comparisons*/
    static int batchSize = 1000;

    /*Options for batch size comparisons*/
    static boolean parallel = true;
    static boolean sampleData = true;
    static int sampleSize = 1000000;
    static int numDiscVars = 5;
    static int numGaussVars = 5;
    static int numStatesHiddenDiscVars = 10;
    static int numHiddenGaussVars = 2;
    static int numStates = 10;

    public static boolean isParallel() {
        return parallel;
    }

    public static void setParallel(boolean parallel) {
        ExperimentsParallelML.parallel = parallel;
    }

    public static int getSampleSize() {
        return sampleSize;
    }

    public static void setSampleSize(int sampleSize) {
        ExperimentsParallelML.sampleSize = sampleSize;
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

    public static int getNumStatesHiddenDiscVars() {
        return numStatesHiddenDiscVars;
    }

    public static void setNumStatesHiddenDiscVars(int numStatesHiddenDiscVars) {
        ExperimentsParallelML.numStatesHiddenDiscVars = numStatesHiddenDiscVars;
    }

    public static int getNumHiddenGaussVars() {
        return numHiddenGaussVars;
    }

    public static void setNumHiddenGaussVars(int numHiddenGaussVars) {
        ExperimentsParallelML.numHiddenGaussVars = numHiddenGaussVars;
    }

    public static int getNumStates() {
        return numStates;
    }

    public static void setNumStates(int numStates) {
        ExperimentsParallelML.numStates = numStates;
    }

    public static boolean isSampleData() {
        return sampleData;
    }

    public static void setSampleData(boolean sampleData) {
        ExperimentsParallelML.sampleData = sampleData;
    }

    public static int getBatchSize() {
        return batchSize;
    }

    public static void setBatchSize(int batchSize) {
        ExperimentsParallelML.batchSize = batchSize;
    }

    static DAG dag;

    public static void compareNumberOfCores() throws IOException {

        System.out.println("Comparison with different number of cores");
        System.out.println("-----------------------------------------");
        createBayesianNetwork();
        if (isSampleData())
            sampleBayesianNetwork();

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "datasets/sampleBatchSize.arff", false);

        ParallelMaximumLikelihood parameterLearningAlgorithm = new ParallelMaximumLikelihood();
        parameterLearningAlgorithm.setDAG(dag);
        parameterLearningAlgorithm.initLearning();


        System.out.println("Available number of processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("AverageTime");
        //We discard the first five experiments and then record the following 10 repetitions
        double average = 0.0;
        for (int j = 0; j < 15; j++) {
            long start = System.nanoTime();
            parameterLearningAlgorithm.updateModel(dataFlink);
            long duration = (System.nanoTime() - start) / 1;
            double seconds = duration / 1000000000.0;
            System.out.println("Iteration [" + j + "] = " + seconds + " secs");
            if (j > 4) {
                average += seconds;
            }
            //dataStream.restart();
        }
        System.out.println(average / 10.0 + " secs");
    }


    private static void createBayesianNetwork() {
        /* ********** */
        /* Create DAG */
        /* Create all variables */
        Variables variables = new Variables();

        IntStream.range(0, getNumDiscVars() - 1)
                .forEach(i -> variables.newMultinomialVariable("DiscreteVar" + i, getNumStates()));

        IntStream.range(0, getNumGaussVars())
                .forEach(i -> variables.newGaussianVariable("GaussianVar" + i));

        Variable classVar = variables.newMultinomialVariable("ClassVar", getNumStates());

        if (getNumHiddenGaussVars() > 0)
            IntStream.rangeClosed(0, getNumHiddenGaussVars() - 1).forEach(i -> variables.newGaussianVariable("GaussianSPVar_" + i));
        //if(numStatesHiddenDiscVars > 0)
        Variable discreteHiddenVar = variables.newMultinomialVariable("DiscreteSPVar", getNumStatesHiddenDiscVars());

        dag = new DAG(variables);

        /* Link variables */
        dag.getParentSets().stream()
                .filter(parentSet -> !parentSet.getMainVar().equals(classVar) && !parentSet.getMainVar().getName().startsWith("GaussianSPVar_")
                        && !parentSet.getMainVar().getName().startsWith("DiscreteSPVar"))
                .forEach(w -> {
                    w.addParent(classVar);
                    w.addParent(discreteHiddenVar);
                    if (getNumHiddenGaussVars() > 0 && w.getMainVar().isNormal())
                        IntStream.rangeClosed(0, getNumHiddenGaussVars() - 1).forEach(i -> w.addParent(variables.getVariableByName("GaussianSPVar_" + i)));
                });

        /*Add classVar as parent of all super-parent variables*/
        if (getNumHiddenGaussVars() > 0)
            IntStream.rangeClosed(0, getNumHiddenGaussVars() - 1).parallel()
                    .forEach(hv -> dag.getParentSet(variables.getVariableByName("GaussianSPVar_" + hv)).addParent(classVar));
        dag.getParentSet(variables.getVariableByName("DiscreteSPVar")).addParent(classVar);


        System.out.println(dag.toString());
    }

    private static void sampleBayesianNetwork() throws IOException {


        BayesianNetwork bn = new BayesianNetwork(dag);

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(0);

        //The method sampleToDataStream returns a DataStream with ten DataInstance objects.
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(getSampleSize());

        //We finally save the sampled data set to an arff file.
        DataStreamWriter.writeDataToFile(dataStream, "datasets/sampleBatchSize.arff");
    }

    public static String classNameID() {
        return "eu.amidst.flinklink.examples.cim2015.ExperimentsParallelML";
    }

    public static String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    public static int getIntOption(String optionName) {
        return Integer.parseInt(getOption(optionName));
    }

    public static boolean getBooleanOption(String optionName) {
        return getOption(optionName).equalsIgnoreCase("true") || getOption(optionName).equalsIgnoreCase("T");
    }

    public static String listOptions() {

        return classNameID() + ",\\" +
                "-sampleSize, 1000000, Sample size of the dataset\\" +
                "-numStates, 10, Num states of all disc. variables (including the class)\\" +
                "-GV, 5, Num of gaussian variables\\" +
                "-DV, 5, Num of discrete variables\\" +
                "-SPGV, 2, Num of gaussian super-parent variables\\" +
                "-SPDV, 10, Num of states for super-parent discrete variable\\" +
                "-sampleData, true, Sample arff data (if not read datasets/sampleBatchSize.arff by default)\\" +
                "-parallelMode, true, Run in parallel\\" +
                "-windowsSize, 1000, Batch size for comparisons in the number of cores\\";
    }




    public static void loadOptions(){
        setNumGaussVars(getIntOption("-GV"));
        setNumDiscVars(getIntOption("-DV"));
        setNumHiddenGaussVars(getIntOption("-SPGV"));
        setNumStatesHiddenDiscVars(getIntOption("-SPDV"));
        setNumStates(getIntOption("-numStates"));
        setSampleSize(getIntOption("-sampleSize"));
        setSampleData(getBooleanOption("-sampleData"));
        setParallel(getBooleanOption("-parallelMode"));
        setBatchSize(getIntOption("-windowsSize"));
    }

    public static void main(String[] args) throws Exception {
        OptionParser.setArgsOptions(ExperimentsParallelML.class, args);
        ExperimentsParallelML.loadOptions();
        compareNumberOfCores();
    }
}
