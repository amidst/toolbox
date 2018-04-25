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

package eu.amidst.winter.Session3;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.ConditionalLinearGaussian;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

/**
 * Created by andresmasegosa on 13/01/2018.
 */
public class Utils {
    public static BayesianNetwork createFireDetectorModel() {

        Variables variables = new Variables();

        Variable fire = variables.newMultinomialVariable("Fire", 2);
        Variable temperature = variables.newGaussianVariable("Temperature");
        Variable smoke = variables.newMultinomialVariable("Smoke", 2);
        Variable sensorT1 = variables.newGaussianVariable("SensorTemp1");
        Variable sensorT2 = variables.newGaussianVariable("SensorTemp2");
        Variable sensorSmoke = variables.newGaussianVariable("SensorSmoke");

        DAG dag = new DAG(variables);

        dag.getParentSet(sensorT1).addParent(temperature);
        dag.getParentSet(sensorT2).addParent(temperature);

        dag.getParentSet(sensorSmoke).addParent(smoke);

        dag.getParentSet(temperature).addParent(fire);
        dag.getParentSet(smoke).addParent(fire);

        BayesianNetwork bayesianNetwork = new BayesianNetwork(dag);

        Multinomial fireprob = bayesianNetwork.getConditionalDistribution(fire);
        fireprob.setProbabilities(new double[]{1.0, 0.0});

        Normal_MultinomialParents tempprob = bayesianNetwork.getConditionalDistribution(temperature);
        tempprob.getNormal(0).setMean(18);
        tempprob.getNormal(0).setVariance(3);

        ConditionalLinearGaussian t1prob = bayesianNetwork.getConditionalDistribution(sensorT1);
        t1prob.setCoeffForParent(temperature, 1.0);
        t1prob.setIntercept(0.0);
        t1prob.setVariance(1.5);

        ConditionalLinearGaussian t2prob = bayesianNetwork.getConditionalDistribution(sensorT2);
        t2prob.setCoeffForParent(temperature, 1.0);
        t2prob.setIntercept(0.0);
        t2prob.setVariance(1.5);


        Multinomial_MultinomialParents smokeProb = bayesianNetwork.getConditionalDistribution(smoke);
        smokeProb.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});

        Normal_MultinomialParents sensorSmokeProb = bayesianNetwork.getConditionalDistribution(sensorSmoke);
        sensorSmokeProb.getNormal(0).setMean(0);
        sensorSmokeProb.getNormal(0).setVariance(0.1);
        sensorSmokeProb.getNormal(1).setMean(5);
        sensorSmokeProb.getNormal(1).setVariance(3);

        System.out.println(bayesianNetwork);

        return bayesianNetwork;
    }

    public static void generateData() throws Exception {

        BayesianNetwork network = createFireDetectorModel();

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(network);
        sampler.setSeed(0);
        sampler.setLatentVar(network.getVariables().getVariableByName("Temperature"));
        sampler.setLatentVar(network.getVariables().getVariableByName("Smoke"));
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(1000);
        DataStreamWriter.writeDataToFile(dataStream, "./datasets/sensorReadings.arff");

    }
    public static void main(String[] args) throws Exception {
        generateData();
    }
}
