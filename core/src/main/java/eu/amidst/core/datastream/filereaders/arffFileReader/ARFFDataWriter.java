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

package eu.amidst.core.datastream.filereaders.arffFileReader;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.DataFileWriter;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;

// TODO Write a quickARFFReader and quickARFFSaver

/**
 * This class implements the interface {@link DataFileWriter} and defines an ARFF (Weka Attribute-Relation File Format) data writer.
 */
public class ARFFDataWriter implements DataFileWriter {

    public static DecimalFormat decimalFormat = new DecimalFormat("#");


    /**
     * Saves a given data stream to an ARFF file.
     * @param dataStream an input {@link DataStream}.
     * @param path the path of the ARFF file where the data stream will be saved.
     * @throws IOException in case of an error when writing to file
     */
    public static void writeToARFFFile(DataStream<? extends DataInstance> dataStream, String path) throws IOException {
        FileWriter fw = new FileWriter(path);
        fw.write("@relation dataset\n\n");

        for (Attribute att : dataStream.getAttributes()){
            fw.write(ARFFDataWriter.attributeToARFFString(att)+"\n");
        }

        fw.write("\n\n@data\n\n");

        dataStream.stream().forEach(e -> {
            try {
                fw.write(ARFFDataWriter.dataInstanceToARFFString(dataStream.getAttributes(), e) + "\n");
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        fw.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileExtension() {
        return "arff";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToFile(DataStream<? extends DataInstance> dataStream, String path) throws IOException {
       ARFFDataWriter.writeToARFFFile(dataStream, path);
    }

    public static String attributeToARFFString(Attribute att){
        if (att.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.REAL) {
            return "@attribute " + att.getName() + " real";
        } else if (att.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
            StringBuilder stringBuilder = new StringBuilder("@attribute " + att.getName() + " {");
            FiniteStateSpace stateSpace = att.getStateSpaceType();
            stateSpace.getStatesNames().stream().limit(stateSpace.getNumberOfStates() - 1).forEach(e -> stringBuilder.append(e + ", "));
            stringBuilder.append(stateSpace.getStatesName(stateSpace.getNumberOfStates() - 1) + "}");
            return stringBuilder.toString();
        }else if (att.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.SPARSE_FINITE_SET) {
                return "@attribute " + att.getName() + " SparseMultinomial "+att.getNumberOfStates();
        }else{
            throw new IllegalArgumentException("Unknown SateSapaceType");
        }
    }

    public static String attributeToARFFStringWithIndex(Attribute att, boolean includeRanges){
        if (att.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.REAL) {
            if(includeRanges && !att.getName().equalsIgnoreCase("SEQUENCE_ID") && !att.getName().equalsIgnoreCase("TIME_ID"))
                return "@attribute " + att.getName() + " " +att.getIndex()+ " real ["+
                        ((RealStateSpace) att.getStateSpaceType()).getMinInterval() +","+
                        ((RealStateSpace) att.getStateSpaceType()).getMaxInterval() +"]";
            return "@attribute " + att.getName() + " " +att.getIndex()+ " real";
        } else if (att.getStateSpaceType().getStateSpaceTypeEnum()== StateSpaceTypeEnum.FINITE_SET) {
            StringBuilder stringBuilder = new StringBuilder("@attribute " + att.getName() + " " +att.getIndex()+ " {");
            FiniteStateSpace stateSpace = att.getStateSpaceType();
            stateSpace.getStatesNames().stream().limit(stateSpace.getNumberOfStates()-1).forEach(e -> stringBuilder.append(e+", "));
            stringBuilder.append(stateSpace.getStatesName(stateSpace.getNumberOfStates()-1)+"}");
            return stringBuilder.toString();
        }else{
            throw new IllegalArgumentException("Unknown SateSapaceType");
        }
    }

    /**
     * Converts a {@link DataInstance} object to an ARFF format {@code String}.
     * @param assignment a {@link DataInstance} object.
     * @return an ARFF format {@code String}.
     */
    public static String dataInstanceToARFFString(DataInstance assignment) {
        return dataInstanceToARFFString(assignment.getAttributes(), assignment);
    }

    /**
     * Converts a {@link DataInstance} object to an ARFF format {@code String}.
     * @param atts an input list of the list of {@link Attributes}.
     * @param assignment a {@link DataInstance} object.
     * @return an ARFF format {@code String}.
     */
    public static String dataInstanceToARFFString(Attributes atts, DataInstance assignment){
        StringBuilder builder = new StringBuilder(atts.getNumberOfAttributes()*2);

        //MEJORAR PONER CUANDO REAL
        for(int i=0; i<atts.getNumberOfAttributes()-1;i++) {
            Attribute att = atts.getFullListOfAttributes().get(i);
            builder.append(dataInstanceToARFFString(att,assignment,","));
        }
        Attribute att = atts.getFullListOfAttributes().get(atts.getNumberOfAttributes()-1);
        builder.append(dataInstanceToARFFString(att,assignment,""));

        return builder.toString();
    }

    public static String dataInstanceToARFFString(Attribute att, DataInstance assignment, String separator) {
        StringBuilder builder = new StringBuilder();

        if (Utils.isMissingValue(assignment.getValue(att))) {
            builder.append("?,");
        } else if (att.getStateSpaceType().getStateSpaceTypeEnum() == StateSpaceTypeEnum.FINITE_SET) {
            FiniteStateSpace stateSpace = att.getStateSpaceType();
            String nameState = stateSpace.getStatesName((int) assignment.getValue(att));
            builder.append(nameState + separator);
        } else if (att.getStateSpaceType().getStateSpaceTypeEnum() == StateSpaceTypeEnum.REAL) {
            //if (att.getNumberFormat() != null) {
                //builder.append(att.getNumberFormat().format(assignment.getValue(att)) + separator);
            if (att.isSpecialAttribute() || att.isTimeId()){
                builder.append(decimalFormat.format(assignment.getValue(att)) + separator);
            } else {
                builder.append(assignment.getValue(att) + separator);
            }
        } else if (att.getStateSpaceType().getStateSpaceTypeEnum() == StateSpaceTypeEnum.SPARSE_FINITE_SET) {
            int val = ((int)assignment.getValue(att));
            builder.append( val + separator);
        } else {
            throw new IllegalArgumentException("Illegal State Space Type: " + att.getStateSpaceType().getStateSpaceTypeEnum());
        }

        return builder.toString();
    }
}
