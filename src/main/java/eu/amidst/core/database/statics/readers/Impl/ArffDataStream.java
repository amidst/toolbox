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

    public ArffDataStream(String fileName) throws FileNotFoundException,
            IOException, ArffParserException {
        CSVReader reader = new CSVReader(new FileReader(fileName));
        List<String []> lines = reader.readAll();

        int headerIndex = 0;
        boolean isHeader = true;
        while (isHeader) {
            String[] line = lines.get(headerIndex);
            String firstWord = line[0].trim().toLowerCase();
            if ( firstWord.isEmpty()) {
                headerIndex = headerIndex+1;
                System.out.println("Blank line   " + headerIndex);
            } else if ( firstWord.startsWith("%") ) {
                headerIndex = headerIndex + 1;
                System.out.println("Comment line   " + headerIndex);
            } else if ( firstWord.startsWith("@relation ") ) {
                headerIndex = headerIndex+1;
                System.out.println("Relation line   " + headerIndex);
            } else if ( firstWord.startsWith("@attribute ") ) {
                headerIndex = headerIndex+1;
                System.out.println("Attribute line   " + headerIndex);
            } else if ( firstWord.equals("@data") ) {
                headerIndex = headerIndex+1;
                System.out.println("Data line   " + headerIndex);
            } else if ( firstWord.startsWith("@") ) {
                throw new ArffParserException( "Illegal header element: " + "'" + line[0].trim() + "'" ) ;
            } else {
                isHeader = false;
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
