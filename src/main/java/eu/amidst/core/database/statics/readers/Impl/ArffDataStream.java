package eu.amidst.core.database.statics.readers.Impl;

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
    private int rowSize;
    private int columnSize;
    private double[][] data;
    private String relation;

    public ArffDataStream(String fileName) throws FileNotFoundException,
            IOException, ArffParserException {
        CSVReader reader = new CSVReader(new FileReader(fileName));
        List<String []> lines = reader.readAll();
        int headerIndex = 0;
        boolean isHeader = true;
        while (isHeader) {
            String[] lineAsArray = lines.get(headerIndex);
            String line = lineArrayToLine( lineAsArray );

            //TODO Replace this with String.split()
            String[] result = "this  is a test".split("\\s+");
            for (int x=0; x<result.length; x++) {
                System.out.println(result[x]);
            }

            StringTokenizer st = new StringTokenizer( line );
            if( !st.hasMoreTokens() ) {
                headerIndex = headerIndex + 1;
                System.out.println("Blank line   " + headerIndex);
            }
            else {
                String firstWord = st.nextToken().toLowerCase();
                if (firstWord.startsWith("%")) {
                    headerIndex = headerIndex + 1;
                    System.out.println("Comment line   " + headerIndex);
                } else if (firstWord.equals("@relation")) {
                    headerIndex = headerIndex + 1;
                    System.out.println("Relation line   " + headerIndex);
                    System.out.println( "    - relation stuff:");
                    while( st.hasMoreTokens() ) {
                        System.out.println( "                 " + st.nextToken() );
                    }
                } else if (firstWord.equals("@attribute")) {
                    headerIndex = headerIndex + 1;
                    System.out.println("Attribute line   " + headerIndex);
                    System.out.println( "    - attribute stuff:");
                    while( st.hasMoreTokens() ) {
                        System.out.println( "                 " + st.nextToken() );
                    }
                } else if (firstWord.equals("@data")) {
                    headerIndex = headerIndex + 1;
                    System.out.println("Data line   " + headerIndex);
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
