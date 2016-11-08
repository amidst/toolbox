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

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.dynamic.DynamicParallelVB;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 *
 *
 *
 *
 *
 */
public class DynamicParallelVMPExtended {

    static Logger logger = LoggerFactory.getLogger(DynamicParallelVMPExtended.class);

    /**
     * Creates a {@link DAG} object with a naive Bayes structure from a given {@link DataStream}.
     * The main variable is defined as a latent binary variable which is set as a parent of all the observed variables.
     * @return a {@link DAG} object.
     */
    public static DynamicDAG getHiddenDynamicNaiveBayesStructure(Attributes attributes, boolean includeHiddenVars) {

        // Create a Variables object from the attributes of the input data stream.
        DynamicVariables modelHeader = new DynamicVariables(attributes);

        // Define the global latent binary variable.
        //Variable globalHiddenVar = modelHeader.newMultinomialDynamicVariable("GlobalHidden", 2);

        // Define the global Gaussian latent binary variable.
        //Variable globalHiddenGaussian = modelHeader.newGaussianDynamicVariable("globalHiddenGaussian");


        // Define the class variable.
        Variable classVar = modelHeader.getVariableById(0);

        // Create a DAG object with the defined model header.
        DynamicDAG dag = new DynamicDAG(modelHeader);

        // Define the structure of the DAG, i.e., set the links between the variables.

        /*
        if(includeHiddenVars) {
            dag.getParentSetsTimeT()
                    .stream()
                    .filter(w -> w.getMainVar() != classVar)
                    .filter(w -> w.getMainVar() != globalHiddenVar)
                    .filter(w -> w.getMainVar() != globalHiddenGaussian)
                    .filter(w -> w.getMainVar().isMultinomial())
                    .forEach(w -> w.addParent(globalHiddenVar));

            dag.getParentSetsTimeT()
                    .stream()
                    .filter(w -> w.getMainVar() != classVar)
                    .filter(w -> w.getMainVar() != globalHiddenVar)
                    .filter(w -> w.getMainVar() != globalHiddenGaussian)
                    .filter(w -> w.getMainVar().isNormal())
                    .forEach(w -> w.addParent(globalHiddenGaussian));
        }*/

        dag.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() != classVar)
                .forEach(w -> w.addParent(classVar));

        /*
        if(includeHiddenVars) {
            dag.getParentSetsTimeT()
                    .stream()
                    .filter(w -> w.getMainVar() == classVar || w.getMainVar() == globalHiddenVar ||
                            w.getMainVar() == globalHiddenGaussian)
                    .forEach(w -> w.addParent(w.getMainVar().getInterfaceVariable()));
        }
        */
        dag.getParentSetsTimeT()
                .stream()
                .filter(w -> w.getMainVar() == classVar)
                .forEach(w -> w.addParent(w.getMainVar().getInterfaceVariable()));


        /*
        if(includeHiddenVars) {
            dag.getParentSetTimeT(globalHiddenGaussian).addParent(globalHiddenVar);
        }
        */

        System.out.println(dag);
        // Return the DAG.
        return dag;
    }

      /**
     *
     * ./bin/flink run -m yarn-cluster -yn 8 -ys 4 -yjm 1024 -ytm 9000
     *              -c eu.amidst.flinklink.examples.misc.DynamicParallelVMPExtended ../flinklink.jar 0 10 1000000 100 100 100 3 0
     *
     *
     * @param args command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        int nCVars = Integer.parseInt(args[0]);
        int nMVars = Integer.parseInt(args[1]);
        int nSamples = Integer.parseInt(args[2]);
        int windowSize = Integer.parseInt(args[3]);
        int globalIter = Integer.parseInt(args[4]);
        int localIter = Integer.parseInt(args[5]);
        int nsets = Integer.parseInt(args[6]);
        int seed = Integer.parseInt(args[7]);
        boolean generateData = Boolean.parseBoolean(args[8]);

        /*
         * Generate dynamic datasets
         */
        if(generateData) {
            String[] argsDatasets = ArrayUtils.addAll(ArrayUtils.subarray(args, 0, 4), ArrayUtils.subarray(args, 6, 8));
            DynamicDataSets dynamicDataSets = new DynamicDataSets();
            dynamicDataSets.generateDynamicDataset(argsDatasets);
        }

        /*
         * Logging
         */
        //PropertyConfigurator.configure(args[6]);
        BasicConfigurator.configure();

        logger.info("Starting DynamicVMPExtended experiments");


        //String fileName = "hdfs:///tmp"+nCVars+"_"+nMVars+"_"+nSamples+"_"+nsets+"_"+seed;
        String fileName = "./datasets/tmp"+nCVars+"_"+nMVars+"_"+nSamples+"_"+nsets+"_"+seed;

        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataFlink<DynamicDataInstance> data0 = DataFlinkLoader.loadDynamicDataFromFolder(env,fileName+"_iter_"+0+".arff", false);

        DynamicDAG hiddenNB = getHiddenDynamicNaiveBayesStructure(data0.getAttributes(), false);

        System.out.println(hiddenNB.toString());

        //Structure learning is excluded from the test, i.e., we use directly the initial Asia network structure
        // and just learn then test the parameter learning

        long start = System.nanoTime();

        //Parameter Learning
        DynamicParallelVB parallelVB = new DynamicParallelVB();
        parallelVB.setGlobalThreshold(0.1);
        parallelVB.setMaximumGlobalIterations(globalIter);
        parallelVB.setLocalThreshold(0.1);
        parallelVB.setMaximumLocalIterations(localIter);
        parallelVB.setSeed(5);
        parallelVB.setOutput(true);

        //Set the window size
        parallelVB.setBatchSize(windowSize);
        parallelVB.setDAG(hiddenNB);
        parallelVB.initLearning();

        System.out.println("--------------- DATA " + 0 + " --------------------------");
        parallelVB.updateModelWithNewTimeSlice(0, data0);


        for (int i = 1; i < nsets; i++) {
            logger.info("--------------- DATA " + i + " --------------------------");
            DataFlink<DynamicDataInstance> dataNew = DataFlinkLoader.loadDynamicDataFromFolder(env,
                    fileName+"_iter_"+i+".arff", false);
            parallelVB.updateModelWithNewTimeSlice(i, dataNew);
        }

        logger.info(parallelVB.getLearntDynamicBayesianNetwork().toString());

        long duration = (System.nanoTime() - start) / 1;
        double seconds = duration / 1000000000.0;
        logger.info("Running time: {} seconds.", seconds);

    }

}
