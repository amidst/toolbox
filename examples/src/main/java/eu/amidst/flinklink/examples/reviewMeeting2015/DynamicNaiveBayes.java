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

package eu.amidst.flinklink.examples.reviewMeeting2015;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ana@cs.aau.dk on 20/01/16.
 */
public class DynamicNaiveBayes {

    static Logger logger = LoggerFactory.getLogger(DynamicNaiveBayes.class);

    public static DynamicDAG createDynamicNaiveBayes(Attributes attributes){
        // Create a Variables object from the attributes of the input data stream.
        DynamicVariables variables = new DynamicVariables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableById(0);

        // Create a DAG object with the defined variables.
        DynamicDAG dynamicDAG = new DynamicDAG(variables);

        // Link the class as parent of all attributes
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(classVar));

        // Link the class and the attributes through time

        dynamicDAG.getParentSetsTimeT()
                .stream()
                .forEach(w -> w.addParent(w.getMainVar().getInterfaceVariable()));

        // Return the DAG.
        return dynamicDAG;
    }

    public static DynamicDAG createDynamicNaiveBayesWithHidden(Attributes attributes){
        // Create a Variables object from the attributes of the input data stream.
        DynamicVariables variables = new DynamicVariables(attributes);

        // Define the global latent binary variable.
        Variable globalHiddenVar = variables.newMultinomialDynamicVariable("GlobalHidden", 2);

        // Define the class variable.
        Variable classVar = variables.getVariableById(0);

        // Create a DAG object with the defined variables.
        DynamicDAG dynamicDAG = new DynamicDAG(variables);

        // Link the class as parent of all attributes (and the hidden)
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(classVar));

        // Link the hidden as parent of all attributes
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(globalHiddenVar));

        // Link the class, the hidden and the attributes through time
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .forEach(w -> w.addParent(w.getMainVar().getInterfaceVariable()));

        // Return the DAG.
        return dynamicDAG;
    }

    public static void main(String[] args) throws Exception {

        //Boolean includeHidden = Boolean.parseBoolean(args[0]);
        Boolean includeHidden = true;
        String fileName = "./datasets/simulated/tmp0_0_100_3_0_iter_";

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,fileName+0+".arff", false);

        DynamicDAG NBdag = includeHidden?
                createDynamicNaiveBayesWithHidden(data0.getAttributes()):
                createDynamicNaiveBayes(data0.getAttributes());

        System.out.println(NBdag.toString());

        int nsets = 3;

        long start = System.nanoTime();

        //Parameter Learning
        DynamicParallelVB parallelVB = new DynamicParallelVB();
        parallelVB.setGlobalThreshold(0.1);
        parallelVB.setMaximumGlobalIterations(10);
        parallelVB.setLocalThreshold(0.1);
        parallelVB.setMaximumLocalIterations(100);
        parallelVB.setSeed(5);
        parallelVB.setOutput(true);

        //Set the window size
        parallelVB.setBatchSize(500);
        parallelVB.setDAG(NBdag);
        parallelVB.initLearning();

        System.out.println("--------------- DATA " + 0 + " --------------------------");
        parallelVB.updateModelWithNewTimeSlice(0, data0);


        for (int i = 1; i < nsets; i++) {
            logger.info("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    fileName+i+".arff", false);
            parallelVB.updateModelWithNewTimeSlice(i, dataNew);
        }

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.printf("Running time: "+seconds+ "seconds");
    }
}
