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
    private int rows;
    private int cols;
    private double[][] data;

    public ArffDataStream(String fileName) throws FileNotFoundException,
                IOException {
            CSVReader reader = new CSVReader(new FileReader(fileName));

            List<String []> xrows = reader.readAll();
            rows =  xrows.size();
            cols = xrows.get(0).length;

            data = new double[rows][cols];
            int j = 0;
            for (String[] row : xrows) {
                for (int i = 0; i < row.length; i++) {
                    data[j][i] = Double.parseDouble(row[i]);
                }
                j = j+1;
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
