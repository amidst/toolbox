package eu.amidst.core.database.statics.readers.impl;

import eu.amidst.core.database.statics.readers.Attribute;
import eu.amidst.core.database.statics.readers.Attributes;
import eu.amidst.core.database.statics.readers.StateSpaceType;
import eu.amidst.core.database.statics.readers.DataInstance;
import eu.amidst.core.database.statics.readers.DataStream;
import eu.amidst.core.database.statics.readers.ArffParserException;
import eu.amidst.core.database.statics.readers.Keys;
import eu.amidst.core.header.statics.StaticDataHeader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import au.com.bytecode.opencsv.CSVReader;


/**
 * Created by sigveh on 10/7/14.
 */
public class ArffDataStream implements DataStream{
    private Iterator iterator;
    private StaticDataHeader staticDataHeader;
    private String fileName;
    private Set<Attribute> attributes;
    private int[] attributesActivation;
    private int rowSize;
    private int doubleColumnSize;
    private int intColumnSize;
    private double[][] doubleData;
    private int[][] intData;
    private String relation;
    private List<Attribute> attributesInHeader = new ArrayList<>();
    private int dataInstance = 0;

    public ArffDataStream(String fileName, Attributes attributes) throws FileNotFoundException,
            IOException, ArffParserException {
        this.fileName = fileName;
        this.attributes = attributes.getSet();
        this.attributesActivation = new int[attributes.getSet().size()];

        CSVReader reader = new CSVReader(new FileReader(fileName));
        List<String []> lines = reader.readAll();
        int headerIndex = 0;
        boolean isHeader = true;
        while (isHeader) {
            String[] lineAsArray = lines.get(headerIndex);
            String line = lineArrayToLine( lineAsArray );
            String[] words = line.split("\\s+");
            words = trimSpacesInBeginning(words);

//            int wordCounter2 = 0;
//            while( wordCounter2 < words.length) {
//                System.out.println( "                 " + words[wordCounter2] );
//                wordCounter2 = wordCounter2 + 1;
//            }

            if(words.length == 0){
                headerIndex = headerIndex + 1;
                //  System.out.println("Blank line   " + headerIndex);
            } else{
                String firstWord = words[0].toLowerCase();
                if (firstWord.startsWith("%")) {
                    headerIndex = headerIndex + 1;
                    // System.out.println("Comment line   " + headerIndex);
                } else if (firstWord.equals("@relation")) {
                    headerIndex = headerIndex + 1;
                    String secondWord = words[1];
                    if (secondWord.startsWith("\"")) {
                        secondWord = secondWord.substring(1);
                        if( secondWord.endsWith("\"")) {
                            secondWord = secondWord.substring(0,secondWord.length()-1);
                        }else {
                            int i = 2;
                            while (i < words.length) {
                                if (!words[i].endsWith("\"")) {
                                    secondWord = secondWord.concat(" ").concat(words[i]);
                                    i = i + 1;
                                } else {
                                    secondWord = secondWord.concat(" ").concat(words[i]);
                                    secondWord = secondWord.substring(0, secondWord.length() - 1);
                                    i = words.length;
                                }
                            }
                        }
                    }
                    relation = secondWord;
                    //System.out.println("Relation:  " + secondWord);
                } else if (firstWord.equals("@attribute")) {
                    headerIndex = headerIndex + 1;
                    String secondWord = words[1];
                    String thirdWord = words[2]; //default
                    if (secondWord.startsWith("\"")) {
                        secondWord = secondWord.substring(1);
                        if( secondWord.endsWith("\"")) {
                            secondWord = secondWord.substring(0,secondWord.length()-1);
                        }else {
                            int i = 2;
                            while (i < words.length) {
                                if (!words[i].endsWith("\"")) {
                                    secondWord = secondWord.concat(" ").concat(words[i]);
                                    i = i + 1;
                                } else {
                                    secondWord = secondWord.concat(" ").concat(words[i]);
                                    secondWord = secondWord.substring(0, secondWord.length() - 1);
                                    thirdWord = words[i+1];
                                    i = words.length;
                                }
                            }
                        }
                    }
                    Attribute a = new Attribute(secondWord,
                            StateSpaceType.parseKind(thirdWord));
                    attributesInHeader.add(a);
                    //System.out.println("Attribute:  " + secondWord + "    Type:  " + thirdWord);
                } else if (firstWord.equals("@data")) {
                    headerIndex = headerIndex + 1;
                    isHeader = false;
                    //System.out.println("Data line   " + headerIndex);
                } else {
                    throw new ArffParserException("Illegal header element: " + "'" + line + "'");
                }
            }
        }

        Keys keys = getHeaderKeys(attributesInHeader, attributes.getSet());


        if( lines.size() - headerIndex == 0){
            throw new ArffParserException("The data section is empty.");
        }

        if(attributesInHeader.size() != lines.get(headerIndex).length){
            throw new ArffParserException("The number of attributes in the header is not equal to " +
                    "the number of elements in the data section.");
        }

        rowSize =  lines.size() - headerIndex;
        doubleColumnSize = keys.getDoubleKeys().length;
        doubleData = new double[rowSize][doubleColumnSize];

        intColumnSize = keys.getIntKeys().length;
        intData = new int[rowSize][intColumnSize];

        for (int j = 0; j < rowSize; j++) {
            String[] row = lines.get(j + headerIndex);
            for (int i = 0; i < doubleColumnSize; i++) {
                int key = keys.getDoubleKeys()[i];
                doubleData[j][i] = Double.parseDouble(row[key]);
                //System.out.print(", " + data[j][i]);
            }

            for (int i = 0; i < intColumnSize; i++) {
                int key = keys.getIntKeys()[i];
                //System.out.print("key:  " + key);
                intData[j][i] = Integer.parseInt(row[key]);
                //System.out.print(", " + data[j][i]);
            }
            //    System.out.println();
        }
        reader.close();
    }

    public static String lineArrayToLine(String[] lineAsArray) {
        String line = "";
        if (lineAsArray.length == 0) {
            return null;
        }
        for (int i = 0; i < lineAsArray.length; ++i) {
            if( i > 0 ) { line = line + ","; }
            line = line + lineAsArray[i];
        }
        return line;
    }


    private String[] trimSpacesInBeginning(String[] s) {
        if(s.length > 0 && s[0].isEmpty()) {
            String[] sTmp = new String[s.length-1];
            for (int i = 1; i < s.length; i++) {
                sTmp[i-1] = s[i];
            }
            s = sTmp;
        }
        return s;
    }

    private static Keys getHeaderKeys(List<Attribute> header,
                                       Set<Attribute> acceptable) throws ArffParserException {
        int doubleCount = 0;
        int intCount = 0;

        for(Attribute a : acceptable){
            switch (a.getStateSpaceType()) {
                case REAL:
                    doubleCount = doubleCount + 1;
                    break;
                case INTEGER:
                    intCount = intCount + 1;
                    break;
                default:
                    throw new ArffParserException("The header contains attributes with unsupported kind.");
            }
        }



        int[] doubleKeys = new int[doubleCount];
        for (int i = 0; i < doubleCount; i++) {
            doubleKeys[i] = -1;
        }

        int[] intKeys = new int[intCount];
        for (int i = 0; i < intCount; i++) {
            intKeys[i] = -1;
        }

        for (int headerIndex = 0; headerIndex < header.size(); headerIndex++) {
            for(Attribute a : acceptable){
                if(a.equals(header.get(headerIndex))){
                    switch (a.getStateSpaceType()){
                        case REAL:
                            if(doubleKeys[a.getIndex()] == -1) {
                                doubleKeys[a.getIndex()] = headerIndex;
                            } else{
                                throw new ArffParserException("The header contains attributes with the same names.");
                            }
                            break;
                        case INTEGER:
                            if(intKeys[a.getIndex()] == -1) {
                                intKeys[a.getIndex()] = headerIndex;
                            } else{
                                throw new ArffParserException("The header contains attributes with the same names.");
                            }
                            break;
                        default:
                            throw new ArffParserException("The header contains attributes with unsupported kind.");
                    }
                }
            }
        }
//        for (int i = 0; i < keys.length; i++) {
//            System.out.println("index " + i + " = " + keys[i]);
//        }

        for (int i = 0; i < doubleKeys.length; i++) {
            if(doubleKeys[i] == -1) {
                throw new ArffParserException( "The header does not contain all attributes of the real kind that " +
                        "are specified in the attributes class.");
            }
        }

        for (int i = 0; i < intKeys.length; i++) {
            if(intKeys[i] == -1) {
                throw new ArffParserException( "The header does not contain all attributes of the integer kind that " +
                        "are specified in the attributes class.");
            }
        }


        return new Keys(doubleKeys,intKeys);
    }


    @Override
    public DataInstance nextDataInstance() {
        DataInstance x = new DefaultDataInstance(dataInstance,doubleData,intData);
        dataInstance = dataInstance + 1;
        return x;
    }

    @Override
    public boolean hasMoreDataInstances() {
        return dataInstance != rowSize;
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {
        dataInstance = 0;
    }

    @Override
    public StaticDataHeader getStaticDataHeader() {
        //TODO rethink this?
        return staticDataHeader;
    }

}
