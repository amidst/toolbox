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

import eu.amidst.dynamic.constraints.Constraint;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.models.DynamicDAG;

import java.io.IOException;

/**
 * Created by andresmasegosa on 16/01/2018.
 */
public class B_ModelLearning {
    public static void main(String[] args) throws IOException {
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.open("./datasets/TimeIndexedSensorReadings.arff");
        Attributes attributes = data.getAttributes();
        DynamicDAG fireDetectorModel = A_ModelDefinition.defineDynamicFireDetectorModel(attributes);

        /********** Model Learning ************/
        //Define the learning engine (Streaming Variational Bayes for dynamic models) and the parameters
        SVB svb = new SVB();
        svb.setDynamicDAG(fireDetectorModel);
        svb.setOutput(true);
        svb.setWindowsSize(1000);

        //Specify the associated constraints (econding prior knowledge)
        Variable sensorT1 = fireDetectorModel.getDynamicVariables().getVariableByName("SensorTemp1");
        Variable sensorT2 = fireDetectorModel.getDynamicVariables().getVariableByName("SensorTemp2");
        svb.addParameterConstraint(new Constraint("alpha", sensorT1, 0.0, true));
        svb.addParameterConstraint(new Constraint("alpha", sensorT2, 0.0, true));
        svb.addParameterConstraint(new Constraint("beta1", sensorT1, 1.0, true));
        svb.addParameterConstraint(new Constraint("beta1", sensorT2, 1.0, true));

        svb.addParameterConstraint(new Constraint("alpha", sensorT1, 0.0, false));
        svb.addParameterConstraint(new Constraint("alpha", sensorT2, 0.0, false));
        svb.addParameterConstraint(new Constraint("beta1", sensorT1, 1.0, false));
        svb.addParameterConstraint(new Constraint("beta1", sensorT2, 1.0, false));

        Variable temp = fireDetectorModel.getDynamicVariables().getVariableByName("Temperature");
        svb.addParameterConstraint(new Constraint("alpha | Fire = 0", temp, 0.0, false));
        svb.addParameterConstraint(new Constraint("beta1 | Fire = 0", temp, 1.0, false));

        //Set-up the learning phase
        svb.initLearning();

        //Perform Learning
        svb.updateModel(data);

        //Get the learnt model
        DynamicBayesianNetwork fireDectector = svb.getLearntDBN();

        //Print the model
        System.out.println(fireDectector);

        //Save to disk the learnt model
        DynamicBayesianNetworkWriter.save(fireDectector,"./models/DynamicFireDetectorModel.dbn");

    }
}
