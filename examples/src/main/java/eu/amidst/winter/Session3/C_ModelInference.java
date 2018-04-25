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

import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.inference.ImportanceSampling;
import eu.amidst.core.inference.InferenceAlgorithm;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.apache.commons.math3.analysis.function.Gaussian;

/**
 *
 * Session 3. Make inference (i.e. predictions) with the learnt model.
 *
 * Created by andresmasegosa on 13/01/2018.
 */
public class C_ModelInference {

    static double[] sensorTemp1Evidence = {22};
    static double[] sensorTemp2Evidence = {23};
    static double[] sensorSmokeEvidence = {5};

    public static void main(String[] args) throws Exception {
        //Load the learnt model
        BayesianNetwork fireDetector = BayesianNetworkLoader.loadFromFile("./models/FireDetectorModel.bn");

        //Set-up the variable of interest.
        Variable fire = fireDetector.getVariables().getVariableByName("Fire");
        Variable temperature = fireDetector.getVariables().getVariableByName("Temperature");
        Variable sensorTemp1 = fireDetector.getVariables().getVariableByName("SensorTemp1");
        Variable sensorTemp2 = fireDetector.getVariables().getVariableByName("SensorTemp2");
        Variable sensorSmoke = fireDetector.getVariables().getVariableByName("SensorSmoke");

        //Create an instance of a inference algorithm. In this case, we use the Importance Sampling .
        InferenceAlgorithm inferenceAlgorithm = new ImportanceSampling();

        //Then, we set the BN model
        inferenceAlgorithm.setModel(fireDetector);

        //Prepare the evidence object
        Assignment assignment = new HashMapAssignment(3);

        for (int i = 0; i < sensorTemp1Evidence.length; i++) {
            //Set the values of the evidence for the given variables.
            assignment.setValue(sensorTemp1,sensorTemp1Evidence[i]);
            assignment.setValue(sensorTemp2,sensorTemp2Evidence[i]);
            assignment.setValue(sensorSmoke,sensorSmokeEvidence[i]);
            inferenceAlgorithm.setEvidence(assignment);

            //Perform the inference
            inferenceAlgorithm.runInference();

            //Retrieve and print the results of the inference.
            System.out.println("Temperature Sensor Readings: "+ sensorTemp1Evidence[i] +" "+ sensorTemp2Evidence[i]);
            System.out.println("Smoke Sensor Reading:" + sensorSmokeEvidence[i]);

            Normal temp = inferenceAlgorithm.getPosterior(temperature);
            System.out.println("Estimated Temperature: " + temp.getMean() +", "+temp.getVariance());

            Multinomial multinomial = inferenceAlgorithm.getPosterior(fire);
            System.out.println("Probability of Fire: " + multinomial.getProbabilityOfState(1));

        }
    }
}
