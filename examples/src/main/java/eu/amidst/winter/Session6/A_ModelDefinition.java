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

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicDAG;
import eu.amidst.dynamic.variables.DynamicVariables;

/**
 *  Session 6. Define and learn a dynamic fire detector model.
 *
 * Created by andresmasegosa on 16/01/2018.
 */
public class A_ModelDefinition {

    /**
     * Define the Dynamic Fire Dectector Model's DAG.
     * @param attributes
     * @return
     */
    public static DynamicDAG defineDynamicFireDetectorModel(Attributes attributes){

        /********** Model Definition ************/
        //Create the object handling the random variables of the model
        DynamicVariables variables = new DynamicVariables();

        //Create the random variables of the model. Some of them are associated to one attribute to retrieve its observed values from the data set.
        Variable fire = variables.newMultinomialDynamicVariable(attributes.getAttributeByName("Fire"));
        Variable temperature = variables.newGaussianDynamicVariable("Temperature");
        Variable smoke = variables.newMultinomialDynamicVariable("Smoke", 2);
        Variable sensorT1 = variables.newGaussianDynamicVariable(attributes.getAttributeByName("SensorTemp1"));
        Variable sensorT2 = variables.newGaussianDynamicVariable(attributes.getAttributeByName("SensorTemp2"));
        Variable sensorSmoke = variables.newGaussianDynamicVariable(attributes.getAttributeByName("SensorSmoke"));

        //Create the directed acyclic graph object encoding the conditional independe relaionship among the variables of the model.
        DynamicDAG dag = new DynamicDAG(variables);

        //Define the parent set for each random variable
        dag.getParentSetTimeT(sensorT1).addParent(temperature);
        dag.getParentSetTimeT(sensorT2).addParent(temperature);

        dag.getParentSetTimeT(sensorSmoke).addParent(smoke);

        dag.getParentSetTimeT(temperature).addParent(fire);

        //Interface varible refers to the occurrence of this variable in the previous time step.
        dag.getParentSetTimeT(temperature).addParent(temperature.getInterfaceVariable());

        dag.getParentSetTimeT(smoke).addParent(fire);
        dag.getParentSetTimeT(smoke).addParent(smoke.getInterfaceVariable());

        dag.getParentSetTimeT(fire).addParent(fire.getInterfaceVariable());

        return dag;
    }
    public static void main(String[] args) {

        /********** DATA LOADING ***************/
        //Load the data set
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.open("./datasets/TimeIndexedSensorReadings.arff");
        Attributes attributes = data.getAttributes();

        /********** Model Definition ************/
        DynamicDAG dag = defineDynamicFireDetectorModel(attributes);

        //Print the dynamic DAG
        System.out.println(dag);
    }
}
