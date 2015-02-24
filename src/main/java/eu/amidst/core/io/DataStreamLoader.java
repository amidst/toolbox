package eu.amidst.core.io;

import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.DataFileReader;
import eu.amidst.core.datastream.filereaders.DataStreamFromFile;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;

/**
 * Created by andresmasegosa on 23/02/15.
 */
public final class DataStreamLoader {
    private static DataFileReader dataFileReader = new ARFFDataReader();


    public static void setDataFileReader(DataFileReader dataFileReader) {
        dataFileReader = dataFileReader;
    }

    public static DataStream<DataInstance> loadFromFile(String path){
        dataFileReader.loadFromFile(path);
        return new DataStreamFromFile(dataFileReader);
    }

}
