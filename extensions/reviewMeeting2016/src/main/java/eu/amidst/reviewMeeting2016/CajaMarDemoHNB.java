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

package eu.amidst.reviewMeeting2016;

import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuIIDReplication;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.conceptdrift.IdentifiableIDAModel;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;
import eu.amidst.huginlink.io.DynamicBayesianNetworkWriterToHugin;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.Arrays;

/**
 * Created by ana@cs.aau.dk on 21/01/16.
 */
public class CajaMarDemoHNB {

    //static Logger logger = LoggerFactory.getLogger(CajaMarDemo.class);

    public static void main(String[] args) throws Exception {

        /*
         * Create flink ExecutionEnvironment variable:
         * The ExecutionEnviroment is the context in which a program is executed. A local environment will cause
         * execution in the current JVM, a remote environment will cause execution on a remote cluster installation.
         */
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        /*************************************************************************************
         * 1.- READ DATA TO GET MODEL HEADER (ATTRIBUTES)
         *************************************************************************************/

        // The demo can be run on your local computer or a cluster with hadoop, (un)comment as appropriate
        String fileName = "hdfs:///tmp_conceptdrift_data";
        //String fileName = "./datasets/dataStream/conceptdrift/data";

        // Load the first batch of data (first month) to get the model header (attributes) necessary to create
        // the dynamic DAG
        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,fileName+0+".arff", false);
        Attributes attributes = data0.getAttributes();

        System.out.println(attributes);
        /*************************************************************************************
         * 2. - CREATE A DYNAMIC NAIVE BAYES DAG WITH HIDDEN GLOBAL VARIABLE
         *************************************************************************************/

        // Create a Variables object from the attributes of the input data stream.
        DynamicVariables variables = new DynamicVariables(attributes);

        // Define the class variable.
        Variable classVar = variables.getVariableById(0);

        // Define the global latent binary variable.
        Variable globalHiddenVar = variables.newGaussianDynamicVariable("GlobalHidden");

        // Create an empty DAG object with the defined variables.
        DynamicDAG dynamicDAG = new DynamicDAG(variables);

        // Link the class as parent of all attributes
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .forEach(w -> w.addParent(classVar));
/*
        for (Variable var: variables){
            if (var==classVar)
                continue;

            dynamicDAG.getParentSetTimeT(var).addParent(classVar);
        }
  */
        // Link the class through time
        dynamicDAG.getParentSetTimeT(classVar).addParent(classVar.getInterfaceVariable());

        // Link the hidden as parent of all predictive attributes
        dynamicDAG.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .filter(w -> w.getMainVar() != globalHiddenVar)
                .forEach(w -> w.addParent(globalHiddenVar));

        // Show the dynamic DAG structure
        System.out.println(dynamicDAG.toString());

        /*************************************************************************************
         * 5.- LEARN DYNAMIC NAIVE BAYES WITH HIDDEN VARIABLE AND SHOW EXPECTED VALUE OF H
         *************************************************************************************/

        // Set the number of available months for learning
        int nMonths = Integer.parseInt(args[0]);

        long start = System.nanoTime();

        //Parallel Bayesian learning engine - parameters
        DynamicParallelVB parallelVB = new DynamicParallelVB();

        //
        parallelVB.setPlateuStructure(new PlateuIIDReplication());
        // Convergence parameters
        parallelVB.setGlobalThreshold(0.1);
        parallelVB.setMaximumGlobalIterations(100);
        parallelVB.setLocalThreshold(0.1);
        parallelVB.setMaximumLocalIterations(100);
        // Set the batch/window size or level of parallelization (result is independent of this parameter)
        parallelVB.setBatchSize(1000);
        // Set the dynamic DAG to learn from (resulting DAG is nVariables*nSamples*nMonths)
        parallelVB.setDAG(dynamicDAG);
        // Show debugging output for VB
        parallelVB.setOutput(true);

        // Create the plateu structure to replicate
        parallelVB.setPlateuStructure(new PlateuIIDReplication(Arrays.asList(globalHiddenVar)));

        // Define the transition for the global hidden variable, starting with a standard N(0,1)
        // Gaussian and transition variance (that is summed to that of the previous step) 1.
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod =
                new GaussianHiddenTransitionMethod(Arrays.asList(globalHiddenVar), 0, 0.1);
        parallelVB.setTransitionMethod(gaussianHiddenTransitionMethod);

        // Update the dynamic DAG to learn from
        parallelVB.setDAG(dynamicDAG);

        //Set the procedure to make the model identifiable
        parallelVB.setIdenitifableModelling(new IdentifiableIDAModel());

        //Init learning
        parallelVB.initLearning();

        //Collect expected output for the global variable each month
        double[] output = new double[nMonths];

        for (int i = 0; i < nMonths; i++) {
            System.out.println("--------------- MONTH " + i + " --------------------------");
            //Load the data for that month
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    fileName + i + ".arff", false);
            parallelVB.updateModelWithNewTimeSlice(i, dataNew);
            Normal normal = parallelVB.getParameterPosteriorTimeT(globalHiddenVar);
            //Compute expected value for H this month
            output[i] = normal.getMean();
        }

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        System.out.println("Running time (DNB with hidden): "+seconds+" seconds.");

        System.out.println(parallelVB.getLearntDynamicBayesianNetwork());

        for (int i = 0; i < nMonths; i++) {
            System.out.println("E(H_"+i+") =\t" + output[i]);
        }

        DynamicBayesianNetworkWriterToHugin.save(parallelVB.getLearntDynamicBayesianNetwork(),"./DNB_withH.net");
    }
}
