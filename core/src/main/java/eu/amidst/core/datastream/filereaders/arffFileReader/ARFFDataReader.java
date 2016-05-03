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
import eu.amidst.core.datastream.filereaders.DataFileReader;
import eu.amidst.core.datastream.filereaders.DataRow;
import eu.amidst.core.variables.StateSpaceTypeEnum;
import eu.amidst.core.variables.stateSpaceTypes.FiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.SparseFiniteStateSpace;
import eu.amidst.core.variables.stateSpaceTypes.RealStateSpace;

import java.io.File;
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

    /** Represents a {@code Stream} of {@code DataRow}. */
    private Stream<DataRow> streamString;

    /**
     * Creates an {@link Attribute} from a given index and line.
     * @param index an {@code int} that represents the index of column to which the Attribute refers.
     * @param line a {@code String} starting with "@attribute" and including the name of the Attribute and its state space type.
     * @return an {@link Attribute} object.
     */
    public static Attribute createAttributeFromLine(int index, String line){
        String[] parts = line.split("\\s+|\t+");

        if (!parts[0].trim().startsWith("@attribute"))
            throw new IllegalArgumentException("Attribute line does not start with @attribute");

        String name = parts[1].trim();
        //name = StringUtils.strip(name,"'");

        name = name.replaceAll("^'+", "");
        name = name.replaceAll("'+$", "");

        parts[2]=parts[2].trim();

        if (parts[2].equals("real") || parts[2].equals("numeric")){
            if(parts.length>3 && parts[3].startsWith("[")){
                parts[3]=line.substring(line.indexOf("[")).replaceAll("\t", "");
                double min = Double.parseDouble(parts[3].substring(parts[3].indexOf("[")+1,parts[3].indexOf(",")));
                double max = Double.parseDouble(parts[3].substring(parts[3].indexOf(",")+1,parts[3].indexOf("]")));
                return new Attribute(index, name, new RealStateSpace(min,max));
            }else
                return new Attribute(index, name, new RealStateSpace());
        }else if (parts[2].startsWith("{")){
            parts[2]=line.substring(line.indexOf("{")).replaceAll("\t", "");
            String[] states = parts[2].substring(1,parts[2].length()-1).split(",");

            List<String> statesNames = Arrays.stream(states).map(String::trim).collect(Collectors.toList());

            return new Attribute(index, name, new FiniteStateSpace(statesNames));
        }else if (parts[2].equals("SparseMultinomial")) {
            return new Attribute(index, name, new SparseFiniteStateSpace(Integer.parseInt(parts[3])));
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
    public boolean doesItReadThisFile(String fileName) {
        if (new File(fileName).isDirectory())
            return false;
        String[] parts = fileName.split("\\.");
        return parts[parts.length-1].equals("arff");
    }

    /**
     * {@inheritDoc}
     */
    //TODO: In principle the "if" should be there to enforce a reset of the stream. But there is a bug.
    @Override
    public Stream<DataRow> stream() {
        //if (streamString ==null) {
        try {
            streamString = Files.lines(pathFile)
                    .filter(w -> !w.isEmpty())
                    .filter(w -> !w.startsWith("%"))
                    .skip(this.dataLineCount)
                    .filter(w -> !w.isEmpty())
                    .map(line -> new DataRowWeka(this.attributes, line));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        //}
        return streamString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart(){
        streamString = null;
    }

}
