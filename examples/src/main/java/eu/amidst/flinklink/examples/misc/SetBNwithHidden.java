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

package eu.amidst.flinklink.examples.misc;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Created by Hanen on 09/12/15.
 */
public class SetBNwithHidden {

    public static DAG getHiddenNaiveBayesStructure(DataFlink<DataInstance> dataStream) {

        // Create a Variables object from the attributes of the input data stream.
        Variables modelHeader = new Variables(dataStream.getAttributes());

        // Define the global latent binary variable.
        Variable globalHiddenDiscrete = modelHeader.newMultinomialVariable("globalHiddenDiscrete", 2);

        // Define the global Gaussian latent binary variable.
        Variable globalHiddenGaussian = modelHeader.newGaussianVariable("globalHiddenGaussian");

        // Define the class variable.
        Variable classVariable = modelHeader.getVariableByName("ClassVar"); //getVariableById(0);


        // Create a DAG object with the defined model header.
        DAG dag = new DAG(modelHeader);

        // Define the structure of the DAG, i.e., set the links between the variables.
        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVariable)
                .filter(w -> w.getMainVar() != globalHiddenDiscrete)
                .filter(w -> w.getMainVar() != globalHiddenGaussian)
                .filter(w -> w.getMainVar().isMultinomial())
                .forEach(w -> w.addParent(globalHiddenDiscrete));

        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVariable)
                .filter(w -> w.getMainVar() != globalHiddenDiscrete)
                .filter(w -> w.getMainVar() != globalHiddenGaussian)
                .filter(w -> w.getMainVar().isNormal())
                .forEach(w -> w.addParent(globalHiddenGaussian));

        dag.getParentSets()
                .stream()
                .filter(w -> w.getMainVar() != classVariable)
                .forEach(w -> w.addParent(classVariable));

        // Return the DAG.
        return dag;
    }

    public static void main(String[] args) throws Exception {

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DataInstance> dataFlink = DataFlinkLoader.loadDataFromFile(env, "./data.arff", false);

        DAG dag = SetBNwithHidden.getHiddenNaiveBayesStructure(dataFlink);

        BayesianNetwork bnet = new BayesianNetwork(dag);

        System.out.println("\n Number of variables \n " + bnet.getDAG().getVariables().getNumberOfVars());

        System.out.println(dag.toString());

        BayesianNetworkWriter.save(bnet, "./BNHiddenExample.bn");

    }

}
