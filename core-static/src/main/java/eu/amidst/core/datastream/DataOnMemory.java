/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream;

import java.util.List;

/**
 *
 *  This class is an specialization of the {@link DataStream} class which keeps all the data on main memory. <p>
 *
 *  This class is designed to provide random accessing to {@link DataInstance} composing the data set. It is also
 *  widely used to deal with mini-batches of data.
 *
 */
public interface DataOnMemory<E extends DataInstance> extends DataStream<E> {

    /**
     * Return the number of data instances of the data set.
     * @return a positive integer
     */
    int getNumberOfDataInstances();

    /**
     * Return the data instance in the i-th positon.
     * @param i, a positive index
     * @return
     */
    E getDataInstance(int i);

    /**
     * Return a list with all the {@link DataInstance} objects of the data set.
     * @return
     */
    List<E> getList();

}

