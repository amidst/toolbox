package eu.amidst.core.database.filereaders.arffWekaReader;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.filereaders.DataFileReader;
import eu.amidst.core.database.filereaders.DataRow;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.*;
import weka.core.converters.ArffLoader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by ana@cs.aau.dk on 14/11/14.
 */
public class WekaDataFileReader implements DataFileReader{

    ArffLoader.ArffReader arff = null;
    Instances dataStructure;

    public WekaDataFileReader(String s){

        try {
            BufferedReader reader = new BufferedReader(new FileReader(s));
            arff = new ArffLoader.ArffReader(reader);
        }catch(IOException e){};

        dataStructure = arff.getStructure();

    }

    @Override
    public Attributes getAttributes() {
        return null;
    }

    @Override
    public DataRow nextDataRow() {
        Instance inst = null;
        try {
            inst = arff.readInstance(dataStructure);
        }catch(IOException e){};
        return new DataRowWeka(inst);
    }

    @Override
    public boolean hasMoreDataRows() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public boolean doesItReadThisFileExtension(String fileExtension) {
        return false;
    }
}
