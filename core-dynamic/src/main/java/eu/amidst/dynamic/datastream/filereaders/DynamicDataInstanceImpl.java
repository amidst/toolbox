/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.dynamic.datastream.filereaders;

import eu.amidst.corestatic.datastream.Attribute;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.corestatic.datastream.filereaders.DataRow;

/**
 * Created by andresmasegosa on 11/11/14.
 */
class DynamicDataInstanceImpl implements DynamicDataInstance {

    private DataRow dataRowPresent;
    private DataRow dataRowPast;

    private int sequenceID;
    /**
     * The timeID of the Present
     */
    private int timeID;


    public DynamicDataInstanceImpl(DataRow dataRowPast1, DataRow dataRowPresent1, int sequenceID1, int timeID1){
        dataRowPresent = dataRowPresent1;
        dataRowPast =  dataRowPast1;
        this.sequenceID = sequenceID1;
        this.timeID = timeID1;
    }

    @Override
    public double getValue(Attribute att, boolean present) {
        if (present){
            return dataRowPresent.getValue(att);
        }else {
            return dataRowPast.getValue(att);
        }
    }

    @Override
    public void setValue(Attribute att, double value, boolean present) {
        if (present){
            dataRowPresent.setValue(att, value);
        }else {
            dataRowPast.setValue(att, value);
        }
    }

    @Override
    public double getValue(Attribute att) {
        return this.dataRowPresent.getValue(att);
    }

    @Override
    public void setValue(Attribute att, double val) {
        this.dataRowPresent.setValue(att,val);
    }

    @Override
    public int getSequenceID() {
        return sequenceID;
    }

    @Override
    public int getTimeID() {
        return timeID;
    }

}
