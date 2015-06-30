
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
package eu.amidst.cim2015.examples;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.learning.parametric.bayesian.ParallelSVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

/**
 * Created by andresmasegosa on 30/06/15.
 */
public class ExperimentsParallelSVB {

    public static int SAMPLES = 200000;
    /**
     * This method creates a DAG object with a naive Bayes structure for the attributes of the passed data stream.
     * The main variable is defined as a latent binary variable which is a parent of all the observed variables.
     *
     * @param dataStream
     * @return
     */
    public static DAG getHiddenNaiveBayesStructure(DataStream<DataInstance> dataStream) {
        //We create a Variables object from the attributes of the data stream
        Variables modelHeader = new Variables(dataStream.getAttributes());

        //We define the global latent binary variable
        Variable globalHiddenVar = modelHeader.newMultionomialVariable("GlobalHidden", 2);

        //We define the global latent binary variable
        Variable globalHiddenGaussian = modelHeader.newGaussianVariable("globalHiddenGaussian");

        Variable classVar = modelHeader.getVariableById(0);

        //Then, we create a DAG object with the defined model header
        DAG dag = new DAG(modelHeader);

        //We set the linkds of the DAG.
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .filter(w -> w.getMainVar() != globalHiddenGaussian)
                .filter(w -> w.getMainVar().isMultinomial())
                .forEach(w -> w.addParent(globalHiddenVar));

        //We set the linkds of the DAG.
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

        return dag;
    }

    public static void parallelSVB(String[] args) throws Exception {

        int nCores = Integer.parseInt(args[0]);
        int nVars = Integer.parseInt(args[1]);
        int windowSize = Integer.parseInt(args[2]);

        //We can open the data stream using the static class DataStreamLoader
        BayesianNetworkGenerator.setNumberOfGaussianVars(nVars);
        BayesianNetworkGenerator.setNumberOfMultinomialVars(nVars, 2);
        BayesianNetwork bn  =BayesianNetworkGenerator.generateBayesianNetwork();
        DataStream<DataInstance> data = new BayesianNetworkSampler(bn).sampleToDataStream(SAMPLES);

        //We create a ParallelSVB object
        ParallelSVB parameterLearningAlgorithm = new ParallelSVB();

        //We fix the number of cores we want to exploit
        parameterLearningAlgorithm.setNCores(nCores);

        //We fix the DAG structure
        parameterLearningAlgorithm.setDAG(ExperimentsParallelSVB.getHiddenNaiveBayesStructure(data));

        //We fix the size of the window, which must be equal to the size of the data batches we use for learning
        parameterLearningAlgorithm.getSVBEngine().setWindowsSize(windowSize);


        //We can activate the output
        parameterLearningAlgorithm.setOutput(false);


        //We set the data which is going to be used for leaning the parameters
        parameterLearningAlgorithm.setDataStream(data);

        //We perform the learning
        parameterLearningAlgorithm.runLearning();

        //And we get the model
        BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

        //We print the model
        //System.out.println(bnModel.toString());

    }

    public static void main(String[] args) throws Exception {

        int[] nCores = {32,16,8,4,2,1};

        for (int K = 0; K <nCores.length; K++) {
            args[0] = nCores[K] + "";


            //for (int i = 0; i < 0; i++) {
                //System.out.println("Discard " + i);
            //    ExperimentsParallelSVB.parallelSVB(args);
            //}

            long currentTime = System.nanoTime();

            for (int i = 0; i < 1; i++) {
                //System.out.println("Test " + i);
                ExperimentsParallelSVB.parallelSVB(args);
            }

            currentTime = (System.nanoTime() - currentTime) / 1;

            double seconds = currentTime / 1000000000.0;
            System.out.println(nCores[K] + "\t" + seconds + "\t" + SAMPLES / seconds);
        }
    }
}