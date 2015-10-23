/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.classifiers;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;

/**
 * The Classifier interface is defined for Bayesian classification models.
 */
public interface Classifier {

    /**
     * Predicts the class membership probabilities for a given instance.
     * @param instance the data instance to be classified.
     * @return an array of double containing the estimated membership probabilities of the data instance for each class label.
     */
    double[] predict(DataInstance instance);

    /**
     * Returns the name of the class variable.
     * @return the name of the class variable.
     */
    String getClassName();

    /**
     * Sets the name of the class variable.
     * @param className the name of the class variable.
     */
    void setClassName(String className);

    /**
     * Trains the classifier using the input data streams.
     * @param dataStream a data stream {@link DataStream}.
     */
    void learn(DataStream<DataInstance> dataStream);

}
