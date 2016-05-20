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

package eu.amidst.dynamic.examples.learning;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.dynamic.learning.parametric.ParameterLearningAlgorithm;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

import java.io.IOException;

/**
 * This example shows how to learn the parameters of a dynamic Bayesian network using maximum likelihood
 * from a given data.
 *
 * Created by ana@cs.aau.dk on 01/12/15.
 */
public class MLforDBNfromDataset {

    /**
     * This method returns a DynamicDAG object with naive Bayes structure for the given attributes.
     * @param attributes object of the class Attributes
     * @param classIndex integer value indicating the possition of the class
     * @return object of the class DynamicDAG
     */
    public static DynamicDAG getNaiveBayesStructure(Attributes attributes, int classIndex){

        //We create a Variables object from the attributes of the data stream
        DynamicVariables dynamicVariables = new DynamicVariables(attributes);

        //We define the predicitive class variable
        Variable classVar = dynamicVariables.getVariableById(classIndex);

        //Then, we create a DAG object with the defined model header
        DynamicDAG dag = new DynamicDAG(dynamicVariables);

        //We set the links of the DAG.
        dag.getParentSetsTimeT().stream()
                .filter(var -> var.getMainVar().getVarID()!=classVar.getVarID())
                .forEach(w -> {
                            w.addParent(classVar);
                            //Connect children in consecutive time steps
                            w.addParent(dynamicVariables.getInterfaceVariable(w.getMainVar()));
                        }
                );

        //Connect the class variable in consecutive time steps
        dag.getParentSetTimeT(classVar).addParent(dynamicVariables.getInterfaceVariable(classVar));

        return dag;
    }


    public static void main(String[] args) throws IOException {
        //We can open the data stream using the static class DataStreamLoader
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(
                "datasets/simulated/WasteIncineratorSample.arff");

        //Parameter Learning

        //We fix the DAG structure, the data and learn the DBN
        ParameterLearningAlgorithm parallelMaximumLikelihood = new ParallelMaximumLikelihood();
        parallelMaximumLikelihood.setWindowsSize(1000);
        parallelMaximumLikelihood.setDynamicDAG(MLforDBNfromDataset.getNaiveBayesStructure(data.getAttributes(),2));
        parallelMaximumLikelihood.initLearning();
        parallelMaximumLikelihood.updateModel(data);

        DynamicBayesianNetwork dbn = parallelMaximumLikelihood.getLearntDBN();


        //We print the model
        System.out.println(dbn.toString());
    }

}
