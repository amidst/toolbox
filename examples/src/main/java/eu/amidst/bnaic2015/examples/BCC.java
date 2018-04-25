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

package eu.amidst.bnaic2015.examples;

import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.ParallelSVB;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuIIDReplication;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.Arrays;

/**
 * This class constains the example code given at the demo session in BNAIC2015 about the AMIDST Toolbox. This code
 * is based on the analysis performed in the following paper:
 *
 * <i>Borchani et al. Modeling concept drift: A probabilistic graphical model based approach. IDA 2015.</i>
 *
 * Created by andresmasegosa on 30/10/15.
 */
public class BCC {

    /** Represents the number of months consider in the simulated data*/
    public static int MONTHS = 60;

    /**
     * This method constains the code needed to learn the model and produce the output.
     * @param parallelSVB a parallel SVB (Streaming Variational Bayes) object
     * @return a Bayesian Network
     * @throws IOException if a file reading error occurs
     */
    public static BayesianNetwork learnModel(ParallelSVB parallelSVB) throws IOException {

        //We access the hidden var
        Variable hiddenGaussian = parallelSVB.getSVBEngine().getDAG().getVariables().getVariableByName("HiddenGaussian");

        //For each month of the period
        for (int i = 0; i < MONTHS; i++) {

            //We load the data for the given month
            DataStream<DataInstance> monthlyData = DataStreamLoader.open("./datasets/bnaic2015/BCC/Month" + i + ".arff");

            //We update the model in parallel with the data from data month
            parallelSVB.updateModel(monthlyData);

            //We query the given var
            Normal normal = parallelSVB.getSVBEngine().getParameterPosterior(hiddenGaussian);

            //We print the mean of this Gaussian var
            System.out.println("E(H) at month "+i+":\t" + normal.getMean());

        }

        //Finally we get the learnt Bayesian network and return it.
        return  parallelSVB.getLearntBayesianNetwork();
    }

    /**
     * This method contains the code to set up the plateau model.
     * @param dag, the DAG to be replicated
     * @return A properly initialized {@link ParallelSVB} object.
     */
    public static ParallelSVB plateuModelSetUp(DAG dag){


        //We access the hidden var
        Variable hiddenGaussian = dag.getVariables().getVariableByName("HiddenGaussian");

        //We create the ParalleVB object which will perform the learning
        ParallelSVB parallelSVB = new ParallelSVB();

        //Set the DAG
        parallelSVB.setDAG(dag);

        //We tell how the above DAG should be expanded.
        parallelSVB.getSVBEngine().setPlateuStructure(new PlateuIIDReplication(Arrays.asList(hiddenGaussian)));

        //We also tell how to evolve the hidden variable over time
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(Arrays.asList(hiddenGaussian), 0, 0.1);
        parallelSVB.getSVBEngine().setTransitionMethod(gaussianHiddenTransitionMethod);

        //We set the window/batch size used for learning
        parallelSVB.getSVBEngine().setWindowsSize(100);

        //We set the maximum number of iteration of the VMP method
        parallelSVB.getSVBEngine().getPlateuStructure().getVMP().setMaxIter(100);

        //We set the threshold definining the convergence of the VMP method
        parallelSVB.getSVBEngine().getPlateuStructure().getVMP().setThreshold(0.001);

        //We do not allow for debuggin info.
        parallelSVB.setOutput(false);


        //We invoke the setup of the underlying data structures
        parallelSVB.initLearning();

        return parallelSVB;
    }

    /**
     * This method contains the code needed to build the NaiveBayes DAG with a global hidden variable modelling
     * concept drift.
     * @return A poperly created {@link DAG} object.
     * @throws Exception if an error occurs while reading the file.
     */
    public static DAG modelBuilding() throws Exception {

        //We load the data for one month
        DataStream<DataInstance> instances = DataStreamLoader.open("./datasets/bnaic2015/BCC/Month0.arff");

        //Define the variables. By default, a random variable is created for each attribute
        Variables variables  = new Variables(instances.getAttributes());

        //We create a new global hidden Gaussian variable
        Variable hiddenGaussian = variables.newGaussianVariable("HiddenGaussian");

        //We get the variable Default
        Variable defaultVariable = variables.getVariableByName("default");

        //We define the DAG
        DAG dag = new DAG(variables);

        //We add the links of the DAG
        dag.getVariables()
                .getListOfVariables()
                .stream()
                .filter(var -> var != defaultVariable)
                .filter(var -> var != hiddenGaussian)
                .forEach(var -> {
                    dag.getParentSet(var).addParent(defaultVariable);
                    dag.getParentSet(var).addParent(hiddenGaussian);
                });


        return dag;

    }

    /**
     * This method contains an example about how to compute the monthly average value of one variable.
     * @throws Exception if an error occurs while reading the file.
     */
    public static void computeMonthlyAverage() throws Exception {

        //For each month of the period
        for (int i = 0; i < MONTHS; i++) {

            //We load the data for that month
            DataStream<DataInstance> instances = DataStreamLoader.open("./datasets/bnaic2015/BCC/Month"+i+".arff");

            //We get the attribute credit
            Attribute credit = instances.getAttributes().getAttributeByName("credit");

            //We compute the average, using a parallel stream.
            double creditMonthlyAverage = instances
                    .parallelStream(1000)
                    .mapToDouble(instance -> instance.getValue(credit))
                    .average()
                    .getAsDouble();

            //We print the computed average
            System.out.println("Average Monthly Credit " + i + ": " + creditMonthlyAverage);
        }

    }

    /**
     * This is the main method of the class which contains the sequence of executions included in the demo.
     * @param args input arguments (not used)
     * @throws Exception if an error occurs while reading the file.
     */
    public static void main(String[] args) throws Exception {

        //Step 1. We show how to compute the monthly average value of the "expenses" variable.
        System.out.println("-----------------------CREDIT MONTHLY AVERAGE--------------------------");
        BCC.computeMonthlyAverage();
        System.out.println("-----------------------------------------------------------------------");

        //Step 2. We build the NaiveBayes DAG with a global hidden var to track the concept drift
        System.out.println("--------------------------------DAG------------------------------------");
        DAG dag = BCC.modelBuilding();
        System.out.println(dag.toString());
        System.out.println("-----------------------------------------------------------------------");

        //Step 3. We set up the plateau structure use for learning
        System.out.println("------------------------DEFINING PLATEAU MODEL-------------------------");
        ParallelSVB parallelSVB = BCC.plateuModelSetUp(dag);
        System.out.println("-----------------------------------------------------------------------");

        //Step 4. We learn the model and print the results.
        System.out.println("------------------------------LEARNING---------------------------------");
        BayesianNetwork bayesianNetwork = BCC.learnModel(parallelSVB);
        System.out.println("\n\nLearnt Bayesian network:\n\n");
        System.out.println(bayesianNetwork.toString());
        System.out.println("-----------------------------------------------------------------------");

    }

}