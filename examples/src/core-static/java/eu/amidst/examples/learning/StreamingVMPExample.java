package eu.amidst.examples.learning;


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

import eu.amidst.corestatic.datastream.DataInstance;
import eu.amidst.corestatic.datastream.DataStream;
import eu.amidst.corestatic.io.DataStreamLoader;
import eu.amidst.corestatic.learning.parametric.MaximumLikelihood;
import eu.amidst.corestatic.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.models.DAG;
import eu.amidst.corestatic.variables.Variable;
import eu.amidst.corestatic.variables.Variables;

/**
 *
 * This examples shows how to learn in parallel the parameters of a Bayesian network from a stream of data.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class StreamingVMPExample {

    private static DAG getNaiveBayesStructure(DataStream<DataInstance> dataStream){
        Variables modelHeader = new Variables(dataStream.getAttributes());
        Variable classVar = modelHeader.getVariableById(modelHeader.getNumberOfVars()-1);
        DAG dag = new DAG(modelHeader);
        dag.getParentSets().stream().filter(w -> w.getMainVar().getVarID() != classVar.getVarID()).forEach(w -> w.addParent(classVar));
        return dag;
    }


    public static void main(String[] args) throws Exception {

        //We can open the data stream using the static class DataStreamLoader
        DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/syntheticData.arff");

        //We create a ParameterLearningAlgorithm object with the MaximumLikehood builder
        ParameterLearningAlgorithm parameterLearningAlgorithm = new MaximumLikelihood();

        //We activate the parallel mode.
        parameterLearningAlgorithm.setParallelMode(true);

        //We fix the DAG structure
        parameterLearningAlgorithm.setDAG(getNaiveBayesStructure(data));

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
