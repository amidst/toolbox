/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */
package eu.amidst.huginlink.examples.io;


import COM.hugin.HAPI.Domain;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.io.BNLoaderFromHugin;
import eu.amidst.huginlink.io.BayesianNetworkWriterToHugin;

/**
 * This example shows how to use the BNLoaderFromHugin and BayesianNetworkWriterToHugin classes to load and
 * write Bayesian networks in Hugin format, respectively.
 */
public class BNLoaderWriterExample {
    public static void main(String[] args) throws Exception {

        //Load from Hugin format
        Domain huginBN = BNLoaderFromHugin.loadFromFile("networks/dataWeka/asia.net");

        //Save a AMIDST BayesianNetwork to Hugin format
        BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
        BayesianNetworkWriterToHugin.save(amidstBN,"networks/simulated/tmp.net");
    }
}
