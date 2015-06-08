/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream.filereaders;

import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by andresmasegosa on 11/11/14.
 */
public class DataOnMemoryFromFile implements DataOnMemory<DataInstance> {

    private DataFileReader reader;

    /**
     * We assume that the dataset here is going to be relatively small and that it is going to be read multiple times
     * so it is better to store it in an array, otherwise it might be just better to keep it in the ArrayList and avoid
     * the extra pass in the constructor.
     */
    private DataInstanceImpl[] dataInstances;


    public DataOnMemoryFromFile(DataFileReader reader) {
        this.reader = reader;

        List<DataInstanceImpl> dataInstancesList = new ArrayList<>();

        for (DataRow row: reader){
            dataInstancesList.add(new DataInstanceImpl(row));
        }
        reader.restart();

        dataInstances = new DataInstanceImpl[dataInstancesList.size()];
        int counter = 0;
        for (DataInstanceImpl inst : dataInstancesList) {
            dataInstances[counter] = inst;
            counter++;
        }

    }

    @Override
    public int getNumberOfDataInstances() {
        return dataInstances.length;
    }

    @Override
    public DataInstance getDataInstance(int i) {
        return dataInstances[i];
    }

    @Override
    public List<DataInstance> getList() {
        return Arrays.asList(this.dataInstances);
    }


    @Override
    public Attributes getAttributes() {
        return this.reader.getAttributes();
    }

    @Override
    public Stream<DataInstance> stream() {
        return Arrays.stream(this.dataInstances);
    }

    @Override
    public void close() {
        this.reader.close();
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {

    }
}