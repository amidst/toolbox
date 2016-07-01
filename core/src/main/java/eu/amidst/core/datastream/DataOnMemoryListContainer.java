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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * The DataOnMemoryListContainer class implements the {@link DataOnMemory} interface.
 * It stores all the {@link DataInstance} objects in a List.
 */
public class DataOnMemoryListContainer <E extends DataInstance> implements DataOnMemory<E> {

    /** Represents the list containing the data instances. */
    List<E> instanceList;

    /** Represents a pointer to the attributes of the data set. */
    Attributes attributes;

    /** Represents an ID*/
    double id=Double.NaN;

    /**
     * Creates a new DataOnMemoryListContainer initialized with the Attributes object of the data set.
     * @param attributes_ a list of attributes
     */
    public DataOnMemoryListContainer(Attributes attributes_){
        this.instanceList=new ArrayList();
        this.attributes=attributes_;
    }

    /**
     * Creates a new DataOnMemoryListContainer initialized with the Attributes object of the data set.
     * @param attributes_ a list of attributes
     */

    /**
     * Creates a new DataOnMemoryListContainer initialized with the Attributes object of the data set.
     * @param attributes_ a list of attributes
     * @param instanceList a list of data instances stored in the object.
     */
    public DataOnMemoryListContainer(Attributes attributes_, List<E> instanceList){
        this.instanceList=new ArrayList();
        for (E dataInstance: instanceList){
            this.instanceList.add(dataInstance);
        }
        this.attributes=attributes_;
    }


    /**
     * Adds a new DataInstance.
     * @param data the data instance to be added.
     */
    public void add(E data){ this.instanceList.add(data); }


    /**
     * Adds a list of DataInstances.
     * @param data the data instance to be added.
     */
    public void addAll(Collection<E> data){ this.instanceList.addAll(data); }

    /**
     * Adds a new DataInstance at a given position.
     * @param id the position where the data instance will be added.
     * @param data the data instance to be added.
     */
    public void set(int id, E data){
        this.instanceList.set(id, data);
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
     * Returns whether this DataOnMemoryListContainer can restart.
     * @return true if this DataOnMemoryListContainer can restart, false otherwise.
     */
    @Override
    public boolean isRestartable() {
        return true;
    }

    /**
     * Restarts this DataOnMemoryListContainer.
     */
    @Override
    public void restart() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        for (E e : instanceList) {
            builder.append(e.toString());
            builder.append("\n");
        }
        return builder.toString();
    }

    public void setId(double id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getBatchID() {
        return id;
    }
}
