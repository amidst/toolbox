
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
package eu.amidst.jmlr2015.examples;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.learning.parametric.bayesian.ParallelSVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

/**
 * This class presents the experiment included in the submitted manuscript
 * to the JMLR Machine Learning Open Source Software.
 */
public class ExperimentsParallelSVB {

    /**
     * Represents the number of samples.
     */
    public static int nbrSamples = 200000;

    /**
     * Creates a {@link DAG} object with a naive Bayes structure from a given {@link DataStream}.
     * The main variable is defined as a latent binary variable which is set as a parent of all the observed variables.
     * @param dataStream a {@link DataStream} object.
     * @return a {@link DAG} object.
     */
    public static DAG getHiddenNaiveBayesStructure(DataStream<DataInstance> dataStream) {

        // Create a Variables object from the attributes of the input data stream.
        Variables modelHeader = new Variables(dataStream.getAttributes());

        // Define the global latent binary variable.
        Variable globalHiddenVar = modelHeader.newMultinomialVariable("GlobalHidden", 2);

        // Define the global Gaussian latent binary variable.
        Variable globalHiddenGaussian = modelHeader.newGaussianVariable("globalHiddenGaussian");

        // Define the class variable.
        Variable classVar = modelHeader.getVariableById(0);

        // Create a DAG object with the defined model header.
        DAG dag = new DAG(modelHeader);

        // Define the structure of the DAG, i.e., set the links between the variables.
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> w.getMainVar() != globalHiddenGaussian)
                .filter(w -> w.getMainVar().isMultinomial())
                .forEach(w -> w.addParent(globalHiddenVar));

        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> w.getMainVar() != globalHiddenGaussian)
                .filter(w -> w.getMainVar().isNormal())
                .forEach(w -> w.addParent(globalHiddenGaussian));

        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(classVar));

        // Return the DAG.
        return dag;
    }

    /**
     * Runs the parallel Streaming Variational Bayes (SVB) algorithm, see {@link ParallelSVB}.
     * @param args an {@code array} of input arguments.
     * @return a {@code double} value that represents the running time of parallel SVB in seconds.
     * @throws Exception
     */
    public static double parallelSVB(String[] args) throws Exception {

        int nCores = Integer.parseInt(args[0]);
        int nVars = Integer.parseInt(args[1]);
        int windowSize = Integer.parseInt(args[2]);

        // Randomly generate the data stream using {@link BayesianNetworkGenerator} and {@link BayesianNetworkSampler}.
        BayesianNetworkGenerator.setNumberOfGaussianVars(nVars);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(nVars, 2);
        BayesianNetwork bn  = BayesianNetworkGenerator.generateBayesianNetwork();
        DataStream<DataInstance> data = new BayesianNetworkSampler(bn).sampleToDataStream(nbrSamples);

        // Save the generated data stream into a file.
        DataStreamWriter.writeDataToFile(data,"./datasets/simulated/tmp.arff");

        long currentTime = System.nanoTime();

        // Load the data stream using {@link DataStreamLoader}.
        data = DataStreamLoader.open("./datasets/simulated/tmp.arff");

        // Create a {@link ParallelSVB} object.
        ParallelSVB parameterLearningAlgorithm = new ParallelSVB();

        // Set the number of cores to be used.
        parameterLearningAlgorithm.setNCores(nCores);

        // Set the {@link DAG} structure.
        parameterLearningAlgorithm.setDAG(ExperimentsParallelSVB.getHiddenNaiveBayesStructure(data));

        // Set the window size, that must be equal to the size of the data batches used for learning.
        parameterLearningAlgorithm.getSVBEngine().setWindowsSize(windowSize);

        // Activate the output.
        parameterLearningAlgorithm.setOutput(false);

        // Set the data to be used for leaning the model parameters.
        parameterLearningAlgorithm.setDataStream(data);

        // Run the learning algorithm.
        parameterLearningAlgorithm.runLearning();

        // Get the learned model.
        BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

        // Print the learned model.
        //System.out.println(bnModel.toString());

        currentTime = (System.nanoTime() - currentTime) / 1;

        // Return the running time in seconds.
        return currentTime / 1000000000.0;
    }

    public static void main(String[] args) throws Exception {

        args = new String[]{"1", "50", "100"};

        int[] nCores = {32,32,16,8,4,2,1};

        for (int K = 0; K <nCores.length; K++) {
            args[0] = nCores[K] + "";

            double seconds = ExperimentsParallelSVB.parallelSVB(args);

            System.out.println(nCores[K] + "\t" + seconds + "\t" + nbrSamples / seconds);
        }
    }
}