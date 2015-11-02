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

package eu.amidst.bnaic2015.examples;

import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.conceptdrift.utils.PlateuHiddenVariableConceptDrift;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.ParallelSVB;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.util.Arrays;

/**
 * Created by andresmasegosa on 30/10/15.
 */
public class BCC {

    public static int MONTHS = 60;

    /**
     *
     * @param parallelSVB
     */
    public static void learnModel(ParallelSVB parallelSVB) {

        //We access the hidden var
        Variable hiddenGaussian = parallelSVB.getSVBEngine().getDAG().getVariables().getVariableByName("HiddenGaussian");

        //For each month of the period
        for (int i = 0; i < MONTHS; i++) {

            //We load the data for the given month
            DataStream<DataInstance> monthlyData = DataStreamLoader.openFromFile("./datasets/bnaic2015/BCC/Month" + i + ".arff");

            //We update the model in parallel with the data from data month
            parallelSVB.updateModelInParallel(monthlyData);

            //We query the given var
            Normal normal = parallelSVB.getSVBEngine().getParameterPosterior(hiddenGaussian);

            //We print the mean of this Gaussian var
            System.out.println("E(H) at month "+i+":\t" + normal.getMean());
        }
    }

    public static ParallelSVB plateuModelSetUp(DAG dag){

        Variable hiddenGaussian = dag.getVariables().getVariableByName("HiddenGaussian");

        ParallelSVB parallelSVB = new ParallelSVB();
        parallelSVB.setDAG(dag);
        parallelSVB.getSVBEngine().setPlateuStructure(new PlateuHiddenVariableConceptDrift(Arrays.asList(hiddenGaussian), true));
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(Arrays.asList(hiddenGaussian), 0, 0.1);
        parallelSVB.getSVBEngine().setTransitionMethod(gaussianHiddenTransitionMethod);
        parallelSVB.getSVBEngine().setWindowsSize(100);
        parallelSVB.getSVBEngine().getPlateuStructure().getVMP().setMaxIter(100);
        parallelSVB.getSVBEngine().getPlateuStructure().getVMP().setThreshold(0.001);
        parallelSVB.setOutput(false);

        parallelSVB.initLearning();

        return parallelSVB;
    }

    public static DAG modelBuilding() throws Exception {

        //We load the data for one month
        DataStream<DataInstance> instances = DataStreamLoader.openFromFile("./datasets/bnaic2015/BCC/Month0.arff");

        //Define the variables. By default, a random variable is created for each attribute
        Variables variables  = new Variables(instances.getAttributes());

        //We get the variable Default
        Variable defaultVariable = variables.getVariableByName("default");

        //We create a new global hidden Gaussian variable
        Variable hiddenGaussian = variables.newGaussianVariable("HiddenGaussian");

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

        //Finally we create the Bayesian network
        //BayesianNetwork bn = new BayesianNetwork(dag);
        //BayesianNetworkWriter.saveToFile(bn, "./BCC/PGM.bn");

        return dag;

    }

    public static void computeMonthlyAverage() throws Exception {

        //For each month of the period
        for (int i = 0; i < MONTHS; i++) {
            //We load the data for that month
            DataStream<DataInstance> instances = DataStreamLoader.openFromFile("./datasets/bnaic2015/BCC/Month"+i+".arff");
            //We get the attribute expenses
            Attribute expenses = instances.getAttributes().getAttributeByName("expenses");

            //We compute the average, using a parallel stream.
            double expensesMonthlyAverage = instances
                                                .parallelStream(1000)
                                                .mapToDouble(instance -> instance.getValue(expenses))
                                                .average()
                                                .getAsDouble();

            //We print the computed average
            System.out.println("Average Monthly Expenses " + i + ": " + expensesMonthlyAverage);
        }

    }

    public static void main(String[] args) throws Exception {

        BCC.computeMonthlyAverage();
        DAG dag = BCC.modelBuilding();
        ParallelSVB parallelSVB = BCC.plateuModelSetUp(dag);
        BCC.learnModel(parallelSVB);

    }

}