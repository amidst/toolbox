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
package eu.amidst.examples.utils;


import eu.amidst.corestatic.io.BayesianNetworkWriter;
import eu.amidst.corestatic.models.BayesianNetwork;
import eu.amidst.corestatic.utils.BayesianNetworkGenerator;

import java.io.IOException;

/**
 *
 * This example shows how to use the BayesianNetworkGenerator static class to randomly generate Bayesian networks.
 *
 * Created by andresmasegosa on 18/6/15.
 */
public class BayesianNetworkGeneratorExample {

    public static void main(String[] agrs) throws IOException, ClassNotFoundException {

        //We set the number of Normally distributed variables
        BayesianNetworkGenerator.setNumberOfGaussianVars(5);

        //We set the number of multinomial variables and their number of states
        BayesianNetworkGenerator.setNumberOfMultinomialVars(5, 2);

        //We set the number of links
        BayesianNetworkGenerator.setNumberOfLinks(15);

        //We set the seed
        BayesianNetworkGenerator.setSeed(0);

        //This method randomly generate a Bayesian network with the aformentioned features
        BayesianNetwork bayesianNetwork = BayesianNetworkGenerator.generateBayesianNetwork();

        //Now we print it
        System.out.println(bayesianNetwork.toString());

        //Finally we save the model to a file
        BayesianNetworkWriter.saveToFile(bayesianNetwork, "networks/Bayesian10Vars15Links.bn");

    }

}
