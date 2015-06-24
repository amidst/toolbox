package eu.amidst.core.examples.learning;


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

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.StreamingVariationalBayesVMP;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

/**
 *
 * This examples shows how to learn in the parameters of a Bayesian network from a stream of data with a Bayesian
 * approach using the following algorithm
 *
 * Broderick, T., Boyd, N., Wibisono, A., Wilson, A. C., & Jordan, M. I. (2013). Streaming variational bayes.
 * In Advances in Neural Information Processing Systems (pp. 1727-1735).
 *
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class StreamingVMPExample {


    /**
     * This method creates a DAG object with a naive Bayes structure for the attributes of the passed data stream.
     * The main variable is defined as a latent binary variable which is a parent of all the observed variables.
     * @param dataStream
     * @return
     */
    private static DAG getHiddenNaiveBayesStructure(DataStream<DataInstance> dataStream){
        //We create a Variables object from the attributes of the data stream
        Variables modelHeader = new Variables(dataStream.getAttributes());

        //We define the global latent binary variable
        Variable globalHiddenVar = modelHeader.newMultionomialVariable("GlobalHidden",2);

        //Then, we create a DAG object with the defined model header
        DAG dag = new DAG(modelHeader);

        //We set the linkds of the DAG.
        dag.getParentSets().stream().filter(w -> w.getMainVar() != globalHiddenVar).forEach(w -> w.addParent(globalHiddenVar));

        return dag;
    }


    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/syntheticData.arff");

        //We create a StreamingVariationalBayesVMP object
        StreamingVariationalBayesVMP parameterLearningAlgorithm = new StreamingVariationalBayesVMP();

        //We fix the DAG structure
        parameterLearningAlgorithm.setDAG(getHiddenNaiveBayesStructure(data));

        //We fix the size of the window, which must be equal to the size of the data batches we use for learning
        parameterLearningAlgorithm.setWindowsSize(5);

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
