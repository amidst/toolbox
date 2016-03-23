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

package eu.amidst.flinklink.core.data;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import org.apache.flink.api.java.DataSet;

import java.io.Serializable;

/**
 * Created by andresmasegosa on 22/09/15.
 */
public class DataFlinkConverter implements Serializable {
    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    public static DataFlink<DataInstance> convertToStatic(DataFlink<DynamicDataInstance> dataFlink){
        return new DataFlinkTmp(dataFlink);
    }

    public static DataFlink<DynamicDataInstance> convertToDynamic(DataFlink<DataInstance> data){
        if (data.getAttributes().getSeq_id()==null && data.getAttributes().getTime_id()==null){
            throw new IllegalArgumentException("Data with no seq_id and time_id attribute can not be converged");
        }
        return new DataWrapper(data);
    }

    private static class DataWrapper implements DataFlink<DynamicDataInstance>, Serializable{
        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = 4107783324901370839L;

        DataFlink<DataInstance> data;

        public DataWrapper(DataFlink<DataInstance> data) {
            this.data = data;
        }

        @Override
        public String getName() {
            return data.getName();
        }

        @Override
        public Attributes getAttributes() {
            return data.getAttributes();
        }

        @Override
        public DataSet<DynamicDataInstance> getDataSet() {
            return this.data.getDataSet().map(d -> new DynamicDataInstanceWrapper(d));
        }
    }

    private static class DynamicDataInstanceWrapper implements DynamicDataInstance, Serializable{

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = 4107783324901370839L;

        DataInstance dataInstance;
        Attribute seqID;
        Attribute timeID;

        public DynamicDataInstanceWrapper(DataInstance dataInstance) {
            this.dataInstance = dataInstance;
            seqID = this.dataInstance.getAttributes().getSeq_id();
            timeID = this.dataInstance.getAttributes().getTime_id();
        }

        @Override
        public long getSequenceID() {
            return (int)this.dataInstance.getValue(seqID);
        }

        @Override
        public long getTimeID() {
            return (int)this.dataInstance.getValue(timeID);
        }

        @Override
        public double getValue(Attribute att, boolean present) {
            if (!present)
                throw new UnsupportedOperationException("This implementation can not query past evidence");
            return this.dataInstance.getValue(att);
        }

        @Override
        public void setValue(Attribute att, double val, boolean present) {
            if (!present)
                throw new UnsupportedOperationException("This implementation can not query past evidence");
            this.dataInstance.setValue(att,val);
        }

        @Override
        public Attributes getAttributes() {
            return this.dataInstance.getAttributes();
        }

        @Override
        public double[] toArray() {
            return this.dataInstance.toArray();
        }

        @Override
        public String toString(){
            return this.dataInstance.toString();
        }
    }

    private static class DataFlinkTmp implements DataFlink<DataInstance>, Serializable{
        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = 4107783324901370839L;

        DataFlink<DynamicDataInstance> data;

        private DataFlinkTmp(DataFlink<DynamicDataInstance> data) {
            this.data = data;
        }

        @Override
        public String getName() {
            return data.getName();
        }

        @Override
        public Attributes getAttributes() {
            return data.getAttributes();
        }

        @Override
        public DataSet<DataInstance> getDataSet() {
            return data.getDataSet().map(d -> d);
        }
    }
}
