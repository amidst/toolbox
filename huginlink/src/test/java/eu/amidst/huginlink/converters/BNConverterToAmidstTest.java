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

package eu.amidst.huginlink.converters;


import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.huginlink.io.BNLoaderFromHugin;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by afa on 19/11/14.
 */
public class BNConverterToAmidstTest {

    @Before
    public void setUp() throws ExceptionHugin, IOException {

        /*ParseListener parseListener = new DefaultClassParseListener();
        Domain huginBN = new Domain ("networks/huginNetworkFromAMIDST.net", parseListener);
        System.out.println("\n\nConverting the Hugin network into AMIDST format ...");
        BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
        System.out.println("\nAMIDST network object created.");

        Domain huginBN2 = new Domain ("networks/asia.net", parseListener);
        BayesianNetwork amidstBN2 = BNConverterToAMIDST.convertToAmidst(huginBN2);
        System.out.println("\nAMIDST network object created.");

        BayesianNetworkWriterToHugin.save(amidstBN2, "networks/asia.bn");

        parseListener = new DefaultClassParseListener();
        huginBN = new Domain ("networks/IS.net", parseListener);*/

        Domain huginBN = BNLoaderFromHugin.loadFromFile("../networks/dataWeka/asia.net");
        BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
        BayesianNetworkWriter.save(amidstBN, "../networks/dataWeka/asia.bn");

    }

    @Test
    public void test() throws ExceptionHugin {


    }


}
