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

package eu.amidst.winter.Session5;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.*;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkWriter;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.Random;

/**
 * Created by andresmasegosa on 13/01/2018.
 */
public class Utils {
    public static BayesianNetwork createBigFireDetectorModel(double tempMean, int nRep) {
        Variables variables = new Variables();

        Variable temperatureBuilding = variables.newGaussianVariable("TemperatureBuilding");

        Variable[] fire = new Variable[nRep];
        Variable[] temperature = new Variable[nRep];
        Variable[] smoke = new Variable[nRep];
        Variable[] sensorT1 = new Variable[nRep];
        Variable[] sensorT2 = new Variable[nRep];
        Variable[] sensorSmoke = new Variable[nRep];

        for (int i = 0; i < nRep; i++) {
            fire[i] = variables.newMultinomialVariable("Fire_"+i, 2);
            temperature[i] = variables.newGaussianVariable("Temperature_"+i);
            smoke[i] = variables.newMultinomialVariable("Smoke_"+i, 2);
            sensorT1[i] = variables.newGaussianVariable("SensorTemp1_"+i);
            sensorT2[i] = variables.newGaussianVariable("SensorTemp2_"+i);
            sensorSmoke[i] = variables.newGaussianVariable("SensorSmoke_"+i);
        }

        DAG dag = new DAG(variables);
        for (int i = 0; i < nRep; i++) {
            dag.getParentSet(sensorT1[i]).addParent(temperature[i]);
            dag.getParentSet(sensorT2[i]).addParent(temperature[i]);

            dag.getParentSet(sensorSmoke[i]).addParent(smoke[i]);

            dag.getParentSet(temperature[i]).addParent(fire[i]);
            dag.getParentSet(smoke[i]).addParent(fire[i]);

            dag.getParentSet(temperature[i]).addParent(temperatureBuilding);
        }

        BayesianNetwork bayesianNetwork = new BayesianNetwork(dag);

        Normal tempprobBuilding = bayesianNetwork.getConditionalDistribution(temperatureBuilding);
        tempprobBuilding.setMean(tempMean);
        tempprobBuilding.setVariance(3);

        Random random = new Random(0);
        for (int i = 0; i < nRep; i++) {

            Multinomial fireprob = bayesianNetwork.getConditionalDistribution(fire[i]);
            fireprob.setProbabilities(new double[]{1.0, 0.0});

            Normal_MultinomialNormalParents tempprob = bayesianNetwork.getConditionalDistribution(temperature[i]);
            tempprob.getNormal_NormalParentsDistribution(0).setIntercept(0.0);
            tempprob.getNormal_NormalParentsDistribution(0).setCoeffForParent(temperatureBuilding,1.0);
            tempprob.getNormal_NormalParentsDistribution(0).setVariance(1);

            tempprob.getNormal_NormalParentsDistribution(1).setIntercept(10.0);
            tempprob.getNormal_NormalParentsDistribution(1).setCoeffForParent(temperatureBuilding,1.0);
            tempprob.getNormal_NormalParentsDistribution(1).setVariance(3);


            ConditionalLinearGaussian t1prob = bayesianNetwork.getConditionalDistribution(sensorT1[i]);
            t1prob.setCoeffForParent(temperature[i], 1.0);
            t1prob.setIntercept(0.0);
            t1prob.setVariance(1.5);

            ConditionalLinearGaussian t2prob = bayesianNetwork.getConditionalDistribution(sensorT2[i]);
            t2prob.setCoeffForParent(temperature[i], 1.0);
            t2prob.setIntercept(0.0);
            t2prob.setVariance(1.5);


            Multinomial_MultinomialParents smokeProb = bayesianNetwork.getConditionalDistribution(smoke[i]);
            smokeProb.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});

            Normal_MultinomialParents sensorSmokeProb = bayesianNetwork.getConditionalDistribution(sensorSmoke[i]);
            sensorSmokeProb.getNormal(0).setMean(0);
            sensorSmokeProb.getNormal(0).setVariance(0.1);
            sensorSmokeProb.getNormal(1).setMean(5);
            sensorSmokeProb.getNormal(1).setVariance(3);
        }
        System.out.println(bayesianNetwork);

        return bayesianNetwork;
    }

    public static BayesianNetwork createFireDetectorModel(double tempMean) {

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
        tempprob.getNormal(0).setMean(tempMean);
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

    public static void generateBigData(double tempMean, int nRep) throws Exception {

        BayesianNetwork network = createBigFireDetectorModel(tempMean, nRep);

        eu.amidst.flinklink.core.utils.BayesianNetworkSampler sampler = new eu.amidst.flinklink.core.utils.BayesianNetworkSampler(network);
        sampler.setSeed(0);
        sampler.setLatentVar(network.getVariables().getVariableByName("TemperatureBuilding"));
        for (int i = 0; i < nRep; i++) {
            sampler.setLatentVar(network.getVariables().getVariableByName("Temperature_"+i));
            sampler.setLatentVar(network.getVariables().getVariableByName("Smoke_"+i));
        }


        ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();
        env.setParallelism(10);
        env.getConfig().disableSysoutLogging();

        DataFlink<DataInstance> data = sampler.sampleToDataFlink(env,10000);
        DataFlinkWriter.writeDataToARFFFolder(data, "./dataSets/BigSensorReadings.arff");
    }

    public static void generateData(int seed, double tempMean, String outputFile) throws Exception {

        BayesianNetwork network = createFireDetectorModel(tempMean);

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(network);
        sampler.setSeed(seed);
        sampler.setLatentVar(network.getVariables().getVariableByName("Temperature"));
        sampler.setLatentVar(network.getVariables().getVariableByName("Smoke"));
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(1000);
        DataStreamWriter.writeDataToFile(dataStream, outputFile);

    }
    public static void main(String[] args) throws Exception {
       String[] monthName = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        double[] tempMean =  {    18,        18,        19,      20,    21,     22,     23,      23,         22,        20,        19,         18};
        for (int i = 0; i < tempMean.length; i++) {
            generateData(i, tempMean[i],"./datasets/bymonth/sensorReadings"+monthName[i]+".arff");
        }

        generateBigData(18,10);

    }
}
