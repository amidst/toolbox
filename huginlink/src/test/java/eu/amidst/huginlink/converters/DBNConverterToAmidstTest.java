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

import COM.hugin.HAPI.Class;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.huginlink.io.DBNLoaderFromHugin;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by afa on 12/1/15.
 */
public class DBNConverterToAmidstTest {

    @Before
    public void setUp() throws ExceptionHugin, IOException {

        /*DynamicBayesianNetwork amidstDBN = DBNExample.getAmidst_DBN_Example();
        System.out.println("\nConverting the AMIDST Dynamic BN into Hugin format ...");
        Class huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);
        //The name of the DBN must be the same as the name of the .net file !!!
        String nameModel = "huginDBNFromAMIDST";
        huginDBN.setName(nameModel);
        String outFile = new String("networks/"+nameModel+".oobn");
        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");

        System.out.println("\nConverting the Hugin Dynamic BN into AMIDST format ...");
        amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);

        System.out.println("\nConverting the AMIDST Dynamic BN into Hugin format ...");
        huginDBN = DBNConverterToHugin.convertToHugin(amidstDBN);

        nameModel = "huginDBNFromAMIDST2";
        //The name of the DBN must be the same as the name of the .net file !!!
        huginDBN.setName(nameModel);
        outFile = new String("networks/"+nameModel+".oobn");
        huginDBN.saveAsNet(outFile);
        System.out.println("Hugin network saved in \"" + outFile + "\"" + ".");*/

        Class huginDBN = DBNLoaderFromHugin.loadFromFile("../networks/bnaic2015/BCC/CajamarDBN.oobn");
        DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
        DynamicBayesianNetworkWriter.save(amidstDBN, "../networks/bnaic2015/BCC/CajamarDBN.dbn");

    }

    @Test
    public void testModels() throws ExceptionHugin {

    }
}