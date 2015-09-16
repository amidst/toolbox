/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */
package eu.amidst.flinklink.core.io;


import eu.amidst.core.datastream.*;
import eu.amidst.core.datastream.filereaders.DataInstanceFromDataRow;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.datastream.filereaders.arffFileReader.DataRowWeka;
import eu.amidst.flinklink.core.data.DataFlink;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.Utils;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 1/9/15.
 */
public class DataSetLoader {


    public static String RELATION_NAME="RELATION_NAME";
    public static String ATTRIBUTES_NAME="ATTRIBUTES";

    Attributes attributes;

    String relationName;
    String pathFileData;

    public static DataFlink<DataInstance> loadData(String pathFileData){
        DataSetLoader loader = new DataSetLoader();
        loader.setPathFileData(pathFileData);
        return new DataFlinkFile(loader);
    }

    private DataSet<DataInstance> loadDataSet(){
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        if (attributes==null)
            this.loadHeader();

        DataSet<Attributes> attsDataSet = env.fromElements(attributes);

        DataSource<String> data = env.readTextFile(pathFileData);

        Configuration config = new Configuration();
        config.setString(DataSetLoader.RELATION_NAME, this.relationName);


        return  data
                .filter(w -> !w.isEmpty())
                .filter(w -> !w.startsWith("%"))
                .filter(line -> !line.startsWith("@attribute"))
                .filter(line -> !line.startsWith("@relation"))
                .filter(line -> !line.startsWith("@data"))
                .map(new DataInstanceBuilder())
                .withParameters(config)
                .withBroadcastSet(attsDataSet, DataSetLoader.ATTRIBUTES_NAME + "_" + this.relationName);
    }

    private DataSet<DataOnMemory<DataInstance>> loadSetOfDataBatches(int batchSize) {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        if (attributes==null)
            this.loadHeader();

        DataSet<Attributes> attsDataSet = env.fromElements(attributes);

        DataSet<String> data = new DataSource<String>(env, new BatchInputFormat(new Path(pathFileData), batchSize), BasicTypeInfo.STRING_TYPE_INFO, Utils.getCallLocationName());

        Configuration config = new Configuration();
        config.setString(DataSetLoader.RELATION_NAME, this.relationName);

        return  data
                .map(new DataBatchBuilder())
                .withParameters(config)
                .withBroadcastSet(attsDataSet, DataSetLoader.ATTRIBUTES_NAME + "_" + this.relationName);
    }

    private void setPathFileData(String pathFileData) {
        this.pathFileData = pathFileData;
    }

    private Attributes getAttributes() {
        return attributes;
    }

    private void loadHeader(){
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        DataSource<String> headerLines = env.readTextFile(pathFileData);
        List<String> relationNameList;

        try {
            relationNameList = headerLines
                    .setParallelism(1)
                    .filter(w -> !w.isEmpty())
                    .filter(w -> !w.startsWith("%"))
                    .filter(line -> line.startsWith("@relation"))
                    .first(1)
                    .collect();
        }catch (Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

        if (relationNameList.size() == 0)
            throw new IllegalArgumentException("ARFF file does not start with a @relation line.");


        try {
            relationName = relationNameList.get(0).split(" ")[1];


            headerLines = env.readTextFile(pathFileData);


            List<String> attLines = headerLines
                    .setParallelism(1)
                    .filter(w -> !w.isEmpty())
                    .filter(w -> !w.startsWith("%"))
                    .filter(line -> line.startsWith("@attribute"))
                    .collect();

            List<Attribute> atts = IntStream.range(0, attLines.size())
                    .mapToObj(i -> ARFFDataReader.createAttributeFromLine(i, attLines.get(i)))
                    .collect(Collectors.toList());

            attributes = new Attributes(atts);
        }catch (Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }

    private static class DataInstanceBuilder extends RichMapFunction<String, DataInstance> {


        Attributes attributes;

        @Override
        public DataInstance map(String value) throws Exception {
            return new DataInstanceFromDataRow(new DataRowWeka(attributes,value));
        }

        @Override
        public void open(Configuration parameters) throws Exception{
            super.open(parameters);
            String relationName = parameters.getString(DataSetLoader.RELATION_NAME,"");
            Collection<Attributes> collection = getRuntimeContext().getBroadcastVariable(DataSetLoader.ATTRIBUTES_NAME+"_"+relationName);
            attributes  = collection.iterator().next();
        }

    }

    private static class DataBatchBuilder extends RichMapFunction<String, DataOnMemory<DataInstance>> {


        Attributes attributes;

        @Override
        public DataOnMemory<DataInstance> map(String value) throws Exception {
            DataOnMemoryListContainer list = new DataOnMemoryListContainer(attributes);

            String[] vals = value.split("\\|");
            for (int i = 0; i < vals.length; i++) {
                  list.add(new DataInstanceFromDataRow(new DataRowWeka(attributes,vals[i])));
            }
            return list;
        }

        @Override
        public void open(Configuration parameters) throws Exception{
            super.open(parameters);
            String relationName = parameters.getString(DataSetLoader.RELATION_NAME,"");
            Collection<Attributes> collection = getRuntimeContext().getBroadcastVariable(DataSetLoader.ATTRIBUTES_NAME+"_"+relationName);
            attributes  = collection.iterator().next();
        }

    }

    private static class DataFlinkFile implements DataFlink<DataInstance>{

        DataSetLoader loader;

        DataFlinkFile(DataSetLoader loader){
            this.loader=loader;
            this.loader.loadHeader();
        }

        @Override
        public Attributes getAttributes() {
            return this.loader.getAttributes();
        }

        @Override
        public DataSet<DataInstance> getDataSet() {
            return this.loader.loadDataSet();
        }
    }
}
