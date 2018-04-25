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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * This class allows to load a {@link DynamicBayesianNetwork} model from a file.
 */
public class DynamicBayesianNetworkLoader {

    /**
     * Loads a {@link DynamicBayesianNetwork} from a file.
     * @param fileName a name of the file from which the Dynamic Bayesian network will be loaded.
     * @return a {@link DynamicBayesianNetwork} model.
     * @throws ClassNotFoundException in case the class is not found.
     * @throws IOException in case of an error occurs while reading the file.
     */
    public static DynamicBayesianNetwork loadFromFile(String fileName) throws ClassNotFoundException, IOException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
        Object obj = ois.readObject();
        ois.close();
        return (DynamicBayesianNetwork)obj;
    }

}
