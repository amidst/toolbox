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

package eu.amidst.huginlink.io;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;

/**
 * This class is a writer of dynamic Bayesian networks in AMIDST format to Hugin files.
 */
public class DynamicBayesianNetworkWriterToHugin {

    /**
     * Creates a Hugin Bayesian network file from a AMIDST Dynamic Bayesian network model.
     * In order to simplify, we assume that the Hugin DBN model name is the file name without extension.
     * @param amidstDBN the <code>DynamicBayesianNetwork</code> in AMIDST format.
     * @param file the name of the Hugin file in which the model is saved.
     * @throws ExceptionHugin
     */
    public static void save(DynamicBayesianNetwork amidstDBN, String file) throws ExceptionHugin {


        BayesianNetworkWriterToHugin.save(amidstDBN.toBayesianNetworkTimeT(),file);

        /*
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);

        //The network name must be the same as the file name (without .dbn)
        String[] parts = file.split("/");
        String fileName = parts[parts.length-1];
        String networkName = fileName.substring(0,fileName.length()-5);

        System.out.println(networkName);
        huginDBN.setName(networkName);

        huginDBN.saveAsNet(file);
        */

    }
}
