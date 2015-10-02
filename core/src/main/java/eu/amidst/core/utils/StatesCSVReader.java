package eu.amidst.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
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


        boolean[] isVariableContinuous = new boolean[numberOfVariables];
        int[] minStateFound = new int[numberOfVariables];
        int[] maxStateFound = new int[numberOfVariables];

        String newLine1 = reader1.readLine();
        String[] values1 = newLine1.split(SEPARATOR);

        IntStream.range(0,values1.length).forEach(k -> {
            if (!values1[k].contains(".")) {
                int value = Integer.parseInt(values1[k]);
                minStateFound[k] = value;
                maxStateFound[k] = value;
            }
        });

        reader1.close();
        source1.close();

        /*
         *  READS EACH LINE OF EACH FILE, AND DETERMINES THE NUMBER OF STATES OF DISCRETE VARIABLES
         */
        CSVfiles.forEach(csvfile -> {
            try {

                Reader source = Files.newBufferedReader(
                        csvfile, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(source);

                String newLine;
                //int value;
                int lineNumber = 1;

                newLine = reader.readLine(); // DISCARD FIRST ROW (HEADER)
                newLine = reader.readLine();
                while (newLine != null) {

                    String[] values = newLine.split(SEPARATOR);

                    IntStream.range(0, values.length).forEach(k -> {
                        //for (int k = 0; k < values.length; k++) {
                        if (values[k].contains(".")) {
                            isVariableContinuous[k] = true;
                        } else {
                            int value = Integer.parseInt(values[k]);

                            if (value < minStateFound[k]) {
                                minStateFound[k] = value;
                            }

                            if (value > maxStateFound[k]) {
                                maxStateFound[k] = value;
                            }
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
            if (isVariableContinuous[i]) {
                varValues = "real";
            } else {
                int[] valuesArray = IntStream.range(minStateFound[i], maxStateFound[i] + 1).toArray();
                varValues = "{";

                for (int j = 0; j < valuesArray.length - 1; j++) {
                    varValues = varValues + valuesArray[j] + ",";
                }
                varValues = varValues + valuesArray[valuesArray.length - 1] + "}";
            }
            System.out.println("@attribute " + variableNames.get(i) + " " + varValues);
        });

    }

    public static void getNumberOfStatesFromCSVFile(String folder, String file) throws IOException {

        System.out.println("Reading file " + file);
        Path path = Paths.get(folder, file); //"./datasets/", "syntheticData.csv");
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
        //variableNames.stream().forEach(st -> System.out.println(st));

        int numberOfVariables = variableNames.size();
        System.out.println("File " + path.getFileName() + " has " + numberOfVariables + " variables");

        boolean[] isVariableContinuous = new boolean[numberOfVariables];
        int[] minStateFound = new int[numberOfVariables];
        int[] maxStateFound = new int[numberOfVariables];

        /*
         *  READS EACH LINE AND DETERMINES THE NUMBER OF STATES OF DISCRETE VARIABLES
         */
        String newLine;

        int lineNumber = 1;
        newLine = reader.readLine();
        while(newLine!=null) {

            String [] values = newLine.split(SEPARATOR);

            for(int k=0; k<values.length; k++) {
                if(values[k].contains(".")) {
                    isVariableContinuous[k]=true;
                }
                else {
                    int value = Integer.parseInt(values[k]);

                    if (lineNumber == 1) {
                        minStateFound[k] = value;
                        maxStateFound[k] = value;
                    }
                    else {
                        if (value < minStateFound[k]) {
                            minStateFound[k] = value;
                        }

                        if (value > maxStateFound[k]) {
                            maxStateFound[k] = value;
                        }
                    }
                }
            }
            newLine = reader.readLine();
            lineNumber++;
        }

        System.out.println("Read " + (lineNumber-1) + " lines");
        //System.out.println(Arrays.toString(isVariableContinuous));
        //System.out.println(Arrays.toString(minStateFound));
        //System.out.println(Arrays.toString(maxStateFound));

        // variableNames.forEach(vn -> System.out.println("@attribute " + vn + isVariableContinuous
        System.out.println();
        System.out.println("ARFF Header:");
        System.out.println();

        for(int i=0; i<variableNames.size(); i++) {
            String varValues;
            if(isVariableContinuous[i]) {
                varValues = "real";
            }
            else {
                int[] valuesArray = IntStream.range(minStateFound[i], maxStateFound[i]+1).toArray();
                varValues = "{";

                for(int j=0; j<valuesArray.length-1; j++) {
                    varValues = varValues + valuesArray[j] + ",";
                }
                varValues = varValues + valuesArray[valuesArray.length-1] + "}";
            }
            System.out.println("@attribute " + variableNames.get(i) + " " + varValues);
        }


        reader.close();
        source.close();

        System.out.println();
    }

    public static void main(String[] args) throws IOException {

        getNumberOfStatesFromCSVFile("./datasets/", "syntheticData.csv");

//      RESULT SHOULD BE:
//        @attribute A {1,2}
//        @attribute B {1,2,3}
//        @attribute C real
//        @attribute D real
//        @attribute E {1,2}
//        @attribute G real
//        @attribute H real
//        @attribute I real



        getNumberOfStatesFromCSVFolder("./datasets/CSVfolder");

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
