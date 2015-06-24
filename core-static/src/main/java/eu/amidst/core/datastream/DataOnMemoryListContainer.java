/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 *
 * This class implements a {@link DataOnMemory} interface. It stores all the {@link DataInstance} objects
 * in a List.
 *
 */
public class DataOnMemoryListContainer <E extends DataInstance> implements DataOnMemory<E> {

    /** The list containing the data instances*/
    List<E> instanceList;

    /** A pointer to the attributes of the data set*/
    Attributes attributes;

    /**
     * A constructor which is initilized with the Attributes object of the data set.
     * @param attributes_
     */
    public DataOnMemoryListContainer(Attributes attributes_){
        this.instanceList=new ArrayList();
        this.attributes=attributes_;
    }

    /**
     * A method for adding a new DataInstance
     * @param data
     */
    public void add(E data){
        this.instanceList.add(data);
    }

    /**
     * A method for adding a new DataInstance at a given position
     * @param id
     * @param data
     */
    public void set(int id, E data){
        this.instanceList.set(id,data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfDataInstances() {
        return this.instanceList.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E getDataInstance(int i) {
        return this.instanceList.get(i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<E> getList() {
        return this.instanceList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<E> stream() {
        return this.instanceList.stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {

    }

    /**
     * This data stream implementation can be restarted.
     * @return
     */
    @Override
    public boolean isRestartable() {
        return true;
    }

    /**
     * This method restarts the data stream
     */
    @Override
    public void restart() {

    }

}
