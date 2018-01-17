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

import com.google.common.base.Stopwatch;
import eu.amidst.core.constraints.Constraint;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.io.DataFlinkLoader;
import eu.amidst.flinklink.core.learning.parametric.dVMP;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;

/**
 * Session 5. Define a large model for fire detection in a building. Then, learn the model from a data set using Apache Flink.
 * Created by andresmasegosa on 14/01/2018.
 */
public class C_ScalableModelLearning {
    /**
     * Define the Fire Dectector Model's DAG.
     * @param attributes
     * @return
     */
    public static DAG creatBigFireDectectorModel(Attributes attributes, int nRooms){

        /********** Model Definition ************/
        //Create the object handling the random variables of the model
        Variables variables = new Variables();

        //Create the random variables of the model.
        // Some of them are associated to one attribute to retrieve its observed values from the data set.
        Variable temperatureBuilding = variables.newGaussianVariable("TemperatureBuilding");

        Variable[] fire = new Variable[nRooms];
        Variable[] temperature = new Variable[nRooms];
        Variable[] smoke = new Variable[nRooms];
        Variable[] sensorT1 = new Variable[nRooms];
        Variable[] sensorT2 = new Variable[nRooms];
        Variable[] sensorSmoke = new Variable[nRooms];

        for (int i = 0; i < nRooms; i++) {
            fire[i] = variables.newMultinomialVariable(attributes.getAttributeByName("Fire_"+i));
            temperature[i] = variables.newGaussianVariable("Temperature_"+i);
            smoke[i] = variables.newMultinomialVariable("Smoke_"+i, 2);
            sensorT1[i] = variables.newGaussianVariable(attributes.getAttributeByName("SensorTemp1_"+i));
            sensorT2[i] = variables.newGaussianVariable(attributes.getAttributeByName("SensorTemp2_"+i));
            sensorSmoke[i] = variables.newGaussianVariable(attributes.getAttributeByName("SensorSmoke_"+i));
        }

        //Create the directed acyclic graph object encoding the conditional independe relaionship among the variables of the model.
        DAG dag = new DAG(variables);
        for (int i = 0; i < nRooms; i++) {
            //Define the parent set for each random variable
            dag.getParentSet(sensorT1[i]).addParent(temperature[i]);
            dag.getParentSet(sensorT2[i]).addParent(temperature[i]);

            dag.getParentSet(sensorSmoke[i]).addParent(smoke[i]);

            dag.getParentSet(temperature[i]).addParent(fire[i]);
            dag.getParentSet(smoke[i]).addParent(fire[i]);

            dag.getParentSet(temperature[i]).addParent(temperatureBuilding);
        }

        //Print the defined DAG
        System.out.println(dag);

        return dag;
    }

    public static void main(String[] args) throws Exception {
        /********** Flink Set-up ***************/
        //Define the execution environment for Flink. In this case, it is a local execution environment.
        ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();
        env.getConfig().disableSysoutLogging();
        env.setParallelism(4);

        /********** DATA LOADING ***************/
        //Load the data set
        DataFlink<DataInstance> data = DataFlinkLoader.open(env,"./datasets/BigSensorReadings.arff");

        int nRooms = 10;

        /********** Model Definition ************/
        DAG fireDetectorModel = creatBigFireDectectorModel(data.getAttributes(),nRooms);

        /********** Model Learning ************/
        //Define the learning engine (distributed Variational Message Passing) and the parameters
        dVMP svb = new dVMP();
        svb.setBatchSize(1000);
        svb.setOutput(true);
        svb.setDAG(fireDetectorModel);

        //Specify the associated constraints (econding prior knowledge)
        for (int i = 0; i < nRooms; i++) {
            Variable sensorT1 = fireDetectorModel.getVariables().getVariableByName("SensorTemp1_"+i);
            Variable sensorT2 = fireDetectorModel.getVariables().getVariableByName("SensorTemp2_"+i);
            svb.addParameterConstraint(new Constraint("alpha", sensorT1, 0.0));
            svb.addParameterConstraint(new Constraint("alpha", sensorT2, 0.0));
            svb.addParameterConstraint(new Constraint("beta1", sensorT1, 1.0));
            svb.addParameterConstraint(new Constraint("beta1", sensorT2, 1.0));

            Variable temp = fireDetectorModel.getVariables().getVariableByName("Temperature_"+i);
            svb.addParameterConstraint(new Constraint("alpha | {Fire_"+i+" = 0}", temp, 0.0));
            svb.addParameterConstraint(new Constraint("beta1 | {Fire_"+i+" = 0}", temp, 1.0));

        }

        //Set-up the learning phase
        svb.initLearning();

        //Perform Learning
        Stopwatch watch = Stopwatch.createStarted();
        svb.updateModel(data);
        System.out.println(watch.stop());


        //Print the learnt model
        System.out.println(svb.getLearntBayesianNetwork());

    }
}
