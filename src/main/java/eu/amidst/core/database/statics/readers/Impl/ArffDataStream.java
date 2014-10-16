package eu.amidst.core.database.statics.readers.Impl;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.statics.DataInstance;
import eu.amidst.core.database.statics.DataStream;
import eu.amidst.core.database.statics.readers.ArffParserException;
import eu.amidst.core.header.statics.StaticDataHeader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import au.com.bytecode.opencsv.CSVReader;


/**
 * Created by sigveh on 10/7/14.
 */
public class ArffDataStream implements DataStream{
    private Iterator iterator;
    private StaticDataHeader staticDataHeader;
    private String fileName;
    private Attributes attributes;
    private int rowSize;
    private int columnSize;
    private double[][] data;
    private String relation;

    public ArffDataStream(String fileName, Attributes attributes) throws FileNotFoundException,
            IOException, ArffParserException {
        this.fileName = fileName;
        this.attributes = attributes;
        CSVReader reader = new CSVReader(new FileReader(fileName));
        List<String []> lines = reader.readAll();
        int headerIndex = 0;
        boolean isHeader = true;
        while (isHeader) {
            String[] lineAsArray = lines.get(headerIndex);
            String line = lineArrayToLine( lineAsArray );

            //TODO Replace this with String.split()
            String[] words = line.split("\\s+");

//            int wordCounter2 = 1;
//            while( wordCounter2 < words.length) {
//                System.out.println( "                 " + words[wordCounter2] );
//                wordCounter2 = wordCounter2 + 1;
//            }

            // StringTokenizer st = new StringTokenizer( line );
            if(words.length == 0 || words[0].isEmpty()) {
                headerIndex = headerIndex + 1;
                //System.out.println("Blank line   " + headerIndex);
            }
            else {
                String firstWord = words[0].toLowerCase();
                if (firstWord.startsWith("%")) {
                    headerIndex = headerIndex + 1;
                    //System.out.println("Comment line   " + headerIndex);
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

                    System.out.println("Attribute:  " + secondWord + "    Type:  " + thirdWord);
                } else if (firstWord.equals("@data")) {
                    headerIndex = headerIndex + 1;
                    //System.out.println("Data line   " + headerIndex);
                } else if (firstWord.startsWith("@")) {
                    throw new ArffParserException("Illegal header element: " + "'" + line + "'");
                } else {
                    isHeader = false;
                }
            }
        }

        rowSize =  lines.size() - headerIndex;
        columnSize = lines.get(headerIndex).length;
        data = new double[rowSize][columnSize];

        for (int j = 0; j < rowSize; j++) {
            for (int i = 0; i < columnSize; i++) {
                String[] row = lines.get(j + headerIndex);
                data[j][i] = Double.parseDouble(row[i]);
                //System.out.print(", " + data[j][i]);
            }
            System.out.println();
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
