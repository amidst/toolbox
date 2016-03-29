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

package eu.amidst.cajamareval;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by dario on 02/10/15.
 */
public class CSVtoARFFHeader {

    public static List<Path> listSourceFiles(Path dir) throws IOException {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.csv")) {
            for (Path entry : stream) {
                result.add(entry);
            }
        } catch (DirectoryIteratorException ex) {
            // I/O error encounted during the iteration, the cause is an IOException
            throw ex.getCause();
        }
        return result;
    }

    public static void getNumberOfStatesFromCSVFolder(String folderName, String outputFolderName) throws IOException {

        //System.out.println("Reading folder " + folderName + ", with files:");
        Path folder = Paths.get(folderName); //"./datasets/CSVfolder/");
        String SEPARATOR = ",";

        List<Path> CSVfiles = listSourceFiles(folder);
        //CSVfiles.forEach(csv -> System.out.println(csv.getFileName()));
        //System.out.println();

        Path firstCSV = CSVfiles.stream().findFirst().get();


        /*
         *  READS THE HEADER OF THE FIRST CSV FILE, WITH VARIABLE NAMES
         */

        Reader source1 = Files.newBufferedReader(
                firstCSV, Charset.forName("UTF-8"));
        BufferedReader reader1 = new BufferedReader(source1);
        List<String> variableNames = reader1.lines()
                .findFirst()
                .map(line -> Arrays.asList(line.split(SEPARATOR)))
                .get();
        //variableNames.stream().forEach(st -> System.out.println(st));

        int numberOfVariables = variableNames.size();
        //System.out.println("File " + firstCSV.getFileName() + " has " + numberOfVariables + " variables");

        source1.close();
        reader1.close();

        boolean[] isVariableCategorical = new boolean[numberOfVariables];
        for (int i = 0; i < isVariableCategorical.length; i++) {
            isVariableCategorical[i]=false;
        }

        boolean[] isVariableContinuous = new boolean[numberOfVariables];
        for (int i = 0; i < isVariableContinuous.length; i++) {
            isVariableContinuous[i]=false;
        }

        List<HashSet<String>> varStates = new ArrayList<>(numberOfVariables);


        IntStream.range(0,numberOfVariables).forEach(k -> {
            varStates.add(new HashSet<>(1));
        });

        /*
         *  READS EACH LINE OF EACH FILE, AND DETERMINES THE NUMBER OF STATES OF DISCRETE VARIABLES
         */
        CSVfiles.forEach(csvfile -> {
            try {

                Reader source = Files.newBufferedReader(
                        csvfile, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(source);

                String newLine;
                int lineNumber = 1;

                newLine = reader.readLine(); // DISCARD FIRST ROW (HEADER)
                newLine = reader.readLine();
                while (newLine != null) {

                    String[] values = newLine.split(SEPARATOR);

                    IntStream.range(0, values.length).forEach(k -> {

//                        if (values[k].contains(".")) {
//                            isVariableContinuous[k] = true;
//                        } else {
//                            varStates.get(k).add(values[k]);
//                        }

                        if ( !values[k].contains("?") ) {
                            double dd=0;
                            try {
                                dd = Double.parseDouble(values[k]);
                                if( !(dd == Math.floor(dd)) ) {
                                    isVariableContinuous[k]=true;

                                    if (varStates.get(k).size()<=15) {
                                        varStates.get(k).add(Double.toString(dd));
                                    }
                                }
                                else {
                                    if (varStates.get(k).size()<=15) {
                                        varStates.get(k).add(Integer.toString((int)dd));
                                    }
                                }
                            }
                            catch (NumberFormatException e) {
                                isVariableCategorical[k] = true;
                                varStates.get(k).add(values[k]);
                            }

                            if ( values[k].contains("s") || Double.isNaN(dd) ) {
                                isVariableCategorical[k] = true;
                                varStates.get(k).add(values[k]);
                            }
                        }

                    });
                    newLine = reader.readLine();
                    lineNumber++;
                }

                reader.close();
                source.close();

                //System.out.println("Read " + (lineNumber - 1) + " lines in file " + csvfile.getFileName());
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }

        });

        /*
         * DISPLAYS OR SAVES THE .ARFF FILE HEADER:
         */

        //System.out.println();
        //System.out.println("ARFF Header:");
        //System.out.println();

        String outputFileName = "attributes.txt";
        Path outputPath = Paths.get(outputFolderName, outputFileName);
        File outputFile = outputPath.toFile();
        PrintWriter writer = new PrintWriter(outputFile, "UTF-8");

        //for(int i=0; i<variableNames.size(); i++) {
        IntStream.range(0, variableNames.size()).forEach(i -> {
            varStates.get(i).remove("?");
            String varValues;

            if ( !isVariableCategorical[i] && varStates.get(i).size() > 10 ) {
                varValues = "real";
            }
//            if ( (!isVariableCategorical[i] && isVariableContinuous[i]) || (!isVariableCategorical[i] && varStates.get(i).size() > 10)) {
//                varValues = "real";
//            }
            else {

                try {
                    StringBuilder builder = new StringBuilder(2 + varStates.get(i).size() * 2);
                    builder.append("{");
                    //varStates.get(i).stream().sorted().forEach(str -> builder.append(str + ","));
                    varStates.get(i).stream()//filter(str -> str.compareTo("?")!=0).
                            .mapToInt(Integer::parseInt).sorted().forEach(str -> builder.append(str + ","));
                    builder.deleteCharAt(builder.lastIndexOf(","));
                    builder.append("}");
                    varValues = builder.toString();
                } catch (NumberFormatException ex) {
                    StringBuilder builder = new StringBuilder(2 + varStates.get(i).size() * 2);
                    builder.append("{");
                    varStates.get(i).stream()//.filter(str -> str.compareTo("?")!=0)
                            .sorted().forEach(str -> builder.append(str + ","));
                    //varStates.get(i).stream().mapToInt(Integer::parseInt).sorted().forEach();
                    builder.deleteCharAt(builder.lastIndexOf(","));
                    builder.append("}");
                    varValues = builder.toString();
                }

            }
            //System.out.println("@attribute " + variableNames.get(i) + " " + varValues);
            writer.println("@attribute " + variableNames.get(i) + " " + varValues);
        });
        writer.close();

    }

    public static void getNumberOfStatesFromCSVFile(String folder, String file, String outputFolderName) throws IOException {

        //System.out.println("Reading file " + file);
        Path path = Paths.get(folder, file);
        Reader source = Files.newBufferedReader(
                path, Charset.forName("UTF-8"));
        String SEPARATOR = ",";

        /*
         *  READS THE HEADER WITH VARIABLE NAMES
         */

        BufferedReader reader = new BufferedReader(source);
        List<String> variableNames = reader.lines()
                .findFirst()
                .map(line -> Arrays.asList(line.split(SEPARATOR)))
                .get();

        int numberOfVariables = variableNames.size();
        //System.out.println("File " + path.getFileName() + " has " + numberOfVariables + " variables");

        boolean[] isVariableContinuous = new boolean[numberOfVariables];
        for (int i = 0; i < isVariableContinuous.length; i++) {
            isVariableContinuous[i]=true;
        }

        List<HashSet<String>> varStates = new ArrayList<>(numberOfVariables);


        IntStream.range(0,numberOfVariables).forEach(k -> {
            varStates.add(new HashSet<>(1));
        });


        /*
         *  READS EACH LINE AND DETERMINES THE NUMBER OF STATES OF DISCRETE VARIABLES
         */
        String newLine;

        int lineNumber = 1;
        newLine = reader.readLine();
        while(newLine!=null) {

            String [] values = newLine.split(SEPARATOR);

            IntStream.range(0, values.length).forEach(k -> {

//                if (values[k].contains(".")) {
//                    isVariableContinuous[k] = true;
//                } else {
//                    varStates.get(k).add(values[k]);
//                }
                if ( !values[k].contains("?") ) {
                    double dd=0;
                    try {
                        dd = Double.parseDouble(values[k]);
                    }
                    catch (NumberFormatException e) {
                        isVariableContinuous[k] = false;
                    }

                    if ( values[k].contains("s") || Double.isNaN(dd) || !isVariableContinuous[k] ) {
                        isVariableContinuous[k] = false;
                        varStates.get(k).add(values[k]);
                    }
                }
            });

            newLine = reader.readLine();
            lineNumber++;
        }

//        System.out.println("Read " + (lineNumber-1) + " lines");

        /*
         * DISPLAYS THE .ARFF FILE HEADER:
         */

//        System.out.println();
//        System.out.println("ARFF Header:");
//        System.out.println();

        //for(int i=0; i<variableNames.size(); i++) {
        String outputFileName = "attributes.txt";
        Path outputPath = Paths.get(outputFolderName, outputFileName);
        File outputFile = outputPath.toFile();
        PrintWriter writer = new PrintWriter(outputFile, "UTF-8");


        IntStream.range(0, variableNames.size()).forEach(i -> {
            varStates.get(i).remove("?");
            String varValues;

            if (isVariableContinuous[i]) {
                varValues = "real";
            } else {

                try {
                    StringBuilder builder = new StringBuilder(2 + varStates.get(i).size() * 2);
                    builder.append("{");
                    //varStates.get(i).stream().sorted().forEach(str -> builder.append(str + ","));
                    varStates.get(i).stream()//.filter(str -> str.compareTo("?")!=0)
                            .mapToInt(Integer::parseInt).sorted().forEach(str -> builder.append(str + ","));
                    builder.deleteCharAt(builder.lastIndexOf(","));
                    builder.append("}");
                    varValues = builder.toString();
                } catch (NumberFormatException ex) {
                    StringBuilder builder = new StringBuilder(2 + varStates.get(i).size() * 2);
                    builder.append("{");
                    varStates.get(i).stream()//.filter(str -> str.compareTo("?")!=0)
                            .sorted().forEach(str -> builder.append(str + ","));
                    //varStates.get(i).stream().mapToInt(Integer::parseInt).sorted().forEach();
                    builder.deleteCharAt(builder.lastIndexOf(","));
                    builder.append("}");
                    varValues = builder.toString();
                }
            }
            //System.out.println("@attribute " + variableNames.get(i) + " " + varValues);
            writer.println("@attribute " + variableNames.get(i) + " " + varValues);
        });

        writer.close();

        reader.close();
        source.close();

        //System.out.println();
    }

    public static void main(String[] args) throws IOException {

        //getNumberOfStatesFromCSVFile("./datasets/", "syntheticData.csv");

//      RESULT SHOULD BE:
//        @attribute A {1,2}
//        @attribute B {1,2,3}
//        @attribute C real
//        @attribute D real
//        @attribute E {1,2}
//        @attribute G real
//        @attribute H real
//        @attribute I real

        if(args.length!=2) {
            System.out.println("Incorrect number of arguments. Please use \"CSVtoARFFHeader 'csv_folder_path' 'output_file_path'\"");
            //getNumberOfStatesFromCSVFolder("./datasets/CSVfolder2");
            //getNumberOfStatesFromCSVFolder("./datasets/CSVfolder");
        }
        else {
            String csvFolderName = args[0];
            String outputFolderName = args[1];
            getNumberOfStatesFromCSVFolder(csvFolderName, outputFolderName);
        }

        //getNumberOfStatesFromCSVFolder("./datasets/CSVfolder");

//      RESULT SHOULD BE:
//        @attribute A {-1,0,1,2}
//        @attribute B {1,2,3,4}
//        @attribute C real
//        @attribute D real
//        @attribute E {1,2,3,4,5,6,7,8}
//        @attribute G real
//        @attribute H real
//        @attribute I real
    }
}
