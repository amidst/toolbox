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

import eu.amidst.core.constraints.Constraint;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

/**
 * Session 5. Define, learn and update the fire detector model.
 * Created by andresmasegosa on 14/01/2018.
 */
public class A_ModelUpdating {

    /**
     * Define the Fire Dectector Model's DAG.
     * @param attributes
     * @return
     */
    public static DAG creatFireDectectorModel(Attributes attributes){

        /********** Model Definition ************/
        //Create the object handling the random variables of the model
        Variables variables = new Variables();

        //Create the random variables of the model. Some of them are associated to one attribute to retrieve its observed values from the data set.
        Variable fire = variables.newMultinomialVariable(attributes.getAttributeByName("Fire"));
        Variable temperature = variables.newGaussianVariable("Temperature");
        Variable smoke = variables.newMultinomialVariable("Smoke",2);
        Variable sensorT1 = variables.newGaussianVariable(attributes.getAttributeByName("SensorTemp1"));
        Variable sensorT2 = variables.newGaussianVariable(attributes.getAttributeByName("SensorTemp2"));
        Variable sensorSmoke = variables.newGaussianVariable(attributes.getAttributeByName("SensorSmoke"));

        //Create the directed acyclic graph object encoding the conditional independe relaionship among the variables of the model.
        DAG dag = new DAG(variables);

        //Define the parent set for each random variable
        dag.getParentSet(sensorT1).addParent(temperature);
        dag.getParentSet(sensorT2).addParent(temperature);

        dag.getParentSet(sensorSmoke).addParent(smoke);

        dag.getParentSet(temperature).addParent(fire);
        dag.getParentSet(smoke).addParent(fire);

        return dag;
    }

    public static void main(String[] args) throws Exception {

        /********** DATA LOADING ***************/
        //Load the data set
        DataStream data = DataStreamLoader.open("./datasets/bymonth/sensorReadingsJanuary.arff");

        /********** Model Definition ************/
        DAG fireDetectorModel = creatFireDectectorModel(data.getAttributes());

        /********** Model Learning ************/
        //Define the learning engine (Streaming Variational Bayes) and the parameters
        SVB svb = new SVB();
        svb.setDAG(fireDetectorModel);
        svb.setOutput(true);
        svb.setWindowsSize(1000);

        //Specify the associated constraints (econding prior knowledge)
        Variable sensorT1 = fireDetectorModel.getVariables().getVariableByName("SensorTemp1");
        Variable sensorT2 = fireDetectorModel.getVariables().getVariableByName("SensorTemp2");
        svb.addParameterConstraint(new Constraint("alpha", sensorT1, 0.0));
        svb.addParameterConstraint(new Constraint("alpha", sensorT2, 0.0));
        svb.addParameterConstraint(new Constraint("beta1", sensorT1, 1.0));
        svb.addParameterConstraint(new Constraint("beta1", sensorT2, 1.0));

        //Set-up the learning phase
        svb.initLearning();

        //Month names
        String[] monthName = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        double[] tempsMonth = new double[12];

        //For-loop iterating over the 12 datasets containing the sensor reading for each of the months.
        for (int i = 0; i < monthName.length; i++) {
            System.out.println("------------Fire Detector Model at Month: " + monthName[i] + "-----------------");

            //Load the data set
            data = DataStreamLoader.open("./datasets/bymonth/sensorReadings" + monthName[i] + ".arff");

            //Perform Learning
            svb.updateModel(data);

            //Get the learnt model
            BayesianNetwork model = svb.getLearntBayesianNetwork();
            System.out.println(model);

            //Access the estimated indoor temperature
            Normal_MultinomialParents dist = model.getConditionalDistribution(fireDetectorModel.getVariables().getVariableByName("Temperature"));
            tempsMonth[i] = dist.getNormal(0).getMean();
        }

        //Output the estimated indoor temperature at every month
        for (int i = 0; i < monthName.length; i++) {
            System.out.println("Estimated Temperature at month "+monthName[i] +": " + tempsMonth[i]);
        }
    }
}
