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
 * Created by andresmasegosa on 11/12/14.
 */
public class DataOnMemoryListContainer <E extends DataInstance> implements DataOnMemory<E> {
    List<E> instanceList;
    Attributes attributes;

    public DataOnMemoryListContainer(Attributes attributes_){
        this.instanceList=new ArrayList();
        this.attributes=attributes_;
    }

    public void add(E data){
        this.instanceList.add(data);
    }

    public void set(int id, E data){
        this.instanceList.set(id,data);
    }

    @Override
    public int getNumberOfDataInstances() {
        return this.instanceList.size();
    }

    @Override
    public E getDataInstance(int i) {
        return this.instanceList.get(i);
    }

    @Override
    public List<E> getList() {
        return this.instanceList;
    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    @Override
    public Stream<E> stream() {
        return this.instanceList.stream();
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {

    }

}
