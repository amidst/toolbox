/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.datastream.filereaders.arffFileReader;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.filereaders.DataFileReader;
import eu.amidst.core.datastream.filereaders.DataRow;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class implements the interface {@link DataFileReader} and defines an ARFF (Weka Attribute-Relation File Format) data reader.
 */
public class ARFFDataReader implements DataFileReader {

    /** Represents the relation name. */
    String relationName;

    /** Represents the list of {@link Attributes}. */
    private Attributes attributes;

    /** Represents the data line count. */
    private int dataLineCount;

    /** Represents the path of the ARFF file to be read. */
    private Path pathFile;

    /** Represents an array of {@link StateSpaceTypeEnum} for the corresponding list of {@link Attributes}. */
    private StateSpaceTypeEnum[] stateSpace;

    /** Represents a {@code Stream} of {@code String}. */
    private Stream<String> streamString;

    /**
     * Creates an {@link Attribute} from a given index and line.
     * @param index an {@code int} that represents the index of column to which the Attribute refers.
     * @param line a {@code String} starting with "@attribute" and including the name of the Attribute and its state space type.
     * @return an {@link Attribute} object.
     */
    private static Attribute createAttributeFromLine(int index, String line){
        String[] parts = line.split(" |\t");

        if (!parts[0].trim().startsWith("@attribute"))
            throw new IllegalArgumentException("Attribute line does not start with @attribute");

        String name = parts[1].trim();
        //name = StringUtils.strip(name,"'");

        name = name.replaceAll("^'+", "");
        name = name.replaceAll("'+$", "");

        parts[2]=line.substring(parts[0].length() + parts[1].length() + 2);

        parts[2]=parts[2].trim();

        if (parts[2].equals("real") || parts[2].equals("numeric")){
            return new Attribute(index, name, new RealStateSpace());
        }else if (parts[2].startsWith("{")){
            String[] states = parts[2].substring(1,parts[2].length()-1).split(",");

            List<String> statesNames = Arrays.stream(states).map(String::trim).collect(Collectors.toList());

            return new Attribute(index, name, new FiniteStateSpace(statesNames));
        }else{
            throw new UnsupportedOperationException("We can not create an attribute from this line: "+line);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromFile(String pathString) {
        pathFile = Paths.get(pathString);
        try {
            Optional<String> atRelation = Files.lines(pathFile)
                    .map(String::trim)
                    .filter(w -> !w.isEmpty())
                    .filter(w -> !w.startsWith("%"))
                    .limit(1)
                    .filter(line -> line.startsWith("@relation"))
                    .findFirst();

            if (!atRelation.isPresent())
                throw new IllegalArgumentException("ARFF file does not start with a @relation line.");

            relationName = atRelation.get().split(" ")[1];

            final int[] count = {0};
            Optional<String> atData = Files.lines(pathFile)
                    .map(String::trim)
                    .filter(w -> !w.isEmpty())
                    .filter(w -> !w.startsWith("%"))
                    .peek(line -> count[0]++)
                    .filter(line -> line.startsWith("@data"))
                    .findFirst();

            if (!atData.isPresent())
                throw new IllegalArgumentException("ARFF file does not contain @data line.");

            dataLineCount = count[0];

            List<String> attLines = Files.lines(pathFile)
                    .map(String::trim)
                    .filter(w -> !w.isEmpty())
                    .filter(w -> !w.startsWith("%"))
                    .limit(dataLineCount)
                    .filter(line -> line.startsWith("@attribute"))
                    .collect(Collectors.toList());

            List<Attribute> atts = IntStream.range(0,attLines.size())
                    .mapToObj( i -> createAttributeFromLine(i, attLines.get(i)))
                    .collect(Collectors.toList());

            this.attributes = new Attributes(atts);

            //
            stateSpace=new StateSpaceTypeEnum[atts.size()];

            for (Attribute att: atts){
                stateSpace[att.getIndex()] = att.getStateSpaceType().getStateSpaceTypeEnum();
            }
        }catch (IOException ex){
            throw new UncheckedIOException(ex);
        }
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
    public boolean doesItReadThisFileExtension(String fileExtension) {
        return fileExtension.equals(".arff");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<DataRow> stream() {
        if (streamString ==null) {
            try {
                streamString = Files.lines(pathFile);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        return streamString.filter(w -> !w.isEmpty()).filter(w -> !w.startsWith("%")).skip(this.dataLineCount).filter(w -> !w.isEmpty()).map(line -> new DataRowWeka(this.attributes, line));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart(){
        streamString = null;
    }

    /**
     * This class implements the interface {@link DataRow} and defines a Weka data row.
     */
    private static class DataRowWeka implements DataRow{

        /** Represents an {@code array} of double. */
        double[] data;

        /** Represents the list of {@link Attributes}. */
        Attributes atts;

        /**
         * Creates a new DataRowWeka from a given line and list of attributes.
         * @param atts_ an input list of the list of {@link Attributes}.
         * @param line a {@code String} including the values of the corresponding input attributes.
         */
        public DataRowWeka(Attributes atts_, String line){
            atts = atts_;
            data = new double[atts.getNumberOfAttributes()];
            String[] parts = line.split(",");
            if (parts.length!=atts.getNumberOfAttributes())
                throw new IllegalStateException("The number of columns does not match the number of attributes.");

            for (int i = 0; i < parts.length; i++) {
                if(parts[i].equals("?")){
                    data[i] = Double.NaN;
                }
                else {
                    switch (atts.getList().get(i).getStateSpaceType().getStateSpaceTypeEnum()) {
                        case REAL:
                            data[i] = Double.parseDouble(parts[i]);
                            break;
                        case FINITE_SET:
                            FiniteStateSpace finiteStateSpace = atts.getList().get(i).getStateSpaceType();
                            data[i] = finiteStateSpace.getIndexOfState(parts[i]);
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getValue(Attribute att) {
            return data[att.getIndex()];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValue(Attribute att, double value) {
            this.data[att.getIndex()]=value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Attributes getAttributes() {
            return atts;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double[] toArray() {
            return data;
        }


    }
}
