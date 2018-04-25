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

import java.io.File;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class implements the interface {@link DataFileReader} for reading the "folder-ARFF" format.
 *
 */
public class ARFFDataFolderReader implements DataFileReader {

    /** Represents the relation name. */
    String relationName;

    /** Represents the list of {@link Attributes}. */
    Attributes attributes;

    /** Represents the file with the name of the data*/
    String pathFileName;

    /** Represents the file with the header of the data*/
    String pathFileHeader;

    /** Represents the folder containing the data*/
    String pathFileData;


    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromFile(String path) {

        this.pathFileData=path+"/data/";
        this.pathFileHeader=path+"/attributes.txt";
        this.pathFileName = path+"/name.txt";


        try {
            this.relationName = Files.lines(Paths.get(pathFileName)).collect(Collectors.toList()).get(0).split(" ")[1];

            Stream<String> headerLines =  Files.lines(Paths.get(pathFileHeader));

            List<String> attLines = headerLines
                    .filter(w -> !w.isEmpty())
                    .filter(w -> !w.startsWith("%"))
                    .filter(line -> line.startsWith("@attribute"))
                    .collect(Collectors.toList());


            List<Attribute> atts = null;
            try {
                atts = attLines.stream()
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

            }catch (Exception e) {
                atts = IntStream.range(0, attLines.size())
                        .mapToObj(i -> ARFFDataReader.createAttributeFromLine(i, attLines.get(i)))
                        .collect(Collectors.toList());
            }

            Collections.sort(atts, (a, b) -> a.getIndex() - b.getIndex());
            attributes = new Attributes(atts);
        }catch (Exception ex){
            throw new UndeclaredThrowableException(ex);
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
        if (!new File(fileName).isDirectory())
            return false;
        String[] parts = fileName.split("\\.");
        return parts[parts.length-1].equals("arff");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<DataRow> stream() {
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(pathFileData));
            Stream<Path> stream = StreamSupport.stream(directoryStream.spliterator(), false);

            Stream<String> fileLines = stream
                    .filter(path ->!path.getFileName().toString().startsWith("."))
                    .flatMap(path -> {
                        try{
                            return Files.lines(path);
                        }catch (Exception ex){
                            throw new UndeclaredThrowableException(ex);
                        }
                    });


            return fileLines
                    .filter(w -> !w.isEmpty())
                    .filter(w -> !w.startsWith("%"))
                    .map(line -> new DataRowWeka(this.attributes, line));

        }catch (Exception ex){
            throw new UndeclaredThrowableException(ex);
        }
    }
}
