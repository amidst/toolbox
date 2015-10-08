package eu.amidst.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
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
public class StatesCSVReader {

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

    public static void getNumberOfStatesFromCSVFolder(String folderName) throws IOException {

        System.out.println("Reading folder " + folderName + ", with files:");
        Path folder = Paths.get(folderName); //"./datasets/CSVfolder/");
        String SEPARATOR = ",";

        List<Path> CSVfiles = listSourceFiles(folder);
        CSVfiles.forEach(csv -> System.out.println(csv.getFileName()));
        System.out.println();

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
        System.out.println("File " + firstCSV.getFileName() + " has " + numberOfVariables + " variables");

        source1.close();
        reader1.close();

        boolean[] isVariableContinuous = new boolean[numberOfVariables];

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

                        if (values[k].contains(".")) {
                            isVariableContinuous[k] = true;
                        } else {
                            varStates.get(k).add(values[k]);
                        }
                    });
                    newLine = reader.readLine();
                    lineNumber++;
                }

                reader.close();
                source.close();

                System.out.println("Read " + (lineNumber - 1) + " lines in file " + csvfile.getFileName());
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }

        });

        /*
         * DISPLAYS THE .ARFF FILE HEADER:
         */

        System.out.println();
        System.out.println("ARFF Header:");
        System.out.println();

        //for(int i=0; i<variableNames.size(); i++) {
        IntStream.range(0, variableNames.size()).forEach(i -> {
            String varValues;
            if (isVariableContinuous[i] || varStates.get(i).size() > 10) {
                varValues = "real";
            } else {

                try{
                    StringBuilder builder = new StringBuilder(2 + varStates.get(i).size() * 2);
                    builder.append("{");
                    //varStates.get(i).stream().sorted().forEach(str -> builder.append(str + ","));
                    varStates.get(i).stream().mapToInt(Integer::parseInt).sorted().forEach(str -> builder.append(str + ","));
                    builder.deleteCharAt(builder.lastIndexOf(","));
                    builder.append("}");
                    varValues = builder.toString();
                }
                catch(NumberFormatException ex) {
                    StringBuilder builder = new StringBuilder(2 + varStates.get(i).size() * 2);
                    builder.append("{");
                    varStates.get(i).stream().sorted().forEach(str -> builder.append(str + ","));
                    //varStates.get(i).stream().mapToInt(Integer::parseInt).sorted().forEach();
                    builder.deleteCharAt(builder.lastIndexOf(","));
                    builder.append("}");
                    varValues = builder.toString();
                }

            }
            System.out.println("@attribute " + variableNames.get(i) + " " + varValues);
        });

    }

    public static void getNumberOfStatesFromCSVFile(String folder, String file) throws IOException {

        System.out.println("Reading file " + file);
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
        System.out.println("File " + path.getFileName() + " has " + numberOfVariables + " variables");

        boolean[] isVariableContinuous = new boolean[numberOfVariables];

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

                if (values[k].contains(".")) {
                    isVariableContinuous[k] = true;
                } else {
                    varStates.get(k).add(values[k]);
                }
            });

            newLine = reader.readLine();
            lineNumber++;
        }

        System.out.println("Read " + (lineNumber-1) + " lines");

        /*
         * DISPLAYS THE .ARFF FILE HEADER:
         */

        System.out.println();
        System.out.println("ARFF Header:");
        System.out.println();

        //for(int i=0; i<variableNames.size(); i++) {
        IntStream.range(0, variableNames.size()).forEach(i -> {
            String varValues;
            if (isVariableContinuous[i] || varStates.get(i).size()>10) {
                varValues = "real";
            } else {

                try{
                    StringBuilder builder = new StringBuilder(2 + varStates.get(i).size() * 2);
                    builder.append("{");
                    //varStates.get(i).stream().sorted().forEach(str -> builder.append(str + ","));
                    varStates.get(i).stream().mapToInt(Integer::parseInt).sorted().forEach(str -> builder.append(str + ","));
                    builder.deleteCharAt(builder.lastIndexOf(","));
                    builder.append("}");
                    varValues = builder.toString();
                }
                catch(NumberFormatException ex) {
                    StringBuilder builder = new StringBuilder(2 + varStates.get(i).size() * 2);
                    builder.append("{");
                    varStates.get(i).stream().sorted().forEach(str -> builder.append(str + ","));
                    //varStates.get(i).stream().mapToInt(Integer::parseInt).sorted().forEach();
                    builder.deleteCharAt(builder.lastIndexOf(","));
                    builder.append("}");
                    varValues = builder.toString();
                }
            }
            System.out.println("@attribute " + variableNames.get(i) + " " + varValues);
        });


        reader.close();
        source.close();

        System.out.println();
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

        if(args.length!=1) {
            System.out.println("Incorrect number of arguments. Please use \"StatesCSVReader CSVFolderPath\"");
        }
        else {
            String dirName = args[0];
            getNumberOfStatesFromCSVFolder(dirName);
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
