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

package eu.amidst.winter.Session2;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.io.DynamicDataStreamLoader;
import eu.amidst.latentvariablemodels.dynamicmodels.AutoRegressiveHMM;
import eu.amidst.latentvariablemodels.dynamicmodels.DynamicModel;
import eu.amidst.latentvariablemodels.dynamicmodels.KalmanFilter;
import eu.amidst.latentvariablemodels.dynamicmodels.SwitchingKalmanFilter;

/**
 *  Session 2.
 * Created by andresmasegosa on 16/01/2018.
 */
public class F_AutoRegressiveHMM {
    public static void main(String[] args) {
        //Load the data set
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.open("./datasets/artificialDataset.arff");

        //Define the model (internally the skeleton is fixed)
        DynamicModel model = new AutoRegressiveHMM(data.getAttributes());

        //Print the skeleton of the model
        System.out.println(model.getDynamicDAG());

        //Learnt the parameters of the model
        model.updateModel(data);

        //Print the learnt model
        System.out.println(model.getModel());
    }
}
