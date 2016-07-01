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

import COM.hugin.HAPI.DefaultClassParseListener;
import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import COM.hugin.HAPI.ParseListener;

/**
 * This class is a loader to create AMIDST Bayesian networks from Hugin Bayesian network files.
 */
public class BNLoaderFromHugin {

    /**
     * Creates an AMIDST Bayesian network model from a Hugin Bayesian network stored in a file.
     * @param file the name of the file including the Hugin model.
     * @return a {@link eu.amidst.core.models.BayesianNetwork} in AMIDST format.
     * @throws ExceptionHugin
     */
    public static Domain loadFromFile(String file) throws ExceptionHugin {
        ParseListener parseListener = new DefaultClassParseListener();
        Domain huginBN = new Domain (file, parseListener);
        return huginBN;
    }
}

