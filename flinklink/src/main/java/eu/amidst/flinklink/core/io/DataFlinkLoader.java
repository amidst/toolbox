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
package eu.amidst.flinklink.core.io;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.filereaders.DataInstanceFromDataRow;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.datastream.filereaders.arffFileReader.DataRowWeka;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.data.DataFlinkConverter;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.configuration.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 1/9/15.
 */
public class DataFlinkLoader implements Serializable{

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    public static String RELATION_NAME="RELATION_NAME";
    public static String ATTRIBUTES_NAME="ATTRIBUTES";

    Attributes attributes;

    String relationName;
    String pathFileName;
    String pathFileHeader;
    String pathFileData;

    boolean isARFFFolder;
    boolean normalize;

    public boolean isNormalize() {
        return normalize;
    }

    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }


    public static DataFlink<DataInstance> open(ExecutionEnvironment env, String pathFileData, boolean normalize) throws FileNotFoundException {
        if(isArffFolder(pathFileData))
            return loadDataFromFolder(env,pathFileData,normalize);
        return loadDataFromFile(env,pathFileData,normalize);
    }


    public static DataFlink<DataInstance> loadDataFromFile(ExecutionEnvironment env, String pathFileData, boolean normalize) throws FileNotFoundException{
        DataFlinkLoader loader = new DataFlinkLoader();
        loader.setPathFileData(pathFileData);
        loader.setNormalize(normalize);
        return new DataFlinkFile(env,loader);
    }

    public static DataFlink<DataInstance> loadDataFromFolder(ExecutionEnvironment env, String pathFileData, boolean normalize) throws FileNotFoundException{
        DataFlinkLoader loader = new DataFlinkLoader();
        loader.setPathFolderData(pathFileData);
        loader.setNormalize(normalize);
        return new DataFlinkFile(env,loader);
    }


    public static DataFlink<DynamicDataInstance> openDynamic(ExecutionEnvironment env, String pathFileData, boolean normalize) throws FileNotFoundException {
        if(isArffFolder(pathFileData))
            return loadDynamicDataFromFolder(env,pathFileData,normalize);
        return loadDynamicDataFromFile(env,pathFileData,normalize);
    }


    public static DataFlink<DynamicDataInstance> loadDynamicDataFromFile(ExecutionEnvironment env, String pathFileData, boolean normalize)
            throws FileNotFoundException{
        return DataFlinkConverter.convertToDynamic(loadDataFromFile(env, pathFileData, normalize));
    }

    public static DataFlink<DynamicDataInstance> loadDynamicDataFromFolder(ExecutionEnvironment env, String pathFileData, boolean normalize)
            throws FileNotFoundException{
        return DataFlinkConverter.convertToDynamic(loadDataFromFolder(env, pathFileData, normalize));
    }

    private DataSet<DataInstance> loadDataSet(ExecutionEnvironment env){

        if (attributes==null)
            this.loadHeader(env);

        DataSet<Attributes> attsDataSet = env.fromElements(attributes);

        DataSource<String> data = env.readTextFile(pathFileData);

        Configuration config = new Configuration();
        config.setString(DataFlinkLoader.RELATION_NAME, this.relationName);


        return  data
                .filter(w -> !w.isEmpty())
                .filter(w -> !w.startsWith("%"))
                .filter(line -> !line.startsWith("@attribute"))
                .filter(line -> !line.startsWith("@relation"))
                .filter(line -> !line.startsWith("@data"))
                .map(new DataInstanceBuilder(isNormalize()))
                .withParameters(config)
                .withBroadcastSet(attsDataSet, DataFlinkLoader.ATTRIBUTES_NAME + "_" + this.relationName);
    }


    private void setPathFolderData(String pathFileData) throws FileNotFoundException {
        this.isARFFFolder= true;
        this.pathFileData=pathFileData+"/data/";
        this.pathFileHeader=pathFileData+"/attributes.txt";
        this.pathFileName = pathFileData+"/name.txt";
    }

    private void setPathFileData(String pathFileData) throws FileNotFoundException {
        this.isARFFFolder= false;
        this.pathFileData = pathFileData;
        this.pathFileHeader = pathFileData;
    }

    private Attributes getAttributes() {
        return attributes;
    }

    private void loadHeader(ExecutionEnvironment env) {
        if (isARFFFolder)
            this.loadHeaderARFFFolder(env);
        else
            this.loadHeaderARFFFile(env);
    }

    private void loadHeaderARFFFolder(ExecutionEnvironment env) {

        try {

            DataSource<String> name = env.readTextFile(pathFileName);
            this.relationName = name.collect().get(0).split(" ")[1];

            DataSource<String> headerLines = env.readTextFile(pathFileHeader);

            List<String> attLines = headerLines
                    .setParallelism(1)
                    .filter(w -> !w.isEmpty())
                    .filter(w -> !w.startsWith("%"))
                    .filter(line -> line.startsWith("@attribute"))
                    .collect();

            List<Attribute> atts = attLines.stream()
                    .map(line -> {
                        String[] parts = line.split(" |\t");
                        int index = Integer.parseInt(parts[2]);
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < parts.length; i++) {
                            if (i == 2)
                                continue;
                            builder.append(parts[i]);
                            builder.append("\t");
                        }
                        return ARFFDataReader.createAttributeFromLine(index, builder.toString());
                    })
                    .collect(Collectors.toList());

            Collections.sort(atts, (a,b) -> a.getIndex() - b.getIndex() );
            attributes = new Attributes(atts);
        }catch (Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }

    private void loadHeaderARFFFile(ExecutionEnvironment env){
        DataSource<String> headerLines = env.readTextFile(pathFileHeader);
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


            headerLines = env.readTextFile(pathFileHeader);


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
        List<Attribute> attributesToNormalize;
        boolean normalize;

        public DataInstanceBuilder(boolean normalize){
            this.normalize = normalize;
        }

        @Override
        public DataInstance map(String value) throws Exception {
            DataInstance dataInstance = new DataInstanceFromDataRow(new DataRowWeka(attributes,value));
            if(normalize) {
                attributesToNormalize.stream()
                        .forEach(att -> {
                                    double factor = 1000;
                                    double interval = factor*(((RealStateSpace) att.getStateSpaceType()).getMaxInterval() -
                                            ((RealStateSpace) att.getStateSpaceType()).getMinInterval());
                                    double Nvalue = (dataInstance.getValue(att) -
                                            ((RealStateSpace) att.getStateSpaceType()).getMinInterval())/interval;
                                    if (Double.isNaN(Nvalue)|| Nvalue<0 || Nvalue>factor)
                                        throw new IllegalStateException("Non proper normalization"+Nvalue);
                                    dataInstance.setValue(att, Nvalue);
                                }
                        );
            }
            return dataInstance;
        }

        @Override
        public void open(Configuration parameters) throws Exception{
            super.open(parameters);
            String relationName = parameters.getString(DataFlinkLoader.RELATION_NAME,"");
            Collection<Attributes> collection = getRuntimeContext().getBroadcastVariable(DataFlinkLoader.ATTRIBUTES_NAME+"_"+relationName);
            attributes  = collection.iterator().next();
            if(normalize) {
                attributesToNormalize = attributes.getFullListOfAttributes().stream()
                        .filter(att -> att.getStateSpaceType().getStateSpaceTypeEnum() == StateSpaceTypeEnum.REAL)
                        .filter(att -> ((RealStateSpace) att.getStateSpaceType()).getMinInterval() != Double.NEGATIVE_INFINITY)
                        .filter(att -> ((RealStateSpace) att.getStateSpaceType()).getMaxInterval() != Double.POSITIVE_INFINITY)
                        .collect(Collectors.toList());
            }
        }

    }


    /**
     * Determines if the path given as argument correspond to an ARFF distributed dataset
     * @param fileName local path to the dataset
     * @return boolean
     */

    public static boolean isArffFolder(String fileName) {
        if (!new File(fileName).isDirectory())
            return false;
        String[] parts = fileName.split("\\.");
        return parts[parts.length - 1].equals("arff");
    }

    private static class DataFlinkFile implements DataFlink<DataInstance>,Serializable{

        /** Represents the serial version ID for serializing the object. */
        private static final long serialVersionUID = 4107783324901370839L;

        DataFlinkLoader loader;
        transient ExecutionEnvironment env;
        DataFlinkFile(ExecutionEnvironment env, DataFlinkLoader loader){
            this.loader=loader;
            this.loader.loadHeader(env);
            this.env = env;
        }

        @Override
        public String getName() {
            return this.loader.relationName;
        }

        @Override
        public Attributes getAttributes() {
            return this.loader.getAttributes();
        }

        @Override
        public DataSet<DataInstance> getDataSet() {
            return this.loader.loadDataSet(env);
        }
    }
}
