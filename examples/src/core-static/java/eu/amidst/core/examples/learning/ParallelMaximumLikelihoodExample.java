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
package eu.amidst.core.examples.learning;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.MaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

/**
 *
 * This example shows how to learn in parallel the parameters of a Bayesian network from a stream of data using maximum
 * likelihood.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class ParallelMaximumLikelihoodExample {

    /**
     * This method returns a DAG object with naive Bayes structure for the attributes of the passed data stream.
     * @param dataStream
     * @param classIndex
     * @return
     */
    private static DAG getNaiveBayesStructure(DataStream<DataInstance> dataStream, int classIndex){

        //We create a Variables object from the attributes of the data stream
        Variables modelHeader = new Variables(dataStream.getAttributes());

        //We define the predicitive class variable
        Variable classVar = modelHeader.getVariableById(classIndex);

        //Then, we create a DAG object with the defined model header
        DAG dag = new DAG(modelHeader);

        //We set the linkds of the DAG.
        dag.getParentSets().stream().filter(w -> w.getMainVar() != classVar).forEach(w -> w.addParent(classVar));

        return dag;
    }


    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/syntheticData.arff");

        //We create a MaximumLikelihood object with the MaximumLikehood builder
        MaximumLikelihood parameterLearningAlgorithm = new MaximumLikelihood();

        //We activate the parallel mode.
        parameterLearningAlgorithm.setParallelMode(true);

        //We fix the DAG structure
        parameterLearningAlgorithm.setDAG(getNaiveBayesStructure(data,0));

        //We set the batch size which will be employed to learn the model in parallel
        parameterLearningAlgorithm.setBatchSize(100);

        //We set the data which is going to be used for leaning the parameters
        parameterLearningAlgorithm.setDataStream(data);

        //We perform the learning
        parameterLearningAlgorithm.runLearning();

        //And we get the model
        BayesianNetwork bnModel = parameterLearningAlgorithm.getLearntBayesianNetwork();

        //We print the model
        System.out.println(bnModel.toString());

    }

}
