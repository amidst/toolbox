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

package eu.amidst.core.datastream;

import java.util.List;

/**
 * The DataOnMemory interface is a specialization of the {@link DataStream} interface that keeps all the data on main memory.
 * <p> This class is designed to provide a random access over the set of {@link DataInstance} in the data set.
 * It is also used to deal with the mini-batches of the data set.</p>
 */
public interface DataOnMemory<E extends DataInstance> extends DataStream<E> {

    /**
     * Returns the number of data instances in the data set.
     * @return a positive integer that represents the total number of data instances.
     */
    int getNumberOfDataInstances();

    /**
     * Returns the data instance in the i-th position.
     * @param i a positive index that represents the position of a data instance.
     * @return a data instance.
     */
    E getDataInstance(int i);

    /**
     * Returns a list with all the {@link DataInstance} objects in the data set.
     * @return a list of data instances.
     */
    List<E> getList();


    /**
     * Returns an ID for the data set.
     */
    default double getBatchID(){
        return Double.NaN;
    }
}

