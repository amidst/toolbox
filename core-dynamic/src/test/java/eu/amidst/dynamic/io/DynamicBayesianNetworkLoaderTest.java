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

package eu.amidst.dynamic.io;

import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by afa on 11/12/14.
 */
public class DynamicBayesianNetworkLoaderTest {

    @Before
    public void setUp() throws IOException, ClassNotFoundException {

    }

    @Test
    public void test() throws Exception {
        DynamicBayesianNetworkLoaderTest.loadAndTestFilesFromFolder("../networks/simulated");
    }

    public static void loadAndTestFilesFromFolder(final String folderName) throws Exception {

        File folder = new File(folderName);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory())
                continue;

            String fileName = fileEntry.getName();
            String fullFileName = folderName + "/" + fileName;

            System.out.println("reading "+fileName);

            if (fileName.endsWith(".dbn")) { //Dynamic BN

                try {
                    DynamicBayesianNetwork amidstDBN = DynamicBayesianNetworkLoader.loadFromFile(fullFileName);
                    DynamicBayesianNetworkWriter.save(amidstDBN, fullFileName);

                    DynamicBayesianNetwork amidstDBN2 = DynamicBayesianNetworkLoader.loadFromFile(fullFileName);

                    if (!amidstDBN.equalDBNs(amidstDBN2, 0.0))
                        throw new Exception("Dynamic Bayesian network loader for " + fileName + " failed. ");
                }catch(java.lang.ClassCastException ex){
                    System.out.println("WARNING: "+fileName+"");

                }

            }

        }
    }
}
