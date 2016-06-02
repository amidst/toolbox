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

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.converters.BNConverterToHugin;

/**
 * This class is a writer to create Hugin Bayesian network files from AMIDST Bayesian networks.
 */
public class BayesianNetworkWriterToHugin {

    /**
     * Creates a Hugin Bayesian network file from a AMIDST Bayesian network model.
     * @param amidstBN the {@link eu.amidst.core.models.BayesianNetwork} in AMIDST format.
     * @param file the name of the Hugin file in which the model will be saved.
     * @throws ExceptionHugin
     */
    public static void save(BayesianNetwork amidstBN, String file) throws ExceptionHugin {
        Domain huginBN = BNConverterToHugin.convertToHugin(amidstBN);
        huginBN.saveAsNet(file);
    }
}
