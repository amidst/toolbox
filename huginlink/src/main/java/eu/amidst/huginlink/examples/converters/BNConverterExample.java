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

package eu.amidst.huginlink.examples.converters;


import COM.hugin.HAPI.Domain;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToAMIDST;
import eu.amidst.huginlink.converters.BNConverterToHugin;
import eu.amidst.huginlink.io.BNLoaderFromHugin;

/**
 * This example shows how to use the class BNConverterToAMIDST and BNConverterToHugin
 * to convert a Bayesian network models from Hugin to AMIDST and vice versa, respectively.
 */
public class BNConverterExample {

    public static void main(String[] args) throws Exception {

        //loads a BN model from Hugin
        Domain huginBN = BNLoaderFromHugin.loadFromFile("networks/dataWeka/asia.net");

        //Converts the Hugin model to an AMIDST BayesianNetwork object
        BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);

        //Converts the AMIDST BayesianNetwork object to a Hugin Bayesian Network object
        huginBN = BNConverterToHugin.convertToHugin(amidstBN);

        System.out.println(amidstBN.toString());
        System.out.println(huginBN.toString());
    }
}
