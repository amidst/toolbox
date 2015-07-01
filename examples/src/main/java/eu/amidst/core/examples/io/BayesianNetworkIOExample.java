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
package eu.amidst.core.examples.io;


import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;

import java.util.Random;

/**
 *
 * In this example we show how to load and save Bayesian networks models for a binary file with ".bn" extension. In
 * this toolbox Bayesian networks models are saved as serialized objects.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class BayesianNetworkIOExample {

    public static void main(String[] args) throws Exception {

        //We can load a Bayesian network using the static class BayesianNetworkLoader
        BayesianNetwork bn = BayesianNetworkLoader.loadFromFile("./networks/WasteIncinerator.bn");

        //Now we print the loaded model
        System.out.println(bn.toString());

        //Now we change the parameters of the model
        bn.randomInitialization(new Random(0));

        //We can save this Bayesian network to using the static class BayesianNetworkWriter
        BayesianNetworkWriter.saveToFile(bn, "networks/tmp.bn");

    }
}
