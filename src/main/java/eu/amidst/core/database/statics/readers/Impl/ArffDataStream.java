package eu.amidst.core.database.statics.readers.Impl;

import eu.amidst.core.database.statics.DataInstance;
import eu.amidst.core.database.statics.DataStream;
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
            IOException {
        CSVReader reader = new CSVReader(new FileReader(fileName));
        char percent = '%';
        char at = '@';

        List<String []> rowList = reader.readAll();

        int headerIndex = 0;
        boolean isHeader = true;
        while (isHeader) {
            String[] row = rowList.get(headerIndex);
            if (row[0].isEmpty()) {
                headerIndex = headerIndex+1;
                System.out.println("Blank line   " + headerIndex);
            } else if (new Character(row[0].charAt(0)).equals(percent)) {
                headerIndex = headerIndex+1;
                System.out.println("Comment line   " + headerIndex);
            } else if (new Character(row[0].charAt(0)).equals(at) &&
                    new Character(row[0].charAt(1)).equals('r')&&
                    new Character(row[0].charAt(2)).equals('e')&&
                    new Character(row[0].charAt(3)).equals('l')&&
                    new Character(row[0].charAt(4)).equals('a')&&
                    new Character(row[0].charAt(5)).equals('t')&&
                    new Character(row[0].charAt(6)).equals('i')&&
                    new Character(row[0].charAt(7)).equals('o')&&
                    new Character(row[0].charAt(8)).equals('n')
                    ) {
                headerIndex = headerIndex+1;
                System.out.println("Relation line   " + headerIndex);
            } else if (new Character(row[0].charAt(0)).equals(at) &&
                    new Character(row[0].charAt(1)).equals('a')&&
                    new Character(row[0].charAt(2)).equals('t')&&
                    new Character(row[0].charAt(3)).equals('t')&&
                    new Character(row[0].charAt(4)).equals('r')&&
                    new Character(row[0].charAt(5)).equals('i')&&
                    new Character(row[0].charAt(6)).equals('b')&&
                    new Character(row[0].charAt(7)).equals('u')&&
                    new Character(row[0].charAt(8)).equals('t')&&
                    new Character(row[0].charAt(9)).equals('e')
                    ) {
                headerIndex = headerIndex+1;
                System.out.println("Attribute line   " + headerIndex);
            } else if (new Character(row[0].charAt(0)).equals(at) &&
                    new Character(row[0].charAt(1)).equals('D')&&
                    new Character(row[0].charAt(2)).equals('A')&&
                    new Character(row[0].charAt(3)).equals('T')&&
                    new Character(row[0].charAt(4)).equals('A')
                    ) {
                headerIndex = headerIndex+1;
                System.out.println("Data line   " + headerIndex);
            } else {
                isHeader = false;
            }
        }

        rowSize =  rowList.size() - headerIndex;
        columnSize = rowList.get(headerIndex).length;
        data = new double[rowSize][columnSize];

        for (int j = 0; j < rowSize; j++) {
            for (int i = 0; i < columnSize; i++) {
                String[] row = rowList.get(j + headerIndex);
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
