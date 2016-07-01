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

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.core.io.BayesianNetworkLoader;
import eu.amidst.dynamic.io.DynamicBayesianNetworkLoader;
import eu.amidst.dynamic.io.DynamicBayesianNetworkWriter;
import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.huginlink.io.BNLoaderFromHugin;
import eu.amidst.huginlink.io.DBNLoaderFromHugin;

import java.io.File;

/**
 * The FileConverterFromHuginToAmidst class converts a set of Hugin networks (static and dynamic) into AMIDST networks.
 */
public class FileConverterFromHuginToAmidst {

    /**
     * Converts a set the Hugin model files (dynamic and static) into AMIDST format.
     * @param folderName the path from which the conversion will be carried out (applied recursively to sub-folders too).
     * @throws Exception
     */
    public static void convertFilesFromFolder(final String folderName) throws Exception {

        File folder = new File(folderName);

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                convertFilesFromFolder(fileEntry.getName());
            } else {

                String amidstBNExtension = ".bn";
                String amidstDBNExtension = ".dbn";
                String huginBNExtension = ".net";
                String huginDBNExtension =".oobn";

                String fileName = fileEntry.getName();
                String fullFileName = folder.getName() + "/" + fileName;


                if (fileName.endsWith(huginBNExtension)) { //Static BN

                    String modelName = fileName.substring(0, fileName.length() - 4);
                    String amidstFileName = modelName + amidstBNExtension;
                    String fullAmidstFileName = folder.getName() + "/" + amidstFileName;

                    System.out.println("Converting " + fileName + " to " + amidstFileName);

                    System.out.println(fullFileName);

                    Domain huginBN = BNLoaderFromHugin.loadFromFile(fullFileName);
                    BayesianNetwork amidstBN = BNConverterToAMIDST.convertToAmidst(huginBN);
                    BayesianNetworkWriter.save(amidstBN, fullAmidstFileName);

                    //***************************************** TEST PART **********************************************
                    BayesianNetwork amidstBN2 = BayesianNetworkLoader.loadFromFile(fullAmidstFileName);
                    if(!amidstBN.equalBNs(amidstBN2, 0.0))
                       throw new Exception("Conversion from " + fileName + " to " + amidstFileName + " failed. ");
                    //**************************************************************************************************
                }

                if (fileName.endsWith(huginDBNExtension)) { //Dynamic BN
                    String modelName = fileName.substring(0, fileName.length() - 5);
                    String amidstFileName = modelName + amidstDBNExtension;
                    String fullAmidstFileName = folder.getName() + "/" + amidstFileName;

                    System.out.println("Converting " + fileName + " to " + amidstFileName);


                    Class huginDBN = DBNLoaderFromHugin.loadFromFile(fullFileName);
                    DynamicBayesianNetwork amidstDBN = DBNConverterToAmidst.convertToAmidst(huginDBN);
                    DynamicBayesianNetworkWriter.save(amidstDBN, fullAmidstFileName);

                    //***************************************** TEST PART **********************************************
                    DynamicBayesianNetwork amidstDBN2 = DynamicBayesianNetworkLoader.loadFromFile(fullAmidstFileName);
                    if(!amidstDBN.equalDBNs(amidstDBN2, 0.0))
                        throw new Exception("Conversion from " + fileName + " to " + amidstFileName + " failed. ");
                    //**************************************************************************************************
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        FileConverterFromHuginToAmidst.convertFilesFromFolder("networks/simulated/");
    }
}
