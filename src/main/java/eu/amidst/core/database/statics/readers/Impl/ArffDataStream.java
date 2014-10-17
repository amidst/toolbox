package eu.amidst.core.database.statics.readers.Impl;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.statics.DataInstance;
import eu.amidst.core.database.statics.DataStream;
import eu.amidst.core.database.statics.readers.ArffParserException;
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
    private Set<Attributes.Attribute> attributes;
    private int[] attributesActivation;
    private int rowSize;
    private int columnSize;
    private double[][] data;
    private String relation;
    private List<Attributes.Attribute> attributesInHeader = new ArrayList<>();

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
                    System.out.println("Relation:  " + secondWord);
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
                    Attributes.Attribute a = new Attributes.Attribute(secondWord,
                            Attributes.Kind.parseKind(thirdWord));
                    attributesInHeader.add(a);
                    System.out.println("Attribute:  " + secondWord + "    Type:  " + thirdWord);
                } else if (firstWord.equals("@data")) {
                    headerIndex = headerIndex + 1;
                    isHeader = false;
                    //System.out.println("Data line   " + headerIndex);
                } else {
                    throw new ArffParserException("Illegal header element: " + "'" + line + "'");
                }
            }
        }

        int[] keys = getHeaderKeys(attributesInHeader, attributes.getSet());


        if( lines.size() - headerIndex == 0){
            throw new ArffParserException("The data section is empty.");
        }

        if(attributesInHeader.size() != lines.get(headerIndex).length){
            throw new ArffParserException("The number of attributes in the header is not equal to " +
                    "the number of elements in the data section.");
        }

        rowSize =  lines.size() - headerIndex;
        columnSize = keys.length;
        data = new double[rowSize][columnSize];

        for (int j = 0; j < rowSize; j++) {
            for (int i = 0; i < columnSize; i++) {
                int key = keys[i];
                String[] row = lines.get(j + headerIndex);
                data[j][i] = Double.parseDouble(row[key]);
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

    private static int[] getHeaderKeys(List<Attributes.Attribute> header,
                                       Set<Attributes.Attribute> acceptable) throws ArffParserException {
        int[] keys = new int[acceptable.size()];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = -1;
        }

        for (int headerIndex = 0; headerIndex < header.size(); headerIndex++) {
            for(Attributes.Attribute a : acceptable){
                if(a.equals(header.get(headerIndex))){
                    if(keys[a.getIndex()] == -1) {
                        keys[a.getIndex()] = headerIndex;
                    } else{
                        throw new ArffParserException("The header contains attributes with the same names.");
                    }
                }
            }
        }
//        for (int i = 0; i < keys.length; i++) {
//            System.out.println("index " + i + " = " + keys[i]);
//        }

        for (int i = 0; i < keys.length; i++) {
            if(keys[i] == -1) {
                throw new ArffParserException( "The header does not contain all attributes that " +
                        "are specified in the attributes class.");
            }
        }

        return keys;
    }


    @Override
    public DataInstance nextDataInstance() {
        return null;
    }

    @Override
    public boolean hasMoreDataInstances() {
        return false;
    }

    @Override
    public boolean isRestartable() {
        return false;
    }

    @Override
    public void restart() {
        iterator.remove();
        //TODO reload iterator
    }

    @Override
    public StaticDataHeader getStaticDataHeader() {
        return staticDataHeader;
    }

}
