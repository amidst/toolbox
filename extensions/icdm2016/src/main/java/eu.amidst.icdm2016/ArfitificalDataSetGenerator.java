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

package eu.amidst.icdm2016;

import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variables;

import java.io.IOException;

/**
 * Created by andresmasegosa on 20/5/16.
 */
public class ArfitificalDataSetGenerator {

    public static void main(String[] args) throws IOException {

        String pathFolder = "./datasets/";
        int predVars = 1;
        int hiddenVars = 2;
        int sampleSize = 1000;
        int nDataSets = 10;

        Variables variables = new Variables();

        for (int i = 0; i < predVars; i++) {
            variables.newGaussianVariable("VAR_" + i);
        }


        for (int i = 0; i < hiddenVars; i++) {
            variables.newGaussianVariable("HiddenVAR_" + i);
        }

        DAG dag = new DAG(variables);

        for (int i = 0; i < predVars; i++) {
            for (int j = 0; j < hiddenVars; j++) {
                dag.getParentSet(variables.getVariableByName("VAR_"+i)).addParent(variables.getVariableByName("HiddenVAR_"+j));
            }
        }

        BayesianNetwork bayesianNetwork = new BayesianNetwork(dag);

        double[] coefficients = new double[hiddenVars];
        for (int i = 0; i < hiddenVars; i++) {
            coefficients[i]=1.0;
        }
        for (int i = 0; i < predVars; i++) {
            ConditionalLinearGaussian dist = bayesianNetwork.getConditionalDistribution(variables.getVariableByName("VAR_"+i));
            dist.setCoeffParents(coefficients);
            dist.setVariance(1);
        }


        //STEP 0
        for (int i = 0; i < hiddenVars; i++) {
            Normal dist = bayesianNetwork.getConditionalDistribution(variables.getVariableByName("HiddenVAR_"+i));
            dist.setMean(0);
            dist.setVariance(1e-10);
        }

        System.out.println("DATA 0");
        System.out.println(bayesianNetwork);

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bayesianNetwork);
        for (int i = 0; i < hiddenVars; i++) {
            sampler.setHiddenVar(variables.getVariableByName("HiddenVAR_"+i));
        }
        sampler.setOmitHiddenVars(true);
        DataStreamWriter.writeDataToFile(sampler.sampleToDataStream(sampleSize),pathFolder+"Data_0.arff");


        //Following Steps
        for (int K = 1; K < nDataSets; K++) {
            for (int i = 0; i < hiddenVars; i++) {
                Normal dist = bayesianNetwork.getConditionalDistribution(variables.getVariableByName("HiddenVAR_"+i));
                dist.setMean(dist.getMean()+(i+1));
                dist.setVariance(1);
            }

            System.out.println("DATA "+K);
            System.out.println(bayesianNetwork);
            sampler.setOmitHiddenVars(true);
            DataStreamWriter.writeDataToFile(sampler.sampleToDataStream(sampleSize),pathFolder+"Data_"+K+".arff");
        }
    }
}
