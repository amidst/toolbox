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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * This class allows to save a {@link DynamicBayesianNetwork} model in a file.
 */
public class DynamicBayesianNetworkWriter {

    /**
     * Saves a {@link DynamicBayesianNetwork} model in a file.
     * @param dbn a {@link DynamicBayesianNetwork} model.
     * @param fileName a name of a file where the Dynamic Bayesian network will be saved.
     * @throws IOException in case of an error occurs while writing to the file.
     */
    public static void save(DynamicBayesianNetwork dbn, String fileName) throws IOException {

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
        out.writeObject(dbn);
        out.close();
    }
}
