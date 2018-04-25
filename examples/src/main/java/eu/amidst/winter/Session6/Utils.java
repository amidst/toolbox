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

package eu.amidst.winter.Session6;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.*;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;
import eu.amidst.dynamic.variables.DynamicVariables;

/**
 * Created by andresmasegosa on 16/01/2018.
 */
public class Utils {
    public static DynamicBayesianNetwork createDynamicFireDetectorModel() {

        DynamicVariables variables = new DynamicVariables();

        Variable fire = variables.newMultinomialDynamicVariable("Fire", 2);
        Variable temperature = variables.newGaussianDynamicVariable("Temperature");
        Variable smoke = variables.newMultinomialDynamicVariable("Smoke", 2);
        Variable sensorT1 = variables.newGaussianDynamicVariable("SensorTemp1");
        Variable sensorT2 = variables.newGaussianDynamicVariable("SensorTemp2");
        Variable sensorSmoke = variables.newGaussianDynamicVariable("SensorSmoke");

        DynamicDAG dag = new DynamicDAG(variables);

        dag.getParentSetTimeT(sensorT1).addParent(temperature);
        dag.getParentSetTimeT(sensorT2).addParent(temperature);

        dag.getParentSetTimeT(sensorSmoke).addParent(smoke);

        dag.getParentSetTimeT(temperature).addParent(fire);
        dag.getParentSetTimeT(temperature).addParent(temperature.getInterfaceVariable());

        dag.getParentSetTimeT(smoke).addParent(fire);
        dag.getParentSetTimeT(smoke).addParent(smoke.getInterfaceVariable());

        dag.getParentSetTimeT(fire).addParent(fire.getInterfaceVariable());



        DynamicBayesianNetwork bayesianNetwork = new DynamicBayesianNetwork(dag);

        // TIME 0
        Multinomial fireprobTime0 = bayesianNetwork.getConditionalDistributionTime0(fire);
        fireprobTime0.setProbabilities(new double[]{1.0, 0.0});

        Normal_MultinomialParents tempprob = bayesianNetwork.getConditionalDistributionTime0(temperature);
        tempprob.getNormal(0).setMean(18);
        tempprob.getNormal(0).setVariance(3);

        ConditionalLinearGaussian t1prob = bayesianNetwork.getConditionalDistributionTime0(sensorT1);
        t1prob.setCoeffForParent(temperature, 1.0);
        t1prob.setIntercept(0.0);
        t1prob.setVariance(1.5);

        ConditionalLinearGaussian t2prob = bayesianNetwork.getConditionalDistributionTime0(sensorT2);
        t2prob.setCoeffForParent(temperature, 1.0);
        t2prob.setIntercept(0.0);
        t2prob.setVariance(1.5);


        Multinomial_MultinomialParents smokeProb = bayesianNetwork.getConditionalDistributionTime0(smoke);
        smokeProb.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});

        Normal_MultinomialParents sensorSmokeProb = bayesianNetwork.getConditionalDistributionTime0(sensorSmoke);
        sensorSmokeProb.getNormal(0).setMean(0);
        sensorSmokeProb.getNormal(0).setVariance(0.1);
        sensorSmokeProb.getNormal(1).setMean(5);
        sensorSmokeProb.getNormal(1).setVariance(3);


        //Time T
        Multinomial_MultinomialParents fireprobTimeT = bayesianNetwork.getConditionalDistributionTimeT(fire);
        fireprobTimeT.getMultinomial(0).setProbabilities(new double[]{1.0, 0.0});

        Normal_MultinomialNormalParents tempprobTimeT = bayesianNetwork.getConditionalDistributionTimeT(temperature);
        tempprobTimeT.getNormal_NormalParentsDistribution(0).setIntercept(0);
        tempprobTimeT.getNormal_NormalParentsDistribution(0).setCoeffForParent(temperature.getInterfaceVariable(),1);
        tempprobTimeT.getNormal_NormalParentsDistribution(0).setVariance(0.00001);

        ConditionalLinearGaussian t1probTimeT = bayesianNetwork.getConditionalDistributionTimeT(sensorT1);
        t1probTimeT.setCoeffForParent(temperature, 1.0);
        t1probTimeT.setIntercept(0.0);
        t1probTimeT.setVariance(1.5);

        ConditionalLinearGaussian t2probTimeT = bayesianNetwork.getConditionalDistributionTimeT(sensorT2);
        t2probTimeT.setCoeffForParent(temperature, 1.0);
        t2probTimeT.setIntercept(0.0);
        t2probTimeT.setVariance(1.5);


        Multinomial_MultinomialParents smokeProbTimeT = bayesianNetwork.getConditionalDistributionTimeT(smoke);
        smokeProbTimeT.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});
        smokeProbTimeT.getMultinomial(1).setProbabilities(new double[]{0.9, 0.1});
        smokeProbTimeT.getMultinomial(2).setProbabilities(new double[]{0.1, 0.9});
        smokeProbTimeT.getMultinomial(3).setProbabilities(new double[]{0.9, 0.1});

        Normal_MultinomialParents sensorSmokeProbTimeT = bayesianNetwork.getConditionalDistributionTimeT(sensorSmoke);
        sensorSmokeProbTimeT.getNormal(0).setMean(0);
        sensorSmokeProbTimeT.getNormal(0).setVariance(0.1);
        sensorSmokeProbTimeT.getNormal(1).setMean(5);
        sensorSmokeProbTimeT.getNormal(1).setVariance(3);

        System.out.println(bayesianNetwork);

        return bayesianNetwork;
    }

    public static void generateData() throws Exception {

        DynamicBayesianNetwork network = createDynamicFireDetectorModel();

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(network);
        sampler.setSeed(1);
        sampler.setLatentVar(network.getDynamicVariables().getVariableByName("Temperature"));
        sampler.setLatentVar(network.getDynamicVariables().getVariableByName("Smoke"));
        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(100,1000);
        DataStreamWriter.writeDataToFile(dataStream, "./datasets/TimeIndexedSensorReadings.arff");

        dataStream = sampler.sampleToDataBase(1,10);
        DataStreamWriter.writeDataToFile(dataStream, "./datasets/TimeIndexedSensorReadingsEvidence.arff");
    }
    public static void generateEvidenceData() throws Exception {

        DynamicBayesianNetwork network = createDynamicFireDetectorModel();

        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(network);
        sampler.setSeed(1);
        sampler.setLatentVar(network.getDynamicVariables().getVariableByName("Temperature"));
        sampler.setLatentVar(network.getDynamicVariables().getVariableByName("Smoke"));

        DataStream<DynamicDataInstance> dataStream = sampler.sampleToDataBase(1, 10);
        DataStreamWriter.writeDataToFile(dataStream, "./datasets/TimeIndexedSensorReadingsEvidence.arff");
    }

    public static void main(String[] args) throws Exception {
        generateData();
        generateEvidenceData();
    }
}
